#!/usr/bin/env python3
"""
Uploads scripts/day_index.sqlite (built by build_day_index.py) into Firestore
for two features:
  - albumsByDay — the "on this day in history" feature (see
    DayInHistoryRepositoryImpl / GetAlbumsByDayUseCase in the app).
  - albumTracks — everything AlbumDetailScreen needs for an album except its
    rating (title, artist, release date, genres, tracklist, streaming-service
    links), read by AlbumTracksRepositoryImpl so AlbumDetailScreen doesn't
    need a live MusicBrainz call at all for a cached album — only a genuine
    cache miss falls back to one. One document per album, id = the same
    release-group gid used in albumsByDay. Rating has no offline snapshot —
    it changes too often to freeze into a dump — so it's deliberately left
    out; a cache-served album just shows no rating.

Writes are batched (Firestore's batch cap is 500 operations) and use each
album's MusicBrainz release-group gid as the document id, so re-running this
script is idempotent — existing documents get overwritten with the same
data rather than duplicated.

Credentials: needs a service account key with write access to the target
Firestore project (this bypasses firestore.rules entirely — that's why only
a script holding this key can write to a collection whose rules say `write:
if false`). Generate one in the Firebase console: Project settings ->
Service accounts -> Generate new private key. Then either pass
--credentials PATH or set the GOOGLE_APPLICATION_CREDENTIALS environment
variable. Never commit that key file.

Testing without touching production or needing a real key at all: run
against the Firestore emulator instead —
    firebase emulators:start --only firestore
    FIRESTORE_EMULATOR_HOST=localhost:8080 python3 scripts/upload_day_index.py \
        --project-id demo-test
(google-cloud-firestore auto-detects FIRESTORE_EMULATOR_HOST and skips real
auth entirely — no credentials needed for this path).

Usage:
    python3 scripts/upload_day_index.py [--input PATH] [--project-id ID]
                                          [--credentials PATH] [--batch-size N]
                                          [--limit N]

--limit caps how many rows/albums get uploaded to each collection, e.g.
--limit 10000 to try a real project run within Firestore's daily free write
quota (20,000/day) before committing to the full ~1.3M-row upload.
"""
from __future__ import annotations

import argparse
import sqlite3
from pathlib import Path
from typing import Callable, Iterator

from google.cloud import firestore
from google.oauth2 import service_account

ALBUMS_COLLECTION = "albumsByDay"
ALBUM_TRACKS_COLLECTION = "albumTracks"
# Comfortable headroom under Firestore's hard 500-operation batch cap.
DEFAULT_BATCH_SIZE = 450


def init_firestore(project_id: str | None, credentials_path: str | None) -> firestore.Client:
    if credentials_path:
        creds = service_account.Credentials.from_service_account_file(credentials_path)
        return firestore.Client(project=project_id, credentials=creds)
    # No explicit credentials: either GOOGLE_APPLICATION_CREDENTIALS is set
    # (real project), or FIRESTORE_EMULATOR_HOST is set (emulator, no auth
    # needed at all) — both are handled by the client automatically.
    return firestore.Client(project=project_id)


def iter_albums(db_path: Path, limit: int | None = None) -> Iterator[dict]:
    conn = sqlite3.connect(db_path)
    conn.row_factory = sqlite3.Row
    try:
        query = "SELECT id, title, artist_name, year, month, day FROM album"
        params: tuple = ()
        if limit is not None:
            query += " LIMIT ?"
            params = (limit,)
        for row in conn.execute(query, params):
            yield dict(row)
    finally:
        conn.close()


def _load_streaming_links(conn: sqlite3.Connection) -> dict[str, dict[str, str]]:
    """release_group_gid -> {"spotify": url, ...} — small enough (at most a
    few links per album, and far from every album has one) to hold entirely
    in memory rather than re-querying per album."""
    links: dict[str, dict[str, str]] = {}
    for gid, service, url in conn.execute("SELECT release_group_gid, service, url FROM streaming_link"):
        links.setdefault(gid, {})[service] = url
    return links


