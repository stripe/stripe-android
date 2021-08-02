package com.stripe.android.paymentsheet

import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.SavedSelection

internal interface PrefsRepository {
    suspend fun getSavedSelection(isGooglePayAvailable: Boolean): SavedSelection
    fun savePaymentSelection(paymentSelection: PaymentSelection?)

    class Noop : PrefsRepository {
        override suspend fun getSavedSelection(isGooglePayAvailable: Boolean): SavedSelection = SavedSelection.None
        override fun savePaymentSelection(paymentSelection: PaymentSelection?) {}
    }
}
