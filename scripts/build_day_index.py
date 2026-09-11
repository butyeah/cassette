#!/usr/bin/env python3
"""
Builds a local SQLite "day index" of albums (title, artist name, release
year/month/day, tracklist, streaming-service links) from MusicBrainz's public
data dump, for the "on this day in history" feature and AlbumDetailScreen's
tracklist/streaming-link display (see the app's Firestore-backed
GetAlbumsByDayUseCase and AlbumTracksRepository).

MusicBrainz's search API can't answer "every album released on this day,
across all years" — no leading wildcards on `date`, and one query per year
would be far too slow given their rate limit — so this script does the
equivalent join offline, once, against MusicBrainz's bulk data dump:
https://musicbrainz.org/doc/MusicBrainz_Database/Download
Tracks and streaming links are joined the same way, for the same reason:
avoiding a live, rate-limited MusicBrainz API call per album on every
AlbumDetailScreen open (see AlbumTracksRepositoryImpl).

Tables needed (column layouts confirmed against admin/sql/CreateTables.sql in
musicbrainz-server as of 2026-09):
  - release_group              title, artist_credit id, primary type id
  - release_group_meta         first_release_date_year/month/day — a
                                *computed/derived* table, so it actually ships
                                in mbdump-derived.tar.bz2, not mbdump.tar.bz2
                                (confirmed by scanning both archives directly)
  - artist_credit               pre-rendered `name` (e.g. "A & B") — so the
                                 artist_credit_name/artist join tables aren't
                                 needed just to show a display name
  - release_group_primary_type  id -> 'Album'/'Single'/'EP'/... name
  - release_status              id -> 'Official'/'Promotion'/... name, used
                                 to prefer an official release's tracklist
  - release                     one row per release; picks the release used
                                 for a release group's tracklist
  - medium                      discs within a release
  - track                       tracks within a medium — position, name,
                                 length
  - url                         a URL entity (id -> the URL string itself)
  - l_release_group_url         release-group <-> url relationships
  - link                        one row per relationship instance -> its type
  - link_type                   relationship type catalog (id -> name,
                                 entity types), used to find "streaming"/
                                 "free streaming" release-group-url links and
                                 classify them (Spotify/Apple Music/YouTube
                                 Music) by the URL's host

Title/artist/release-date and tracks/streaming-links together cover most of
what `MusicBrainzRepository.getAlbumDetail`'s live `getReleaseGroup` call
returns. Two fields are deliberately NOT in this offline cache:
  - rating — no offline snapshot; it changes too often to freeze into a
    dump, so a cache-served album just shows no rating.
  - genres — would need MusicBrainz's `tag`/`release_group_tag` tables to
    associate a genre name with a specific release group, but neither table
    exists anywhere in MusicBrainz's public bulk export (confirmed by
    listing every member of every archive in a dump directory directly —
    not a download flake, not fixable by retrying). build_genre_index()
    writes an always-empty `genre` table documenting exactly this.

Usage:
    python3 scripts/build_day_index.py [--export-base-url URL] [--raw-dir DIR] [--output PATH]

Safe to re-run: the download step is skipped if the raw table files already
exist in --raw-dir. Downloads mbdump-derived.tar.bz2 (~500MB compressed) and
mbdump.tar.bz2 (~7GB compressed, now streamed further into it for the
release/medium/track/url/link tables above) on first run — that's the one
genuinely slow/expensive step here.
"""
from __future__ import annotations

