package com.stripe.android.paymentsheet.model

import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.forms.Delayed
import com.stripe.android.paymentsheet.forms.PIRequirement
import com.stripe.android.paymentsheet.forms.SIRequirement
import com.stripe.android.paymentsheet.forms.ShippingAddress
import com.stripe.android.paymentsheet.state.PaymentSheetData
import com.stripe.android.ui.core.PaymentSheetMode
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
    data: PaymentSheetData,
    config: PaymentSheet.Configuration?
) = requireNotNull(
    getSpecWithFulfilledRequirements(
        data = data,
        config = config,
    )
)

/**
 * This function will determine if there is a valid for the payment method
 * given the [PaymentSheet.Configuration] and the [StripeIntent]
 */
internal fun SupportedPaymentMethod.getSpecWithFulfilledRequirements(
    data: PaymentSheetData,
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

    if (!data.paymentMethodTypes.contains(code)) {
        return null
    }

    return when (data.mode) {
        is PaymentSheetMode.Payment -> {
            if (data.setupFutureUse != null || data.isLpmLevelSetupFutureUsageSet(code)) {
                if (supportsPaymentIntentSfuSet(data.shippingDetails, config)) {
                    merchantRequestedSave
                } else {
                    null
                }
            } else {
                when {
                    supportsPaymentIntentSfuSettable(data.shippingDetails, config) -> {
                        userSelectableSave
                    }
                    supportsPaymentIntentSfuNotSettable(data.shippingDetails, config) -> {
                        oneTimeUse
                    }
                    else -> {
                        null
                    }
                }
            }
        }
        is PaymentSheetMode.Setup -> {
            when {
                supportsSetupIntent(config) -> {
                    merchantRequestedSave
                }
                else -> {
                    null
                }
            }
        }
    }
}

/**
 * This checks to see if the payment method is supported from a SetupIntent.
 */
private fun SupportedPaymentMethod.supportsSetupIntent(
    config: PaymentSheet.Configuration?
) = requirement.getConfirmPMFromCustomer(code) &&
    checkSetupIntentRequirements(requirement.siRequirements, config)

/**
 * This checks if there is support using this payment method when SFU
 * is already set in the PaymentIntent.  If SFU is set it must be possible
 * to confirm this payment method from a PM ID attached to a customer for
 * a consistent user experience.
 */
private fun SupportedPaymentMethod.supportsPaymentIntentSfuSet(
    shippingDetails: PaymentIntent.Shipping?,
    config: PaymentSheet.Configuration?
) = requirement.getConfirmPMFromCustomer(code) &&
    checkSetupIntentRequirements(requirement.siRequirements, config) &&
    checkPaymentIntentRequirements(requirement.piRequirements, shippingDetails, config)

/**
 * This detects if there is support with using this PM with the PI
 * where SFU is not settable by the user.
 */
private fun SupportedPaymentMethod.supportsPaymentIntentSfuNotSettable(
    shippingDetails: PaymentIntent.Shipping?,
    config: PaymentSheet.Configuration?
) = checkPaymentIntentRequirements(requirement.piRequirements, shippingDetails, config)

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
    shippingDetails: PaymentIntent.Shipping?,
    config: PaymentSheet.Configuration?
) = config?.customer != null &&
    requirement.getConfirmPMFromCustomer(code) &&
    checkPaymentIntentRequirements(requirement.piRequirements, shippingDetails, config) &&
    checkSetupIntentRequirements(requirement.siRequirements, config)

/**
 * Verifies that all Setup Intent [requirements] are met.
 */
private fun checkSetupIntentRequirements(
    requirements: Set<SIRequirement>?,
    config: PaymentSheet.Configuration?
): Boolean {
    return requirements?.map { requirement ->
        when (requirement) {
            Delayed -> config?.allowsDelayedPaymentMethods == true
        }
    }?.contains(false) == false
}

/**
 * Verifies that all Payment Intent [requirements] are met.
 */
private fun checkPaymentIntentRequirements(
    requirements: Set<PIRequirement>?,
    shippingDetails: PaymentIntent.Shipping?,
    config: PaymentSheet.Configuration?
): Boolean {
    return requirements?.map { requirement ->
        when (requirement) {
            Delayed -> {
                config?.allowsDelayedPaymentMethods == true
            }
            ShippingAddress -> {
                val forceAllow = config?.allowsPaymentMethodsRequiringShippingAddress == true
                (shippingDetails?.isValid == true) || forceAllow
            }
        }
    }?.contains(false) == false
}

private val PaymentIntent.Shipping.isValid: Boolean
    get() = name != null &&
        address.line1 != null &&
        address.country != null &&
        address.postalCode != null

/**
 * Get the LPMS that are supported when used as a Customer Saved LPM given
 * the intent.
 */
internal fun getSupportedSavedCustomerPMs(
    data: PaymentSheetData,
    config: PaymentSheet.Configuration?,
    lpmRepository: LpmRepository
) = data.paymentMethodTypes.mapNotNull {
    lpmRepository.fromCode(it)
}.filter { paymentMethod ->
    val fulfilledRequirements = paymentMethod.getSpecWithFulfilledRequirements(
        data = data,
        config = config,
    )

    paymentMethod.supportsCustomerSavedPM() && fulfilledRequirements != null
}

/**
 * This will return a list of payment methods that have a supported form given
 * the [PaymentSheet.Configuration] and [StripeIntent].
 */
internal fun getPMsToAdd(
    data: PaymentSheetData,
    config: PaymentSheet.Configuration?,
    lpmRepository: LpmRepository
) = data.paymentMethodTypes.mapNotNull {
    lpmRepository.fromCode(it)
}.filter { supportedPaymentMethod ->
    supportedPaymentMethod.getSpecWithFulfilledRequirements(
        data = data,
        config = config,
    ) != null
}.filterNot { supportedPaymentMethod ->
    data.isLiveMode && data.unactivatedPaymentMethods.contains(supportedPaymentMethod.code)
}
