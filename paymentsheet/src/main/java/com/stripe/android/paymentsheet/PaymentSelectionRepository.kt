package com.stripe.android.paymentsheet

import com.stripe.android.paymentsheet.model.PaymentSelection

internal interface PaymentSelectionRepository {
    val paymentSelection: PaymentSelection?
}
