package com.stripe.android.customersheet.data

import com.stripe.android.model.ElementsSession
import com.stripe.android.model.PaymentMethod

internal data class CustomerSheetSession(
    val elementsSession: ElementsSession,
    val paymentMethods: List<PaymentMethod>
)
