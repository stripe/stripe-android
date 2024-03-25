package com.stripe.android.financialconnections.core

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stripe.android.financialconnections.core.Async.Fail
import com.stripe.android.financialconnections.core.Async.Loading
import com.stripe.android.financialconnections.core.Async.Success
import com.stripe.android.financialconnections.core.Async.Uninitialized
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.reflect.KProperty1

internal abstract class FinancialConnectionsViewModel<S>(
    initialState: S
) : ViewModel() {

    private val _stateFlow: MutableStateFlow<S> = MutableStateFlow(initialState)
    val stateFlow: StateFlow<S> = _stateFlow.asStateFlow()

    protected open fun <T : Any?> (suspend () -> T).execute(
        retainValue: KProperty1<S, Async<T>>? = null,
        reducer: S.(Async<T>) -> S,
    ): Job {
        return viewModelScope.launch {
            setState { reducer(Loading(value = retainValue?.get(this)?.invoke())) }
            val result = runCatching { this@execute() }
            // update state.
            result.fold(
                onSuccess = { data ->
                    setState { reducer(Success(data)) }
                },
                onFailure = { throwable ->
                    setState { reducer(Fail(throwable)) }
                }
            )
        }
    }

    protected open fun <T> onAsync(
        prop: KProperty1<S, Async<T>>,
        onSuccess: suspend (T) -> Unit = {},
        onFail: suspend (Throwable) -> Unit = {}
    ) {
        viewModelScope.launch {
            stateFlow.map { prop.get(it) }
                .distinctUntilChanged()
                .collect { async ->
                    when (async) {
                        is Success -> onSuccess(async())
                        is Fail -> onFail(async.error)
                        is Loading -> Unit
                        Uninitialized -> Unit
                    }
                }
        }
    }

    protected fun setState(reducer: S.() -> S) = _stateFlow.update(reducer)
}

internal sealed class Async<out T>(
    private val value: T?
) {
    data object Uninitialized : Async<Nothing>(value = null)
    data class Loading<T>(val value: T? = null) : Async<T>(value = value)
    data class Success<out T>(private val value: T) : Async<T>(value = value) {
        override operator fun invoke(): T = value
    }

    data class Fail<out T>(val error: Throwable) : Async<T>(value = null)

    open operator fun invoke(): T? = value
}
