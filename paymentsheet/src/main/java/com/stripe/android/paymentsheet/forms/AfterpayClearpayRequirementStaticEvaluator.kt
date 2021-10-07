package com.stripe.android.paymentsheet.forms

import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.PaymentSheet

internal object AfterpayClearpayRequirementStaticEvaluator :
    OneTimeUseRequirementStaticEvaluator() {
    override fun supportsPISfuNotSetable(
        stripeIntent: StripeIntent,
        config: PaymentSheet.Configuration?
    ) = false
    // This is not supported until we have afterpay cancellation support
//        if (stripeIntent is PaymentIntent) {
//        stripeIntent.shipping?.name != null &&
//            stripeIntent.shipping?.address?.line1 != null &&
//            stripeIntent.shipping?.address?.country != null &&
//            stripeIntent.shipping?.address?.postalCode != null
//    } else {
//        false
//    }
}
