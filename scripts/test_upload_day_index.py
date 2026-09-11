#!/usr/bin/env python3
"""
Offline unit test for upload_day_index.py's grouping/batching logic — a fake
Firestore client/batch (no network, no emulator, no credentials) verifying
documents are chunked at the given batch size, any remainder is still
committed, and album extras (full metadata minus rating, tracks, genres,
streaming links) are merged into one document per album correctly.

Run: python3 scripts/test_upload_day_index.py
"""
from __future__ import annotations

import sqlite3
import tempfile
import unittest
from pathlib import Path

from upload_day_index import iter_album_extras, iter_albums, upload, upload_album_extras


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
    def __init__(self, name: str):
        self.name = name

    def document(self, id_: str) -> FakeDocRef:
        return FakeDocRef(id_)


class FakeFirestoreClient:
    def __init__(self):
        self.commits: list[list[tuple[str, dict]]] = []
        self.collection_names_used: list[str] = []

    def collection(self, name: str) -> FakeCollection:
        self.collection_names_used.append(name)
        return FakeCollection(name)

    def batch(self) -> FakeBatch:
        return FakeBatch(self.commits)


def make_albums(n: int) -> list[dict]:
    return [
        {"id": f"album-{i}", "title": f"Title {i}", "artist_name": "Artist", "year": 2000, "month": 1, "day": 1}
        for i in range(n)
    ]


