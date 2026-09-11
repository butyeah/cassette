#!/usr/bin/env python3
"""
Offline smoke test for build_day_index.py's parsing/join logic — no network
access, exercises unescape_copy_value(), classify_streaming_service() and
build_index() (which also drives build_track_index()/
build_streaming_link_index()) against small hand-written fixtures shaped
exactly like real mbdump table files (same column order as MusicBrainz's
current schema, see build_day_index.py's module docstring).

Run: python3 scripts/test_build_day_index.py
"""
from __future__ import annotations

import sqlite3
import tempfile
import unittest
from pathlib import Path

from build_day_index import build_index, classify_streaming_service, unescape_copy_value


class UnescapeCopyValueTest(unittest.TestCase):
    def test_null(self):
        self.assertIsNone(unescape_copy_value("\\N"))

    def test_plain_value(self):
        self.assertEqual("Origin of Symmetry", unescape_copy_value("Origin of Symmetry"))

    def test_escaped_tab_and_newline(self):
        self.assertEqual("a\tb\nc", unescape_copy_value("a\\tb\\nc"))

    def test_escaped_backslash(self):
        self.assertEqual("a\\b", unescape_copy_value("a\\\\b"))


class ClassifyStreamingServiceTest(unittest.TestCase):
    def test_spotify(self):
        self.assertEqual("spotify", classify_streaming_service("https://open.spotify.com/album/abc"))

    def test_apple_music(self):
        self.assertEqual("appleMusic", classify_streaming_service("https://music.apple.com/us/album/xyz"))

    def test_youtube_music(self):
        self.assertEqual(
            "youtubeMusic", classify_streaming_service("https://music.youtube.com/playlist?list=abc")
        )

    def test_www_prefix_still_matches(self):
        self.assertEqual("spotify", classify_streaming_service("https://www.open.spotify.com/album/abc"))

    def test_unrecognized_host_is_none(self):
        self.assertIsNone(classify_streaming_service("https://bandcamp.com/album/def"))


def _write_fixtures(raw_dir: Path) -> None:
    """Every table build_index()/build_track_index()/build_streaming_link_index() reads,
    covering: the existing album join, a two-release/two-disc tracklist pick, and a
    streaming-link join with a wrong-entity-type link_type, an unrelated release group,
    and a non-streaming host all present as things that must be filtered out."""
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

    # release_status: id, name, parent, child_order, description, gid
    (raw_dir / "release_status").write_text(
        "1\tOfficial\t\\N\t1\t\\N\tstatus-gid-1\n"
        "2\tPromotion\t\\N\t2\t\\N\tstatus-gid-2\n"
    )
    # release: id, gid, name, artist_credit, release_group, status
    (raw_dir / "release").write_text(
        # a Promotion release for rg 100 — should lose to the Official one below
        "1000\tr-gid-1000\tOrigin of Symmetry (promo)\t10\t100\t2\n"
        # the Official release for rg 100 — should be the one whose tracks are kept
        "1001\tr-gid-1001\tOrigin of Symmetry\t10\t100\t1\n"
        # rg 102's only release has no status at all — still picked (no Official to lose to)
        "1002\tr-gid-1002\tTogether Again\t11\t102\t\\N\n"
    )
    # medium: id, release, position
    (raw_dir / "medium").write_text(
        "1\t1001\t1\n"  # disc 1 of the chosen (Official) release for rg 100
        "2\t1001\t2\n"  # disc 2 of that same release
        "3\t1000\t1\n"  # a disc of the losing Promotion release — must be ignored entirely
        "4\t1002\t1\n"  # rg 102's only disc
    )
    # track: id, gid, recording, medium, position, number, name, artist_credit, length
    (raw_dir / "track").write_text(
        "1\tt-gid-1\trec-1\t1\t1\t1\tNew Born\t10\t400000\n"
        "2\tt-gid-2\trec-2\t1\t2\t2\tBliss\t10\t350000\n"
        "3\tt-gid-3\trec-3\t2\t1\t1\tDisc2Track1\t10\t200000\n"
        # on the losing Promotion release's disc — must not appear in the output
        "4\tt-gid-4\trec-4\t3\t1\t1\tShouldBeIgnored\t10\t\\N\n"
        "5\tt-gid-5\trec-5\t4\t1\t1\tTogether Again Track\t11\t300000\n"
    )

    # link_type: id, parent, child_order, gid, entity_type0, entity_type1, name
    (raw_dir / "link_type").write_text(
        "1\t\\N\t1\tlt-gid-1\trelease_group\turl\tstreaming\n"
        "2\t\\N\t2\tlt-gid-2\trelease_group\turl\tfree streaming\n"
        # right name, wrong entity types — must not count as a release-group streaming link
        "3\t\\N\t3\tlt-gid-3\tartist\turl\tstreaming\n"
    )
    # link: id, link_type
    (raw_dir / "link").write_text("5000\t1\n5001\t2\n5002\t3\n")
    # l_release_group_url: id, link, entity0 (release_group id), entity1 (url id)
    (raw_dir / "l_release_group_url").write_text(
        "1\t5000\t100\t9000\n"  # rg 100 -> spotify
        "2\t5001\t100\t9001\n"  # rg 100 -> apple music (free streaming)
        "3\t5002\t100\t9002\n"  # wrong link_type (artist-url) — must be dropped
        "4\t5000\t999\t9003\n"  # rg 999 isn't a wanted release group — must be dropped
        "5\t5000\t102\t9004\n"  # rg 102 -> youtube music
        "6\t5000\t100\t9005\n"  # rg 100 -> a non-streaming host — must be dropped
    )
    # url: id, gid, url
    (raw_dir / "url").write_text(
        "9000\tu-gid-9000\thttps://open.spotify.com/album/abc\n"
        "9001\tu-gid-9001\thttps://music.apple.com/us/album/xyz\n"
        "9002\tu-gid-9002\thttps://tidal.com/album/should-be-ignored-anyway\n"
        "9003\tu-gid-9003\thttps://open.spotify.com/album/orphan\n"
        "9004\tu-gid-9004\thttps://music.youtube.com/playlist?list=abc\n"
        "9005\tu-gid-9005\thttps://bandcamp.com/album/def\n"
    )


