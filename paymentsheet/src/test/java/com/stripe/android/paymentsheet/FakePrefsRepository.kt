package com.stripe.android.paymentsheet

import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.SavedSelection

internal class FakePrefsRepository(
    private val setSavedSelectionResult: Boolean = true,
) : PrefsRepository {
    internal val paymentSelectionArgs = mutableListOf<PaymentSelection?>()

    private var savedSelection: SavedSelection = SavedSelection.None

    override suspend fun getSavedSelection(isGooglePayAvailable: Boolean, isLinkAvailable: Boolean): SavedSelection =
        savedSelection

    override fun setSavedSelection(savedSelection: SavedSelection?): Boolean {
        savedSelection?.let {
            this.savedSelection = it
        }
        return setSavedSelectionResult
    }

    override fun savePaymentSelection(paymentSelection: PaymentSelection?) {
        paymentSelectionArgs.add(paymentSelection)

        when (paymentSelection) {
            is PaymentSelection.Link -> {
                SavedSelection.Link
            }
            PaymentSelection.GooglePay -> {
                SavedSelection.GooglePay
            }
            is PaymentSelection.Saved -> {
                SavedSelection.PaymentMethod(paymentSelection.paymentMethod.id.orEmpty())
            }
            is PaymentSelection.New.Card,
            is PaymentSelection.New.GenericPaymentMethod,
            is PaymentSelection.New.LinkInline,
            is PaymentSelection.New.USBankAccount,
            is PaymentSelection.ExternalPaymentMethod,
            is PaymentSelection.CustomPaymentMethod,
            null -> SavedSelection.None
        }.let {
            savedSelection = it
        }
    }
}
