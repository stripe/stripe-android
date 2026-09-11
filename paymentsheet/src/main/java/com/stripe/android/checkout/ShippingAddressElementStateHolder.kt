package com.stripe.android.checkout

import androidx.lifecycle.SavedStateHandle
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class ShippingAddressElementStateHolder @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
) : AwaitingReadyState {
    override var isAwaitingReady: Boolean
        get() = savedStateHandle.get<Boolean>(IS_AWAITING_READY_KEY) == true
        set(value) = savedStateHandle.set(IS_AWAITING_READY_KEY, value)

    private companion object {
        const val IS_AWAITING_READY_KEY = "ShippingAddressElementStateHolder_IS_AWAITING_READY_KEY"
    }
}
