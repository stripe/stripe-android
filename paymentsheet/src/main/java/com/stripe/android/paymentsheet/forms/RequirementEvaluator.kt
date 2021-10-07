package com.stripe.android.paymentsheet.forms

import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.PaymentSheet

internal sealed interface Requirement
internal sealed interface PIRequirement : Requirement
internal sealed interface SIRequirement : Requirement
internal sealed interface CustomerSavedPMRequirement : Requirement

/**
 * This requirement is dependent on the configuration passed by the app to the SDK.
 */
internal object Delayed : PIRequirement, SIRequirement, CustomerSavedPMRequirement

/**
 * This requirement is dependent on field set in the intent.  Shipping is not present
 * on SetupIntents so this is only a PIRequirement
 */
internal sealed class ShippingIntentRequirement : PIRequirement {
    /**
     * Different payment method might require some subset of the address
     * fields, so they must be individually declared
     */
    object AddressLine1 : ShippingIntentRequirement()
    object AddressLine2 : ShippingIntentRequirement()
    object AddressCountry : ShippingIntentRequirement()
    object AddressState : ShippingIntentRequirement()
    object AddressPostal : ShippingIntentRequirement()
    object Name : ShippingIntentRequirement()
}

/**
 * This is a requirements matcher that operates off of the pi and si requirements
 * in the supported payment method.  It does not concern itself with deficencies in
 * supporting mandates or Payment Methods that are included in the PI, but don't
 * support userSelected PaymentIntents.
 *
 * Once these deficiencies are removed, these requirements can be used for simpler modeling.
 */
internal sealed class RequirementEvaluator(

    /**
     * These are the requirements for using a PaymentIntent.
     *  - Only [PIRequirement]s are allowed in this set.
     * - If this is null, PaymentIntents (even if SFU is set) are not supported by this LPM.
     */
    val piRequirements: Set<PIRequirement>?,

    /**
     * These are the requirements for using a SetupIntent.
     *   - Only [SIRequirement]s are allowed in this set.
     *   - If this is null SetupIntents and PaymentIntents with SFU set are not
     *   supported by this LPM. If SetupIntents are supported, but there are
     *   no additional requirements this must be an emptySet.
     */
    val siRequirements: Set<SIRequirement>?,
) {

    fun supportsCustomerSavedPM(
        stripeIntent: StripeIntent,
        config: PaymentSheet.Configuration?
    ) = true

    fun supportsSI(
        stripeIntent: StripeIntent,
        config: PaymentSheet.Configuration?
    ) = checkRequirements(siRequirements, stripeIntent, config)

    fun supportsPISfuSet(
        stripeIntent: StripeIntent,
        config: PaymentSheet.Configuration?
    ) = checkRequirements(siRequirements, stripeIntent, config) &&
        checkRequirements(piRequirements, stripeIntent, config)

    fun supportsPISfuNotSetable(
        stripeIntent: StripeIntent,
        config: PaymentSheet.Configuration?
    ) = checkRequirements(piRequirements, stripeIntent, config)

    fun supportsPISfuSettable(
        stripeIntent: StripeIntent,
        config: PaymentSheet.Configuration?
    ) = allHaveKnownReuseSupport(stripeIntent.paymentMethodTypes) &&
        config?.customer != null &&
        checkRequirements(piRequirements, stripeIntent, config) &&
        checkRequirements(siRequirements, stripeIntent, config)

    private fun checkRequirements(
        requirements: Set<Requirement>?,
        stripeIntent: StripeIntent,
        config: PaymentSheet.Configuration?
    ) =
        requirements?.map { requirement ->
            when (requirement) {
                Delayed -> config?.allowsDelayedPaymentMethods == true
                is ShippingIntentRequirement -> requirement.isRequirementMet(stripeIntent)
            }
        }?.contains(false) == false
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
                    stripeIntent.shipping?.address?.line2 != null
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
