package com.stripe.android.paymentsheet.forms

import com.stripe.android.paymentsheet.elements.EmailSpec
import com.stripe.android.paymentsheet.elements.IdentifierSpec
import com.stripe.android.paymentsheet.elements.KlarnaCountrySpec
import com.stripe.android.paymentsheet.elements.LayoutSpec
import com.stripe.android.paymentsheet.elements.SectionSpec
import com.stripe.android.paymentsheet.elements.SimpleTextHeaderSpec
import com.stripe.android.paymentsheet.elements.billingParams

/**
 * This defines the requirements for usage as a Payment Method.
 */
internal val KlarnaRequirement = PaymentMethodRequirements(
    piRequirements = emptySet(),
    siRequirements = null,
    confirmPMFromCustomer = null
)

internal val KlarnaParamKey: MutableMap<String, Any?> = mutableMapOf(
    "type" to "klarna",
    "billing_details" to billingParams
)

internal val klarnaHeader = SimpleTextHeaderSpec(
    IdentifierSpec.Generic("klarna_header")
)

internal val klarnaEmailSection =
    SectionSpec(IdentifierSpec.Generic("email_section"), EmailSpec)

internal val klarnaBillingSection = SectionSpec(
    IdentifierSpec.Generic("country_section"),
    KlarnaCountrySpec()
)

internal val KlarnaForm = LayoutSpec.create(
    klarnaHeader,
    klarnaEmailSection,
    klarnaBillingSection
)
