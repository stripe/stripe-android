package com.stripe.android.financialconnections.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator.Message.UpdateTopAppBar
import com.stripe.android.financialconnections.navigation.topappbar.TopAppBarStateUpdate
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
    initialState: S,
    private val nativeAuthFlowCoordinator: NativeAuthFlowCoordinator,
) : ViewModel() {

    private val _stateFlow: MutableStateFlow<S> = MutableStateFlow(initialState)
    val stateFlow: StateFlow<S> = _stateFlow.asStateFlow()

    init {
        updateHostWithTopAppBarState(initialState)
        viewModelScope.launch {
            stateFlow.collect(::updateHostWithTopAppBarState)
        }
    }

    abstract fun updateTopAppBar(state: S): TopAppBarStateUpdate?

    private fun updateHostWithTopAppBarState(state: S) {
        viewModelScope.launch {
            val update = updateTopAppBar(state) ?: return@launch
            nativeAuthFlowCoordinator().emit(UpdateTopAppBar(update))
        }
    }

    protected open fun <T : Any?> (suspend () -> T).execute(
        retainValue: KProperty1<S, Async<T>>? = null,
        reducer: S.(Async<T>) -> S,
    ): Job {
        return viewModelScope.launch {
            setState { reducer(Async.Loading(value = retainValue?.get(this)?.invoke())) }
            val result = runCatching { this@execute() }
            // update state.
            result.fold(
                onSuccess = { data ->
                    setState { reducer(Async.Success(data)) }
                },
                onFailure = { throwable ->
                    setState { reducer(Async.Fail(throwable)) }
                }
            )
        }
    }

    protected fun withState(action: (state: S) -> Unit) = stateFlow.value.let(action)

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
                        is Async.Success -> onSuccess(async())
                        is Async.Fail -> onFail(async.error)
                        is Async.Loading -> Unit
                        Async.Uninitialized -> Unit
                    }
                }
        }
    }

    protected fun setState(reducer: S.() -> S) = _stateFlow.update(reducer)
}

internal fun <A : FinancialConnectionsViewModel<B>, B, C> withState(viewModel: A, block: (B) -> C) =
    block(viewModel.stateFlow.value)
