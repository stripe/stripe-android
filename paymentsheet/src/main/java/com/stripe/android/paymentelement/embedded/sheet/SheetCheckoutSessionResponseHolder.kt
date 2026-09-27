package com.stripe.android.paymentelement.embedded.sheet

import androidx.lifecycle.SavedStateHandle
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class SheetCheckoutSessionResponseHolder @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
) {
    val response: CheckoutSessionResponse?
        get() = savedStateHandle[CHECKOUT_SESSION_RESPONSE_KEY]

    fun set(response: CheckoutSessionResponse?) {
        savedStateHandle[CHECKOUT_SESSION_RESPONSE_KEY] = response
    }

    companion object {
        internal const val CHECKOUT_SESSION_RESPONSE_KEY = "SHEET_CHECKOUT_SESSION_RESPONSE"
    }
}
