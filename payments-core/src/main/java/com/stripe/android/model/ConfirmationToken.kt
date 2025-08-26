package com.stripe.android.model

import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.stripe.android.core.model.StripeModel
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
 * Related guides: [Elements](https://stripe.com/docs/payments/elements)
 */
@Parcelize
data class ConfirmationToken
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
constructor(
    /**
     * Unique identifier for the object.
     */
    @JvmField val id: String,

    /**
     * The type of the object. Always "confirmation_token".
     */
    @JvmField val `object`: String = "confirmation_token",

    /**
     * Time at which the object was created. Measured in seconds since the Unix epoch.  
     */
    @JvmField val created: Long,

    /**
     * Has the value `true` if the object exists in live mode or the value `false` if the object exists in test mode.
     */
    @JvmField val liveMode: Boolean,

    /**
     * Payment method data collected from Elements. This represents the transactional checkout state,
     * not a reusable PaymentMethod object.
     */
    @JvmField val paymentMethodData: PaymentMethodData? = null,

    /**
     * Return URL that will be used for any redirect-based payment methods.
     */
    @JvmField val returnUrl: String? = null,

    /**
     * Shipping information collected by Elements (e.g., from Address Element).
     */
    @JvmField val shipping: ShippingDetails? = null,

    /**
     * Indicates how you intend to use the payment method for future payments.
     * This is automatically determined based on Elements configuration and user input.
     */
    @JvmField val setupFutureUsage: SetupFutureUsage? = null,

    /**
     * Payment method options containing both public confirmation parameters and 
     * validation context from Elements configuration.
     */
    @JvmField val paymentMethodOptions: PaymentMethodOptions? = null,

    /**
     * Mandate data for this confirmation token. This is automatically generated based on
     * the payment method and usage, eliminating the need for manual mandate handling.
     */
    @JvmField val mandateData: MandateDataParams? = null
) : StripeModel, Parcelable {

    /**
     * Payment method data captured from Elements at the time of ConfirmationToken creation.
     * This represents transactional checkout state, not a reusable payment method.
     */
    @Parcelize
    data class PaymentMethodData(
        /**
         * The type of payment method.
         */
        @JvmField val type: PaymentMethod.Type,

        /**
         * Billing details collected from Elements.
         */
        @JvmField val billingDetails: PaymentMethod.BillingDetails? = null,

        /**
         * Card data collected from Elements (for card payment methods).
         */
        @JvmField val card: Card? = null,

        /**
         * US bank account data collected from Elements.
         */
        @JvmField val usBankAccount: USBankAccount? = null,

        /**
         * SEPA debit data collected from Elements.
         */
        @JvmField val sepaDebit: SepaDebit? = null,

        /**
         * Metadata associated with the payment method data.
         */
        @JvmField val metadata: Map<String, String>? = null
    ) : Parcelable {

        @Parcelize
        data class Card(
            /**
             * CVC token generated for saved payment method CVC recollection.
             */
            @JvmField val cvcToken: String? = null,

            /**
             * Encrypted card data for new card entries.
             */
            @JvmField val encryptedData: String? = null
        ) : Parcelable

        @Parcelize  
        data class USBankAccount(
            /**
             * Account holder type.
             */
            @JvmField val accountHolderType: PaymentMethod.USBankAccount.USBankAccountHolderType? = null,

            /**
             * Account type.
             */
            @JvmField val accountType: PaymentMethod.USBankAccount.USBankAccountType? = null,

            /**
             * Financial connections account token for verified accounts.
             */
            @JvmField val financialConnectionsAccount: String? = null
        ) : Parcelable

        @Parcelize
        data class SepaDebit(
            /**
             * IBAN for SEPA debit payments.
             */
            @JvmField val iban: String? = null
        ) : Parcelable
    }

    /**
     * Shipping details collected by Elements.
     */
    @Parcelize
    data class ShippingDetails(
        /**
         * Shipping address.
         */
        @JvmField val address: Address,

        /**
         * Recipient name.
         */
        @JvmField val name: String,

        /**
         * Recipient phone number.
         */
        @JvmField val phone: String? = null
    ) : Parcelable

    /**
     * Payment method options containing configuration and collected data.
     * These include both public parameters (applied to Intent) and private validation context.
     */
    @Parcelize
    data class PaymentMethodOptions(
        /**
         * Card-specific options.
         */
        @JvmField val card: Card? = null,

        /**
         * US bank account specific options.
         */
        @JvmField val usBankAccount: USBankAccount? = null,

        /**
         * SEPA debit specific options.
         */
        @JvmField val sepaDebit: SepaDebit? = null
    ) : Parcelable {

        @Parcelize
        data class Card(
            /**
             * CVC token for saved payment method recollection (public parameter).
             * This gets applied to the Intent during confirmation.
             */
            @JvmField val cvcToken: String? = null,

            /**
             * Network for card payments (public parameter).
             */
            @JvmField val network: String? = null,

            /**
             * Setup future usage for this payment method.
             */
            @JvmField val setupFutureUsage: SetupFutureUsage? = null
        ) : Parcelable

        @Parcelize
        data class USBankAccount(
            /**
             * Verification method used.
             */
            @JvmField val verificationMethod: String? = null
        ) : Parcelable

        @Parcelize
        data class SepaDebit(
            /**
             * Setup future usage for SEPA debit.
             */
            @JvmField val setupFutureUsage: SetupFutureUsage? = null
        ) : Parcelable
    }

    /**
     * Setup future usage values.
     */
    enum class SetupFutureUsage(val code: String) {
        /**
         * Use the payment method for future on-session payments.
         */
        OnSession("on_session"),

        /**
         * Use the payment method for future off-session payments.
         */
        OffSession("off_session")
    }

    companion object {
        internal const val OBJECT_TYPE = "confirmation_token"
        internal const val FIELD_ID = "id"
        internal const val FIELD_OBJECT = "object"
        internal const val FIELD_CREATED = "created"
        internal const val FIELD_LIVEMODE = "livemode"
        internal const val FIELD_PAYMENT_METHOD_DATA = "payment_method_data"
        internal const val FIELD_RETURN_URL = "return_url"
        internal const val FIELD_SHIPPING = "shipping"
        internal const val FIELD_SETUP_FUTURE_USAGE = "setup_future_usage"
        internal const val FIELD_PAYMENT_METHOD_OPTIONS = "payment_method_options"
        internal const val FIELD_MANDATE_DATA = "mandate_data"
    }
}