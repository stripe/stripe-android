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


internal fun <T> FinancialConnectionsViewModel<*>.execute(
    block: suspend () -> T,
    onResultUpdated: (Result<T>) -> Unit,
    onSuccess: (T) -> Unit = {},
    onFail: (Throwable) -> Unit = {}
) {
    onResultUpdated(Result(loading = true))
    viewModelScope.launch {
        val result = runCatching { block() }
        onResultUpdated(
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