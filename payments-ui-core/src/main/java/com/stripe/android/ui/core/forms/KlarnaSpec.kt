package com.stripe.android.paymentsheet.forms

import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.elements.EmailSpec
import com.stripe.android.paymentsheet.elements.IdentifierSpec
import com.stripe.android.paymentsheet.elements.KlarnaCountrySpec
import com.stripe.android.paymentsheet.elements.KlarnaHelper
import com.stripe.android.paymentsheet.elements.LayoutSpec
import com.stripe.android.paymentsheet.elements.SectionSpec
import com.stripe.android.paymentsheet.elements.StaticTextSpec
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

internal val klarnaHeader = StaticTextSpec(
    identifier = IdentifierSpec.Generic("klarna_header"),
    stringResId = KlarnaHelper.getKlarnaHeader(),
    fontSizeSp = 13,
    letterSpacingSp = -.15,
    color = R.color.stripe_paymentsheet_googlepay_divider_text
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
