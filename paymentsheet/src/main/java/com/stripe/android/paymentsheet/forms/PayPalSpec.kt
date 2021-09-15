package com.stripe.android.paymentsheet.forms

import com.stripe.android.paymentsheet.elements.FormSpec
import com.stripe.android.paymentsheet.elements.LayoutSpec


internal val payPalParamKey: MutableMap<String, Any?> = mutableMapOf(
    "type" to "paypal",
)

internal val paypal = FormSpec(
    LayoutSpec(
        emptyList()
    ),
    payPalParamKey,
)
