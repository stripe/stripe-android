package com.stripe.android.paymentsheet

import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.SavedSelection

internal class FakePrefsRepository : PrefsRepository {
    private var savedSelection: SavedSelection? = null

    override suspend fun getSavedSelection(): SavedSelection? = savedSelection

    override fun savePaymentSelection(paymentSelection: PaymentSelection?) {
        when (paymentSelection) {
            PaymentSelection.GooglePay -> {
                SavedSelection.GooglePay
            }
            is PaymentSelection.Saved -> {
                SavedSelection.PaymentMethod(paymentSelection.paymentMethod.id.orEmpty())
            }
            else -> null
        }.let {
            savedSelection = it
        }
    }
}
