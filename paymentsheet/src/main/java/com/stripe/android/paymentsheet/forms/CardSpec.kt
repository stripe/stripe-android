package com.stripe.android.paymentsheet.forms

import com.stripe.android.paymentsheet.R

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

internal val creditDetailsSection = SectionSpec(
    IdentifierSpec("credit"),
    CreditDetailSpec
)

internal val creditBillingSection = SectionSpec(
    IdentifierSpec("credit_billing"),
    CreditBillingSpec,
    R.string.billing_details
)

internal val card = FormSpec(
    LayoutSpec(
        listOf(
            creditDetailsSection,
            creditBillingSection
        )
    ),
    cardParamKey,
)
