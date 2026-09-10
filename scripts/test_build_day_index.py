#!/usr/bin/env python3
"""
Offline smoke test for build_day_index.py's parsing/join logic — no network
access, exercises unescape_copy_value() and build_index() against small
hand-written fixtures shaped exactly like real mbdump table files (same
column order as MusicBrainz's current schema, see build_day_index.py's
module docstring).

Run: python3 scripts/test_build_day_index.py
"""
from __future__ import annotations

import sqlite3
import tempfile
import unittest
from pathlib import Path

from build_day_index import build_index, unescape_copy_value


class UnescapeCopyValueTest(unittest.TestCase):
    def test_null(self):
        self.assertIsNone(unescape_copy_value("\\N"))

    def test_plain_value(self):
        self.assertEqual("Origin of Symmetry", unescape_copy_value("Origin of Symmetry"))

    def test_escaped_tab_and_newline(self):
        self.assertEqual("a\tb\nc", unescape_copy_value("a\\tb\\nc"))

    def test_escaped_backslash(self):
        self.assertEqual("a\\b", unescape_copy_value("a\\\\b"))


class BuildIndexTest(unittest.TestCase):
    def test_join_filters_and_writes_expected_rows(self):
        with tempfile.TemporaryDirectory() as tmp:
            raw_dir = Path(tmp) / "raw"
            raw_dir.mkdir()
            output = Path(tmp) / "day_index.sqlite"

            # release_group_primary_type: id, name, parent, child_order, description, gid
            (raw_dir / "release_group_primary_type").write_text(
                "1\tAlbum\t\\N\t1\t\\N\tsome-gid-1\n"
                "2\tSingle\t\\N\t2\t\\N\tsome-gid-2\n"
            )
            # artist_credit: id, name, artist_count, ref_count, created, edits_pending, gid
            (raw_dir / "artist_credit").write_text(
                "10\tMuse\t1\t1\t2020-01-01\t0\tac-gid-10\n"
                "11\tJohn McCutcheon & Tom Paxton\t2\t1\t2020-01-01\t0\tac-gid-11\n"
            )
            # release_group: id, gid, name, artist_credit, type, comment, edits_pending, last_updated
            (raw_dir / "release_group").write_text(
                # kept: Album, full date, known artist credit
                "100\trg-gid-100\tOrigin of Symmetry\t10\t1\t\t0\t2020-01-01\n"
                # dropped: type is Single, not Album
                "101\trg-gid-101\tSome Single\t10\t2\t\t0\t2020-01-01\n"
                # kept: multi-artist credit name passes through as-is
                "102\trg-gid-102\tTogether Again\t11\t1\t\t0\t2020-01-01\n"
                # dropped: no release_group_meta row at all for id 103
                "103\trg-gid-103\tOrphaned\t10\t1\t\t0\t2020-01-01\n"
            )
            # release_group_meta: id, release_count, year, month, day, rating, rating_count
            (raw_dir / "release_group_meta").write_text(
                "100\t1\t2001\t2\t19\t\\N\t\\N\n"
                "101\t1\t2001\t2\t19\t\\N\t\\N\n"
                "102\t1\t2020\t3\t15\t\\N\t\\N\n"
                # dropped: partial date (no day)
                "104\t1\t1999\t6\t\\N\t\\N\t\\N\n"
            )

            build_index(raw_dir, output)

            conn = sqlite3.connect(output)
            rows = conn.execute(
                "SELECT id, title, artist_name, year, month, day FROM album ORDER BY id"
            ).fetchall()
            conn.close()

            self.assertEqual(
                [
                    ("rg-gid-100", "Origin of Symmetry", "Muse", 2001, 2, 19),
                    ("rg-gid-102", "Together Again", "John McCutcheon & Tom Paxton", 2020, 3, 15),
                ],
                rows,
            )


if __name__ == "__main__":
    unittest.main()
