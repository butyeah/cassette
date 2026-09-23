package com.ruidoespontaneo.cassette.albumdetail.presentation

private const val UNKNOWN_TIME = "--:--"

/**
 * The preview display's countdown, `mm:ss`. Rounds up so a clip starts on its full length (`00:30`,
 * not `00:29`) and only reads `00:00` once it's actually over; `--:--` while nothing is playing or
 * the clip's duration isn't known yet.
 */
fun remainingTimeText(remainingMs: Long?): String {
    if (remainingMs == null) return UNKNOWN_TIME
    val totalSeconds = (remainingMs.coerceAtLeast(0) + 999) / 1000
    return "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
