package com.stripe.android.checkout

import androidx.lifecycle.SavedStateHandle
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class ShippingAddressElementStateHolder @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
) {
    var isPresenting: Boolean
        get() = savedStateHandle.get<Boolean>(IS_PRESENTING_KEY) == true
        set(value) {
            savedStateHandle[IS_PRESENTING_KEY] = value
        }

    private companion object {
        const val IS_PRESENTING_KEY = "ShippingAddressElementStateHolder_IS_PRESENTING_KEY"
    }
}
