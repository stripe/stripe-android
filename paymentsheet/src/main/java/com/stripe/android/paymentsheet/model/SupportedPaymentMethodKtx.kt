package com.stripe.android.paymentsheet.model

import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.payments.financialconnections.DefaultIsFinancialConnectionsAvailable
import com.stripe.android.payments.financialconnections.IsFinancialConnectionsAvailable
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.forms.Delayed
import com.stripe.android.paymentsheet.forms.PIRequirement
import com.stripe.android.paymentsheet.forms.SIRequirement
import com.stripe.android.paymentsheet.forms.ShippingAddress
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
    config: PaymentSheet.Configuration
) = requireNotNull(getSpecWithFullfilledRequirements(stripeIntent, config))

/**
 * This function will determine if there is a valid for the payment method
 * given the [PaymentSheet.Configuration] and the [StripeIntent]
 */
internal fun SupportedPaymentMethod.getSpecWithFullfilledRequirements(
    stripeIntent: StripeIntent,
    config: PaymentSheet.Configuration
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
            val isSetupFutureUsageSet = stripeIntent.isSetupFutureUsageSet(code)

            if (isSetupFutureUsageSet) {
                if (supportsPaymentIntentSfuSet(stripeIntent, config)) {
                    merchantRequestedSave
                } else {
                    null
                }
            } else {
                when {
                    supportsPaymentIntentSfuSettable(
                        stripeIntent,
                        config
                    ) -> userSelectableSave
                    supportsPaymentIntentSfuNotSettable(
                        stripeIntent,
                        config
                    ) -> oneTimeUse
                    else -> null
                }
            }
        }
        is SetupIntent -> {
            when {
                supportsSetupIntent(
                    config
                ) -> merchantRequestedSave
                else -> null
            }
        }
    }
}

/**
 * This checks to see if the payment method is supported from a SetupIntent.
 */
private fun SupportedPaymentMethod.supportsSetupIntent(
    config: PaymentSheet.Configuration
) = requirement.getConfirmPMFromCustomer(code) &&
    checkSetupIntentRequirements(requirement.siRequirements, config)

/**
 * This checks if there is support using this payment method when SFU
 * is already set in the PaymentIntent.  If SFU is set it must be possible
 * to confirm this payment method from a PM ID attached to a customer for
 * a consistent user experience.
 */
private fun SupportedPaymentMethod.supportsPaymentIntentSfuSet(
    paymentIntent: PaymentIntent,
    config: PaymentSheet.Configuration
) = requirement.getConfirmPMFromCustomer(code) &&
    checkSetupIntentRequirements(requirement.siRequirements, config) &&
    checkPaymentIntentRequirements(requirement.piRequirements, paymentIntent, config)

/**
 * This detects if there is support with using this PM with the PI
 * where SFU is not settable by the user.
 */
private fun SupportedPaymentMethod.supportsPaymentIntentSfuNotSettable(
    paymentIntent: PaymentIntent,
    config: PaymentSheet.Configuration
) = checkPaymentIntentRequirements(requirement.piRequirements, paymentIntent, config)

/**
 * This checks to see if this PM is supported with the given
 * payment intent and configuration.
 *
 * The customer ID is required to be passed in the configuration
 * (the sdk cannot know if the PI has a customer ID associated with it),
 * so that we can guarantee to the user that the PM will be associated
 * with their customer object AND accessible when opening PaymentSheet
 * and seeing the saved PMs associate with their customer object.
 */
private fun SupportedPaymentMethod.supportsPaymentIntentSfuSettable(
    paymentIntent: PaymentIntent,
    config: PaymentSheet.Configuration
) = config.customer != null &&
    requirement.getConfirmPMFromCustomer(code) &&
    checkPaymentIntentRequirements(requirement.piRequirements, paymentIntent, config) &&
    checkSetupIntentRequirements(requirement.siRequirements, config)

/**
 * Verifies that all Setup Intent [requirements] are met.
 */
private fun checkSetupIntentRequirements(
    requirements: Set<SIRequirement>?,
    config: PaymentSheet.Configuration
): Boolean {
    return requirements?.map { requirement ->
        when (requirement) {
            Delayed -> config.allowsDelayedPaymentMethods
        }
    }?.contains(false) == false
}

/**
 * Verifies that all Payment Intent [requirements] are met.
 */
private fun checkPaymentIntentRequirements(
    requirements: Set<PIRequirement>?,
    paymentIntent: PaymentIntent,
    config: PaymentSheet.Configuration
): Boolean {
    return requirements?.map { requirement ->
        when (requirement) {
            Delayed -> {
                config.allowsDelayedPaymentMethods
            }
            ShippingAddress -> {
                val forceAllow = config.allowsPaymentMethodsRequiringShippingAddress
                paymentIntent.containsValidShippingInfo || forceAllow
            }
        }
    }?.contains(false) == false
}

private val PaymentIntent.containsValidShippingInfo: Boolean
    get() = shipping?.name != null &&
        shipping?.address?.line1 != null &&
        shipping?.address?.country != null &&
        shipping?.address?.postalCode != null

/**
 * Get the LPMS that are supported when used as a Customer Saved LPM given
 * the intent.
 */
internal fun getSupportedSavedCustomerPMs(
    stripeIntent: StripeIntent?,
    config: PaymentSheet.Configuration,
    lpmRepository: LpmRepository
) = stripeIntent?.paymentMethodTypes?.mapNotNull {
    lpmRepository.fromCode(it)
}?.filter { paymentMethod ->
    paymentMethod.supportsCustomerSavedPM() &&
        paymentMethod.getSpecWithFullfilledRequirements(stripeIntent, config) != null
} ?: emptyList()

internal fun getPMsToAdd(
    stripeIntent: StripeIntent?,
    config: PaymentSheet.Configuration,
    lpmRepository: LpmRepository,
    isFinancialConnectionsAvailable: IsFinancialConnectionsAvailable = DefaultIsFinancialConnectionsAvailable()
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
}?.filterNot { supportedPaymentMethod ->
    !isFinancialConnectionsAvailable() &&
        supportedPaymentMethod.code == PaymentMethod.Type.USBankAccount.code
} ?: emptyList()
