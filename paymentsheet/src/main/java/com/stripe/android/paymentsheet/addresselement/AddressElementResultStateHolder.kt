package com.stripe.android.paymentsheet.addresselement

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class AddressElementResultStateHolder @Inject constructor() {
    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    fun tryStartSaving(): Boolean {
        return _state.compareAndSet(expect = State.Idle, update = State.Saving)
    }

    fun onSaveFailed() {
        _state.compareAndSet(expect = State.Saving, update = State.Idle)
    }

    fun onSaveCompleted(result: AddressElementActivityContract.Result) {
        _state.compareAndSet(expect = State.Saving, update = State.Finished(result))
    }

    fun onUserCancel(): Boolean {
        return _state.compareAndSet(
            expect = State.Idle,
            update = State.Finished(AddressElementActivityContract.Result.Canceled),
        )
    }

    sealed interface State {
        data object Idle : State

        data object Saving : State

        data class Finished(val result: AddressElementActivityContract.Result) : State
    }
}
