package com.stripe.android.paymentsheet.forms

import com.stripe.android.paymentsheet.elements.FormSpec
import com.stripe.android.paymentsheet.elements.IdentifierSpec
import com.stripe.android.paymentsheet.elements.LayoutSpec
import com.stripe.android.paymentsheet.elements.SectionSpec
import com.stripe.android.paymentsheet.elements.SimpleTextSpec
import com.stripe.android.paymentsheet.elements.billingParams

internal val giropayParamKey: MutableMap<String, Any?> = mutableMapOf(
    "type" to "giropay",
    "billing_details" to billingParams,
)

internal val giropayNameSection = SectionSpec(
    IdentifierSpec.Generic("name_section"),
    SimpleTextSpec.NAME
)

internal val giropay = FormSpec(
    LayoutSpec(
        listOf(
            giropayNameSection
        )
    ),
    giropayParamKey,
)
