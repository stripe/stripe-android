package com.stripe.android.paymentsheet.forms

import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.PaymentSheet

sealed interface Requirement
sealed interface PIRequirement : Requirement
sealed interface SIRequirement : Requirement
sealed interface CustomerSavedPMRequirement : Requirement

/**
 * This requirement is dependent on the configuration passed by the app to the SDK.
 */
object Delayed : PIRequirement, SIRequirement, CustomerSavedPMRequirement

/**
 * This requirement is dependent on field set in the intent.  Shipping is not present
 * on SetupIntents so this is only a PIRequirement
 */
sealed class ShippingIntentRequirement : PIRequirement {
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
 * TODO: No longer accurate
 * This is a requirements matcher that operates off of the pi and si requirements
 * in the supported payment method.  It does not concern itself with deficencies in
 * supporting mandates or Payment Methods that are included in the PI, but don't
 * support userSelected PaymentIntents.
 *
 * Once these deficiencies are removed, these requirements can be used for simpler modeling.
 */
internal sealed class RequirementDynamicEvaluator(

    /**
     * These are the requirements for using a PaymentIntent.
     *  - Only [PIRequirement]s are allowed in this set.
     * - If this is null, PaymentIntents (even if SFU is set) are not supported by this LPM.
     * - It should be noted that if SFU is set then SDK
     * code will verify that the siRequirement is also satisfied in order to use it.
     * This is to give a good experience to our users so that after using this with
     * a customer (either in the payment intent or passed to payment sheet) saving a
     * payment method in payment sheet they can see it again.  Granted it is possible
     * to use PI w/SFU set and SI with no customer, but we will not make this distinction.
     */
    val piRequirements: Set<PIRequirement>?,

    /**
     * These are the requirements for using a SetupIntent.
     *   - Only [SIRequirement]s are allowed in this set.
     *   - If this is null PaymentIntents are not supported by this LPM.
     *   If PaymentIntents are supported, but there are no additional requirements
     *   this must be an emptySet.
     */
    val siRequirements: Set<SIRequirement>?,

    /**
     * These are the requirements to be able to use an LPM after it has been saved/associated
     * to/with a customer.  When retrieving saved payment methods for a customer we will
     * only retrieve and show those that satisfy this requirement and the
     * [PaymentMethodFormSpec.piRequirement] or [PaymentMethodFormSpec.siRequirement]
     *    - In cases where the saved payment method is different from
     * the original PM, these requirements maybe be different from the SI Requirements.
     * One example is that iDEAL is not a delayed payment method so the SI requirement does not contain
     * Delayed, but iDEAL is saved as a SEPA payment method, which is delayed.
     *    - Cannot be null if you want to use setup intent, or sfu set by merchant or
     * by user.  It can be an empty set if you have no extra requirements.
     *    - It is possible that there are payment methods saved from another Stripe integration
     *    with the merchant, so even though it can't be saved by PaymentSheet doesn't mean
     *    it won't be saved through another mechanism TODO: What does this mean for ideal/sofort
     */

    // Bancontact, sofort and ideal payments cannot be attached to a customer, it feels
    // more like this should be

    //val customerSavedPMRequirement: Set<CustomerSavedPMRequirement>?
) : RequirementEvaluator() {

    override fun supportsCustomerSavedPM(
        stripeIntent: StripeIntent,
        config: PaymentSheet.Configuration?
    ) = true

    override fun supportsSI(
        stripeIntent: StripeIntent,
        config: PaymentSheet.Configuration?
    ) = checkRequirements(siRequirements, stripeIntent, config)

    override fun supportsPISfuSet(
        stripeIntent: StripeIntent,
        config: PaymentSheet.Configuration?
    ) = checkRequirements(siRequirements, stripeIntent, config) &&
        checkRequirements(piRequirements, stripeIntent, config)

    override fun supportsPISfuNotSetable(
        stripeIntent: StripeIntent,
        config: PaymentSheet.Configuration?
    ) = checkRequirements(piRequirements, stripeIntent, config)

    override fun supportsPISfuSettable(
        stripeIntent: StripeIntent,
        config: PaymentSheet.Configuration?
    ) = checkRequirements(piRequirements, stripeIntent, config) &&
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
        }?.contains(false) != true


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
