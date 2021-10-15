package com.stripe.android.paymentsheet.forms

import com.stripe.android.paymentsheet.elements.IdentifierSpec
import com.stripe.android.paymentsheet.elements.LayoutSpec
import com.stripe.android.paymentsheet.elements.SectionSpec
import com.stripe.android.paymentsheet.elements.SimpleTextSpec
import com.stripe.android.paymentsheet.elements.billingParams

internal val GiropayRequirement = PaymentMethodRequirements(
    /**
     * Disabling this support so that it doesn't negatively impact our ability
     * to save cards when the user selects SFU set and the PI has PM that don't support
     * SFU to be set.
     *
     * When supported there are no known PI requirements and can be set to an empty set.
     */
    piRequirements = null,
    siRequirements = null, // this is not supported by this payment method
    confirmPMFromCustomer = null
)

internal val GiropayParamKey: MutableMap<String, Any?> = mutableMapOf(
    "type" to "giropay",
    "billing_details" to billingParams,
)

internal val giropayNameSection = SectionSpec(
    IdentifierSpec.Generic("name_section"),
    SimpleTextSpec.NAME
)

internal val GiropayForm = LayoutSpec.create(giropayNameSection)
