package com.stripe.android.paymentsheet.elements

/**
 * This class defines requirements of the payment method
 */
internal enum class Requirement {
    DelayedPaymentMethodSupport,

    AfterpayCancelSupport,
    Customer,

    OneTimeUse,

    // capability if: SetupIntent or PaymentIntent w/setup_future_usage = on/off
    MerchantRequiresSave,

    // capability if: PaymentIntent w/setup_future_usage off
    UserSelectableSave,

    /**
     * Different payment method might require some subset of the address
     * fields, so they must be individually declared
     */
    ShippingInIntentAddressLine1,
    ShippingInIntentAddressLine2,
    ShippingInIntentAddressCountry,
    ShippingInIntentAddressState,
    ShippingInIntentAddressPostal,
    ShippingInIntentName,

    /**
     * ReusableMandateSupport means that a mandate needs to be attached
     * at confirm time to support the payment method. Currently the api
     * does not support doing this from the SDK.
     */
    ReusableMandateSupport,
}

enum class SaveMode {
    PaymentIntentAndSetupFutureUsageNotSet,
    SetupIntentOrPaymentIntentWithFutureUsageSet,
}


/**
 * This class is used to define different forms full of fields.
 */
internal data class PaymentMethodFormSpec(
    val paramKey: MutableMap<String, Any?>,

    /** Unordered list of specs **/
    val requirementFormMapping: Map<FormRequirement, LayoutSpec>
)

/**
 * This is the list of requirements for the form
 */
internal data class FormRequirement(
    val intentRequirement: IntentRequirement,
    val sdkRequirements: Set<SdkRequirement>
)

sealed class IntentRequirement {

    object SetupIntentRequirement : IntentRequirement()

    internal data class PaymentIntentRequirement(
        val setupFutureUsage: SetupFutureUsageRequirement,
        val shipping: Set<ShippingIntentRequirement> = emptySet()
    ) : IntentRequirement()
}

internal class SetupFutureUsageRequirement private constructor(
    val set: Boolean,
    val modifiable: Boolean
) {
    companion object {
        fun createSet() = SetupFutureUsageRequirement(set = true, modifiable = false)
        fun createNotSet(modifiable: Boolean) = SetupFutureUsageRequirement(set = false, modifiable)
    }
}

internal enum class SdkRequirement {
    ReuseMandateSupport,
    AfterpayCancelSupport,
    AllowDelayedPaymentMethods
}

internal enum class ShippingIntentRequirement {
    /**
     * Different payment method might require some subset of the address
     * fields, so they must be individually declared
     */
    AddressLine1,
    AddressLine2,
    AddressCountry,
    AddressState,
    AddressPostal,
    Name,

}
