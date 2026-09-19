package com.ruidoespontaneo.cassette.albumdetail.preview

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Drives the real ExoPlayer against generated silent WAV files, so it needs no network and no
 * dependence on any iTunes URL staying valid. ExoPlayer is main-thread-only, so every player call
 * goes through [onMain]; waiting on [PreviewPlayer.playback] is thread-safe and stays on the test
 * thread.
 */
class Media3PreviewPlayerTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context: Context = instrumentation.targetContext
    private lateinit var player: Media3PreviewPlayer

    @Before
    fun setUp() {
        player = Media3PreviewPlayer(context)
    }

    @After
    fun tearDown() {
        onMain { player.stop() }
    }

    @Test
    fun playsThenResetsToIdleWhenTheClipEnds() {
        val url = silentWav("short", seconds = 1)

        onMain { player.play(url) }
        assertEquals(PreviewPlayback(url, PreviewPlayback.Status.Loading), player.playback.value)

        awaitPlayback { it?.url == url && it.status == PreviewPlayback.Status.Playing }
        awaitPlayback { it == null }
    }

    @Test
    fun stopReturnsToIdleMidClip() {
        val url = silentWav("long", seconds = 30)
        onMain { player.play(url) }
        awaitPlayback { it?.status == PreviewPlayback.Status.Playing }

        onMain { player.stop() }

        assertNull(player.playback.value)
    }

    @Test
    fun startingASecondClipReplacesTheFirst() {
        val first = silentWav("first", seconds = 30)
        val second = silentWav("second", seconds = 30)
        onMain { player.play(first) }
        awaitPlayback { it?.url == first && it.status == PreviewPlayback.Status.Playing }

        onMain { player.play(second) }

        awaitPlayback { it?.url == second && it.status == PreviewPlayback.Status.Playing }
        assertEquals(second, player.playback.value?.url)
    }

    @Test
    fun anUnplayableUrlResetsToIdleInsteadOfStickingOnLoading() {
        val missing = File(context.cacheDir, "does-not-exist.wav").toURI().toString()

        onMain { player.play(missing) }

        awaitPlayback { it == null }
    }

    private fun onMain(block: () -> Unit) = instrumentation.runOnMainSync(block)

    private fun awaitPlayback(condition: (PreviewPlayback?) -> Boolean) = runBlocking {
        withTimeout(TIMEOUT_MS) { player.playback.first(condition) }
    }

    /** A mono 8 kHz 16-bit PCM WAV of silence, as a `file://` URI string. */
    private fun silentWav(name: String, seconds: Int): String {
        val sampleRate = 8_000
        val dataSize = sampleRate * 2 * seconds
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray()); putInt(36 + dataSize); put("WAVE".toByteArray())
            put("fmt ".toByteArray()); putInt(16); putShort(1); putShort(1)
            putInt(sampleRate); putInt(sampleRate * 2); putShort(2); putShort(16)
            put("data".toByteArray()); putInt(dataSize)
        }
        val file = File(context.cacheDir, "$name.wav")
        file.outputStream().use { it.write(header.array()); it.write(ByteArray(dataSize)) }
        return file.toURI().toString()
    }

    private companion object {
        const val TIMEOUT_MS = 10_000L
    }
}
