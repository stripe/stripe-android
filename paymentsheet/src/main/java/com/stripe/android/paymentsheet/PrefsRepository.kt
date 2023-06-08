package com.stripe.android.paymentsheet

import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.SavedSelection

internal interface PrefsRepository {
    suspend fun getSavedSelection(
        isGooglePayAvailable: Boolean,
        isLinkAvailable: Boolean
    ): SavedSelection

    fun setSavedSelection(savedSelection: SavedSelection?): Result<Boolean>

    fun savePaymentSelection(paymentSelection: PaymentSelection?)
}
