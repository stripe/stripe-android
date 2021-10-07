package com.stripe.android.paymentsheet.forms

import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.PaymentSheet

internal object AfterpayClearpayRequirementStaticEvaluator :
    OneTimeUseRequirementStaticEvaluator() {
    // This is not supported until we have afterpay cancellation support
    override fun supportsPISfuNotSetable(
        stripeIntent: StripeIntent,
        config: PaymentSheet.Configuration?
    ) = false
//        if (stripeIntent is PaymentIntent) {
//        stripeIntent.shipping?.name != null &&
//            stripeIntent.shipping?.address?.line1 != null &&
//            stripeIntent.shipping?.address?.country != null &&
//            stripeIntent.shipping?.address?.postalCode != null
//    } else {
//        false
//    }
}
