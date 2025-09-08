package com.stripe.android.model

import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.stripe.android.core.model.StripeModel
import dev.drewhamilton.poko.Poko
import kotlinx.parcelize.Parcelize

/**
 * Confirmation Token objects help transport client-side data collected by Elements to your server
 * for payment confirmation. They capture payment method information, shipping details, and other
 * checkout state from Elements, then pass them to your server where you can use them to confirm
 * a PaymentIntent or SetupIntent.
 *
 * Confirmation Tokens are single-use and expire 15 minutes after creation.
 *
 * Think of the ConfirmationToken as an immutable bag of data that serves two purposes:
 * 1. Transport checkout state collected by Elements needed to confirm an Intent
 * 2. Capture Elements configuration for validation at confirmation time
 *
 * Related guides: [Confirmation Tokens](https://stripe.com/docs/api/confirmation_tokens)
 */
@Poko
@Parcelize
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class ConfirmationToken internal constructor(
    /**
     * Unique identifier for the object.
     */
    @JvmField val id: String,

    /**
     * Time at which the object was created. Measured in seconds since the Unix epoch.
     */
    @JvmField val created: Long,

    /**
     * Time at which this ConfirmationToken expires and can no longer be used to confirm a PaymentIntent or SetupIntent.
     */
    @JvmField val expiresAt: Long?,

    /**
     * Has the value `true` if the object exists in live mode or the value `false` if the object exists in test mode.
     */
    @JvmField val liveMode: Boolean,

    /**
     * Data used for generating a Mandate.
     */
    @JvmField val mandateData: MandateData?,

    /**
     * ID of the PaymentIntent that this ConfirmationToken was used to confirm,
     * or null if this ConfirmationToken has not yet been used.
     */
    @JvmField val paymentIntentId: String?,

    /**
     * Payment method data collected from Elements. This represents the transactional checkout state,
     * not a reusable PaymentMethod object.
     */
    @JvmField val paymentMethodPreview: PaymentMethodPreview?,

    /**
     * Return URL used to confirm the intent for redirect-based methods.
     */
    @JvmField val returnUrl: String?,

    /**
     * Indicates how you intend to use the payment method for future payments.
     * This is automatically determined based on Elements configuration and user input.
     */
    @JvmField val setupFutureUsage: ConfirmPaymentIntentParams.SetupFutureUsage?,

    /**
     * ID of the SetupIntent that this ConfirmationToken was used to confirm,
     * or null if this ConfirmationToken has not yet been used.
     */
    @JvmField val setupIntentId: String?,

    /**
     * Shipping information collected on this ConfirmationToken.
     */
    @JvmField val shipping: ShippingInformation?,
) : StripeModel {
    /**
     * Preview of the payment method data collected from Elements.
     * This represents the transactional checkout state, not a reusable PaymentMethod object.
     */
    @Poko
    @Parcelize
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class PaymentMethodPreview internal constructor(
        /**
         *  This field indicates whether this payment method can be shown again to its customer in a checkout flow.
         *  Stripe products such as Checkout and Elements use this field to determine
         *  whether a payment method can be shown as a saved payment method in a checkout flow.
         */
        @JvmField val allowRedisplay: PaymentMethod.AllowRedisplay? = null,

        /**
         *  Billing information associated with the PaymentMethod that may be used or required by
         *  particular types of payment methods.
         */
        @JvmField val billingDetails: PaymentMethod.BillingDetails? = null,

        /**
         *  The ID of the Customer to which this PaymentMethod is saved.
         *  This will only be set when the PaymentMethod has been saved to a Customer.
         */
        @JvmField val customerId: String? = null,

        /**
         * The type of the PaymentMethod.
         * An additional hash is included on the PaymentMethod with a name matching this value.
         */
        @JvmField val type: PaymentMethod.Type,

        @JvmField internal val allResponseFields: String
    ) : StripeModel
}
