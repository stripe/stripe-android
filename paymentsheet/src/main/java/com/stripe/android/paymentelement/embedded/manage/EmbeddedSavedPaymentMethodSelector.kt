package com.stripe.android.paymentelement.embedded.manage

import com.stripe.android.paymentsheet.model.PaymentSelection

internal fun interface EmbeddedSavedPaymentMethodSelector {
    suspend fun select(selection: PaymentSelection.Saved): Result<Unit>
}
