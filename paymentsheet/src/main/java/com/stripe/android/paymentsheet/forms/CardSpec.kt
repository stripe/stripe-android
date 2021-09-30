package com.stripe.android.paymentsheet.forms

import com.stripe.android.paymentsheet.elements.FormSpec
import com.stripe.android.paymentsheet.elements.LayoutSpec
import com.stripe.android.paymentsheet.elements.PaymentMethodSpec

internal val cardFormSpec = FormSpec(
    LayoutSpec(
        emptyList() // Not supported in form view
    ),
    requirements = emptySet()
)

internal val card = PaymentMethodSpec(
    mutableMapOf(),
    listOf(cardFormSpec)
)
