package com.stripe.android.paymentsheet.forms

import androidx.annotation.VisibleForTesting
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.elements.Blah
import com.stripe.android.paymentsheet.elements.FormRequirement
import com.stripe.android.paymentsheet.elements.MandateRequirement
import com.stripe.android.paymentsheet.elements.PaymentMethodFormSpec
import com.stripe.android.paymentsheet.elements.Requirement
import com.stripe.android.paymentsheet.elements.SdkRequirement
import com.stripe.android.paymentsheet.elements.ShippingIntentRequirement
import com.stripe.android.paymentsheet.model.SupportedPaymentMethod

internal data class SavedCustomerCardRequirementMatcher(
    val stripeIntent: StripeIntent,
    val config: PaymentSheet.Configuration?
) {
    val mode: Mode = Mode.SavedCustomerCard

    /**
     * Get the cards that are supported given the current cpabilities.  For saved cards, this
     * should never be cards that are only offered for one time use.
     */
    internal fun getSupportedSavedCustomerCards(
        stripeIntent: StripeIntent,
        config: PaymentSheet.Configuration?
    ) = when(stripeIntent){
        is PaymentIntent -> {

        }
        is SetupIntent -> {

        }
    }
}

internal data class AddNewRequirementMatcher(
    val stripeIntent: StripeIntent,
    val config: PaymentSheet.Configuration?
) {
    val mode: Mode = Mode.AddNew

    /**
     * This will get the form layout for the supported method that matches the top pick for the
     * payment method.  It should be known that this payment method has a form
     * that matches the capabilities already.
     */
    internal fun getForm(spec: PaymentMethodFormSpec) = getSpecWithFullfilledRequirements(spec)

    /**
     * This will return a list of payment methods that have a supported form given the capabilities.
     */
    @VisibleForTesting
    internal fun getSupportedPaymentMethods(
    ): List<SupportedPaymentMethod> {
        return stripeIntent.paymentMethodTypes.asSequence().mapNotNull {
            SupportedPaymentMethod.fromCode(it)
        }.filter { supportedPaymentMethod ->
            satisfiedBy(supportedPaymentMethod.formSpec)
        }//TODO.filter { it == SupportedPaymentMethod.Card }
            .toList()
    }

    fun satisfiedBy(
        spec: PaymentMethodFormSpec
    ) = getSpecWithFullfilledRequirements(spec) != null

    fun getSpecWithFullfilledRequirements(spec: PaymentMethodFormSpec): Blah? {
        val oneTimeUse = Blah(spec.layoutSpec, false, false)
        val merchantRequestedSave = Blah(spec.layoutSpec, false, true)
        val userSelectableSave = Blah(spec.layoutSpec, true, null)

        return when (stripeIntent) {
            is PaymentIntent -> {
                if ((stripeIntent.setupFutureUsage == null || stripeIntent.setupFutureUsage == StripeIntent.Usage.OneTime)
                ) {
                    if (config?.customer != null &&
                        allHaveKnownReuseSupport(stripeIntent.paymentMethodTypes) &&
                        fullfilledRequirements(spec.PIRequirementWithSave)
                    ) {
                        userSelectableSave
                    } else if (fullfilledRequirements(spec.PIRequirement)) {
                        oneTimeUse
                    } else {
                        null
                    }
                } else if (fullfilledRequirements(spec.PIRequirementWithSave)) {
                    merchantRequestedSave
                } else {
                    null
                }
            }
            is SetupIntent -> {
                if (fullfilledRequirements(spec.SIRequirement)) {
                    merchantRequestedSave
                } else {
                    null
                }
            }
        }
    }

    /**
     * Capabilities are fulfilled only if all requirements are in the set of capabilities
     */
    private fun fullfilledRequirements(
        requirements: Set<Requirement>
    ) = !requirements.map {
        when (it) {
            is MandateRequirement -> it.isRequirementMet(stripeIntent, config, mode)
            is SdkRequirement -> it.isRequirementMet(stripeIntent, config, mode)
            is ShippingIntentRequirement -> it.isRequirementMet(stripeIntent, config, mode)
        }
    }.contains(false)
}


//private fun PaymentMethodFormSpec.getSpecWithFullfilledRequirementssdfsdfsdf(
//)  : Blah? {
//    val oneTimeUse = Blah(layoutSpec, false, false)
//    val merchantRequestedSave = Blah(layoutSpec, false, true)
//    val userSelectableSave = Blah(layoutSpec, true, null)
//
//    when (stripeIntent) {
//        is PaymentIntent -> {
//            if ((stripeIntent.setupFutureUsage == null || stripeIntent.setupFutureUsage == StripeIntent.Usage.OneTime)
//            ) {
//                if (config?.customer != null &&
//                    allHaveKnownReuseSupport(stripeIntent.paymentMethodTypes) &&
//                    fullfilledRequirements(this.PIRequirementWithSave, stripeIntent, config, mode)
//                ) {
//                    userSelectableSave
//                } else if(fullfilledRequirements(this.PIRequirement, stripeIntent, config, mode)){
//                    oneTimeUse
//                }
//            } else if(fullfilledRequirements(this.PIRequirementWithSave, stripeIntent, config, mode)){
//                merchantRequestedSave
//            }
//            else {
//                null
//            }
//        }
//        is SetupIntent -> {
//            if(fullfilledRequirements(this.SIWithSave, stripeIntent, config, mode)) {
//                merchantRequestedSave
//            }
//            else {
//                null
//            }
//        }
//    }
//}

