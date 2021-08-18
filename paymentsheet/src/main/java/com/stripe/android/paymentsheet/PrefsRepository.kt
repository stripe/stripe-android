package com.stripe.android.paymentsheet

import com.stripe.android.paymentsheet.model.PaymentSelection

internal interface PrefsRepository {
    suspend fun getSavedSelection(isGooglePayAvailable: Boolean): com.stripe.android.paymentsheet.model.SavedSelection
    fun savePaymentSelection(paymentSelection: PaymentSelection?)

    class Noop : PrefsRepository {
        override suspend fun getSavedSelection(isGooglePayAvailable: Boolean): com.stripe.android.paymentsheet.model.SavedSelection =
            com.stripe.android.paymentsheet.model.SavedSelection.None

        override fun savePaymentSelection(paymentSelection: PaymentSelection?) {}
    }
}
