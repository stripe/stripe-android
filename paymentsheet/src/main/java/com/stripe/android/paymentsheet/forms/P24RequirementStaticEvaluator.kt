package com.stripe.android.paymentsheet.forms

import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.PaymentSheet

internal object P24RequirementStaticEvaluator : OneTimeUseRequirementStaticEvaluator() {
    override fun supportsPISfuNotSetable(
        stripeIntent: StripeIntent,
        config: PaymentSheet.Configuration?
    ) = true
}
