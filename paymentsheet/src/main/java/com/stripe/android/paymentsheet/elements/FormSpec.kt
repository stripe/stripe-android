package com.stripe.android.paymentsheet.elements

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

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
 * This is the list of requirements for the form
 */
@Parcelize
internal data class FormRequirement(
    val saveMode: SaveMode,
    val requirements: Set<Requirement>
) : Parcelable

/**
 * This class is used to define different forms full of fields.
 */
internal data class PaymentMethodFormSpec(
    val paramKey: MutableMap<String, Any?>,

    /** Unordered list of specs **/
    val requirementFormMapping: Map<FormRequirement, LayoutSpec>
)
