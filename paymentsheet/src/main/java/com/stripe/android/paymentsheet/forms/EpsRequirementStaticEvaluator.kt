package com.stripe.android.paymentsheet.forms

import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.PaymentSheet

internal object EpsRequirementStaticEvaluator : OneTimeUseRequirementStaticEvaluator() {
    // Disabling this support so that it doesn't negatively impact our ability
    // to save cards when the user selects SFU set and the PI has PM that don't support
    // SFU to be set.
    override fun supportsPISfuNotSetable(
        stripeIntent: StripeIntent,
        config: PaymentSheet.Configuration?
    ) = false
}
