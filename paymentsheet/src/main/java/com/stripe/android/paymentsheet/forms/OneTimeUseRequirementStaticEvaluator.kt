package com.stripe.android.paymentsheet.forms

import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.PaymentSheet

internal abstract class OneTimeUseRequirementStaticEvaluator : RequirementEvaluator() {
    override fun supportsCustomerSavedPM(
        stripeIntent: StripeIntent,
        config: PaymentSheet.Configuration?
    ) = false

    override fun supportsPISfuSet(
        stripeIntent: StripeIntent,
        config: PaymentSheet.Configuration?
    ) = false

    override fun supportsSI(
        stripeIntent: StripeIntent,
        config: PaymentSheet.Configuration?
    ) = false

    override fun supportsPISfuSettable(
        stripeIntent: StripeIntent,
        config: PaymentSheet.Configuration?
    ) = false

}
