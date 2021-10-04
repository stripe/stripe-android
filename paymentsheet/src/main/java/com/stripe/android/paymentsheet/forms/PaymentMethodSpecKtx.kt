package com.stripe.android.paymentsheet.forms

import androidx.annotation.VisibleForTesting
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.elements.FormRequirement
import com.stripe.android.paymentsheet.elements.IntentRequirement
import com.stripe.android.paymentsheet.elements.PaymentMethodFormSpec
import com.stripe.android.paymentsheet.elements.SdkRequirement
import com.stripe.android.paymentsheet.elements.SetupFutureUsageRequirement
import com.stripe.android.paymentsheet.elements.ShippingIntentRequirement
import com.stripe.android.paymentsheet.model.SupportedPaymentMethod

/**
 * Get the cards that are supported given the current cpabilities.  For saved cards, this
 * should never be cards that are only offered for one time use.
 */
internal fun getSupportedSavedCustomerCards(
    stripeIntent: StripeIntent,
    config: PaymentSheet.Configuration?
) = getSupportedPaymentMethods(
    stripeIntent,
    config,
    getAllCapabilities(
        stripeIntent,
        config
    ) // TODO: what if this returns customer with setup future usage
)

/**
 * This will get the form layout for the supported method that matches the top pick for the
 * payment method.  It should be known that this payment method has a form
 * that matches the capabilities already.
 */
internal fun PaymentMethodFormSpec.getForm(capabilities: FormRequirement) =
    requirementFormMapping[getSpecWithFullfilledRequirements(capabilities)]

/**
 * This will return a list of payment methods that have a supported form given the capabilities.
 */
@VisibleForTesting
internal fun getSupportedPaymentMethods(
    stripeIntent: StripeIntent,
    config: PaymentSheet.Configuration?,
    capabilities: FormRequirement = getAllCapabilities(stripeIntent, config)
): List<SupportedPaymentMethod> {
    return stripeIntent.paymentMethodTypes.asSequence().mapNotNull {
        SupportedPaymentMethod.fromCode(it)
    }.filter { supportedPaymentMethod ->
        supportedPaymentMethod.formSpec.satisfiedBy(capabilities)
    }//TODO.filter { it == SupportedPaymentMethod.Card }
        .toList()
}

internal fun PaymentMethodFormSpec.satisfiedBy(
    capabilities: FormRequirement
) = getSpecWithFullfilledRequirements(capabilities) != null

private fun PaymentMethodFormSpec.getSpecWithFullfilledRequirements(
    capabilities: FormRequirement
): FormRequirement? {
    val supportedForms = this.requirementFormMapping.keys.filter {
        fullfilledRequirements(it, capabilities)
    }

    return if (supportedForms.isEmpty()) {
        null
    } else {
        // Prefer requirement with customer support if possible so we can
        // show user selectable save
        return supportedForms.firstOrNull {
            (it.intentRequirement as? IntentRequirement.PaymentIntentRequirement)
                ?.setupFutureUsage == SetupFutureUsageRequirement.createNotSet(true)
        } ?: supportedForms.first()
    }
}

/**
 * Capabilities are fulfilled only if all requirements are in the set of capabilities
 */
private fun fullfilledRequirements(
    formRequirements: FormRequirement,
    formCapabilities: FormRequirement
): Boolean {
//    // TODO: switch to use the logger
//    if (formRequirements?.saveMode != formCapabilities.saveMode) {
//        println(
//            "Save mode does not match: ${formRequirements?.saveMode} != ${formCapabilities.saveMode}"
//        )
//    }
//    if (formRequirements?.requirements?.map {
//            formCapabilities.requirements.contains(it)
//        }?.contains(false) == true) {
//        println(
//            "Requirements don't match: ${formRequirements.requirements} != ${formCapabilities.requirements}"
//        )
//
//    }

    return when (formCapabilities.intentRequirement) {
        is IntentRequirement.PaymentIntentRequirement -> {
            if (formRequirements.intentRequirement !is IntentRequirement.PaymentIntentRequirement) {
                false
            } else {
                return (formCapabilities.intentRequirement.setupFutureUsage ==
                    formRequirements.intentRequirement.setupFutureUsage) &&
                    (
                        formRequirements.intentRequirement.shipping.isEmpty() ||
                            formCapabilities.intentRequirement.shipping.containsAll(
                                formRequirements.intentRequirement.shipping
                            )
                        ) &&
                    formCapabilities.sdkRequirements.containsAll(formRequirements.sdkRequirements)
            }
        }
        IntentRequirement.SetupIntentRequirement -> {
            formCapabilities.sdkRequirements.containsAll(formRequirements.sdkRequirements)
        }// continue
    }
}

/**
 * Capabilities is used to refer to what is supported by the combination of the code base,
 * configuration, and values in the StripeIntent. (i.e. SetupIntent, PaymentIntent w/SFU,
 * customer, allowedDelayedSettlement)
 */
internal fun getAllCapabilities(
    stripeIntent: StripeIntent,
    config: PaymentSheet.Configuration?
) = FormRequirement(
    getIntentRequirements(stripeIntent, config),
    setOfNotNull(
        SdkRequirement.AllowDelayedPaymentMethods.takeIf { config?.allowsDelayedPaymentMethods == true },
    )
)

/**
 * These are the capabilities that define if the intent or configuration requires one time use
 * merchant requres save through SetupIntent or PaymentIntent with setup_future_usage = on/off session
 */
private fun getIntentRequirements(
    stripeIntent: StripeIntent,
    config: PaymentSheet.Configuration?,
) = when (stripeIntent) {
    is PaymentIntent -> {
        val shippingRequirement = intentShippingCapabilities(stripeIntent)
        if (stripeIntent.setupFutureUsage == null || stripeIntent.setupFutureUsage == StripeIntent.Usage.OneTime) {
            if (config?.customer != null &&
                allHaveKnownReuseSupport(stripeIntent.paymentMethodTypes)
            ) {
                IntentRequirement.PaymentIntentRequirement(
                    SetupFutureUsageRequirement.createNotSet(true),
                    shippingRequirement
                )
            } else {
                IntentRequirement.PaymentIntentRequirement(
                    SetupFutureUsageRequirement.createNotSet(false),
                    shippingRequirement
                )
            }
        } else {
            IntentRequirement.PaymentIntentRequirement(
                SetupFutureUsageRequirement.createSet(),
                shippingRequirement
            )
        }
    }
    is SetupIntent -> {
        IntentRequirement.SetupIntentRequirement
    }
}

/**
 * This gets the specific shipping capabilities in the intent
 */
private fun intentShippingCapabilities(stripeIntent: PaymentIntent?) =
    stripeIntent?.let { paymentIntent ->
        setOfNotNull(
            ShippingIntentRequirement.Name.takeIf {
                paymentIntent.shipping?.name != null
            },
            ShippingIntentRequirement.AddressLine1.takeIf {
                paymentIntent.shipping?.address?.line1 != null
            },
            ShippingIntentRequirement.AddressLine2.takeIf {
                paymentIntent.shipping?.address?.line1 != null
            },
            ShippingIntentRequirement.AddressCountry.takeIf {
                paymentIntent.shipping?.address?.country != null
            },
            ShippingIntentRequirement.AddressState.takeIf {
                paymentIntent.shipping?.address?.state != null
            },
            ShippingIntentRequirement.AddressPostal.takeIf {
                paymentIntent.shipping?.address?.postalCode != null
            },
        )
    } ?: emptySet()

private fun allHaveKnownReuseSupport(paymentMethodsInIntent: List<String?>): Boolean {
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

