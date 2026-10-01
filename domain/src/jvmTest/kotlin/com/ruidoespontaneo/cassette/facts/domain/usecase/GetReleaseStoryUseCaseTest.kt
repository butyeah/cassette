package com.ruidoespontaneo.cassette.facts.domain.usecase

import com.ruidoespontaneo.cassette.facts.domain.model.AlbumFacts
import com.ruidoespontaneo.cassette.facts.domain.model.ReleaseStory
import com.ruidoespontaneo.cassette.facts.domain.model.ReleaseStory.Phrase
import com.ruidoespontaneo.cassette.musicbrainz.domain.model.AlbumDetail
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GetReleaseStoryUseCaseTest {

    private val useCase = GetReleaseStoryUseCase()
    private val released = LocalDate(1997, 5, 21)

    private fun album(artist: String = "Radiohead", date: LocalDate? = released) = AlbumDetail(
        id = "mbid",
        title = "OK Computer",
        artistName = artist,
        primaryType = "Album",
        firstReleaseDate = date,
        genres = emptyList(),
        ratingValue = null,
        ratingVotesCount = 0
    )

    private fun phrase(labels: List<String>, producers: List<String>, artist: String = "Radiohead") =
        useCase(album(artist), AlbumFacts(labels = labels, producers = producers))?.phrase

    @Test
    fun `with a label, the producer picks the phrase`() {
        val label = listOf("Parlophone")
        assertEquals(Phrase.LabelAndProducer, phrase(label, listOf("Nigel Godrich")))
        assertEquals(Phrase.LabelSelfProduced, phrase(label, listOf("Radiohead")))
        assertEquals(Phrase.LabelAndProducers, phrase(label, listOf("Nigel Godrich", "John Leckie")))
        assertEquals(Phrase.LabelOnly, phrase(label, emptyList()))
    }

    @Test
    fun `without a label, the producer picks the phrase`() {
        assertEquals(Phrase.Producer, phrase(emptyList(), listOf("Nigel Godrich")))
        assertEquals(Phrase.SelfProduced, phrase(emptyList(), listOf("Radiohead")))
        assertEquals(Phrase.Producers, phrase(emptyList(), listOf("Nigel Godrich", "John Leckie")))
        assertEquals(Phrase.DateOnly, phrase(emptyList(), emptyList()))
    }

    @Test
    fun `carries everything the sentence needs`() {
        val story = useCase(album(), AlbumFacts(labels = listOf("Parlophone", "Capitol"), producers = listOf("Nigel Godrich")))

        assertEquals(
            ReleaseStory(Phrase.LabelAndProducer, released, "Radiohead", listOf("Parlophone", "Capitol"), listOf("Nigel Godrich")),
            story
        )
    }

    @Test
    fun `no facts at all is the date alone`() {
        assertEquals(Phrase.DateOnly, useCase(album(), facts = null)?.phrase)
    }

    @Test
    fun `the artist is matched despite case, accents and spacing`() {
        assertEquals(Phrase.SelfProduced, phrase(emptyList(), listOf("Björk"), artist = "bjork"))
        assertEquals(Phrase.SelfProduced, phrase(emptyList(), listOf(" PRINCE "), artist = "Prince"))
        assertEquals(Phrase.SelfProduced, phrase(emptyList(), listOf("Café  Tacvba"), artist = "Café Tacvba"))
    }

    @Test
    fun `the artist among several producers is several, not self-produced`() {
        assertEquals(Phrase.Producers, phrase(emptyList(), listOf("Radiohead", "Nigel Godrich")))
    }

    @Test
    fun `no precise date is no story`() {
        assertNull(useCase(album(date = null), AlbumFacts(labels = listOf("Parlophone"))))
    }
}
