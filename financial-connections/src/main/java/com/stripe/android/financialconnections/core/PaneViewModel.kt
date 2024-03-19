package com.stripe.android.financialconnections.core

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal abstract class PaneViewModel<S>(
    initialState: S
) : ViewModel() {

    val stateFlow: MutableStateFlow<S> = MutableStateFlow(initialState)

    protected open fun <T : Any?> (suspend () -> T).execute(
        reducer: S.(Result<T>) -> S,
        onSuccess: (T) -> Unit = {},
        onFail: (Throwable) -> Unit = {}
    ): Job {
        return viewModelScope.launch {
            stateFlow.update { state -> state.reducer(Result.Loading) }
            val result = kotlin.runCatching { this@execute() }
            // update state.
            result.fold(
                onSuccess = { data ->
                    stateFlow.update { state -> state.reducer(Result.Success(data)) }
                    onSuccess(data)
                },
                onFailure = { throwable ->
                    stateFlow.update { state -> state.reducer(Result.Error(throwable)) }
                    onFail(throwable)
                }
            )
        }
    }
}

internal sealed class Result<out T>(
    private val value: T?
) {
    data object Uninitialized : Result<Nothing>(value = null)
    data object Loading : Result<Nothing>(value = null)
    data class Success<out T>(val value: T) : Result<T>(value = value)
    data class Error<out T>(val throwable: Throwable) : Result<T>(value = null)

    open operator fun invoke(): T? = value
}
