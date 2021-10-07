package com.stripe.android.paymentsheet.forms

internal object P24RequirementEvaluator : RequirementEvaluator(
    // Disabling this support so that it doesn't negatively impact our ability
    // to save cards when the user selects SFU set and the PI has PM that don't support
    // SFU to be set.
    piRequirements = null, // emptySet(),
    siRequirements = null // this is not supported by this payment method
)
