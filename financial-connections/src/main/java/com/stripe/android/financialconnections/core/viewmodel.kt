package com.stripe.android.financialconnections.core

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

internal abstract class FinancialConnectionsViewModel<S>(
    initialState: S
) : ViewModel() {

    val stateFlow: MutableStateFlow<S> = MutableStateFlow(initialState)
}

internal data class Result<T>(
    val loading: Boolean = false,
    val data: T? = null,
    val error: Throwable? = null
) {
    operator fun invoke(): T? = data
}


internal fun <T> FinancialConnectionsViewModel<*>.executeAsync(
    block: suspend () -> T,
    updateAsync: (Result<T>) -> Unit,
    onSuccess: (T) -> Unit = {},
    onFail: (Throwable) -> Unit = {}
) {
    updateAsync(Result(loading = true))
    viewModelScope.launch {
        val result = runCatching { block() }
        updateAsync(
            Result(
                loading = false,
                data = result.getOrNull(),
                error = result.exceptionOrNull()
            )
        )
        result
            .onSuccess { onSuccess(it) }
            .onFailure { onFail(it) }
    }
}