def iter_album_extras(db_path: Path, limit: int | None = None) -> Iterator[dict]:
    """One dict per row of `album` (i.e. every cached album, so every one gets a full
    getReleaseGroup-replacement document even if it has no tracks/genres/links yet):
    {"id", "title", "artist_name", "year", "month", "day", "genres", "tracks",
    "streaming_links"}.

    `album` drives the iteration (every album must be emitted, tracks or not); `track` and
    `genre` are merged in via a synchronized ordered scan — all three queries are ordered by
    id/release_group_gid, so advancing a small "peek" cursor per side table and consuming its
    matching run is enough, without ever loading either table (both can be tens of millions of
    rows) into memory whole. `streaming_link` stays a plain preloaded dict — only a minority of
    albums have one at all, so it's small regardless of how big the other tables get.
    """
    conn = sqlite3.connect(db_path)
    conn.row_factory = sqlite3.Row
    try:
        streaming_links = _load_streaming_links(conn)
        album_rows = conn.execute("SELECT id, title, artist_name, year, month, day FROM album ORDER BY id")
        track_rows = conn.execute(
            "SELECT release_group_gid, position, title, length_ms FROM track "
            "ORDER BY release_group_gid, medium_position, position"
        )
        genre_rows = conn.execute(
            "SELECT release_group_gid, name FROM genre ORDER BY release_group_gid, count DESC, name"
        )
        track_peek = next(track_rows, None)
        genre_peek = next(genre_rows, None)

        emitted = 0
        for album in album_rows:
            if limit is not None and emitted >= limit:
                break
            album_id = album["id"]

            tracks: list[dict] = []
            while track_peek is not None and track_peek["release_group_gid"] == album_id:
                tracks.append(
                    {
                        "position": track_peek["position"],
                        "title": track_peek["title"],
                        "lengthMs": track_peek["length_ms"],
                    }
                )
                track_peek = next(track_rows, None)

            genres: list[str] = []
            while genre_peek is not None and genre_peek["release_group_gid"] == album_id:
                genres.append(genre_peek["name"])
                genre_peek = next(genre_rows, None)

            emitted += 1
            yield {
                "id": album_id,
                "title": album["title"],
                "artist_name": album["artist_name"],
                "year": album["year"],
                "month": album["month"],
                "day": album["day"],
                "genres": genres,
                "tracks": tracks,
                "streaming_links": streaming_links.get(album_id, {}),
            }
    finally:
        conn.close()


def upload_documents(
    items: Iterator[dict],
    db: firestore.Client,
    collection_name: str,
    batch_size: int,
    build_doc: Callable[[dict], dict],
) -> int:
    """Batch-writes one Firestore document per item, id = item["id"] — shared by both
    upload() and upload_album_extras()."""
    collection = db.collection(collection_name)
    batch = db.batch()
    pending = 0
    total = 0
    for item in items:
        doc_ref = collection.document(item["id"])
        batch.set(doc_ref, build_doc(item))
        pending += 1
        total += 1
        if pending >= batch_size:
            batch.commit()
            print(f"  committed {total:,} so far")
            batch = db.batch()
            pending = 0
    if pending:
        batch.commit()
    return total


def upload(albums: Iterator[dict], db: firestore.Client, batch_size: int) -> int:
    return upload_documents(
        albums,
        db,
        ALBUMS_COLLECTION,
        batch_size,
        lambda album: {
            "title": album["title"],
            "artistName": album["artist_name"],
            "year": album["year"],
            "month": album["month"],
            "day": album["day"],
        },
    )


def upload_album_extras(extras: Iterator[dict], db: firestore.Client, batch_size: int) -> int:
    def build_doc(item: dict) -> dict:
        doc: dict = {
            "title": item["title"],
            "artistName": item["artist_name"],
            "year": item["year"],
            "month": item["month"],
            "day": item["day"],
            "tracks": item["tracks"],
        }
        if item["genres"]:
            doc["genres"] = item["genres"]
        if item["streaming_links"]:
            doc["streamingLinks"] = item["streaming_links"]
        return doc

    return upload_documents(extras, db, ALBUM_TRACKS_COLLECTION, batch_size, build_doc)


def main() -> None:
    parser = argparse.ArgumentParser(
        description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter
    )
    parser.add_argument("--input", type=Path, default=Path("scripts/day_index.sqlite"))
    parser.add_argument("--project-id", default=None, help="Defaults to cassette-c8951.")
    parser.add_argument("--credentials", default=None, help="Path to a service account JSON key.")
    parser.add_argument("--batch-size", type=int, default=DEFAULT_BATCH_SIZE)
    parser.add_argument(
        "--limit", type=int, default=None, help="Only upload the first N rows/albums per collection."
    )
    args = parser.parse_args()

    if not args.input.exists():
        raise SystemExit(f"{args.input} doesn't exist — run build_day_index.py first.")
    if args.batch_size > 500:
        raise SystemExit("Firestore batched writes cap at 500 operations.")

    db = init_firestore(args.project_id or "cassette-c8951", args.credentials)
    suffix = f" (limit {args.limit:,})" if args.limit is not None else ""

    print(f"Uploading {args.input} -> Firestore collection '{ALBUMS_COLLECTION}'{suffix} ...")
    album_total = upload(iter_albums(args.input, args.limit), db, args.batch_size)
    print(f"Uploaded {album_total:,} albums to '{ALBUMS_COLLECTION}'.")

    print(f"Uploading {args.input} -> Firestore collection '{ALBUM_TRACKS_COLLECTION}'{suffix} ...")
    extras_total = upload_album_extras(iter_album_extras(args.input, args.limit), db, args.batch_size)
    print(f"Uploaded {extras_total:,} albums' tracks/streaming links to '{ALBUM_TRACKS_COLLECTION}'.")


if __name__ == "__main__":
    main()
