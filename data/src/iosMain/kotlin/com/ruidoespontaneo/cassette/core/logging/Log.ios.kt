package com.ruidoespontaneo.cassette.core.logging

internal actual fun logError(throwable: Throwable, message: String) {
    println("E/Cassette: $message: $throwable")
}
