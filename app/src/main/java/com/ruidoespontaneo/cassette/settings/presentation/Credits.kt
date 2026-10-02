package com.ruidoespontaneo.cassette.settings.presentation

import androidx.annotation.StringRes
import com.ruidoespontaneo.cassette.R

/** One source the app's data comes from: what it gives, under which licence, and where it lives. */
data class Credit(
    @StringRes val nameRes: Int,
    @StringRes val contributionRes: Int,
    @StringRes val licenseRes: Int,
    val url: String
)

/** Every source credited on the Credits page, in the order the album page uses them. */
val credits = listOf(
    Credit(R.string.credits_musicbrainz, R.string.credits_musicbrainz_gives, R.string.credits_musicbrainz_license, "https://musicbrainz.org"),
    Credit(R.string.credits_wikidata, R.string.credits_wikidata_gives, R.string.credits_wikidata_license, "https://www.wikidata.org"),
    Credit(R.string.credits_wikipedia, R.string.credits_wikipedia_gives, R.string.credits_wikipedia_license, "https://www.wikipedia.org")
)
