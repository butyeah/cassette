#!/usr/bin/env python3
"""
Offline unit test for upload_day_index.py's batching logic — a fake Firestore
client/batch (no network, no emulator, no credentials) verifying documents
are chunked at the given batch size and any remainder is still committed.

Run: python3 scripts/test_upload_day_index.py
"""
from __future__ import annotations

import unittest

from upload_day_index import upload


class FakeBatch:
    def __init__(self, commits: list[list[tuple[str, dict]]]):
        self._commits = commits
        self._pending: list[tuple[str, dict]] = []

    def set(self, doc_ref, data: dict) -> None:
        self._pending.append((doc_ref.id, data))

    def commit(self) -> None:
        self._commits.append(self._pending)
        self._pending = []


class FakeDocRef:
    def __init__(self, id_: str):
        self.id = id_


class FakeCollection:
    def document(self, id_: str) -> FakeDocRef:
        return FakeDocRef(id_)


class FakeFirestoreClient:
    def __init__(self):
        self.commits: list[list[tuple[str, dict]]] = []

    def collection(self, name: str) -> FakeCollection:
        return FakeCollection()

    def batch(self) -> FakeBatch:
        return FakeBatch(self.commits)


def make_albums(n: int) -> list[dict]:
    return [
        {"id": f"album-{i}", "title": f"Title {i}", "artist_name": "Artist", "year": 2000, "month": 1, "day": 1}
        for i in range(n)
    ]


class UploadTest(unittest.TestCase):
    def test_chunks_at_batch_size_and_commits_remainder(self):
        db = FakeFirestoreClient()
        total = upload(iter(make_albums(7)), db, batch_size=3)

        self.assertEqual(7, total)
        # 7 albums at batch size 3 -> commits of [3, 3, 1]
        self.assertEqual([3, 3, 1], [len(commit) for commit in db.commits])

    def test_exact_multiple_of_batch_size_has_no_empty_trailing_commit(self):
        db = FakeFirestoreClient()
        total = upload(iter(make_albums(6)), db, batch_size=3)

        self.assertEqual(6, total)
        self.assertEqual([3, 3], [len(commit) for commit in db.commits])

    def test_writes_expected_fields(self):
        db = FakeFirestoreClient()
        upload(iter(make_albums(1)), db, batch_size=450)

        [(doc_id, data)] = db.commits[0]
        self.assertEqual("album-0", doc_id)
        self.assertEqual(
            {"title": "Title 0", "artistName": "Artist", "year": 2000, "month": 1, "day": 1}, data
        )

    def test_empty_input_commits_nothing(self):
        db = FakeFirestoreClient()
        total = upload(iter([]), db, batch_size=450)

        self.assertEqual(0, total)
        self.assertEqual([], db.commits)


if __name__ == "__main__":
    unittest.main()