import argparse
import sqlite3
import ssl
import tarfile
import urllib.parse
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
    "release_status": "mbdump.tar.bz2",
    "release": "mbdump.tar.bz2",
    "medium": "mbdump.tar.bz2",
    "track": "mbdump.tar.bz2",
    "url": "mbdump.tar.bz2",
    "l_release_group_url": "mbdump.tar.bz2",
    "link": "mbdump.tar.bz2",
    "link_type": "mbdump.tar.bz2",
}
# `genre` (the curated genre-name catalog) ships in mbdump.tar.bz2 — confirmed by listing every
# member of the 20260909-002431 dump directly — but `tag` and `release_group_tag`, the tables
# that would actually *associate* a genre with a release group, do not exist anywhere in
# MusicBrainz's public bulk export (checked the full mbdump.tar.bz2 listing and every other
# archive in the same dump directory — neither table appears at all, not a download flake).
# `genre` alone can't produce a release-group -> genre mapping without them, so it isn't worth
# downloading either: genres just aren't obtainable from the offline dump, full stop. This is a
# structural limitation of MusicBrainz's export, not something a retry or a different table name
# would fix — see build_genre_index().
# Resolved at runtime from release_group_primary_type/release_status/link_type
# themselves (not hardcoded as magic ids) in case MusicBrainz ever renumbers
# them.
ALBUM_TYPE_NAME = "Album"
OFFICIAL_STATUS_NAME = "Official"
# MusicBrainz has two release-group-url relationship types for this ("streaming"
# is paid/subscription, "free streaming" is free) — both count.
STREAMING_LINK_TYPE_NAMES = ("streaming", "free streaming")
# Which streaming services we surface, keyed by the linked URL's host.
STREAMING_HOSTS = {
    "open.spotify.com": "spotify",
    "music.apple.com": "appleMusic",
    "music.youtube.com": "youtubeMusic",
}


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


# How long a single socket read may block before it's treated as a stalled connection rather
# than a slow-but-alive one — without this, a connection that goes silent mid-transfer (server or
# intermediate NAT/proxy drops the stream without ever sending FIN/RST) hangs the read() syscall
# forever: the OS still reports the TCP connection as ESTABLISHED, CPU sits at 0%, and nothing
# in the process ever raises — it just looks stuck indefinitely with no error to react to.
SOCKET_TIMEOUT_SECONDS = 60
# How many times to retry one archive's download+extraction after a stall/network error before
# giving up on it entirely.
MAX_DOWNLOAD_ATTEMPTS = 5


def resolve_dump_base_url(export_base_url: str, ssl_context: ssl.SSLContext) -> str:
    """LATEST is a plain-text file (not a directory alias) containing the
    current dated export directory's name, e.g. `20260909-002431`."""
    with urllib.request.urlopen(
        f"{export_base_url}/LATEST", context=ssl_context, timeout=SOCKET_TIMEOUT_SECONDS
    ) as resp:
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
        still_missing = download_archive(dump_base_url, archive, raw_dir, wanted_from_archive, ssl_context)
        missing -= (wanted_from_archive - still_missing)
        if still_missing:
            raise RuntimeError(f"Never found these tables in {archive}: {sorted(still_missing)}")
    if missing:
        raise RuntimeError(f"Never found these tables in any archive: {sorted(missing)}")


