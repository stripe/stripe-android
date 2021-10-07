package com.stripe.android.paymentsheet.forms

internal object EpsRequirementDynamicEvaluator : RequirementDynamicEvaluator(
    piRequirements = emptySet(),
    siRequirements = null // this is not supported by this payment method
)
