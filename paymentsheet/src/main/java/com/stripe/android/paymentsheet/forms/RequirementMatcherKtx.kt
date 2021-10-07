package com.stripe.android.paymentsheet.forms

import androidx.annotation.VisibleForTesting
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.elements.LayoutFormDescriptor
import com.stripe.android.paymentsheet.model.SupportedPaymentMethod

/**
 * Get the LPMS that are supported when used as a Customer Saved LPM given
 * the intent.
 */
// What if the SI or PI already has a customer associated do we use the cusomter
// from the PI or the Customer from the config?
internal fun getSupportedSavedCustomerPMs(
    stripeIntent: StripeIntent,
    config: PaymentSheet.Configuration?
) =
    stripeIntent.paymentMethodTypes.mapNotNull {
        SupportedPaymentMethod.fromCode(it)
    }.filter {
        it.requirements.supportsCustomerSavedPM(stripeIntent, config) &&
            getSpecWithFullfilledRequirements(it, stripeIntent, config) != null
    }

/**
 * This will get the form layout for the supported method that matches the top pick for the
 * payment method.  It should be known that this payment method has a form
 * that matches the capabilities already.
 */
internal fun getPMAddForm(
    paymentMethod: SupportedPaymentMethod,
    stripeIntent: StripeIntent,
    config: PaymentSheet.Configuration?
) =
    requireNotNull(getSpecWithFullfilledRequirements(paymentMethod, stripeIntent, config))

/**
 * This will return a list of payment methods that have a supported form given the capabilities.
 */
@VisibleForTesting
internal fun getPMsToAdd(
    stripeIntent: StripeIntent,
    config: PaymentSheet.Configuration?
) = stripeIntent.paymentMethodTypes.asSequence().mapNotNull {
    SupportedPaymentMethod.fromCode(it)
}.filter { supportedPaymentMethod ->
    getSpecWithFullfilledRequirements(
        supportedPaymentMethod,
        stripeIntent,
        config
    ) != null
} // TODO.filter { it == SupportedPaymentMethod.Card }
    .toList()

@VisibleForTesting
internal fun getSpecWithFullfilledRequirements(
    paymentMethod: SupportedPaymentMethod,
    stripeIntent: StripeIntent,
    config: PaymentSheet.Configuration?
): LayoutFormDescriptor? {
    val formSpec = paymentMethod.formSpec
    val oneTimeUse = LayoutFormDescriptor(
        formSpec,
        showCheckbox = false,
        showCheckboxControlledFields = false
    )
    val merchantRequestedSave = LayoutFormDescriptor(
        formSpec,
        showCheckbox = false,
        showCheckboxControlledFields = true
    )
    val userSelectableSave = LayoutFormDescriptor(
        formSpec,
        showCheckbox = true,
        showCheckboxControlledFields = true
    )

    return when (stripeIntent) {
        is PaymentIntent -> {
            if (isSetupFutureUsageSet(stripeIntent.setupFutureUsage)) {
                if (paymentMethod.requirements.supportsPISfuSet(stripeIntent, config)
                ) {
                    merchantRequestedSave
                } else {
                    null
                }
            } else {
                when {
                    paymentMethod.requirements.supportsPISfuSettable(
                        stripeIntent,
                        config
                    ) -> userSelectableSave
                    paymentMethod.requirements.supportsPISfuNotSetable(
                        stripeIntent,
                        config
                    ) -> oneTimeUse
                    else -> null
                }
            }
        }
        is SetupIntent -> {
            when {
                paymentMethod.requirements.supportsPISfuSettable(
                    stripeIntent,
                    config
                ) -> merchantRequestedSave
                else -> null
            }
        }
    }
}

/**
 * TODO: Write this out in more details.
 */
private fun isSetupFutureUsageSet(setupFutureUsage: StripeIntent.Usage?) =
    when (setupFutureUsage) {
        StripeIntent.Usage.OnSession -> true
        StripeIntent.Usage.OffSession -> true
        StripeIntent.Usage.OneTime -> false
        null -> false
    }
