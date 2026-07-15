package com.stripe.android.checkout

import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.stripe.android.paymentelement.CheckoutSessionPreview
import kotlinx.parcelize.Parcelize

/**
 * Configuration for how billing details are collected during checkout.
 */
@CheckoutSessionPreview
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class BillingDetailsCollectionConfiguration {

    private var name: CollectionMode = CollectionMode.Automatic
    private var phone: CollectionMode = CollectionMode.Automatic
    private var email: CollectionMode = CollectionMode.Automatic
    private var address: AddressCollectionMode = AddressCollectionMode.Automatic

    /** How to collect the name field. */
    fun name(name: CollectionMode): BillingDetailsCollectionConfiguration = apply {
        this.name = name
    }

    /** How to collect the phone field. */
    fun phone(phone: CollectionMode): BillingDetailsCollectionConfiguration = apply {
        this.phone = phone
    }

    /** How to collect the email field. */
    fun email(email: CollectionMode): BillingDetailsCollectionConfiguration = apply {
        this.email = email
    }

    /** How to collect the billing address. */
    fun address(address: AddressCollectionMode): BillingDetailsCollectionConfiguration = apply {
        this.address = address
    }

    @Parcelize
    internal data class State(
        val name: CollectionMode,
        val phone: CollectionMode,
        val email: CollectionMode,
        val address: AddressCollectionMode,
    ) : Parcelable

    internal fun build(): State = State(
        name = name,
        phone = phone,
        email = email,
        address = address,
    )

    /**
     * Billing details fields collection options.
     */
    @CheckoutSessionPreview
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    enum class CollectionMode {
        /**
         * The field will be collected depending on the Payment Method's requirements.
         */
        Automatic,

        /**
         * The field will never be collected.
         * If this field is required by the Payment Method, you must provide it as part of
         * the default billing details.
         */
        Never,

        /**
         * The field will always be collected, even if it isn't required for the Payment
         * Method.
         */
        Always,
    }

    /**
     * Billing address collection options.
     */
    @CheckoutSessionPreview
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    enum class AddressCollectionMode {
        /**
         * Only the fields required by the Payment Method will be collected, this may be
         * none.
         */
        Automatic,

        /**
         * Collect the full billing address, regardless of the Payment Method requirements.
         */
        Full,

        // Note: a `Never` mode is intentionally omitted for the CheckoutSession private
        // preview — suppressing billing collection is not supported with a CheckoutSession.
        // It can be added at public preview/GA if that use case is supported.
    }
}
