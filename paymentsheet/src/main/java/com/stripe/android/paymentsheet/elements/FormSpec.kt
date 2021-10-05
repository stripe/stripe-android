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

    val PIRequirement: Set<PIRequirement>?,
    val SIRequirement: Set<SIRequirement>?,

    // Cannot be null if you want to use setup intent, or sfu set by merchant
    // or by user.  It can be an empty set if you have no requirements but want
    // these scenarios supported.
    val customerSavedPMRequirement: Set<CustomerSavedPMRequirement>?,

    /** Unordered list of specs **/
    val layoutSpec: LayoutSpec
)

sealed interface Requirement
sealed interface PIRequirement : Requirement
sealed interface PIWithSFURequirement : Requirement
sealed interface SIRequirement : Requirement
sealed interface CustomerSavedPMRequirement : Requirement

object Mandate : PIRequirement, PIWithSFURequirement, SIRequirement, CustomerSavedPMRequirement
object Delayed : PIRequirement, PIWithSFURequirement, SIRequirement, CustomerSavedPMRequirement
object AfterpayCancel : PIRequirement
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


