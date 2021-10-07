package com.stripe.android.paymentsheet.forms

internal object AfterpayClearpayRequirementEvaluator : RequirementEvaluator(
    // This is null until we have after cancellation support.
    piRequirements = null,
//    setOf(
//        ShippingIntentRequirement.Name,
//        ShippingIntentRequirement.AddressLine1,
//        ShippingIntentRequirement.AddressCountry,
//        ShippingIntentRequirement.AddressPostal,
//    ),
    siRequirements = null // setup intents are not supported by this payment method
)
