package com.stripe.android.paymentsheet

import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.SavedSelection

internal interface PrefsRepository {
    suspend fun getSavedSelection(): SavedSelection
    fun savePaymentSelection(paymentSelection: PaymentSelection?)

    class Noop : PrefsRepository {
        override suspend fun getSavedSelection(): SavedSelection = SavedSelection.None
        override fun savePaymentSelection(paymentSelection: PaymentSelection?) {}
    }
}
