package com.ruidoespontaneo.cassette.dayinhistory.presentation

import java.time.LocalDate
import java.time.MonthDay
import java.time.ZoneOffset

// MonthDay carries no year, but DatePickerState needs a UTC millis timestamp — pick whatever
// nearby year makes this day valid (matters only for Feb 29 landing in a non-leap year).
fun MonthDay.toUtcMillis(): Long {
    val year = nearestValidYear(this, LocalDate.now().year)
    return atYear(year).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
}

private fun nearestValidYear(day: MonthDay, from: Int): Int =
    generateSequence(from) { it + 1 }.first { day.isValidYear(it) }
