package com.stripe.android.paymentsheet.forms

internal val EpsRequirement = PaymentMethodRequirements(

    /**
     * Disabling this support so that it doesn't negatively impact our ability
     * to save cards when the user selects SFU set and the PI has PM that don't support
     * SFU to be set.
     *
     * When supported there are no known pi requirements and can be set to an empty set.
     */
    piRequirements = null,
    siRequirements = null, // this is not supported by this payment method
    confirmPMFromCustomer = null
)
