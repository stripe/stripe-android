package com.stripe.android.model

import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.stripe.android.core.model.StripeModel
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

/**
 * Request parameters for creating a [ConfirmationToken].
 *
 * ConfirmationTokens help transport client-side data collected by Elements to your server
 * for payment confirmation. They capture payment method, shipping details, and other
 * checkout state, then pass them to your server to confirm a PaymentIntent or SetupIntent.
 *
 * Use [createWithPaymentMethodCreateParams] for new payment methods or
 * [createWithPaymentMethodId] for saved payment methods.
 *
 * Related guides: [Elements](https://stripe.com/docs/payments/elements)
 */
@Parcelize
data class ConfirmationTokenCreateParams
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
constructor(
    /**
     * The type of payment method associated with this confirmation token.
     */
    internal val paymentMethodType: PaymentMethod.Type,

    /**
     * Parameters for creating a new payment method. Use this for new payment methods.
     * Either this or [paymentMethodId] must be provided.
     */
    val paymentMethodCreateParams: PaymentMethodCreateParams? = null,

    /**
     * ID of an existing payment method. Use this for saved payment methods.
     * Either this or [paymentMethodCreateParams] must be provided.
     */
    val paymentMethodId: String? = null,

    /**
     * Return URL that will be used for any redirect-based payment methods.
     */
    val returnUrl: String? = null,

    /**
     * Whether to save the payment method for future payments.
     * If not provided, the value will be determined based on the payment method type and setup.
     */
    val save: Boolean? = null,

    /**
     * Indicates how you intend to use the payment method for future payments.
     * This will be automatically determined based on the payment method and configuration if not provided.
     */
    val setupFutureUsage: SetupFutureUsage? = null,

    /**
     * Details about the mandate to create for this payment method.
     * If not provided and the payment method requires a mandate, one will be created automatically.
     */
    val mandateData: MandateDataParams? = null,

    /**
     * Email address to send the receipt for this payment.
     */
    val receiptEmail: String? = null,

    /**
     * Shipping information for this payment.
     */
    val shipping: ShippingInformation? = null,

    /**
     * Override parameter map for internal use and testing.
     */
    private val overrideParamMap: Map<String, @RawValue Any>? = null
) : StripeParamsModel, Parcelable {

    private val paymentMethodParamMap: Map<String, Any>
        get() = when {
            paymentMethodCreateParams != null -> {
                mapOf(PARAM_PAYMENT_METHOD_DATA to paymentMethodCreateParams.toParamMap())
            }
            paymentMethodId != null -> {
                mapOf(PARAM_PAYMENT_METHOD to paymentMethodId)
            }
            else -> emptyMap()
        }

    override fun toParamMap(): Map<String, Any> {
        return overrideParamMap ?: buildMap<String, Any> {
            put(PARAM_PAYMENT_METHOD_TYPE, paymentMethodType.code)
            putAll(paymentMethodParamMap)
            
            returnUrl?.let { put(PARAM_RETURN_URL, it) }
            save?.let { put(PARAM_SAVE, it) }
            setupFutureUsage?.let { put(PARAM_SETUP_FUTURE_USAGE, it.code) }
            mandateData?.let { put(PARAM_MANDATE_DATA, it.toParamMap()) }
            receiptEmail?.let { put(PARAM_RECEIPT_EMAIL, it) }
            shipping?.let { put(PARAM_SHIPPING, it.toParamMap()) }
        }
    }

    /**
     * Builder for [ConfirmationTokenCreateParams].
     */
    class Builder {
        private var paymentMethodType: PaymentMethod.Type? = null
        private var paymentMethodCreateParams: PaymentMethodCreateParams? = null
        private var paymentMethodId: String? = null
        private var returnUrl: String? = null
        private var save: Boolean? = null
        private var setupFutureUsage: SetupFutureUsage? = null
        private var mandateData: MandateDataParams? = null
        private var receiptEmail: String? = null
        private var shipping: ShippingInformation? = null

        /**
         * Set the payment method type.
         */
        fun setPaymentMethodType(paymentMethodType: PaymentMethod.Type): Builder = apply {
            this.paymentMethodType = paymentMethodType
        }

        /**
         * Set parameters for creating a new payment method.
         */
        fun setPaymentMethodCreateParams(paymentMethodCreateParams: PaymentMethodCreateParams): Builder = apply {
            this.paymentMethodCreateParams = paymentMethodCreateParams
            this.paymentMethodId = null // Clear conflicting field
            this.paymentMethodType = PaymentMethod.Type.fromCode(paymentMethodCreateParams.code)
        }

        /**
         * Set the ID of an existing payment method.
         */
        fun setPaymentMethodId(paymentMethodId: String, paymentMethodType: PaymentMethod.Type): Builder = apply {
            this.paymentMethodId = paymentMethodId
            this.paymentMethodCreateParams = null // Clear conflicting field
            this.paymentMethodType = paymentMethodType
        }

        /**
         * Set the return URL for redirect-based payment methods.
         */
        fun setReturnUrl(returnUrl: String?): Builder = apply {
            this.returnUrl = returnUrl
        }

        /**
         * Set whether to save the payment method.
         */
        fun setSave(save: Boolean?): Builder = apply {
            this.save = save
        }

        /**
         * Set how the payment method will be used for future payments.
         */
        fun setSetupFutureUsage(setupFutureUsage: SetupFutureUsage?): Builder = apply {
            this.setupFutureUsage = setupFutureUsage
        }

        /**
         * Set mandate data for this payment method.
         */
        fun setMandateData(mandateData: MandateDataParams?): Builder = apply {
            this.mandateData = mandateData
        }

        /**
         * Set the receipt email address.
         */
        fun setReceiptEmail(receiptEmail: String?): Builder = apply {
            this.receiptEmail = receiptEmail
        }

        /**
         * Set shipping information.
         */
        fun setShipping(shipping: ShippingInformation?): Builder = apply {
            this.shipping = shipping
        }

        /**
         * Build the [ConfirmationTokenCreateParams].
         */
        fun build(): ConfirmationTokenCreateParams {
            val type = requireNotNull(paymentMethodType) {
                "Payment method type is required"
            }

            require(paymentMethodCreateParams != null || paymentMethodId != null) {
                "Either paymentMethodCreateParams or paymentMethodId must be provided"
            }

            return ConfirmationTokenCreateParams(
                paymentMethodType = type,
                paymentMethodCreateParams = paymentMethodCreateParams,
                paymentMethodId = paymentMethodId,
                returnUrl = returnUrl,
                save = save,
                setupFutureUsage = setupFutureUsage,
                mandateData = mandateData,
                receiptEmail = receiptEmail,
                shipping = shipping
            )
        }
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
        private const val PARAM_PAYMENT_METHOD_TYPE = "payment_method_type"
        private const val PARAM_PAYMENT_METHOD_DATA = "payment_method_data"
        private const val PARAM_PAYMENT_METHOD = "payment_method"
        private const val PARAM_RETURN_URL = "return_url"
        private const val PARAM_SAVE = "save"
        private const val PARAM_SETUP_FUTURE_USAGE = "setup_future_usage"
        private const val PARAM_MANDATE_DATA = "mandate_data"
        private const val PARAM_RECEIPT_EMAIL = "receipt_email"
        private const val PARAM_SHIPPING = "shipping"

        /**
         * Create confirmation token parameters with new payment method data.
         */
        @JvmStatic
        @JvmOverloads
        fun createWithPaymentMethodCreateParams(
            paymentMethodCreateParams: PaymentMethodCreateParams,
            returnUrl: String? = null,
            save: Boolean? = null,
            setupFutureUsage: SetupFutureUsage? = null,
            mandateData: MandateDataParams? = null,
            receiptEmail: String? = null,
            shipping: ShippingInformation? = null
        ): ConfirmationTokenCreateParams {
            val paymentMethodType = PaymentMethod.Type.fromCode(paymentMethodCreateParams.code)
                ?: throw IllegalArgumentException("Invalid payment method code: ${paymentMethodCreateParams.code}")

            return ConfirmationTokenCreateParams(
                paymentMethodType = paymentMethodType,
                paymentMethodCreateParams = paymentMethodCreateParams,
                returnUrl = returnUrl,
                save = save,
                setupFutureUsage = setupFutureUsage,
                mandateData = mandateData,
                receiptEmail = receiptEmail,
                shipping = shipping
            )
        }

        /**
         * Create confirmation token parameters with existing payment method ID.
         */
        @JvmStatic
        @JvmOverloads
        fun createWithPaymentMethodId(
            paymentMethodId: String,
            paymentMethodType: PaymentMethod.Type,
            returnUrl: String? = null,
            save: Boolean? = null,
            setupFutureUsage: SetupFutureUsage? = null,
            mandateData: MandateDataParams? = null,
            receiptEmail: String? = null,
            shipping: ShippingInformation? = null
        ): ConfirmationTokenCreateParams {
            return ConfirmationTokenCreateParams(
                paymentMethodType = paymentMethodType,
                paymentMethodId = paymentMethodId,
                returnUrl = returnUrl,
                save = save,
                setupFutureUsage = setupFutureUsage,
                mandateData = mandateData,
                receiptEmail = receiptEmail,
                shipping = shipping
            )
        }

        /**
         * Create confirmation token parameters for a card payment method.
         */
        @JvmStatic
        @JvmOverloads
        fun createCard(
            card: PaymentMethodCreateParams.Card,
            billingDetails: PaymentMethod.BillingDetails? = null,
            returnUrl: String? = null,
            save: Boolean? = null,
            setupFutureUsage: SetupFutureUsage? = null,
            receiptEmail: String? = null,
            shipping: ShippingInformation? = null
        ): ConfirmationTokenCreateParams {
            return createWithPaymentMethodCreateParams(
                paymentMethodCreateParams = PaymentMethodCreateParams.create(card, billingDetails),
                returnUrl = returnUrl,
                save = save,
                setupFutureUsage = setupFutureUsage,
                receiptEmail = receiptEmail,
                shipping = shipping
            )
        }
    }
}