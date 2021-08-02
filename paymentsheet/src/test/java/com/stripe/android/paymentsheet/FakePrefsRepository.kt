package com.stripe.android.paymentsheet

import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.SavedSelection

internal class FakePrefsRepository : PrefsRepository {
    internal val paymentSelectionArgs = mutableListOf<PaymentSelection?>()

    private var savedSelection: SavedSelection = SavedSelection.None

    override suspend fun getSavedSelection(): SavedSelection = savedSelection

    override fun savePaymentSelection(paymentSelection: PaymentSelection?) {
        paymentSelectionArgs.add(paymentSelection)

        when (paymentSelection) {
            PaymentSelection.GooglePay -> {
                SavedSelection.GooglePay
            }
            is PaymentSelection.Saved -> {
                SavedSelection.PaymentMethod(paymentSelection.paymentMethod.id.orEmpty())
            }
            else -> SavedSelection.None
        }.let {
            savedSelection = it
        }
    }
}
