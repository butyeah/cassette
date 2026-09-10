package com.ruidoespontaneo.cassette.core.mvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Base class for a screen's ViewModel in an MVI (Model-View-Intent) setup.
 *
 * The screen observes [state] to render itself and [effect] to consume one-off
 * events (navigation, a snackbar, ...); it drives the ViewModel by calling
 * [onIntent] with a [UiIntent]. Subclasses implement [onIntent] to translate
 * each intent into calls to [setState] and/or [sendEffect].
 */
abstract class MviViewModel<S : UiState, I : UiIntent, E : UiEffect>(
    initialState: S
) : ViewModel() {

    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<S> = _state.asStateFlow()

    private val _effect = Channel<E>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    protected val currentState: S
        get() = _state.value

    abstract fun onIntent(intent: I)

    protected fun setState(reduce: S.() -> S) {
        _state.update(reduce)
    }

    protected fun sendEffect(builder: () -> E) {
        viewModelScope.launch { _effect.send(builder()) }
    }
}