class BuildIndexTest(unittest.TestCase):
    def test_join_filters_and_writes_expected_albums(self):
        with tempfile.TemporaryDirectory() as tmp:
            raw_dir = Path(tmp) / "raw"
            raw_dir.mkdir()
            output = Path(tmp) / "day_index.sqlite"
            _write_fixtures(raw_dir)

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

    def test_track_table_prefers_official_release_and_orders_multi_disc(self):
        with tempfile.TemporaryDirectory() as tmp:
            raw_dir = Path(tmp) / "raw"
            raw_dir.mkdir()
            output = Path(tmp) / "day_index.sqlite"
            _write_fixtures(raw_dir)

            build_index(raw_dir, output)

            conn = sqlite3.connect(output)
            rows = conn.execute(
                "SELECT release_group_gid, medium_position, position, title, length_ms FROM track "
                "ORDER BY release_group_gid, medium_position, position"
            ).fetchall()
            conn.close()

            self.assertEqual(
                [
                    ("rg-gid-100", 1, 1, "New Born", 400000),
                    ("rg-gid-100", 1, 2, "Bliss", 350000),
                    ("rg-gid-100", 2, 1, "Disc2Track1", 200000),
                    ("rg-gid-102", 1, 1, "Together Again Track", 300000),
                ],
                rows,
            )

    def test_streaming_link_table_filters_and_classifies_by_host(self):
        with tempfile.TemporaryDirectory() as tmp:
            raw_dir = Path(tmp) / "raw"
            raw_dir.mkdir()
            output = Path(tmp) / "day_index.sqlite"
            _write_fixtures(raw_dir)

            build_index(raw_dir, output)

            conn = sqlite3.connect(output)
            rows = conn.execute(
                "SELECT release_group_gid, service, url FROM streaming_link "
                "ORDER BY release_group_gid, service"
            ).fetchall()
            conn.close()

            self.assertEqual(
                [
                    ("rg-gid-100", "appleMusic", "https://music.apple.com/us/album/xyz"),
                    ("rg-gid-100", "spotify", "https://open.spotify.com/album/abc"),
                    ("rg-gid-102", "youtubeMusic", "https://music.youtube.com/playlist?list=abc"),
                ],
                rows,
            )

    def test_genre_table_exists_but_is_always_empty(self):
        # MusicBrainz's public dump has no tag/release_group_tag data to build genres from (see
        # build_genre_index()'s doc comment) — the table still exists so downstream code (the
        # upload script's genre query) doesn't need a special case for "no genre data this run".
        with tempfile.TemporaryDirectory() as tmp:
            raw_dir = Path(tmp) / "raw"
            raw_dir.mkdir()
            output = Path(tmp) / "day_index.sqlite"
            _write_fixtures(raw_dir)

            build_index(raw_dir, output)

            conn = sqlite3.connect(output)
            rows = conn.execute("SELECT * FROM genre").fetchall()
            conn.close()

            self.assertEqual([], rows)


if __name__ == "__main__":
    unittest.main()
