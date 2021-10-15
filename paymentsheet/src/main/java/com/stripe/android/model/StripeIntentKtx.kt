package com.stripe.android.model

import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.SupportedPaymentMethod

/**
 * Get the LPMS that are supported when used as a Customer Saved LPM given
 * the intent.
 */
internal fun StripeIntent.getSupportedSavedCustomerPMs(
    config: PaymentSheet.Configuration?
) = paymentMethodTypes.mapNotNull {
    SupportedPaymentMethod.fromCode(it)
}.filter { paymentMethod ->
    paymentMethod.supportsCustomerSavedPM() &&
        paymentMethod.getSpecWithFullfilledRequirements(this, config) != null
}

/**
 * This will return a list of payment methods that have a supported form given
 * the [PaymentSheet.Configuration] and [StripeIntent].
 */
internal fun StripeIntent.getPMsToAdd(
    config: PaymentSheet.Configuration?
) = this.paymentMethodTypes.mapNotNull {
    SupportedPaymentMethod.fromCode(it)
}.filter { supportedPaymentMethod ->
    supportedPaymentMethod.getSpecWithFullfilledRequirements(
        this,
        config
    ) != null
}
