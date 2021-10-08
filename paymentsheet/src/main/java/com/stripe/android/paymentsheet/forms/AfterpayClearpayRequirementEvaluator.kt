package com.stripe.android.paymentsheet.forms

internal object AfterpayClearpayRequirementEvaluator : RequirementEvaluator(
    /**
     * This is null until we have after cancellation support.
     */
    piRequirements = null,

// We will only require a subset of all shipping fields in the PaymentIntent
//    setOf(
//        ShippingIntentRequirement.Name,
//        ShippingIntentRequirement.AddressLine1,
//        ShippingIntentRequirement.AddressCountry,
//        ShippingIntentRequirement.AddressPostal,
//    ),

    /**
     * SetupIntents are not supported by this payment method, in addition,
     * setup intents do not have shipping information
     */
    siRequirements = null,
    confirmPMFromCustomer = null
)
