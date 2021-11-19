package com.stripe.android.paymentsheet.forms

import com.stripe.android.paymentsheet.elements.LayoutSpec

/**
 * This defines the requirements for usage as a Payment Method.
 */
internal val PaypalRequirement = PaymentMethodRequirements(
    piRequirements = emptySet(),

    /**
     * SetupIntents are not supported by this payment method. Currently Paypal should only be set up
     * one time, but the Stripe API will let you keep doing it resulting in several paypals being
     * listed. It's better to leave this disabled than to enable it, let users save multiple
     * paypals, and then fix it leaving the users in a weird state. See oof #5
     */
    siRequirements = null,
    confirmPMFromCustomer = null
)

internal val PaypalParamKey: MutableMap<String, Any?> = mutableMapOf(
    "type" to "paypal",
)

internal val PaypalForm = LayoutSpec.create()
