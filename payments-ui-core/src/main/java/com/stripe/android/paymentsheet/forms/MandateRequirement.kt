package com.stripe.android.paymentsheet.forms

import androidx.annotation.RestrictTo
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.ui.core.forms.resources.LpmRepository

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
enum class MandateRequirement {
    Always,
    Dynamic,
    Never,
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun LpmRepository.SupportedPaymentMethod.requiresMandateFor(stripeIntent: StripeIntent): Boolean {
    return when (mandateRequirement) {
        MandateRequirement.Always -> true
        MandateRequirement.Dynamic -> {
            when (stripeIntent) {
                is PaymentIntent -> stripeIntent.setupFutureUsage != StripeIntent.Usage.OffSession // TODO is this correct?
                is SetupIntent -> true
            }
        }
        MandateRequirement.Never -> false
    }
}