def download_archive(
    dump_base_url: str,
    archive: str,
    raw_dir: Path,
    wanted_from_archive: set[str],
    ssl_context: ssl.SSLContext
) -> set[str]:
    """Downloads+extracts [wanted_from_archive] from one dump archive, retrying up to
    [MAX_DOWNLOAD_ATTEMPTS] times on a stalled/dropped connection (a [SOCKET_TIMEOUT_SECONDS]
    read timeout is what turns a silent hang into a retriable error in the first place — see
    its comment).

    @return whichever of [wanted_from_archive] are still missing after all retries — empty if
    every one was found. [wanted_from_archive] itself is read-only here.
    """
    archive_url = f"{dump_base_url}/{archive}"
    still_wanted = set(wanted_from_archive)
    for attempt in range(1, MAX_DOWNLOAD_ATTEMPTS + 1):
        if not still_wanted:
            break
        print(f"Downloading and extracting {sorted(still_wanted)} from {archive_url} (attempt {attempt}) ...")
        in_progress_dest: Path | None = None
        try:
            with urllib.request.urlopen(archive_url, context=ssl_context, timeout=SOCKET_TIMEOUT_SECONDS) as response:
                with tarfile.open(fileobj=response, mode="r|bz2") as tar:
                    for member in tar:
                        name = Path(member.name).name
                        if name not in still_wanted:
                            continue
                        extracted = tar.extractfile(member)
                        if extracted is None:
                            continue
                        dest = raw_dir / name
                        in_progress_dest = dest
                        with open(dest, "wb") as out:
                            _copy_stream(extracted, out)
                        in_progress_dest = None
                        print(f"  extracted {name} ({dest.stat().st_size:,} bytes)")
                        still_wanted.discard(name)
                        if not still_wanted:
                            break  # stop reading this archive early
        except (OSError, TimeoutError, tarfile.TarError) as e:
            # A file left mid-write when the stall/error hit is truncated, not complete — leaving
            # it in place would make the *next* attempt's existence check think it's done.
            if in_progress_dest is not None:
                in_progress_dest.unlink(missing_ok=True)
            if attempt == MAX_DOWNLOAD_ATTEMPTS:
                raise RuntimeError(
                    f"Giving up on {archive} after {MAX_DOWNLOAD_ATTEMPTS} attempts — still missing "
                    f"{sorted(still_wanted)}. Last error: {e!r}"
                ) from e
            print(f"  {e!r} — retrying ({MAX_DOWNLOAD_ATTEMPTS - attempt} attempt(s) left)")
    return still_wanted


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
    # rg_id (release_group.id, the internal integer key) -> gid (MBID) — the
    # release groups actually kept in `album`, i.e. exactly the ones
    # build_track_index/build_streaming_link_index below should bother with.
    wanted_rg_ids: dict[str, str] = {}

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
        wanted_rg_ids[rg_id] = gid
        matched += 1
        if len(batch) >= 5000:
            flush()
    flush()

    conn.execute("CREATE INDEX idx_album_month_day ON album (month, day)")
    conn.commit()
    print(f"Considered {considered:,} release groups with a full date, kept {matched:,} albums.")

    build_track_index(raw_dir, wanted_rg_ids, conn)
    build_streaming_link_index(raw_dir, wanted_rg_ids, conn)
    build_genre_index(conn)

    conn.close()
    print(f"Wrote {output}")


