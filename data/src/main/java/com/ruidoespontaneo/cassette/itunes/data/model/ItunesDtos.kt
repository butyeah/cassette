package com.ruidoespontaneo.cassette.itunes.data.model

import com.squareup.moshi.JsonClass

/** Response body of iTunes' `GET /lookup` and `GET /search`. */
@JsonClass(generateAdapter = true)
data class ItunesResponse(
    val results: List<ItunesResultDto> = emptyList()
)

/**
 * One row of an [ItunesResponse]. A lookup mixes row kinds: a `wrapperType == "collection"` row
 * describing the album, followed by `wrapperType == "track"` rows for its songs. Every field is
 * optional because each kind only fills in its own.
 */
@JsonClass(generateAdapter = true)
data class ItunesResultDto(
    val wrapperType: String? = null,
    val kind: String? = null,
    val collectionId: Long? = null,
    val collectionName: String? = null,
    val artistName: String? = null,
    val trackName: String? = null,
    val trackNumber: Int? = null,
    val discNumber: Int? = null,
    val previewUrl: String? = null
)
