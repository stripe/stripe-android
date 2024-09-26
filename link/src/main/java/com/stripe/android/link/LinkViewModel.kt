package com.stripe.android.link

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal abstract class LinkViewModel<State, Action, Result, Effect>(
    initialState: State,
) : ViewModel() {
    private val actionChannel = Channel<Action>(Channel.UNLIMITED)

    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<State> = _state

    private val _effect = MutableSharedFlow<Effect>()
    val effect: Flow<Effect> = _effect

    init {
        viewModelScope.launch {
            actionChannel.receiveAsFlow()
                .flatMapConcat(::actionToResult)
                .collect { result ->
                    _state.update { currentState ->
                        resultToState(currentState, result)
                    }
                    resultToEffect(result)?.let { newEffect ->
                        _effect.emit(newEffect)
                    }
                }
        }
    }

    fun handleAction(action: Action) {
        actionChannel.trySend(action)
    }

    protected abstract fun actionToResult(action: Action): Flow<Result>

    protected abstract fun resultToState(currentState: State, result: Result): State

    protected abstract fun resultToEffect(result: Result): Effect?
}
