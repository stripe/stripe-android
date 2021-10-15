package com.stripe.android.paymentsheet.forms

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
