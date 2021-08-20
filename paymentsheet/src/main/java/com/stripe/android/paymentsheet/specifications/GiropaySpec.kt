package com.stripe.android.paymentsheet.specifications

internal val giropayParamKey: MutableMap<String, Any?> = mutableMapOf(
    "type" to "giropay",
    "billing_details" to billingParams,
)

internal val giropayNameSection = FormItemSpec.SectionSpec(
    IdentifierSpec.Generic("name section"),
    SectionFieldSpec.NAME
)

internal val giropay = FormSpec(
    LayoutSpec(
        listOf(
            giropayNameSection
        )
    ),
    giropayParamKey,
)
