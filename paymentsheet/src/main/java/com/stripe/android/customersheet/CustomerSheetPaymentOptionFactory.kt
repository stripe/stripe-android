package com.stripe.android.customersheet

import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.PaymentOption
import com.stripe.android.paymentsheet.model.PaymentSelection

internal fun interface CustomerSheetPaymentOptionFactory {
    fun create(
        selection: PaymentSelection,
        appearance: PaymentSheet.Appearance,
    ): PaymentOption
}