def build_track_index(raw_dir: Path, wanted_rg_ids: dict[str, str], conn: sqlite3.Connection) -> None:
    """Writes the `track` table: one row per track of one representative
    release per release group in [wanted_rg_ids] — the same release/tracklist
    shape `MusicBrainzRepositoryImpl.getAlbumDetail` picks live today
    (releases-browse, preferring an Official release)."""
    print("Loading release_status ...")
    official_status_id: str | None = None
    for row in read_copy_file(raw_dir / "release_status"):
        status_id, name = row[0], row[1]
        if name == OFFICIAL_STATUS_NAME:
            official_status_id = status_id
    if official_status_id is None:
        raise RuntimeError(
            f"No release_status row named {OFFICIAL_STATUS_NAME!r} — "
            "MusicBrainz's schema may have changed; check this table by hand."
        )
    print(f"  {OFFICIAL_STATUS_NAME!r} == status id {official_status_id}")

    print("Loading release (picking one release per release group) ...")
    # For each release group, keep the release with the smallest
    # (is_not_official, release_id) key — i.e. prefer Official, then the
    # lowest release id for a stable, deterministic pick.
    best_release_key: dict[str, tuple[int, int]] = {}
    chosen_release_id: dict[str, str] = {}
    for row in read_copy_file(raw_dir / "release"):
        release_id, rg_id, status_id = row[0], row[4], row[5]
        if rg_id not in wanted_rg_ids:
            continue
        key = (0 if status_id == official_status_id else 1, int(release_id))
        if rg_id not in best_release_key or key < best_release_key[rg_id]:
            best_release_key[rg_id] = key
            chosen_release_id[rg_id] = release_id
    release_id_to_rg_gid = {
        release_id: wanted_rg_ids[rg_id] for rg_id, release_id in chosen_release_id.items()
    }
    print(f"  {len(release_id_to_rg_gid):,} releases chosen (one per release group)")

    print("Loading medium ...")
    # medium id -> (release group gid, disc position) — only for media on a
    # chosen release.
    medium_index: dict[str, tuple[str, int]] = {}
    for row in read_copy_file(raw_dir / "medium"):
        medium_id, release_id, position = row[0], row[1], row[2]
        rg_gid = release_id_to_rg_gid.get(release_id)
        if rg_gid is None:
            continue
        medium_index[medium_id] = (rg_gid, int(position))
    print(f"  {len(medium_index):,} media kept")

    conn.execute(
        """
        CREATE TABLE track (
            release_group_gid TEXT NOT NULL,
            medium_position INTEGER NOT NULL,
            position INTEGER NOT NULL,
            title TEXT NOT NULL,
            length_ms INTEGER
        )
        """
    )

    print("Loading track and writing the track index ...")
    # Buffered per album so multi-disc tracklists can be written out in
    # (medium_position, position) order — the dump's row order isn't
    # guaranteed to already be sorted that way.
    tracks_by_rg: dict[str, list[tuple[int, int, str, str | None]]] = {}
    kept = 0
    for row in read_copy_file(raw_dir / "track"):
        medium_id, position, name, length = row[3], row[4], row[6], row[8]
        entry = medium_index.get(medium_id)
        if entry is None:
            continue
        rg_gid, medium_position = entry
        tracks_by_rg.setdefault(rg_gid, []).append((medium_position, int(position), name, length))
        kept += 1
    print(f"  {kept:,} tracks matched")

    batch: list[tuple[str, int, int, str, int | None]] = []
    for rg_gid, tracks in tracks_by_rg.items():
        tracks.sort(key=lambda t: (t[0], t[1]))
        for medium_position, position, title, length in tracks:
            length_ms = int(length) if length is not None else None
            batch.append((rg_gid, medium_position, position, title, length_ms))
    conn.executemany("INSERT INTO track VALUES (?, ?, ?, ?, ?)", batch)
    conn.execute(
        "CREATE INDEX idx_track_release_group_gid ON track (release_group_gid, medium_position, position)"
    )
    conn.commit()
    print(f"Wrote {len(batch):,} track rows for {len(tracks_by_rg):,} albums.")


