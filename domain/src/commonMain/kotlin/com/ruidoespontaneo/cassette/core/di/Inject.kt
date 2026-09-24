package com.ruidoespontaneo.cassette.core.di

/**
 * `javax.inject.Inject` isn't multiplatform, so use cases are annotated with this instead. On the
 * JVM it's a typealias for the real annotation, which is what Hilt reads; on iOS it's inert, and
 * the use cases are built by hand.
 */
@Target(AnnotationTarget.CONSTRUCTOR)
expect annotation class Inject()
