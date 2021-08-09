package com.stripe.android.paymentsheet.specifications

internal val cardParams: MutableMap<String, Any?> = mutableMapOf(
    "number" to null,
    "expiryMonth" to null,
    "expiryYear" to null,
    "cvc" to null,
    "attribution" to listOf("PaymentSheet.Form")
)

internal val cardParamKey: MutableMap<String, Any?> = mutableMapOf(
    "type" to "card",
    "billing_details" to billingParams,
    "card" to cardParams
)

internal val cardSection = FormItemSpec.SectionSpec(
    IdentifierSpec("credit"),
    SectionFieldSpec.CreditSpec
)

internal val card = FormSpec(
    LayoutSpec(
        listOf(
            cardSection
        )
    ),
    cardParamKey,
)
