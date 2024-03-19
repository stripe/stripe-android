package com.stripe.android.financialconnections.core

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stripe.android.financialconnections.presentation.FinancialConnectionsSheetNativeViewModel
import com.stripe.android.financialconnections.ui.FinancialConnectionsSheetNativeActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

abstract class FinancialConnectionsViewModel<S>(
    initialState: S
) : ViewModel() {

    val stateFlow: MutableStateFlow<S> = MutableStateFlow(initialState)
}

data class Result<T>(
    val loading: Boolean = false,
    val data: T? = null,
    val error: Throwable? = null
) {
    operator fun invoke(): T? = data
}


fun <T> FinancialConnectionsViewModel<*>.executeAsync(
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