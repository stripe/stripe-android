package com.stripe.android.model

import androidx.annotation.RestrictTo
import com.stripe.android.core.model.StripeJsonUtils
import com.stripe.android.core.model.StripeModel
import com.stripe.android.model.PaymentIntent.CaptureMethod
import com.stripe.android.model.PaymentIntent.ConfirmationMethod
import com.stripe.android.model.parsers.PaymentIntentJsonParser
import kotlinx.parcelize.Parcelize
import org.json.JSONObject
import java.util.regex.Pattern

/**
 * A [PaymentIntent] tracks the process of collecting a payment from your customer.
 *
 * - [Payment Intents Overview](https://stripe.com/docs/payments/payment-intents)
 * - [PaymentIntents API Reference](https://stripe.com/docs/api/payment_intents)
 */
@Parcelize
data class PaymentIntent
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
constructor(
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
     * Controls when the funds will be captured from the customer’s account.
     * See [CaptureMethod].
     */
    val captureMethod: CaptureMethod = CaptureMethod.Automatic,

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
     * One of automatic (default) or manual. See [ConfirmationMethod].
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
    val confirmationMethod: ConfirmationMethod = ConfirmationMethod.Automatic,

    /**
     * Country code of the user.
     */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override val countryCode: String?,

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

    val setupFutureUsage: StripeIntent.Usage? = null,

    /**
     * The payment error encountered in the previous [PaymentIntent] confirmation.
     */
    val lastPaymentError: Error? = null,

    /**
     * Shipping information for this [PaymentIntent].
     */
    val shipping: Shipping? = null,

    /**
     * Payment types that have not been activated in livemode, but have been activated in testmode.
     */
    override val unactivatedPaymentMethods: List<String>,

    /**
     * Payment types that are accepted when paying with Link.
     */
    override val linkFundingSources: List<String> = emptyList(),

    override val nextActionData: StripeIntent.NextActionData? = null,

    private val paymentMethodOptionsJsonString: String? = null,

) : StripeIntent {

    override fun getPaymentMethodOptions() = paymentMethodOptionsJsonString?.let {
        StripeJsonUtils.jsonObjectToMap(JSONObject(it))
    } ?: emptyMap()

    override val nextActionType: StripeIntent.NextActionType?
        get() = when (nextActionData) {
            is StripeIntent.NextActionData.SdkData -> {
                StripeIntent.NextActionType.UseStripeSdk
            }
            is StripeIntent.NextActionData.RedirectToUrl -> {
                StripeIntent.NextActionType.RedirectToUrl
            }
            is StripeIntent.NextActionData.DisplayOxxoDetails -> {
                StripeIntent.NextActionType.DisplayOxxoDetails
            }
            is StripeIntent.NextActionData.DisplayBoletoDetails -> {
                StripeIntent.NextActionType.DisplayBoletoDetails
            }
            is StripeIntent.NextActionData.DisplayPayNowDetails -> {
                StripeIntent.NextActionType.DisplayPayNowDetails
            }
            is StripeIntent.NextActionData.DisplayKonbiniDetails -> {
                StripeIntent.NextActionType.DisplayKonbiniDetails
            }
            is StripeIntent.NextActionData.DisplayMultibancoDetails -> {
                StripeIntent.NextActionType.DisplayMultibancoDetails
            }
            is StripeIntent.NextActionData.VerifyWithMicrodeposits -> {
                StripeIntent.NextActionType.VerifyWithMicrodeposits
            }
            is StripeIntent.NextActionData.UpiAwaitNotification -> {
                StripeIntent.NextActionType.UpiAwaitNotification
            }
            is StripeIntent.NextActionData.CashAppRedirect -> {
                StripeIntent.NextActionType.CashAppRedirect
            }
            is StripeIntent.NextActionData.BlikAuthorize -> {
                StripeIntent.NextActionType.BlikAuthorize
            }
            is StripeIntent.NextActionData.SwishRedirect -> {
                StripeIntent.NextActionType.SwishRedirect
            }
            is StripeIntent.NextActionData.AlipayRedirect,
            is StripeIntent.NextActionData.WeChatPayRedirect,
            null -> {
                null
            }
        }

    override val isConfirmed: Boolean
        get() = setOf(
            StripeIntent.Status.Processing,
            StripeIntent.Status.RequiresCapture,
            StripeIntent.Status.Succeeded
        ).contains(status)

    override val lastErrorMessage: String?
        get() = lastPaymentError?.message

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    val requireCvcRecollection: Boolean
        get() = paymentMethodOptionsJsonString?.let { json ->
            JSONObject(json).optJSONObject(CARD)?.optBoolean(REQUIRE_CVC_RECOLLECTION) ?: false
        } ?: false

    override fun requiresAction(): Boolean {
        return status === StripeIntent.Status.RequiresAction
    }

    override fun requiresConfirmation(): Boolean {
        return status === StripeIntent.Status.RequiresConfirmation
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun isSetupFutureUsageSet(code: PaymentMethodCode): Boolean {
        return isTopLevelSetupFutureUsageSet() || isLpmLevelSetupFutureUsageSet(code)
    }

    /**
     * SetupFutureUsage is considered to be set if it is on or off session.
     */
    private fun isTopLevelSetupFutureUsageSet(): Boolean {
        return when (setupFutureUsage) {
            StripeIntent.Usage.OnSession -> true
            StripeIntent.Usage.OffSession -> true
            StripeIntent.Usage.OneTime -> false
            null -> false
        }
    }

    private fun isLpmLevelSetupFutureUsageSet(code: PaymentMethodCode): Boolean {
        return paymentMethodOptionsJsonString?.let { json ->
            val pmOptions = JSONObject(json).optJSONObject(code)
            pmOptions?.has("setup_future_usage") ?: false
        } ?: false
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
                fun fromCode(typeCode: String?) = entries.firstOrNull { it.code == typeCode }
            }
        }

        internal companion object {
            internal const val CODE_AUTHENTICATION_ERROR = "payment_intent_authentication_failure"
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
            require(isMatch(value)) {
                "Invalid Payment Intent client secret: $value"
            }
        }

        internal companion object {
            private val PATTERN = Pattern.compile("^pi_[^_]+_secret_[^_]+$")

            fun isMatch(value: String) = PATTERN.matcher(value).matches()
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
            fun fromCode(code: String?) = entries.firstOrNull { it.code == code }
        }
    }

    /**
     * Controls when the funds will be captured from the customer’s account.
     */
    enum class CaptureMethod(
        @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        val code: String,
    ) {
        /**
         * (Default) Stripe automatically captures funds when the customer authorizes the payment.
         */
        Automatic("automatic"),

        /**
         * Stripe asynchronously captures funds when the customer authorizes the payment.
         * Recommended over [Automatic] due to improved latency, but may require additional
         * integration changes.
         */
        AutomaticAsync("automatic_async"),

        /**
         * Place a hold on the funds when the customer authorizes the payment, but don’t capture
         * the funds until later. (Not all payment methods support this.)
         */
        Manual("manual");

        internal companion object {
            fun fromCode(code: String?) = entries.firstOrNull { it.code == code } ?: Automatic
        }
    }

    enum class ConfirmationMethod(private val code: String) {
        /**
         * (Default) PaymentIntent can be confirmed using a publishable key. After `next_action`s
         * are handled, no additional confirmation is required to complete the payment.
         */
        Automatic("automatic"),

        /**
         * All payment attempts must be made using a secret key. The PaymentIntent returns to the
         * `requires_confirmation` state after handling `next_action`s, and requires your server to
         * initiate each payment attempt with an explicit confirmation.
         */
        Manual("manual");

        internal companion object {
            fun fromCode(code: String?) = entries.firstOrNull { it.code == code } ?: Automatic
        }
    }

    companion object {
        @JvmStatic
        fun fromJson(jsonObject: JSONObject?): PaymentIntent? {
            return jsonObject?.let {
                PaymentIntentJsonParser().parse(it)
            }
        }

        internal const val CARD = "card"
        internal const val REQUIRE_CVC_RECOLLECTION = "require_cvc_recollection"
    }
}
