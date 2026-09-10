#!/usr/bin/env python3
"""
Builds a local SQLite "day index" of albums (title, artist name, release
year/month/day) from MusicBrainz's public data dump, for the "on this day in
history" feature (see the app's Firestore-backed GetAlbumsByDayUseCase).

MusicBrainz's search API can't answer "every album released on this day,
across all years" — no leading wildcards on `date`, and one query per year
would be far too slow given their rate limit — so this script does the
equivalent join offline, once, against MusicBrainz's bulk data dump:
https://musicbrainz.org/doc/MusicBrainz_Database/Download

Only 4 of MusicBrainz's ~30 core tables are needed (column layouts confirmed
against admin/sql/CreateTables.sql in musicbrainz-server as of 2026-09):
  - release_group              title, artist_credit id, primary type id
  - release_group_meta         first_release_date_year/month/day — a
                                *computed/derived* table, so it actually ships
                                in mbdump-derived.tar.bz2, not mbdump.tar.bz2
                                (confirmed by scanning both archives directly)
  - artist_credit               pre-rendered `name` (e.g. "A & B") — so the
                                 artist_credit_name/artist join tables aren't
                                 needed just to show a display name
  - release_group_primary_type  id -> 'Album'/'Single'/'EP'/... name

Usage:
    python3 scripts/build_day_index.py [--export-base-url URL] [--raw-dir DIR] [--output PATH]

Safe to re-run: the download step is skipped if the raw table files already
exist in --raw-dir. Downloads mbdump-derived.tar.bz2 (~500MB compressed) and
mbdump.tar.bz2 (~7GB compressed) on first run — that's the one genuinely
slow/expensive step here.
"""
from __future__ import annotations

import argparse
import sqlite3
import ssl
import tarfile
import urllib.request
from pathlib import Path
from typing import BinaryIO, Iterator

# The dump directories are dated (e.g. .../fullexport/20260909-002431/), so
# there's no fixed URL for "the current dump" — LATEST is a small text file
# at this base URL containing that dated directory's name, resolved below.
DEFAULT_EXPORT_BASE_URL = "https://data.metabrainz.org/pub/musicbrainz/data/fullexport"
# Which dump archive each wanted table actually ships in — everything else in
# each archive is skipped without ever being written to disk. Verified by
# scanning both archives directly rather than assumed, since MusicBrainz
# splits "core" vs. "derived/computed" tables across them and that split
# isn't documented anywhere obvious.
TABLE_ARCHIVES: dict[str, str] = {
    "release_group": "mbdump.tar.bz2",
    "artist_credit": "mbdump.tar.bz2",
    "release_group_primary_type": "mbdump.tar.bz2",
    "release_group_meta": "mbdump-derived.tar.bz2",
}
# Resolved at runtime from release_group_primary_type itself (not hardcoded
# as a magic id) in case MusicBrainz ever renumbers it.
ALBUM_TYPE_NAME = "Album"


def make_ssl_context() -> ssl.SSLContext:
    """A default context wired to a real CA bundle — some Python installs
    (notably Homebrew's on macOS) don't trust the system store out of the
    box, which otherwise fails every HTTPS request with
    CERTIFICATE_VERIFY_FAILED."""
    try:
        import certifi

        return ssl.create_default_context(cafile=certifi.where())
    except ImportError:
        pass
    for candidate in ("/etc/ssl/cert.pem", "/etc/ssl/certs/ca-certificates.crt"):
        if Path(candidate).exists():
            return ssl.create_default_context(cafile=candidate)
    return ssl.create_default_context()


def resolve_dump_base_url(export_base_url: str, ssl_context: ssl.SSLContext) -> str:
    """LATEST is a plain-text file (not a directory alias) containing the
    current dated export directory's name, e.g. `20260909-002431`."""
    with urllib.request.urlopen(f"{export_base_url}/LATEST", context=ssl_context) as resp:
        latest_dir = resp.read().decode("ascii").strip()
    return f"{export_base_url}/{latest_dir}"


def download_tables(dump_base_url: str, raw_dir: Path, ssl_context: ssl.SSLContext) -> None:
    """Streams each dump archive that has at least one still-missing wanted
    table, extracting only those tables into raw_dir — without ever writing
    a full archive (or its much larger uncompressed contents) to disk."""
    raw_dir.mkdir(parents=True, exist_ok=True)
    missing = {t for t in TABLE_ARCHIVES if not (raw_dir / t).exists()}
    if not missing:
        print(f"All tables already present in {raw_dir}, skipping download.")
        return

    archives_needed = {TABLE_ARCHIVES[t] for t in missing}
    for archive in sorted(archives_needed):
        wanted_from_archive = {t for t in missing if TABLE_ARCHIVES[t] == archive}
        archive_url = f"{dump_base_url}/{archive}"
        print(f"Downloading and extracting {sorted(wanted_from_archive)} from {archive_url} ...")
        with urllib.request.urlopen(archive_url, context=ssl_context) as response:
            with tarfile.open(fileobj=response, mode="r|bz2") as tar:
                for member in tar:
                    name = Path(member.name).name
                    if name not in wanted_from_archive:
                        continue
                    extracted = tar.extractfile(member)
                    if extracted is None:
                        continue
                    dest = raw_dir / name
                    with open(dest, "wb") as out:
                        _copy_stream(extracted, out)
                    print(f"  extracted {name} ({dest.stat().st_size:,} bytes)")
                    wanted_from_archive.discard(name)
                    missing.discard(name)
                    if not wanted_from_archive:
                        break  # stop reading this archive early
        if wanted_from_archive:
            raise RuntimeError(f"Never found these tables in {archive}: {sorted(wanted_from_archive)}")
    if missing:
        raise RuntimeError(f"Never found these tables in any archive: {sorted(missing)}")


