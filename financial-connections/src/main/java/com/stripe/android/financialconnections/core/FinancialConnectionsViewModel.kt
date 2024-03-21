package com.stripe.android.financialconnections.core

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stripe.android.financialconnections.core.Async.Fail
import com.stripe.android.financialconnections.core.Async.Loading
import com.stripe.android.financialconnections.core.Async.Success
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal abstract class FinancialConnectionsViewModel<S>(
    initialState: S
) : ViewModel() {

    private val _stateFlow: MutableStateFlow<S> = MutableStateFlow(initialState)
    val stateFlow: StateFlow<S> = _stateFlow

    protected open fun <T : Any?> (suspend () -> T).execute(
        reducer: S.(Async<T>) -> S,
        onSuccess: (T) -> Unit = {},
        onFail: (Throwable) -> Unit = {}
    ): Job {
        return viewModelScope.launch {
            setState { reducer(Loading) }
            val result = runCatching { this@execute() }
            // update state.
            result.fold(
                onSuccess = { data ->
                    setState { reducer(Success(data)) }
                    onSuccess(data)
                },
                onFailure = { throwable ->
                    setState { reducer(Fail(throwable)) }
                    onFail(throwable)
                }
            )
        }
    }

    protected fun setState(reducer: S.() -> S) = _stateFlow.update(reducer)
}

internal sealed class Async<out T>(
    private val value: T?
) {
    data object Uninitialized : Async<Nothing>(value = null)
    data object Loading : Async<Nothing>(value = null)
    data class Success<out T>(private val value: T) : Async<T>(value = value) {
        override operator fun invoke(): T = value
    }
    data class Fail<out T>(val error: Throwable) : Async<T>(value = null)

    open operator fun invoke(): T? = value
}