internal enum class Mode {
    AddNew,
    SavedCustomerCard
}


private fun isSetupIntentOrSetupFutureUsageSet(stripeIntent: StripeIntent) =
    when (stripeIntent) {
        is PaymentIntent -> stripeIntent.setupFutureUsage != null && stripeIntent.setupFutureUsage != StripeIntent.Usage.OneTime
        is SetupIntent -> true
    }


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

internal fun MandateRequirement.isRequirementMet(
    stripeIntent: StripeIntent,
    config: PaymentSheet.Configuration?,
    mode: Mode
) = when (this) {
    MandateRequirement.NotRequired -> true
    MandateRequirement.Required ->
        mode != Mode.SavedCustomerCard && !isSetupIntentOrSetupFutureUsageSet(stripeIntent)
}


internal fun ShippingIntentRequirement.isRequirementMet(
    stripeIntent: StripeIntent,
    config: PaymentSheet.Configuration?,
    mode: Mode,
) {
    when (stripeIntent) {
        is PaymentIntent -> {
            when (this) {
                is ShippingIntentRequirement.Name -> {
                    stripeIntent.shipping?.name != null
                }
                is ShippingIntentRequirement.AddressLine1 -> {
                    stripeIntent.shipping?.address?.line1 != null
                }
                is ShippingIntentRequirement.AddressLine2 -> {
                    stripeIntent.shipping?.address?.line1 != null
                }
                is ShippingIntentRequirement.AddressCountry -> {
                    stripeIntent.shipping?.address?.country != null
                }
                is ShippingIntentRequirement.AddressState -> {
                    stripeIntent.shipping?.address?.state != null
                }
                is ShippingIntentRequirement.AddressPostal -> {
                    stripeIntent.shipping?.address?.postalCode != null
                }
            }
        }
        is SetupIntent -> {
            false
        }
    }
}

internal fun SdkRequirement.isRequirementMet(
    stripeIntent: StripeIntent,
    config: PaymentSheet.Configuration?,
    mode: Mode
) {
    when (this) {
        SdkRequirement.AfterpayCancelSupport -> false
        SdkRequirement.AllowDelayedPaymentMethods -> config?.allowsDelayedPaymentMethods == true
    }
}

/**
 * These are the capabilities that define if the intent or configuration requires one time use
 * merchant requres save through SetupIntent or PaymentIntent with setup_future_usage = on/off session
 */
//private fun getIntentRequirements(
//    stripeIntent: StripeIntent,
//    config: PaymentSheet.Configuration?,
//) = when (stripeIntent) {
//    is PaymentIntent -> {
//        val shippingRequirement = intentShippingCapabilities(stripeIntent)
//        if (stripeIntent.setupFutureUsage == null || stripeIntent.setupFutureUsage == StripeIntent.Usage.OneTime) {
//            if (config?.customer != null &&
//                allHaveKnownReuseSupport(stripeIntent.paymentMethodTypes)
//            ) {
//                IntentRequirement.PaymentIntentRequirement(
//                    SetupFutureUsageRequirement.createNotSet(true),
//                    shippingRequirement,
//                    MandateRequirement.Required or NotRquired
//                )
//            } else {
//                IntentRequirement.PaymentIntentRequirement(
//                    SetupFutureUsageRequirement.createNotSet(false),
//                    shippingRequirement,
//                    MandateRequirement.NotRequired
//                )
//            }
//        } else {
//            IntentRequirement.PaymentIntentRequirement(
//                SetupFutureUsageRequirement.createSet(),
//                shippingRequirement,
//                MandateRequirement.NotRequired
//            )
//        }
//    }
//    is SetupIntent -> {
//        IntentRequirement.SetupIntentRequirement(
//            MandateRequirement.Required
//        )
//    }
//}


/**
 * This gets the specific shipping capabilities in the intent
 */
//private fun intentShippingCapabilities(stripeIntent: PaymentIntent?) =
//    stripeIntent?.let { paymentIntent ->
//        setOfNotNull(
//            ShippingIntentRequirement.Name.takeIf {
//                paymentIntent.shipping?.name != null
//            },
//            ShippingIntentRequirement.AddressLine1.takeIf {
//                paymentIntent.shipping?.address?.line1 != null
//            },
//            ShippingIntentRequirement.AddressLine2.takeIf {
//                paymentIntent.shipping?.address?.line1 != null
//            },
//            ShippingIntentRequirement.AddressCountry.takeIf {
//                paymentIntent.shipping?.address?.country != null
//            },
//            ShippingIntentRequirement.AddressState.takeIf {
//                paymentIntent.shipping?.address?.state != null
//            },
//            ShippingIntentRequirement.AddressPostal.takeIf {
//                paymentIntent.shipping?.address?.postalCode != null
//            },
//        )
//    } ?: emptySet()
