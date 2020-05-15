package com.stripe.android.model

import android.net.Uri
import com.stripe.android.model.parsers.PaymentIntentJsonParser
import com.stripe.android.utils.Either
import java.util.regex.Pattern
import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.RawValue
import org.json.JSONObject

/**
 * A PaymentIntent tracks the process of collecting a payment from your customer.
 *
 * - [Payment Intents Overview](https://stripe.com/docs/payments/payment-intents)
 * - [PaymentIntents API](https://stripe.com/docs/api/payment_intents)
 */
@Parcelize
data class PaymentIntent internal constructor(
    /**
     * Unique identifier for the object.
     */
    override val id: String?,

    /**
     * The list of payment method types (e.g. card) that this [PaymentIntent] is allowed to
     * use.
     */
    override val paymentMethodTypes: List<String>,

    /**
     * Amount intended to be collected by this [PaymentIntent]. A positive integer
     * representing how much to charge in the smallest currency unit (e.g., 100 cents to charge
     * $1.00 or 100 to charge ¥100, a zero-decimal currency). The minimum amount is $0.50 US or
     * equivalent in charge currency. The amount value supports up to eight digits (e.g., a value
     * of 99999999 for a USD charge of $999,999.99).
     */
    val amount: Long?,

    /**
     * Populated when `status` is `canceled`, this is the time at which the [PaymentIntent]
     * was canceled. Measured in seconds since the Unix epoch. If unavailable, will return 0.
     */
    val canceledAt: Long = 0L,

    /**
     * Reason for cancellation of this [PaymentIntent].
     */
    val cancellationReason: CancellationReason? = null,

    /**
     * One of `automatic` (default) or `manual`.
     *
     * When the capture method is `automatic`,
     * Stripe automatically captures funds when the customer authorizes the payment.
     */
    val captureMethod: String?,

    /**
     * The client secret of this [PaymentIntent]. Used for client-side retrieval using a
     * publishable key.
     *
     * The client secret can be used to complete a payment from your frontend.
     * It should not be stored, logged, embedded in URLs, or exposed to anyone other than the
     * customer. Make sure that you have TLS enabled on any page that includes the client
     * secret.
     */
    override val clientSecret: String?,

    /**
     * One of automatic (default) or manual.
     *
     * When [confirmationMethod] is `automatic`, a [PaymentIntent] can be confirmed
     * using a publishable key. After `next_action`s are handled, no additional
     * confirmation is required to complete the payment.
     *
     * When [confirmationMethod] is `manual`, all payment attempts must be made
     * using a secret key. The [PaymentIntent] returns to the
     * [RequiresConfirmation][StripeIntent.Status.RequiresConfirmation]
     * state after handling `next_action`s, and requires your server to initiate each
     * payment attempt with an explicit confirmation.
     */
    val confirmationMethod: String? = null,

    /**
     * Time at which the object was created. Measured in seconds since the Unix epoch.
     */
    override val created: Long,

    /**
     * Three-letter ISO currency code, in lowercase. Must be a supported currency.
     */
    val currency: String?,

    /**
     * An arbitrary string attached to the object. Often useful for displaying to users.
     */
    override val description: String? = null,

    /**
     * Has the value `true` if the object exists in live mode or the value
     * `false` if the object exists in test mode.
     */
    override val isLiveMode: Boolean,

    /**
     * If present, this property tells you what actions you need to take in order for your
     * customer to fulfill a payment using the provided source.
     */
    val nextAction: Map<String, @RawValue Any?>? = null,

    override val paymentMethod: PaymentMethod? = null,

    /**
     * ID of the payment method (a PaymentMethod, Card, BankAccount, or saved Source object)
     * to attach to this [PaymentIntent].
     */
    override val paymentMethodId: String? = null,

    /**
     * Email address that the receipt for the resulting payment will be sent to.
     */
    val receiptEmail: String? = null,

    /**
     * Status of this [PaymentIntent].
     */
    override val status: StripeIntent.Status? = null,

    private val setupFutureUsage: StripeIntent.Usage? = null,

    /**
     * The payment error encountered in the previous [PaymentIntent] confirmation.
     */
    val lastPaymentError: Error? = null,

    /**
     * Shipping information for this [PaymentIntent].
     */
    val shipping: Shipping? = null,

    internal val nextActionData: StripeIntent.Companion.NextActionData? = null
) : StripeIntent {
    override val nextActionType: StripeIntent.NextActionType?
        get() = when (nextActionData) {
                is StripeIntent.Companion.NextActionData.SdkData -> StripeIntent.NextActionType.UseStripeSdk
                is StripeIntent.Companion.NextActionData.RedirectToUrl -> StripeIntent.NextActionType.RedirectToUrl
                is StripeIntent.Companion.NextActionData.DisplayOxxoDetails -> StripeIntent.NextActionType.DisplayOxxoDetails
                else -> null
            }

    /**
     * The URL you must redirect your customer to in order to authenticate the payment.
     */
    val redirectUrl: Uri?
        get() {
            return redirectData?.url
        }

    override val stripeSdkData: StripeIntent.SdkData?
        get() = when (nextActionData) {
            is StripeIntent.Companion.NextActionData.SdkData.`3DS1` -> StripeIntent.SdkData(true, false, Either.Right(nextActionData))
            is StripeIntent.Companion.NextActionData.SdkData.`3DS2` -> StripeIntent.SdkData(false, true, Either.Right(nextActionData))
            else -> null
        }

    override val redirectData: StripeIntent.RedirectData?
        get() = when (nextActionData) {
                is StripeIntent.Companion.NextActionData.RedirectToUrl ->
                    StripeIntent.RedirectData(nextActionData.url, nextActionData.returnUrl)
                else -> null
            }

    override fun requiresAction(): Boolean {
        return status === StripeIntent.Status.RequiresAction
    }

    override fun requiresConfirmation(): Boolean {
        return status === StripeIntent.Status.RequiresConfirmation
    }

    /**
     * The payment error encountered in the previous [PaymentIntent] confirmation.
     *
     * See [last_payment_error](https://stripe.com/docs/api/payment_intents/object#payment_intent_object-last_payment_error).
     */
    @Parcelize
    data class Error internal constructor(
        /**
         * For card errors, the ID of the failed charge.
         */
        val charge: String?,

        /**
         * For some errors that could be handled programmatically, a short string indicating the
         * [error code](https://stripe.com/docs/error-codes) reported.
         */
        val code: String?,

        /**
         * For card errors resulting from a card issuer decline, a short string indicating the
         * [card issuer’s reason for the decline](https://stripe.com/docs/declines#issuer-declines)
         * if they provide one.
         */
        val declineCode: String?,

        /**
         * A URL to more information about the
         * [error code](https://stripe.com/docs/error-codes) reported.
         */
        val docUrl: String?,

        /**
         * A human-readable message providing more details about the error. For card errors,
         * these messages can be shown to your users.
         */
        val message: String?,

        /**
         * If the error is parameter-specific, the parameter related to the error.
         * For example, you can use this to display a message near the correct form field.
         */
        val param: String?,

        /**
         * The PaymentMethod object for errors returned on a request involving a PaymentMethod.
         */
        val paymentMethod: PaymentMethod?,

        /**
         * The type of error returned.
         */
        val type: Type?
    ) : StripeModel {
        enum class Type(val code: String) {
            ApiConnectionError("api_connection_error"),
            ApiError("api_error"),
            AuthenticationError("authentication_error"),
            CardError("card_error"),
            IdempotencyError("idempotency_error"),
            InvalidRequestError("invalid_request_error"),
            RateLimitError("rate_limit_error");

            internal companion object {
                internal fun fromCode(typeCode: String?): Type? {
                    return values().firstOrNull { it.code == typeCode }
                }
            }
        }
    }

    /**
     * Shipping information for this [PaymentIntent].
     *
     * See [shipping](https://stripe.com/docs/api/payment_intents/object#payment_intent_object-shipping)
     */
    @Parcelize
    data class Shipping(
        /**
         * Shipping address.
         *
         * See [shipping.address](https://stripe.com/docs/api/payment_intents/object#payment_intent_object-shipping-address)
         */
        val address: Address,

        /**
         * The delivery service that shipped a physical product, such as Fedex, UPS, USPS, etc.
         *
         * See [shipping.carrier](https://stripe.com/docs/api/payment_intents/object#payment_intent_object-shipping-carrier)
         */
        val carrier: String? = null,

        /**
         * Recipient name.
         *
         * See [shipping.name](https://stripe.com/docs/api/payment_intents/object#payment_intent_object-shipping-name)
         */
        val name: String? = null,

        /**
         * Recipient phone (including extension).
         *
         * See [shipping.phone](https://stripe.com/docs/api/payment_intents/object#payment_intent_object-shipping-phone)
         */
        val phone: String? = null,

        /**
         * The tracking number for a physical product, obtained from the delivery service.
         * If multiple tracking numbers were generated for this purchase, please separate them
         * with commas.
         *
         * See [shipping.tracking_number](https://stripe.com/docs/api/payment_intents/object#payment_intent_object-shipping-tracking_number)
         */
        val trackingNumber: String? = null
    ) : StripeModel

    internal data class ClientSecret(internal val value: String) {
        internal val paymentIntentId: String =
            value.split("_secret".toRegex())
                .dropLastWhile { it.isEmpty() }.toTypedArray()[0]

        init {
            require(PATTERN.matcher(value).matches()) {
                "Invalid client secret: $value"
            }
        }

        private companion object {
            private val PATTERN = Pattern.compile("^pi_[^_]+_secret_[^_]+$")
        }
    }

    /**
     * Reason for cancellation of this [PaymentIntent], either user-provided (duplicate, fraudulent,
     * requested_by_customer, or abandoned) or generated by Stripe internally (failed_invoice,
     * void_invoice, or automatic).
     */
    enum class CancellationReason(private val code: String) {
        Duplicate("duplicate"),
        Fraudulent("fraudulent"),
        RequestedByCustomer("requested_by_customer"),
        Abandoned("abandoned"),
        FailedInvoice("failed_invoice"),
        VoidInvoice("void_invoice"),
        Automatic("automatic");

        internal companion object {
            internal fun fromCode(code: String?): CancellationReason? {
                return values().firstOrNull { it.code == code }
            }
        }
    }

    companion object {
        private const val FIELD_NEXT_ACTION_TYPE = "type"

        fun fromJson(jsonObject: JSONObject?): PaymentIntent? {
            return jsonObject?.let {
                PaymentIntentJsonParser().parse(it)
            }
        }
    }
}
