#!/usr/bin/env python3
"""
Uploads scripts/day_index.sqlite (built by build_day_index.py) into Firestore
for two features:
  - albumsByDay — the "on this day in history" feature (see
    DayInHistoryRepositoryImpl / GetAlbumsByDayUseCase in the app).
  - albumTracks — per-album tracklist + streaming-service links (Spotify,
    Apple Music, YouTube Music), read by AlbumTracksRepositoryImpl so
    AlbumDetailScreen doesn't need a live, rate-limited MusicBrainz call for
    that data on every open. One document per album, id = the same
    release-group gid used in albumsByDay.

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
    """One dict per album that has cached tracks and/or streaming links:
    {"id", "tracks", "streaming_links"}. `track` rows are grouped by
    release_group_gid in Python (SQL alone can't build a nested list per
    group) — ordering the query by (release_group_gid, medium_position,
    position) means each group's rows already arrive in the right tracklist
    order, so grouping is just "start a new list when the gid changes"."""
    conn = sqlite3.connect(db_path)
    conn.row_factory = sqlite3.Row
    try:
        streaming_links = _load_streaming_links(conn)
        emitted = 0
        current_gid: str | None = None
        current_tracks: list[dict] = []

        def flush() -> dict | None:
            if current_gid is None:
                return None
            return {
                "id": current_gid,
                "tracks": current_tracks,
                "streaming_links": streaming_links.pop(current_gid, {}),
            }

        for row in conn.execute(
            "SELECT release_group_gid, position, title, length_ms FROM track "
            "ORDER BY release_group_gid, medium_position, position"
        ):
            if limit is not None and emitted >= limit:
                break
            if row["release_group_gid"] != current_gid:
                extras = flush()
                if extras is not None:
                    emitted += 1
                    yield extras
                current_gid, current_tracks = row["release_group_gid"], []
            current_tracks.append(
                {"position": row["position"], "title": row["title"], "lengthMs": row["length_ms"]}
            )
        # The loop above only flushes the *previous* group when a new gid starts, so whatever
        # group was accumulating when the query ran out of rows is still pending here — unless
        # the limit was already hit, in which case it must stay unflushed rather than sneak one
        # more album past the cap.
        if limit is None or emitted < limit:
            extras = flush()
            if extras is not None:
                emitted += 1
                yield extras

        # Albums with streaming links but no cached tracks at all (rare, but
        # possible — e.g. the tracklist join came up empty for that release).
        for gid, links in streaming_links.items():
            if limit is not None and emitted >= limit:
                break
            emitted += 1
            yield {"id": gid, "tracks": [], "streaming_links": links}
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
        doc: dict = {"tracks": item["tracks"]}
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
