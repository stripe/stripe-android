package com.stripe.android.uicore.presentation

import androidx.annotation.RestrictTo
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.reflect.KProperty1

/**
 * A reactive ViewModel implementation that manages UI state through [StateFlow].
 *
 * [ReactiveStateViewModel] provides a structured way to handle state changes, asynchronous operations,
 * and their results (success, failure, loading) in a type-safe manner. It follows a unidirectional
 * data flow pattern where all state modifications happen through reducers.
 *
 * @param S The type representing the UI state managed by this ViewModel
 * @param initialState The initial state to start with
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
abstract class ReactiveStateViewModel<S>(
    initialState: S,
) : ViewModel() {

    private val _stateFlow: MutableStateFlow<S> = MutableStateFlow(initialState)
    val stateFlow: StateFlow<S> = _stateFlow.asStateFlow()

    /**
     * Executes an asynchronous operation and updates the state based on the result.
     *
     * This method handles the common pattern of showing loading state, executing an operation,
     * and then showing either success or failure state.
     *
     * @param T The type of the result from the asynchronous operation
     * @param retainValue An optional property reference to retain the previous value during loading state
     * @param reducer A function that determines how the state should be updated based on the async result
     * @return A [Job] that can be used to cancel the operation if needed
     */
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

    /**
     * Provides a safe way to access the current state value.
     *
     * @param action A function that will be called with the current state
     */
    protected fun withState(action: (state: S) -> Unit) = stateFlow.value.let(action)

    /**
     * Observes changes to a specific async property within the state and triggers
     * appropriate callbacks based on its status.
     *
     * @param T The type of data wrapped by the Async property
     * @param prop The property reference to the Async field within state to observe
     * @param onSuccess Callback executed when the async operation succeeds
     * @param onFail Callback executed when the async operation fails
     */
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

    /**
     * Updates the current state using a reducer function.
     *
     * The reducer receives the current state and should return a new state.
     * This ensures all state transitions happen in a controlled manner.
     *
     * @param reducer A function that receives the current state and returns a new state
     */
    protected fun setState(reducer: S.() -> S) = _stateFlow.update(reducer)
}

internal fun <A : ReactiveStateViewModel<B>, B, C> withState(viewModel: A, block: (B) -> C) =
    block(viewModel.stateFlow.value)
