package com.stripe.android.paymentsheet.forms

import androidx.annotation.VisibleForTesting
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.elements.PaymentMethodSpec
import com.stripe.android.paymentsheet.elements.Requirement
import com.stripe.android.paymentsheet.model.SupportedPaymentMethod

/**
 * Get the cards that are supported given the current cpabilities.  For saved cards, this
 * should never be cards that are only offered for one time use.
 */
internal fun getSupportedSavedCustomerCards(
    stripeIntent: StripeIntent?,
    config: PaymentSheet.Configuration?
) = getSupportedPaymentMethods(
    stripeIntent,
    config,
    getAllCapabilities(stripeIntent, config).minus(Requirement.OneTimeUse)
)

/**
 * This will get the form layout for the supported method that matches the top pick for the
 * payment method.
 */
internal fun PaymentMethodSpec.getForm(capabilities: Set<Requirement>) =
    getSpecWithFullfilledRequirements(
        capabilities
    )?.layout

/**
 * This will return a list of payment methods that have a supported form given the capabilities.
 */
@VisibleForTesting
internal fun getSupportedPaymentMethods(
    stripeIntentParameter: StripeIntent?,
    config: PaymentSheet.Configuration?,
    capabilities: Set<Requirement> = getAllCapabilities(stripeIntentParameter, config)
): List<SupportedPaymentMethod> {
    stripeIntentParameter?.let { stripeIntent ->
        return stripeIntent.paymentMethodTypes.asSequence().mapNotNull {
            SupportedPaymentMethod.fromCode(it)
        }.filter { supportedPaymentMethod ->
            supportedPaymentMethod.spec.satisfiedBy(capabilities)
        } // .filter { it == SupportedPaymentMethod.Card }
            .toList()
    }

    return emptyList()
}

internal fun PaymentMethodSpec.satisfiedBy(
    capabilities: Set<Requirement>
) = getSpecWithFullfilledRequirements(capabilities) != null

private fun PaymentMethodSpec.getSpecWithFullfilledRequirements(
    capabilities: Set<Requirement>
) = this.specs.firstOrNull {
    fullfilledRequirements(it.requirements, capabilities)
}

/**
 * Capabilities are fulfilled only if all requirements are in the set of capabilities
 */
private fun fullfilledRequirements(
    requirements: Set<Requirement>?,
    capabilities: Set<Requirement>
) = requirements?.map { capabilities.contains(it) }
    ?.contains(false) == false

/**
 * Capabilities is used to refer to what is supported by the combination of the code base,
 * configuration, and values in the StripeIntent. (i.e. SetupIntent, PaymentIntent w/SFU,
 * customer, allowedDelayedSettlement)
 */
internal fun getAllCapabilities(
    stripeIntent: StripeIntent?,
    config: PaymentSheet.Configuration?
) = setOfNotNull(
    Requirement.DelayedPaymentMethodSupport.takeIf { config?.allowsDelayedPaymentMethods == true },
)
    .plus(getReusableCapabilities(stripeIntent, config))
    .plus(intentShippingCapabilities(stripeIntent as? PaymentIntent))

/**
 * These are the capabilities that define if the intent or configuration requires one time use
 * merchant requres save through SetupIntent or PaymentIntent with setup_future_usage = on/off session
 */
private fun getReusableCapabilities(
    stripeIntent: StripeIntent?,
    config: PaymentSheet.Configuration?,
) = when (stripeIntent) {
    is PaymentIntent -> {
        if (stripeIntent.setupFutureUsage == null) {
            if (config?.customer != null &&
                allHaveKnownReuseSupport(stripeIntent.paymentMethodTypes)
            ) {
                listOf(Requirement.UserSelectableSave, Requirement.OneTimeUse)
            } else {
                listOf(Requirement.OneTimeUse)
            }
        } else {
            listOf(Requirement.MerchantRequiresSave)
        }
    }
    is SetupIntent -> {
        listOf(Requirement.MerchantRequiresSave)
    }
    else -> emptyList()
}

/**
 * This gets the specific shipping capabilities in the intent
 */
private fun intentShippingCapabilities(stripeIntent: PaymentIntent?) =
    stripeIntent?.let { paymentIntent ->
        listOfNotNull(
            Requirement.ShippingInIntentName.takeIf {
                paymentIntent.shipping?.name != null
            },
            Requirement.ShippingInIntentAddressLine1.takeIf {
                paymentIntent.shipping?.address?.line1 != null
            },
            Requirement.ShippingInIntentAddressLine2.takeIf {
                paymentIntent.shipping?.address?.line1 != null
            },
            Requirement.ShippingInIntentAddressCountry.takeIf {
                paymentIntent.shipping?.address?.country != null
            },
            Requirement.ShippingInIntentAddressState.takeIf {
                paymentIntent.shipping?.address?.state != null
            },
            Requirement.ShippingInIntentAddressPostal.takeIf {
                paymentIntent.shipping?.address?.postalCode != null
            },
        )
    } ?: emptyList()

private fun allHaveKnownReuseSupport(paymentMethodsInIntent: List<String?>): Boolean {
    // The following PaymentMethods are know to work when PaymentIntent.setup_future_usage = on/off session
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

    //    // PaymentMethods that do not work when PaymentIntent.setup_future_usage = on/off session
//    val notReusable = setOf(
//        "blik",
//        "weChatPay",
//        "grabPay",
//        "FPX",
//        "giropay",
//        "przelewy24",
//        "EPS",
//        "netBanking",
//        "OXXO",
//        "afterpayClearpay",
//        "payPal",
//        "UPI",
//        "boleto"
//    )
}

// /**
// * List of requirements must match all of the requirements in one of the lists
// */
// internal fun ModeConfigurationSpec.getRequirements() = setOfNotNull(
//    oneTimeUseSpec?.requirements?.plus(Requirement.OneTimeUse),
//    merchantRequiredSpec?.requirements?.plus(Requirement.MerchantSelectedSave),
//    userSelectedSaveSpec?.requirements?.plus(Requirement.UserSelectableSave)
// )
//
// internal fun ModeConfigurationSpec.getSpecInMode(modeSelector: SaveModeSelector) =
//    when (modeSelector) {
//        SaveModeSelector.OneTimeUse -> oneTimeUseSpec
//        SaveModeSelector.UserSelectableSave -> userSelectedSaveSpec
//        SaveModeSelector.MerchantSelectableSave -> merchantRequiredSpec
//        SaveModeSelector.None -> null
//    }

// internal fun getSupportedPaymentMethods(
//    stripeIntent: StripeIntent?,
//    config: PaymentSheet.Configuration?
// ) {
//    val supportedPaymentMethod = mutableListOf<SupportedPaymentMethod>()
//    getPreferredSaveModeSelector(stripeIntent, config).forEach {
//        supportedPaymentMethod.addAll(
//            getSupportedPaymentMethods(
//                stripeIntent,
//                config,
//                it
//            )
//        )
//    }
// }