def _copy_stream(src: BinaryIO, dst: BinaryIO, chunk_size: int = 1024 * 1024) -> None:
    while True:
        chunk = src.read(chunk_size)
        if not chunk:
            break
        dst.write(chunk)


_ESCAPES = {"\\": "\\", "t": "\t", "n": "\n", "r": "\r"}


def unescape_copy_value(raw: str) -> str | None:
    """Un-escapes one field of Postgres COPY TEXT format (tab-delimited,
    backslash escapes, `\\N` for NULL)."""
    if raw == "\\N":
        return None
    if "\\" not in raw:
        return raw
    out: list[str] = []
    i = 0
    while i < len(raw):
        c = raw[i]
        if c == "\\" and i + 1 < len(raw):
            out.append(_ESCAPES.get(raw[i + 1], raw[i + 1]))
            i += 2
        else:
            out.append(c)
            i += 1
    return "".join(out)


def read_copy_file(path: Path) -> Iterator[list[str | None]]:
    """Yields one row (list of unescaped column values, None for NULL) per
    line of a raw mbdump COPY-format table file."""
    with open(path, "r", encoding="utf-8", newline="\n") as f:
        for line in f:
            line = line.rstrip("\n")
            if not line:
                continue
            yield [unescape_copy_value(v) for v in line.split("\t")]


def build_index(raw_dir: Path, output: Path) -> None:
    print("Loading artist_credit ...")
    artist_credit_name: dict[str, str] = {}
    for row in read_copy_file(raw_dir / "artist_credit"):
        credit_id, name = row[0], row[1]
        assert credit_id is not None and name is not None, f"malformed artist_credit row: {row}"
        artist_credit_name[credit_id] = name
    print(f"  {len(artist_credit_name):,} artist credits")

    print("Loading release_group_primary_type ...")
    album_type_id: str | None = None
    for row in read_copy_file(raw_dir / "release_group_primary_type"):
        type_id, name = row[0], row[1]
        if name == ALBUM_TYPE_NAME:
            album_type_id = type_id
    if album_type_id is None:
        raise RuntimeError(
            f"No release_group_primary_type row named {ALBUM_TYPE_NAME!r} — "
            "MusicBrainz's schema may have changed; check this table by hand."
        )
    print(f"  {ALBUM_TYPE_NAME!r} == type id {album_type_id}")

    print("Loading release_group ...")
    # id -> (gid, title, artist_credit_id, type_id)
    release_groups: dict[str, tuple[str, str, str, str | None]] = {}
    for row in read_copy_file(raw_dir / "release_group"):
        rg_id, gid, title, artist_credit_id, type_id = row[0], row[1], row[2], row[3], row[4]
        assert rg_id and gid and title and artist_credit_id, f"malformed release_group row: {row}"
        release_groups[rg_id] = (gid, title, artist_credit_id, type_id)
    print(f"  {len(release_groups):,} release groups")

    output.parent.mkdir(parents=True, exist_ok=True)
    output.unlink(missing_ok=True)
    conn = sqlite3.connect(output)
    conn.execute(
        """
        CREATE TABLE album (
            id TEXT PRIMARY KEY,
            title TEXT NOT NULL,
            artist_name TEXT NOT NULL,
            year INTEGER NOT NULL,
            month INTEGER NOT NULL,
            day INTEGER NOT NULL
        )
        """
    )

    print("Joining release_group_meta and writing the index ...")
    considered = 0
    matched = 0
    batch: list[tuple[str, str, str, int, int, int]] = []

    def flush() -> None:
        if batch:
            conn.executemany("INSERT OR IGNORE INTO album VALUES (?, ?, ?, ?, ?, ?)", batch)
            batch.clear()

    for row in read_copy_file(raw_dir / "release_group_meta"):
        considered += 1
        rg_id, _release_count, year, month, day = row[0], row[1], row[2], row[3], row[4]
        if year is None or month is None or day is None:
            continue  # partial date — can't place it on a calendar day
        rg = release_groups.get(rg_id)
        if rg is None:
            continue
        gid, title, artist_credit_id, type_id = rg
        if type_id != album_type_id:
            continue
        artist_name = artist_credit_name.get(artist_credit_id)
        if artist_name is None:
            continue
        batch.append((gid, title, artist_name, int(year), int(month), int(day)))
        matched += 1
        if len(batch) >= 5000:
            flush()
    flush()

    conn.execute("CREATE INDEX idx_album_month_day ON album (month, day)")
    conn.commit()
    conn.close()
    print(f"Considered {considered:,} release groups with a full date, kept {matched:,} albums.")
    print(f"Wrote {output}")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--export-base-url", default=DEFAULT_EXPORT_BASE_URL)
    parser.add_argument(
        "--dump-base-url",
        default=None,
        help="Skip LATEST resolution and use this dated export directory URL directly "
        "(e.g. https://.../fullexport/20260909-002431).",
    )
    parser.add_argument("--raw-dir", type=Path, default=Path("scripts/.mbdump-raw"))
    parser.add_argument("--output", type=Path, default=Path("scripts/day_index.sqlite"))
    args = parser.parse_args()

    ssl_context = make_ssl_context()
    dump_base_url = args.dump_base_url or resolve_dump_base_url(args.export_base_url, ssl_context)
    if not args.dump_base_url:
        print(f"Resolved LATEST -> {dump_base_url}")

    download_tables(dump_base_url, args.raw_dir, ssl_context)
    build_index(args.raw_dir, args.output)


if __name__ == "__main__":
    main()
