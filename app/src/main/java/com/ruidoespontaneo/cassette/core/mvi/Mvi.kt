package com.ruidoespontaneo.cassette.core.mvi

/**
 * Marker for a screen's state: a single immutable snapshot of everything the UI
 * needs to render. There is exactly one instance alive at a time per screen.
 */
interface UiState

/** Marker for a user action or external event a screen's ViewModel can react to. */
interface UiIntent

/**
 * Marker for a one-off side effect (navigation, a snackbar, ...) that the UI
 * should consume exactly once, as opposed to state, which can be re-rendered
 * any number of times.
 */
interface UiEffect
