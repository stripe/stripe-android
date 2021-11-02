package com.stripe.android.paymentsheet.forms

import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.elements.CardBillingSpec
import com.stripe.android.paymentsheet.elements.CardDetailsSpec
import com.stripe.android.paymentsheet.elements.IdentifierSpec
import com.stripe.android.paymentsheet.elements.LayoutSpec
import com.stripe.android.paymentsheet.elements.SaveForFutureUseSpec
import com.stripe.android.paymentsheet.elements.SectionSpec
import com.stripe.android.paymentsheet.elements.billingParams

internal val CardRequirement = PaymentMethodRequirements(
    piRequirements = emptySet(),
    siRequirements = emptySet(),
    confirmPMFromCustomer = true
)

internal val cardParams: MutableMap<String, Any?> = mutableMapOf(
    "number" to null,
    "expiryMonth" to null,
    "expiryYear" to null,
    "cvc" to null,
    "attribution" to listOf("PaymentSheet.Form")
)

internal val CardParamKey: MutableMap<String, Any?> = mutableMapOf(
    "type" to "card",
    "billing_details" to billingParams,
    "card" to cardParams
)

internal val creditDetailsSection = SectionSpec(
    IdentifierSpec.Generic("credit_details_section"),
    CardDetailsSpec
)

internal val creditBillingSection = SectionSpec(
    IdentifierSpec.Generic("credit_billing_section"),
    CardBillingSpec,
    R.string.billing_details
)

internal val CardForm = LayoutSpec.create(
    creditDetailsSection,
    creditBillingSection,
    SaveForFutureUseSpec(emptyList())
)
