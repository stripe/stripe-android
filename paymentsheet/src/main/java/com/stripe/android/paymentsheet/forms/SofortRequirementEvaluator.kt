package com.stripe.android.paymentsheet.forms

internal object SofortRequirementEvaluator : RequirementEvaluator(
    piRequirements = setOf(Delayed),
    siRequirements = null,//setOf(Delayed),
    confirmPMFromCustomer = null
)