def make_extras(n: int) -> list[dict]:
    return [
        {
            "id": f"rg-{i}",
            "title": f"Title {i}",
            "artist_name": "Artist",
            "year": 2000,
            "month": 1,
            "day": 1,
            "genres": [],
            "tracks": [],
            "streaming_links": {},
        }
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
        self.assertEqual(["albumsByDay"], db.collection_names_used)

    def test_empty_input_commits_nothing(self):
        db = FakeFirestoreClient()
        total = upload(iter([]), db, batch_size=450)

        self.assertEqual(0, total)
        self.assertEqual([], db.commits)


class UploadAlbumExtrasTest(unittest.TestCase):
    def test_writes_full_metadata_tracks_and_streaming_links(self):
        db = FakeFirestoreClient()
        extras = [
            {
                "id": "rg-1",
                "title": "Origin of Symmetry",
                "artist_name": "Muse",
                "year": 2001,
                "month": 6,
                "day": 17,
                "genres": ["alternative rock"],
                "tracks": [{"position": 1, "title": "New Born", "lengthMs": 400000}],
                "streaming_links": {"spotify": "https://open.spotify.com/album/abc"},
            }
        ]
        upload_album_extras(iter(extras), db, batch_size=450)

        [(doc_id, data)] = db.commits[0]
        self.assertEqual("rg-1", doc_id)
        self.assertEqual(
            {
                "title": "Origin of Symmetry",
                "artistName": "Muse",
                "year": 2001,
                "month": 6,
                "day": 17,
                "tracks": [{"position": 1, "title": "New Born", "lengthMs": 400000}],
                "genres": ["alternative rock"],
                "streamingLinks": {"spotify": "https://open.spotify.com/album/abc"},
            },
            data,
        )
        self.assertEqual(["albumTracks"], db.collection_names_used)

    def test_omits_genres_and_streaming_links_keys_when_empty(self):
        db = FakeFirestoreClient()
        upload_album_extras(iter(make_extras(1)), db, batch_size=450)

        [(_doc_id, data)] = db.commits[0]
        self.assertNotIn("genres", data)
        self.assertNotIn("streamingLinks", data)
        self.assertIn("tracks", data)  # tracks stays present (as []) even when empty

    def test_chunks_at_batch_size(self):
        db = FakeFirestoreClient()
        total = upload_album_extras(iter(make_extras(5)), db, batch_size=2)

        self.assertEqual(5, total)
        self.assertEqual([2, 2, 1], [len(commit) for commit in db.commits])


class IterAlbumsTest(unittest.TestCase):
    def _write_fixture(self, tmp: str, rows: int) -> Path:
        db_path = Path(tmp) / "day_index.sqlite"
        conn = sqlite3.connect(db_path)
        conn.execute(
            "CREATE TABLE album (id TEXT PRIMARY KEY, title TEXT, artist_name TEXT, "
            "year INTEGER, month INTEGER, day INTEGER)"
        )
        conn.executemany(
            "INSERT INTO album VALUES (?, ?, ?, ?, ?, ?)",
            [(f"id-{i}", f"Title {i}", "Artist", 2000, 1, 1) for i in range(rows)],
        )
        conn.commit()
        conn.close()
        return db_path

    def test_no_limit_reads_every_row(self):
        with tempfile.TemporaryDirectory() as tmp:
            db_path = self._write_fixture(tmp, rows=5)
            self.assertEqual(5, sum(1 for _ in iter_albums(db_path)))

    def test_limit_caps_the_number_of_rows(self):
        with tempfile.TemporaryDirectory() as tmp:
            db_path = self._write_fixture(tmp, rows=5)
            self.assertEqual(3, sum(1 for _ in iter_albums(db_path, limit=3)))

    def test_limit_larger_than_table_reads_every_row(self):
        with tempfile.TemporaryDirectory() as tmp:
            db_path = self._write_fixture(tmp, rows=5)
            self.assertEqual(5, sum(1 for _ in iter_albums(db_path, limit=100)))


class IterAlbumExtrasTest(unittest.TestCase):
    def _write_fixture(self, tmp: str) -> Path:
        db_path = Path(tmp) / "day_index.sqlite"
        conn = sqlite3.connect(db_path)
        conn.execute(
            "CREATE TABLE album (id TEXT PRIMARY KEY, title TEXT, artist_name TEXT, "
            "year INTEGER, month INTEGER, day INTEGER)"
        )
        conn.execute(
            "CREATE TABLE track (release_group_gid TEXT, medium_position INTEGER, "
            "position INTEGER, title TEXT, length_ms INTEGER)"
        )
        conn.execute("CREATE TABLE streaming_link (release_group_gid TEXT, service TEXT, url TEXT)")
        conn.execute("CREATE TABLE genre (release_group_gid TEXT, name TEXT, count INTEGER)")
        conn.executemany(
            "INSERT INTO album VALUES (?, ?, ?, ?, ?, ?)",
            [
                ("rg-a", "Origin of Symmetry", "Muse", 2001, 6, 17),
                ("rg-b", "Solo Album", "Someone", 2010, 3, 3),
                # rg-c: cached metadata only — no tracks, no genres, no links yet. Must still be
                # emitted, since every cached album needs a document to replace getReleaseGroup.
                ("rg-c", "Untracked Album", "Nobody", 1999, 12, 25),
            ],
        )
        conn.executemany(
            "INSERT INTO track VALUES (?, ?, ?, ?, ?)",
            [
                # rg-a: two discs, out of dump order on purpose — grouping must still come out
                # in (medium_position, position) order.
                ("rg-a", 2, 1, "Disc2Track1", 200000),
                ("rg-a", 1, 1, "New Born", 400000),
                ("rg-a", 1, 2, "Bliss", 350000),
                ("rg-b", 1, 1, "Solo", None),
            ],
        )
        conn.executemany(
            "INSERT INTO streaming_link VALUES (?, ?, ?)",
            [("rg-a", "spotify", "https://open.spotify.com/album/a")],
        )
        conn.executemany(
            "INSERT INTO genre VALUES (?, ?, ?)",
            [
                # Out of count order on purpose — must come out sorted by count desc, name asc.
                ("rg-a", "space rock", 1),
                ("rg-a", "alternative rock", 5),
            ],
        )
        conn.commit()
        conn.close()
        return db_path

    def test_every_cached_album_is_emitted_with_its_metadata(self):
        with tempfile.TemporaryDirectory() as tmp:
            db_path = self._write_fixture(tmp)
            extras = {item["id"]: item for item in iter_album_extras(db_path)}

        self.assertEqual({"rg-a", "rg-b", "rg-c"}, extras.keys())
        self.assertEqual(
            {"title": "Untracked Album", "artist_name": "Nobody", "year": 1999, "month": 12, "day": 25},
            {k: extras["rg-c"][k] for k in ("title", "artist_name", "year", "month", "day")},
        )
        self.assertEqual([], extras["rg-c"]["tracks"])
        self.assertEqual([], extras["rg-c"]["genres"])
        self.assertEqual({}, extras["rg-c"]["streaming_links"])

    def test_groups_tracks_in_medium_then_position_order_and_attaches_links(self):
        with tempfile.TemporaryDirectory() as tmp:
            db_path = self._write_fixture(tmp)
            extras = {item["id"]: item for item in iter_album_extras(db_path)}

        self.assertEqual(
            [
                {"position": 1, "title": "New Born", "lengthMs": 400000},
                {"position": 2, "title": "Bliss", "lengthMs": 350000},
                {"position": 1, "title": "Disc2Track1", "lengthMs": 200000},
            ],
            extras["rg-a"]["tracks"],
        )
        self.assertEqual({"spotify": "https://open.spotify.com/album/a"}, extras["rg-a"]["streaming_links"])
        self.assertEqual({}, extras["rg-b"]["streaming_links"])

    def test_genres_ordered_by_count_descending_then_name(self):
        with tempfile.TemporaryDirectory() as tmp:
            db_path = self._write_fixture(tmp)
            extras = {item["id"]: item for item in iter_album_extras(db_path)}

        self.assertEqual(["alternative rock", "space rock"], extras["rg-a"]["genres"])

    def test_limit_caps_the_number_of_albums_not_rows(self):
        with tempfile.TemporaryDirectory() as tmp:
            db_path = self._write_fixture(tmp)
            # rg-a alone has 3 track rows, so a naive row-based limit of 2 would cut its
            # tracklist short instead of capping at 2 whole albums.
            ids = [item["id"] for item in iter_album_extras(db_path, limit=2)]

        self.assertEqual(["rg-a", "rg-b"], ids)


if __name__ == "__main__":
    unittest.main()
