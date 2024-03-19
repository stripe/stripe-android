package com.stripe.android.financialconnections.core

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal abstract class FinancialConnectionsViewModel<S>(
    initialState: S
) : ViewModel() {

    val stateFlow: MutableStateFlow<S> = MutableStateFlow(initialState)

    protected open fun <T : Any?> (suspend () -> T).execute(
        reducer: S.(Result<T>) -> S,
        onSuccess: (T) -> Unit = {},
        onFail: (Throwable) -> Unit = {}
    ): Job {
        return viewModelScope.launch {
            stateFlow.update { state -> state.reducer(Result(loading = true)) }
            val result = kotlin.runCatching { this@execute() }
            // update state.
            stateFlow.update { state ->
                state.reducer(
                    Result(
                        loading = false,
                        data = result.getOrNull(),
                        error = result.exceptionOrNull()
                    )
                )
            }
            // trigger side effects.
            result
                .onSuccess(onSuccess)
                .onFailure(onFail)
        }
    }
}

internal data class Result<T>(
    val loading: Boolean = false,
    val data: T? = null,
    val error: Throwable? = null
) {
    operator fun invoke(): T? = data
}
