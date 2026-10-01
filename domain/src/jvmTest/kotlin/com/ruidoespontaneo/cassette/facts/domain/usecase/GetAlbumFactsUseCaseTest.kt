package com.ruidoespontaneo.cassette.facts.domain.usecase

import com.ruidoespontaneo.cassette.facts.domain.AlbumFactsRepository
import com.ruidoespontaneo.cassette.facts.domain.model.AlbumFacts
import com.ruidoespontaneo.cassette.musicbrainz.domain.model.AlbumDetail
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class GetAlbumFactsUseCaseTest {

    @Test
    fun `asks by the album's release-group id, in the given language`() = runBlocking {
        val calls = mutableListOf<Pair<String, String>>()
        val facts = AlbumFacts(producers = listOf("Nigel Godrich"))
        val useCase = GetAlbumFactsUseCase(
            object : AlbumFactsRepository {
                override suspend fun getAlbumFacts(releaseGroupId: String, language: String): Result<AlbumFacts?> {
                    calls += releaseGroupId to language
                    return Result.success(facts)
                }
            }
        )
        val album = AlbumDetail(
            id = "b1392450-e666-3926-a536-22c65f834433",
            title = "OK Computer",
            artistName = "Radiohead",
            primaryType = "Album",
            firstReleaseDate = LocalDate(1997, 5, 21),
            genres = emptyList(),
            ratingValue = null,
            ratingVotesCount = 0
        )

        assertEquals(facts, useCase(album, language = "es").getOrNull())
        assertEquals(listOf("b1392450-e666-3926-a536-22c65f834433" to "es"), calls)
    }

    @Test
    fun `facts with nothing in them are empty`() {
        assertEquals(true, AlbumFacts().isEmpty)
        assertEquals(false, AlbumFacts(awards = listOf("Grammy")).isEmpty)
    }
}
