package com.stripe.android.paymentsheet.forms

import com.stripe.android.paymentsheet.elements.LayoutSpec
import com.stripe.android.paymentsheet.elements.billingParams

/**
 * This defines the requirements for usage as a Payment Method.
 */
internal val PaypalRequirement = PaymentMethodRequirements(
    piRequirements = emptySet(),
    siRequirements = null,
    confirmPMFromCustomer = null
)

internal val PaypalParamKey: MutableMap<String, Any?> = mutableMapOf(
    "type" to "paypal",
    "billing_details" to billingParams
)

internal val PaypalForm = LayoutSpec.create()
