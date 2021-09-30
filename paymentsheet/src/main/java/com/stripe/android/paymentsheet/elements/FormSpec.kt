package com.stripe.android.paymentsheet.elements

/**
 * This class defines requirements of the payment method
 */
internal enum class Requirement {
    DelayedPaymentMethodSupport,

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

/**
 * This class is used to define different forms full of fields.
 */
internal data class FormSpec(
    val layout: LayoutSpec,
    val requirements: Set<Requirement>,
)

internal data class PaymentMethodSpec(
    val paramKey: MutableMap<String, Any?>,

    /** Ordered list of specs in terms of preference **/
    val specs: List<FormSpec>
)
