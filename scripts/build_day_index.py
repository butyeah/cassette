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
  - release_group_meta         first_release_date_year/month/day
  - artist_credit               pre-rendered `name` (e.g. "A & B") — so the
                                 artist_credit_name/artist join tables aren't
                                 needed just to show a display name
  - release_group_primary_type  id -> 'Album'/'Single'/'EP'/... name

Usage:
    python3 scripts/build_day_index.py [--dump-url URL] [--raw-dir DIR] [--output PATH]

Safe to re-run: the download step is skipped if the raw table files already
exist in --raw-dir. Downloads and extracts ~7GB (compressed) of MusicBrainz
data on first run — that's the one genuinely slow/expensive step here.
"""
from __future__ import annotations

import argparse
import sqlite3
import tarfile
import urllib.request
from pathlib import Path
from typing import BinaryIO, Iterator

DEFAULT_DUMP_URL = (
    "https://data.metabrainz.org/pub/musicbrainz/data/fullexport/LATEST/mbdump.tar.bz2"
)
# Members inside mbdump.tar.bz2 we actually need — everything else is skipped
# without ever being written to disk.
WANTED_TABLES = (
    "release_group",
    "release_group_meta",
    "artist_credit",
    "release_group_primary_type",
)
# Resolved at runtime from release_group_primary_type itself (not hardcoded
# as a magic id) in case MusicBrainz ever renumbers it.
ALBUM_TYPE_NAME = "Album"


def download_tables(dump_url: str, raw_dir: Path) -> None:
    """Streams mbdump.tar.bz2 and extracts only WANTED_TABLES into raw_dir,
    without ever writing the full ~7GB archive (or its much larger
    uncompressed contents) to disk."""
    raw_dir.mkdir(parents=True, exist_ok=True)
    missing = {t for t in WANTED_TABLES if not (raw_dir / t).exists()}
    if not missing:
        print(f"All tables already present in {raw_dir}, skipping download.")
        return

    print(f"Downloading and extracting {sorted(missing)} from {dump_url} ...")
    with urllib.request.urlopen(dump_url) as response:
        with tarfile.open(fileobj=response, mode="r|bz2") as tar:
            for member in tar:
                name = Path(member.name).name
                if name not in missing:
                    continue
                extracted = tar.extractfile(member)
                if extracted is None:
                    continue
                dest = raw_dir / name
                with open(dest, "wb") as out:
                    _copy_stream(extracted, out)
                print(f"  extracted {name} ({dest.stat().st_size:,} bytes)")
                missing.discard(name)
                if not missing:
                    break  # stop reading the stream early once we have everything
    if missing:
        raise RuntimeError(f"Never found these tables in the dump: {sorted(missing)}")


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
    parser.add_argument("--dump-url", default=DEFAULT_DUMP_URL)
    parser.add_argument("--raw-dir", type=Path, default=Path("scripts/.mbdump-raw"))
    parser.add_argument("--output", type=Path, default=Path("scripts/day_index.sqlite"))
    args = parser.parse_args()

    download_tables(args.dump_url, args.raw_dir)
    build_index(args.raw_dir, args.output)


if __name__ == "__main__":
    main()
