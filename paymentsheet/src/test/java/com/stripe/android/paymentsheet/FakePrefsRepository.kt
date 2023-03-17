package com.stripe.android.paymentsheet

import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.SavedSelection

internal class FakePrefsRepository : PrefsRepository {
    internal val paymentSelectionArgs = mutableListOf<PaymentSelection?>()

    private var savedSelection: SavedSelection = SavedSelection.None

    override suspend fun getSavedSelection(isGooglePayAvailable: Boolean): SavedSelection =
        savedSelection

    override fun savePaymentSelection(paymentSelection: PaymentSelection?) {
        paymentSelectionArgs.add(paymentSelection)

        when (paymentSelection) {
            PaymentSelection.GooglePay -> {
                SavedSelection.GooglePay
            }
            is PaymentSelection.Saved -> {
                SavedSelection.PaymentMethod(paymentSelection.paymentMethod.id.orEmpty())
            }
            is PaymentSelection.New.Card,
            is PaymentSelection.New.GenericPaymentMethod,
            is PaymentSelection.New.USBankAccount,
            null -> SavedSelection.None
        }.let {
            savedSelection = it
        }
    }
}
