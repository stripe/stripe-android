package com.stripe.android.paymentsheet.forms

import androidx.annotation.VisibleForTesting
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.elements.AfterpayCancel
import com.stripe.android.paymentsheet.elements.Blah
import com.stripe.android.paymentsheet.elements.CustomerSavedPMRequirement
import com.stripe.android.paymentsheet.elements.Delayed
import com.stripe.android.paymentsheet.elements.Mandate
import com.stripe.android.paymentsheet.elements.PIRequirement
import com.stripe.android.paymentsheet.elements.PaymentMethodFormSpec
import com.stripe.android.paymentsheet.elements.SIRequirement
import com.stripe.android.paymentsheet.elements.ShippingIntentRequirement
import com.stripe.android.paymentsheet.model.SupportedPaymentMethod

internal data class SavedCustomerCardRequirementMatcher(
    val stripeIntent: StripeIntent,
    val config: PaymentSheet.Configuration?
) {
    val mode: Mode = Mode.SavedCustomerCard

}

internal data class AddNewRequirementMatcher(
    val stripeIntent: StripeIntent,
    val config: PaymentSheet.Configuration?
) {
    /**
     * Get the LPMS that are supported given the current cpabilities.
     */
    internal fun getSupportedSavedCustomerPMs() =
        stripeIntent.paymentMethodTypes.asSequence().mapNotNull {
            SupportedPaymentMethod.fromCode(it)
        }.filter {
            checkCustomerSavedRequirements(it.formSpec.customerSavedPMRequirement)
        }.filter {
            // Here SFU doesn't matter because it is already saved.
            when (stripeIntent) {
                is PaymentIntent -> checkPIRequirement(it.formSpec.PIRequirement)
                is SetupIntent -> checkSIRequirement(it.formSpec.SIRequirement)
            }
        }

    /**
     * This will get the form layout for the supported method that matches the top pick for the
     * payment method.  It should be known that this payment method has a form
     * that matches the capabilities already.
     */
    internal fun getPMAddForm(spec: PaymentMethodFormSpec) = getSpecWithFullfilledRequirements(spec)

    /**
     * This will return a list of payment methods that have a supported form given the capabilities.
     */
    @VisibleForTesting
    internal fun getPMsToAdd(
    ) = stripeIntent.paymentMethodTypes.asSequence().mapNotNull {
        SupportedPaymentMethod.fromCode(it)
    }.filter { supportedPaymentMethod ->
        getSpecWithFullfilledRequirements(supportedPaymentMethod.formSpec) != null
    }//TODO.filter { it == SupportedPaymentMethod.Card }
        .toList()

    private fun getSpecWithFullfilledRequirements(spec: PaymentMethodFormSpec): Blah? {
        val oneTimeUse = Blah(spec.layoutSpec, false, false)
        val merchantRequestedSave = Blah(spec.layoutSpec, false, true)
        val userSelectableSave = Blah(spec.layoutSpec, true, null)

        return when (stripeIntent) {
            is PaymentIntent -> {
                if ((stripeIntent.setupFutureUsage == null || stripeIntent.setupFutureUsage == StripeIntent.Usage.OneTime)
                ) {
                    if (config?.customer != null &&
                        allHaveKnownReuseSupport(stripeIntent.paymentMethodTypes) &&
                        checkPIRequirement(spec.PIRequirement) &&
                        checkCustomerSavedRequirements(spec.customerSavedPMRequirement)
                    ) {
                        userSelectableSave
                    } else if (checkPIRequirement(spec.PIRequirement)) {
                        oneTimeUse
                    } else {
                        null
                    }
                } else if (
                    checkPIRequirement(spec.PIRequirement) &&
                    checkCustomerSavedRequirements(spec.customerSavedPMRequirement)
                ) {
                    merchantRequestedSave
                } else {
                    null
                }
            }
            is SetupIntent -> {
                if (checkSIRequirement(spec.SIRequirement)) {
                    merchantRequestedSave
                } else {
                    null
                }
            }
        }
    }

    private fun checkPIRequirement(requirements: Set<PIRequirement>?) = requirements?.map {
        when (it) {
            AfterpayCancel -> false
            Delayed -> config?.allowsDelayedPaymentMethods == true
            Mandate -> true
            is ShippingIntentRequirement -> it.isRequirementMet(stripeIntent)
        }
    }?.contains(false) == false

    private fun checkSIRequirement(requirements: Set<SIRequirement>?) = requirements?.map {
        when (it) {
            Mandate -> true
            Delayed -> config?.allowsDelayedPaymentMethods == true
        }
    }?.contains(false) == false

    private fun checkCustomerSavedRequirements(requirements: Set<CustomerSavedPMRequirement>?) =
        requirements?.map {
            when (it) {
                Mandate -> false
                Delayed -> config?.allowsDelayedPaymentMethods == true
            }
        }?.contains(false) == false

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
}


internal fun ShippingIntentRequirement.isRequirementMet(
    stripeIntent: StripeIntent
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
