package com.stripe.android.paymentsheet.forms

import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.PaymentSheet

internal abstract class RequirementEvaluator {
    abstract fun supportsCustomerSavedPM(
        stripeIntent: StripeIntent,
        config: PaymentSheet.Configuration?
    ): Boolean

    abstract fun supportsSI(
        stripeIntent: StripeIntent,
        config: PaymentSheet.Configuration?
    ): Boolean

    abstract fun supportsPISfuSet(
        stripeIntent: StripeIntent,
        config: PaymentSheet.Configuration?
    ): Boolean

    abstract fun supportsPISfuNotSetable(
        stripeIntent: StripeIntent,
        config: PaymentSheet.Configuration?
    ): Boolean

    abstract fun supportsPISfuSettable(
        stripeIntent: StripeIntent,
        config: PaymentSheet.Configuration?
    ): Boolean

    fun allHaveKnownReuseSupport(paymentMethodsInIntent: List<String?>): Boolean {
        // The following PaymentMethods are know to work when
        // PaymentIntent.setup_future_usage = on/off session
        // This list is different from the PaymentMethod.Type.isReusable
        // It is expected that this check will be removed soon
        val knownReusable = setOf(
            PaymentMethod.Type.Alipay.code,
            PaymentMethod.Type.Card.code,
            PaymentMethod.Type.SepaDebit.code,
            PaymentMethod.Type.AuBecsDebit.code,
            PaymentMethod.Type.Bancontact.code,
            PaymentMethod.Type.Sofort.code,
            PaymentMethod.Type.BacsDebit.code,
            PaymentMethod.Type.Ideal.code
        )
        return paymentMethodsInIntent.filterNot { knownReusable.contains(it) }.isEmpty()
    }
}
