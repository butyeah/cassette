package com.ruidoespontaneo.cassette.core.network

import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RateLimiterTest {

    @Test
    fun `spaces calls at least the interval apart`() = runTest {
        val limiter = RateLimiter(minInterval = 1.seconds, timeSource = testScheduler.timeSource)
        val startedAt = mutableListOf<Long>()

        repeat(3) {
            limiter.awaitTurn()
            startedAt += testScheduler.currentTime
        }

        assertEquals(listOf(0L, 1_000L, 2_000L), startedAt)
    }

    @Test
    fun `doesn't wait once the interval has already passed`() = runTest {
        val limiter = RateLimiter(minInterval = 1.seconds, timeSource = testScheduler.timeSource)

        limiter.awaitTurn()
        testScheduler.advanceTimeBy(5_000)
        val before = testScheduler.currentTime
        limiter.awaitTurn()

        assertEquals(before, testScheduler.currentTime)
    }
}
