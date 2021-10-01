package com.stripe.android.paymentsheet.forms

import androidx.annotation.VisibleForTesting
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.elements.FormRequirement
import com.stripe.android.paymentsheet.elements.PaymentMethodFormSpec
import com.stripe.android.paymentsheet.elements.Requirement
import com.stripe.android.paymentsheet.elements.SaveMode
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
    if (supportedForms.isEmpty()) {
        return null
    }

    // TODO: THis is jsut getting hte first accross payment methods?
    // Prefer requirement with customer support if possible so we can
    // show user selectable save
    return supportedForms.firstOrNull {
        it.saveMode == SaveMode.PaymentIntentAndSetupFutureUsageNotSet &&
            it.requirements.contains(Requirement.Customer)
    } ?: supportedForms.first()
}

/**
 * Capabilities are fulfilled only if all requirements are in the set of capabilities
 */
private fun fullfilledRequirements(
    formRequirements: FormRequirement?,
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

    return formRequirements?.saveMode == formCapabilities.saveMode &&
        !formRequirements.requirements.map {
            formCapabilities.requirements.contains(it)
        }.contains(false)
}

/**
 * Capabilities is used to refer to what is supported by the combination of the code base,
 * configuration, and values in the StripeIntent. (i.e. SetupIntent, PaymentIntent w/SFU,
 * customer, allowedDelayedSettlement)
 */
internal fun getAllCapabilities(
    stripeIntent: StripeIntent,
    config: PaymentSheet.Configuration?
): FormRequirement {
    val formRequirement = getSaveMode(stripeIntent, config)
    return FormRequirement(
        formRequirement.saveMode,
        formRequirement.requirements.plus(
            setOfNotNull(
                Requirement.DelayedPaymentMethodSupport.takeIf { config?.allowsDelayedPaymentMethods == true },
            ).plus(
                intentShippingCapabilities(stripeIntent as? PaymentIntent)
            )
        )
    )
}

/**
 * These are the capabilities that define if the intent or configuration requires one time use
 * merchant requres save through SetupIntent or PaymentIntent with setup_future_usage = on/off session
 */
private fun getSaveMode(
    stripeIntent: StripeIntent,
    config: PaymentSheet.Configuration?,
) = when (stripeIntent) {
    is PaymentIntent -> {
        if (stripeIntent.setupFutureUsage == null || stripeIntent.setupFutureUsage == StripeIntent.Usage.OneTime) {
            if (config?.customer != null &&
                allHaveKnownReuseSupport(stripeIntent.paymentMethodTypes)
            ) {
                FormRequirement(
                    SaveMode.PaymentIntentAndSetupFutureUsageNotSet,
                    requirements = setOf(Requirement.Customer)
                )
            } else {
                FormRequirement(
                    SaveMode.PaymentIntentAndSetupFutureUsageNotSet,
                    requirements = emptySet()
                )
            }
        } else {
            FormRequirement(
                SaveMode.SetupIntentOrPaymentIntentWithFutureUsageSet,
                requirements = emptySet()
            )
        }
    }
    is SetupIntent -> {
        FormRequirement(
            SaveMode.SetupIntentOrPaymentIntentWithFutureUsageSet,
            requirements = emptySet()
        )
    }
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

