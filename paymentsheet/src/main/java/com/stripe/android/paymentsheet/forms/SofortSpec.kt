package com.stripe.android.paymentsheet.forms

import com.stripe.android.paymentsheet.forms.SectionSpec.SectionFieldSpec.Country
import com.stripe.android.paymentsheet.forms.SectionSpec.SectionFieldSpec.Email
import com.stripe.android.paymentsheet.forms.SectionSpec.SectionFieldSpec.Name

internal val sofortParams: MutableMap<String, Any?> = mutableMapOf(
    "country" to null,
)

internal val sofortParamKey: MutableMap<String, Any?> = mutableMapOf(
    "type" to "sofort",
    "billing_details" to billingParams,
    "sofort" to sofortParams
)

val sofort = FormSpec(
    FieldLayoutSpec(
        listOf(
            SectionSpec(Name),
            SectionSpec(Email),
            SectionSpec(Country)
        )
    ),
    sofortParamKey,
)

