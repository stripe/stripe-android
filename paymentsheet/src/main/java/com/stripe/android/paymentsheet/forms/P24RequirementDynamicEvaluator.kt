package com.stripe.android.paymentsheet.forms

internal object P24RequirementDynamicEvaluator : RequirementDynamicEvaluator(
    piRequirements = emptySet(),
    siRequirements = null // this is not supported by this payment method
)
