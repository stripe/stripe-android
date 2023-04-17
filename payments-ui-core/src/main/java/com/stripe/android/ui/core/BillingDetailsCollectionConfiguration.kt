package com.stripe.android.ui.core

import android.os.Parcelable
import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize

/**
 * Configuration for how billing details are collected during checkout.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Parcelize
data class BillingDetailsCollectionConfiguration(
    /**
     * How to collect the name field.
     */
    val name: CollectionMode = CollectionMode.Automatic,

    /**
     * How to collect the phone field.
     */
    val phone: CollectionMode = CollectionMode.Automatic,

    /**
     * How to collect the email field.
     */
    val email: CollectionMode = CollectionMode.Automatic,

    /**
     * How to collect the billing address.
     */
    val address: AddressCollectionMode = AddressCollectionMode.Automatic,

    /**
     * Whether the values included in `PaymentSheet.Configuration.defaultBillingDetails`
     * should be attached to the payment method, this includes fields that aren't displayed in the form.
     *
     * If `false` (the default), those values will only be used to prefill the corresponding fields in the form.
     */
    val attachDefaultsToPaymentMethod: Boolean = false,
) : Parcelable {

    /**
     * Billing details fields collection options.
     */
    enum class CollectionMode {
        /**
         * The field will be collected depending on the Payment Method's requirements.
         */
        Automatic,

        /**
         * The field will never be collected.
         * If this field is required by the Payment Method, you must provide it as part of `defaultBillingDetails`.
         */
        Never,

        /**
         * The field will always be collected, even if it isn't required for the Payment Method.
         */
        Always,
    }

    /**
     * Billing address collection options.
     */
    enum class AddressCollectionMode {
        /**
         * Only the fields required by the Payment Method will be collected, this may be none.
         */
        Automatic,

        /**
         * Address will never be collected.
         * If the Payment Method requires a billing address, you must provide it as part of `defaultBillingDetails`.
         */
        Never,

        /**
         * Collect the full billing address, regardless of the Payment Method requirements.
         */
        Full,
    }
}
