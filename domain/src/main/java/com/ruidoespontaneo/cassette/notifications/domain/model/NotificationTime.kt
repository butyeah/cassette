package com.ruidoespontaneo.cassette.notifications.domain.model

/** The time of day the daily reminder notification should fire, in 24-hour clock. */
data class NotificationTime(
    val hour: Int,
    val minute: Int
)
