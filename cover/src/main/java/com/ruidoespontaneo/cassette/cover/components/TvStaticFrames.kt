package com.ruidoespontaneo.cassette.cover.components

/** How many frames [TvStatic] cycles through. */
const val TV_STATIC_FRAME_COUNT = 6

/** Each frame's width and height, in pixels, before it's scaled up. */
const val TV_STATIC_FRAME_SIZE = 64

/** Fixed, so the static looks the same on every launch, and on iOS. */
const val TV_STATIC_SEED = 0xCA55E77EL

/**
 * [count] frames of [size]×[size] grey levels (0..255), row by row: a random brightness each, with
 * every other row darkened to three quarters for scanlines. Drawn from [SplitMix64], the same way
 * iOS's TVStatic.swift does, so both apps show the same snow.
 */
fun tvStaticFrames(
    count: Int = TV_STATIC_FRAME_COUNT,
    size: Int = TV_STATIC_FRAME_SIZE,
    seed: Long = TV_STATIC_SEED
): List<IntArray> {
    val random = SplitMix64(seed)
    return List(count) {
        IntArray(size * size) { index ->
            val level = (random.next() ushr 56).toInt()
            val isScanline = (index / size) % 2 == 1
            if (isScanline) level * 3 / 4 else level
        }
    }
}

/** [tvStaticFrames]' grey levels as opaque ARGB pixels. */
fun IntArray.toArgbPixels(): IntArray = IntArray(size) { i ->
    val level = this[i]
    (0xFF shl 24) or (level shl 16) or (level shl 8) or level
}

/** SplitMix64: small, fast, and simple to write the same way in Swift. */
class SplitMix64(seed: Long) {
    private var state = seed

    fun next(): Long {
        state += -0x61c8864680b583ebL // 0x9E3779B97F4A7C15
        var z = state
        z = (z xor (z ushr 30)) * -0x40a7b892e31b1a47L // 0xBF58476D1CE4E5B9
        z = (z xor (z ushr 27)) * -0x6b2fb644ecceee15L // 0x94D049BB133111EB
        return z xor (z ushr 31)
    }
}