def build_streaming_link_index(raw_dir: Path, wanted_rg_ids: dict[str, str], conn: sqlite3.Connection) -> None:
    """Writes the `streaming_link` table: each release group's Spotify/Apple
    Music/YouTube Music link, from MusicBrainz's own release-group-url
    "streaming"/"free streaming" relationships — the same data an
    `inc=url-rels` release-group lookup would return, joined offline instead
    of live so it costs nothing per AlbumDetailScreen open."""
    print("Loading link_type ...")
    wanted_link_type_ids: set[str] = set()
    for row in read_copy_file(raw_dir / "link_type"):
        type_id, entity_type0, entity_type1, name = row[0], row[4], row[5], row[6]
        if (
            entity_type0 == "release_group"
            and entity_type1 == "url"
            and name in STREAMING_LINK_TYPE_NAMES
        ):
            wanted_link_type_ids.add(type_id)
    if not wanted_link_type_ids:
        raise RuntimeError(
            f"No release_group-url link_type named any of {STREAMING_LINK_TYPE_NAMES} — "
            "MusicBrainz's schema may have changed; check this table by hand."
        )
    print(f"  {len(wanted_link_type_ids)} matching link types")

    print("Loading link ...")
    wanted_link_ids: set[str] = set()
    for row in read_copy_file(raw_dir / "link"):
        link_id, link_type_id = row[0], row[1]
        if link_type_id in wanted_link_type_ids:
            wanted_link_ids.add(link_id)
    print(f"  {len(wanted_link_ids):,} streaming link instances")

    print("Loading l_release_group_url ...")
    rg_url_pairs: list[tuple[str, str]] = []  # (release group gid, url id)
    wanted_url_ids: set[str] = set()
    for row in read_copy_file(raw_dir / "l_release_group_url"):
        link_id, rg_id, url_id = row[1], row[2], row[3]
        if link_id not in wanted_link_ids or rg_id not in wanted_rg_ids:
            continue
        rg_url_pairs.append((wanted_rg_ids[rg_id], url_id))
        wanted_url_ids.add(url_id)
    print(f"  {len(rg_url_pairs):,} release-group/url pairs kept")

    print("Loading url ...")
    url_id_to_url: dict[str, str] = {}
    for row in read_copy_file(raw_dir / "url"):
        url_id, url_value = row[0], row[2]
        if url_id in wanted_url_ids:
            url_id_to_url[url_id] = url_value

    conn.execute(
        """
        CREATE TABLE streaming_link (
            release_group_gid TEXT NOT NULL,
            service TEXT NOT NULL,
            url TEXT NOT NULL
        )
        """
    )

    print("Classifying streaming links by host and writing the index ...")
    # (release group gid, service) -> (url id as int, url) — lowest url id
    # wins on a rare duplicate, for a deterministic pick.
    best: dict[tuple[str, str], tuple[int, str]] = {}
    for rg_gid, url_id in rg_url_pairs:
        url_value = url_id_to_url.get(url_id)
        if url_value is None:
            continue
        service = classify_streaming_service(url_value)
        if service is None:
            continue
        candidate = (int(url_id), url_value)
        key = (rg_gid, service)
        if key not in best or candidate < best[key]:
            best[key] = candidate

    batch = [(rg_gid, service, url_value) for (rg_gid, service), (_url_id, url_value) in best.items()]
    conn.executemany("INSERT INTO streaming_link VALUES (?, ?, ?)", batch)
    conn.execute("CREATE INDEX idx_streaming_link_release_group_gid ON streaming_link (release_group_gid)")
    conn.commit()
    albums_with_links = len({rg_gid for rg_gid, _service in best})
    print(f"Wrote {len(batch):,} streaming links for {albums_with_links:,} albums.")


def build_genre_index(conn: sqlite3.Connection) -> None:
    """Writes an always-empty `genre` table: (release_group_gid, name, count), never populated.

    Genres would need MusicBrainz's `tag`/`release_group_tag` tables (to associate a curated
    `genre` name with a specific release group — `genre` alone is just the name catalog, not a
    mapping) — confirmed, by listing every single member of mbdump.tar.bz2 and every other
    archive in the same dump directory directly, that neither table is included in MusicBrainz's
    public bulk export at all. Not a download flake, not fixable by retrying: genres are
    structurally unavailable from the offline dump, so this just documents that rather than
    pretending to compute something it can't. If MusicBrainz ever starts shipping these tables,
    this is where the join from the old (now-removed) version of this function would go again."""
    conn.execute(
        """
        CREATE TABLE genre (
            release_group_gid TEXT NOT NULL,
            name TEXT NOT NULL,
            count INTEGER NOT NULL
        )
        """
    )
    conn.execute("CREATE INDEX idx_genre_release_group_gid ON genre (release_group_gid)")
    conn.commit()
    print("genre table left empty — MusicBrainz's public dump has no tag/release_group_tag data to build it from.")


def classify_streaming_service(url: str) -> str | None:
    """Which of [STREAMING_HOSTS] (if any) a URL belongs to, by its host —
    e.g. "https://open.spotify.com/album/..." -> "spotify"."""
    host = urllib.parse.urlparse(url).netloc.lower().removeprefix("www.")
    return STREAMING_HOSTS.get(host)


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
