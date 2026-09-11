package com.stripe.android.paymentsheet.addresselement

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class AddressElementActivityStateHolder @Inject constructor() {
    private val _state = MutableStateFlow<State>(State.Idle)

    val state: StateFlow<State> = _state.asStateFlow()

    fun tryStartProcessing(): Boolean {
        return _state.compareAndSet(expect = State.Idle, update = State.Processing)
    }

    fun finishProcessing(): Boolean {
        return _state.compareAndSet(expect = State.Processing, update = State.Idle)
    }

    fun complete(result: AddressElementActivityContract.Result.StandaloneSucceeded): Boolean {
        return _state.compareAndSet(expect = State.Idle, update = State.Completed(result))
    }

    fun complete(result: AddressElementActivityContract.Result.CheckoutShippingSucceeded): Boolean {
        return _state.compareAndSet(expect = State.Processing, update = State.Completed(result))
    }

    fun tryCancel(): Boolean {
        return _state.compareAndSet(
            expect = State.Idle,
            update = State.Completed(AddressElementActivityContract.Result.Canceled),
        )
    }

    sealed interface State {
        data object Idle : State
        data object Processing : State
        data class Completed(
            val result: AddressElementActivityContract.Result,
        ) : State
    }
}
