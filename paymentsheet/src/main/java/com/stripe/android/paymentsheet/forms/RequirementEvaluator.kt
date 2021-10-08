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
 * in the supported payment method.
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
     *   - In order to make sure the PM can be used when attached to a customer it
     *   must include the requirements of the saved payment method.  For instance,
     *   Bancontact is not delayed, but when saved it is represented as a SEPA paymnent
     *   method which is delayed.  So there must be Delay support in order to meet
     *   the requiremetns of this PM.  (There was a consideration of adding a SaveType
     *   that in cases where SI or PIw/SFU it would also check the requirements of
     *   the SaveType - not sure if the SaveType pi and/or si requirements should be checked).
     */
    val siRequirements: Set<SIRequirement>?,
    val confirmPMFromCustomer: Boolean?,
) {
    fun supportsCustomerSavedPM() = confirmPMFromCustomer == true

    fun supportsSI(
        stripeIntent: StripeIntent,
        config: PaymentSheet.Configuration?
    ) = checkRequirements(siRequirements, stripeIntent, config)

    /**
     * This checks if there is support using this payment method
     * when SFU is already set in the PaymentIntent
     */
    fun supportsPISfuSet(
        stripeIntent: StripeIntent,
        config: PaymentSheet.Configuration?
    ) = checkRequirements(siRequirements, stripeIntent, config) &&
        checkRequirements(piRequirements, stripeIntent, config)

    /**
     * This detects if there is support with using this with the PI
     * even while not allowing the user to set SFU.
     */
    fun supportsPISfuNotSetable(
        stripeIntent: StripeIntent,
        config: PaymentSheet.Configuration?
    ) = checkRequirements(piRequirements, stripeIntent, config)

    /**
     * This checks to see if this PM is supported with the given
     * payment intent and configuration.
     *
     * The customer ID is required to be passed in the configuration
     * (the sdk cannot know if the PI has a customer ID associated with it),
     * so that we can guarantee to the user that the PM will be associated
     * with their customer object AND accessible when opening PaymentSheet
     * and seeing the saved PMs associate with their customer object.
     *
     * There is also a requirement here that all PMs in the Intent have support
     * for reuse, this is due to a bug.
     */
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
