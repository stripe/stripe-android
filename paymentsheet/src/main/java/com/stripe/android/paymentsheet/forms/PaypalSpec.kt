package com.stripe.android.paymentsheet.forms

import com.stripe.android.paymentsheet.elements.LayoutSpec

/**
 * This defines the requirements for usage as a Payment Method.
 */
internal val PaypalRequirement = PaymentMethodRequirements(
    piRequirements = emptySet(),

    /**
     * SetupIntents are not supported by this payment method. Currently, for paypal (and others see
     * oof #5) customers are not able to set up saved payment methods for reuse. The API errors if
     * confirming PI+SFU or SI with these methods.
     */
    siRequirements = null,
    confirmPMFromCustomer = null
)

internal val PaypalParamKey: MutableMap<String, Any?> = mutableMapOf(
    "type" to "paypal",
)

internal val PaypalForm = LayoutSpec.create()
