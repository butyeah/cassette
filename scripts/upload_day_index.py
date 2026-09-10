#!/usr/bin/env python3
"""
Uploads scripts/day_index.sqlite (built by build_day_index.py) into the
albumsByDay Firestore collection, for the "on this day in history" feature
(see DayInHistoryRepositoryImpl / GetAlbumsByDayUseCase in the app).

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

--limit caps how many rows get uploaded, e.g. --limit 10000 to try a real
project run within Firestore's daily free write quota (20,000/day) before
committing to the full ~1.3M-row upload.
"""
from __future__ import annotations

import argparse
import sqlite3
from pathlib import Path
from typing import Iterator

from google.cloud import firestore
from google.oauth2 import service_account

COLLECTION = "albumsByDay"
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


def upload(albums: Iterator[dict], db: firestore.Client, batch_size: int) -> int:
    collection = db.collection(COLLECTION)
    batch = db.batch()
    pending = 0
    total = 0
    for album in albums:
        doc_ref = collection.document(album["id"])
        batch.set(
            doc_ref,
            {
                "title": album["title"],
                "artistName": album["artist_name"],
                "year": album["year"],
                "month": album["month"],
                "day": album["day"],
            },
        )
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


def main() -> None:
    parser = argparse.ArgumentParser(
        description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter
    )
    parser.add_argument("--input", type=Path, default=Path("scripts/day_index.sqlite"))
    parser.add_argument("--project-id", default=None, help="Defaults to cassette-c8951.")
    parser.add_argument("--credentials", default=None, help="Path to a service account JSON key.")
    parser.add_argument("--batch-size", type=int, default=DEFAULT_BATCH_SIZE)
    parser.add_argument(
        "--limit", type=int, default=None, help="Only upload the first N rows."
    )
    args = parser.parse_args()

    if not args.input.exists():
        raise SystemExit(f"{args.input} doesn't exist — run build_day_index.py first.")
    if args.batch_size > 500:
        raise SystemExit("Firestore batched writes cap at 500 operations.")

    db = init_firestore(args.project_id or "cassette-c8951", args.credentials)
    suffix = f" (limit {args.limit:,})" if args.limit is not None else ""
    print(f"Uploading {args.input} -> Firestore collection '{COLLECTION}'{suffix} ...")
    total = upload(iter_albums(args.input, args.limit), db, args.batch_size)
    print(f"Uploaded {total:,} albums to '{COLLECTION}'.")


if __name__ == "__main__":
    main()
