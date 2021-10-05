package com.stripe.android.paymentsheet.elements

/**
 * This class defines requirements of the payment method
 */
//internal enum class Requirement {
//    DelayedPaymentMethodSupport,
//
//    AfterpayCancelSupport,
//    Customer,
//
//    OneTimeUse,
//
//    // capability if: SetupIntent or PaymentIntent w/setup_future_usage = on/off
//    MerchantRequiresSave,
//
//    // capability if: PaymentIntent w/setup_future_usage off
//    UserSelectableSave,
//
//    /**
//     * Different payment method might require some subset of the address
//     * fields, so they must be individually declared
//     */
//    ShippingInIntentAddressLine1,
//    ShippingInIntentAddressLine2,
//    ShippingInIntentAddressCountry,
//    ShippingInIntentAddressState,
//    ShippingInIntentAddressPostal,
//    ShippingInIntentName,
//
//    /**
//     * ReusableMandateSupport means that a mandate needs to be attached
//     * at confirm time to support the payment method. Currently the api
//     * does not support doing this from the SDK.
//     */
//    ReusableMandateSupport,
//}

enum class SaveMode {
    PaymentIntentAndSetupFutureUsageNotSet,
    SetupIntentOrPaymentIntentWithFutureUsageSet,
}

internal data class Blah(
    val layoutSpec: LayoutSpec,
    val showCheckbox: Boolean,
    val showCheckboxControlledFields: Boolean?
)

/**
 * This class is used to define different forms full of fields.
 */
internal data class PaymentMethodFormSpec(
    val paramKey: MutableMap<String, Any?>,

    val PIRequirement: Set<PIRequirement>, // shipping, delayed
    val PIRequirementWithSFU: Set<PIWithSFURequirement>,
    val SIRequirement: Set<SIRequirement>,
    val customerRequirement: Set<CustomerCardRequirement>,

    /** Here consider the type of the saved payment method and what it's requirements are
     * for instance after saving Sofort, Bancontact, and iDEAL it appears as a SEPA payment
     * method which has different effects from it's original form
     */
    val reuseRequirement: Set<MandateRequirement>, // madate, delayed

    /** Unordered list of specs **/
    val layoutSpec: LayoutSpec
)

/**
 * This is the list of requirements for the form
 */
internal data class FormRequirement(
    val intentRequirement: IntentRequirement,
    val sdkRequirements: Set<SdkRequirement>
) {
    val requirements
        get() = intentRequirement.requirements.plus(sdkRequirements)
}

sealed class IntentRequirement(
    val requirements: Set<Requirement>
) {
    internal class SetupIntentRequirement(
        val mandate: MandateRequirement
    ) : IntentRequirement(setOf(mandate))

    internal data class PaymentIntentRequirement(
        val shipping: Set<ShippingIntentRequirement> = emptySet(),
        val mandate: MandateRequirement
    ) : IntentRequirement(shipping.plus(mandate))
}


sealed class Requirement
sealed interface PIRequirement
sealed interface PIWithSFURequirement
sealed interface SIRequirement
sealed interface CustomerCardRequirement

object Mandate : PIRequirement, PIWithSFURequirement, SIRequirement, CustomerCardRequirement
object Shipping : PIRequirement
object Delayed : PIRequirement, PIWithSFURequirement, SIRequirement, CustomerCardRequirement
object AfterpayCancel : PIRequirement


internal class SetupFutureUsageRequirement private constructor(
    val set: Boolean,
    val modifiable: Boolean
) {
    companion object {
        fun createSet() = SetupFutureUsageRequirement(set = true, modifiable = false)
        fun createNotSet(modifiable: Boolean) = SetupFutureUsageRequirement(set = false, modifiable)
    }
}

sealed class MandateRequirement : Requirement() {
    object Required : MandateRequirement()
    object NotRequired : MandateRequirement()
}

sealed class SdkRequirement : Requirement() {
    object AfterpayCancelSupport : SdkRequirement()
    object AllowDelayedPaymentMethods : SdkRequirement()
}

sealed class ShippingIntentRequirement : Requirement() {
    /**
     * Different payment method might require some subset of the address
     * fields, so they must be individually declared
     */
    object NotRequired : ShippingIntentRequirement()
    object AddressLine1 : ShippingIntentRequirement()
    object AddressLine2 : ShippingIntentRequirement()
    object AddressCountry : ShippingIntentRequirement()
    object AddressState : ShippingIntentRequirement()
    object AddressPostal : ShippingIntentRequirement()
    object Name : ShippingIntentRequirement()
}


