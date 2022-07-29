package com.stripe.android.paymentsheet.model

import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.ui.core.elements.LayoutFormDescriptor
import com.stripe.android.ui.core.forms.resources.LpmRepository
import com.stripe.android.ui.core.forms.resources.LpmRepository.SupportedPaymentMethod

/**
 * This file hold functions that extend the functionality of the SupportedPaymentMethod
 * Placed here so that they can access the PaymentSheet functionality.  Goal is
 * for much of this logic to move to the server in the future.
 */

/**
 * This will get the form layout for the supported method that matches the top pick for the
 * payment method.  It should be known that this payment method has a form
 * that matches the capabilities already.
 */
internal fun SupportedPaymentMethod.getPMAddForm(
    stripeIntent: StripeIntent,
    config: PaymentSheet.Configuration?
) = requireNotNull(getSpecWithFullfilledRequirements(stripeIntent, config))

/**
 * This function will determine if there is a valid for the payment method
 * given the [PaymentSheet.Configuration] and the [StripeIntent]
 */
internal fun SupportedPaymentMethod.getSpecWithFullfilledRequirements(
    stripeIntent: StripeIntent,
    config: PaymentSheet.Configuration?
): LayoutFormDescriptor? {
    val formSpec = formSpec
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
        showCheckboxControlledFields = false
    )

    if (!stripeIntent.paymentMethodTypes.contains(code)) {
        return null
    }

    return when (stripeIntent) {
        is PaymentIntent -> {
            if ((stripeIntent.isSetupFutureUsageSet())) {
                merchantRequestedSave
            } else {
                if (config?.customer != null) {
                    userSelectableSave
                } else {
                    oneTimeUse
                }
            }
        }
        is SetupIntent -> {
            merchantRequestedSave
        }
    }
}

/**
 * Get the LPMS that are supported when used as a Customer Saved LPM given
 * the intent.
 */
internal fun getSupportedSavedCustomerPMs(
    stripeIntent: StripeIntent?,
    config: PaymentSheet.Configuration?,
    lpmRepository: LpmRepository
) = stripeIntent?.paymentMethodTypes?.mapNotNull {
    lpmRepository.fromCode(it)
}?.filter { paymentMethod ->
    paymentMethod.supportsCustomerSavedPM() &&
        paymentMethod.getSpecWithFullfilledRequirements(stripeIntent, config) != null
} ?: emptyList()

/**
 * This will return a list of payment methods that have a supported form given
 * the [PaymentSheet.Configuration] and [StripeIntent].
 */
internal fun getPMsToAdd(
    stripeIntent: StripeIntent?,
    config: PaymentSheet.Configuration?,
    lpmRepository: LpmRepository
) = stripeIntent?.paymentMethodTypes?.mapNotNull {
    lpmRepository.fromCode(it)
}?.filter { supportedPaymentMethod ->
    supportedPaymentMethod.getSpecWithFullfilledRequirements(
        stripeIntent,
        config
    ) != null
}?.filterNot { supportedPaymentMethod ->
    stripeIntent.isLiveMode &&
        stripeIntent.unactivatedPaymentMethods.contains(supportedPaymentMethod.code)
} ?: emptyList()
