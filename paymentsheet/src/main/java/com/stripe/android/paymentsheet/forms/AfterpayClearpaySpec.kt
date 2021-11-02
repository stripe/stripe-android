package com.stripe.android.paymentsheet.forms

import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.elements.AddressSpec
import com.stripe.android.paymentsheet.elements.AfterpayClearpayHeaderSpec
import com.stripe.android.paymentsheet.elements.EmailSpec
import com.stripe.android.paymentsheet.elements.IdentifierSpec
import com.stripe.android.paymentsheet.elements.LayoutSpec
import com.stripe.android.paymentsheet.elements.SectionSpec
import com.stripe.android.paymentsheet.elements.SimpleTextSpec
import com.stripe.android.paymentsheet.elements.billingParams

/**
 * This defines the requirements for usage as a Payment Method.
 */
internal val AfterpayClearpayRequirement = PaymentMethodRequirements(
    /**
     * This is null until we have after cancellation support.  When we have cancellation support
     * this will require Shipping name, address line 1, address country, and postal
     */
    piRequirements = null,

    /**
     * SetupIntents are not supported by this payment method, in addition,
     * setup intents do not have shipping information
     */
    siRequirements = null,
    confirmPMFromCustomer = null
)

internal val AfterpayClearpayParamKey: MutableMap<String, Any?> = mutableMapOf(
    "type" to "afterpay_clearpay",
    "billing_details" to billingParams
)

internal val afterpayClearpayHeader = AfterpayClearpayHeaderSpec(
    IdentifierSpec.Generic("afterpay_clearpay_header")
)
internal val afterpayClearpayNameSection = SectionSpec(
    IdentifierSpec.Generic("name_section"),
    SimpleTextSpec.NAME
)
internal val afterpayClearpayEmailSection =
    SectionSpec(IdentifierSpec.Generic("email_section"), EmailSpec)

internal val afterpayClearpayBillingSection = SectionSpec(
    IdentifierSpec.Generic("address_section"),
    AddressSpec(IdentifierSpec.Generic("address")),
    R.string.billing_details
)

internal val AfterpayClearpayForm = LayoutSpec.create(
    afterpayClearpayHeader,
    afterpayClearpayNameSection,
    afterpayClearpayEmailSection,
    afterpayClearpayBillingSection
)
