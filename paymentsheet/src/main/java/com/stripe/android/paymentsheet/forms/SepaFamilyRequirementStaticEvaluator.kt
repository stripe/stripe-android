package com.stripe.android.paymentsheet.forms

import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.PaymentSheet

/**
 * This check is used in the needed places where we want to enforce a block
 * until there is a way of retrieving valid mandates associated with a customer PM.
 *
 * This is just a artificial block we put in to make it easy to explain
 * that we are not allowing SetupIntent, or PaymentIntent with setupFuture
 * usage, even though this is possible (both with and without a customer).
 * The reason we are excluding it is for the case where the customer
 * has a SEPA payment method attached.   The payment sheet will show
 * this saved customer PM.   We don't want to create a new mandate and
 * we don't have a way to get the existing mandate or know if it is
 * still valid.
 */
internal abstract class SepaFamilyRequirementStaticEvaluator : RequirementEvaluator() {
    // It is not possible to attach this to a customer
    override fun supportsCustomerSavedPM(
        stripeIntent: StripeIntent,
        config: PaymentSheet.Configuration?
    ) = false

    // Not valid for this payment method
    override fun supportsPISfuSet(
        stripeIntent: StripeIntent,
        config: PaymentSheet.Configuration?
    ) = false

    // Not valid for this payment method
    override fun supportsSI(
        stripeIntent: StripeIntent,
        config: PaymentSheet.Configuration?
    ) = false

    // Not valid for this payment method
    override fun supportsPISfuSettable(
        stripeIntent: StripeIntent,
        config: PaymentSheet.Configuration?
    ) = false
}
