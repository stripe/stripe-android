package com.stripe.android.model

import android.net.Uri
import com.stripe.android.model.parsers.PaymentIntentJsonParser
import java.util.regex.Pattern
import kotlinx.android.parcel.IgnoredOnParcel
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
     * @return Unique identifier for the object.
     */
    override val id: String?,

    private val objectType: String?,

    /**
     * @return The list of payment method types (e.g. card) that this PaymentIntent is allowed to
     * use.
     */
    override val paymentMethodTypes: List<String>,

    /**
     * @return Amount intended to be collected by this PaymentIntent.
     */
    val amount: Long?,

    /**
     * @return Populated when status is canceled, this is the time at which the PaymentIntent
     * was canceled. Measured in seconds since the Unix epoch. If unavailable, will return 0.
     */
    val canceledAt: Long,

    /**
     * @return Reason for cancellation of this PaymentIntent
     */
    val cancellationReason: CancellationReason?,

    /**
     * @return One of `automatic` (default) or `manual`.
     *
     * When the capture method is `automatic`,
     * Stripe automatically captures funds when the customer authorizes the payment.
     */
    val captureMethod: String?,

    /**
     * @return The client secret of this PaymentIntent. Used for client-side retrieval using a
     * publishable key.
     *
     * The client secret can be used to complete a payment from your frontend.
     * It should not be stored, logged, embedded in URLs, or exposed to anyone other than the
     * customer. Make sure that you have TLS enabled on any page that includes the client
     * secret.
     */
    override val clientSecret: String?,

    /**
     * @return One of automatic (default) or manual.
     *
     * When the confirmation method is `automatic`, a PaymentIntent can be confirmed
     * using a publishable key. After `next_action`s are handled, no additional
     * confirmation is required to complete the payment.
     *
     * When the confirmation method is `manual`, all payment attempts must be made
     * using a secret key. The PaymentIntent returns to the `requires_confirmation`
     * state after handling `next_action`s, and requires your server to initiate each
     * payment attempt with an explicit confirmation.
     */
    val confirmationMethod: String?,

    /**
     * @return Time at which the object was created. Measured in seconds since the Unix epoch.
     */
    override val created: Long,

    /**
     * @return Three-letter ISO currency code, in lowercase. Must be a supported currency.
     */
    val currency: String?,

    /**
     * @return An arbitrary string attached to the object. Often useful for displaying to users.
     */
    override val description: String?,

    /**
     * @return Has the value `true` if the object exists in live mode or the value
     * `false` if the object exists in test mode.
     */
    override val isLiveMode: Boolean,

    /**
     * @return If present, this property tells you what actions you need to take in order for your
     * customer to fulfill a payment using the provided source.
     */
    val nextAction: Map<String, @RawValue Any?>?,

    /**
     * @return ID of the payment method (a PaymentMethod, Card, BankAccount, or saved Source object)
     * to attach to this PaymentIntent.
     */
    override val paymentMethodId: String?,

    /**
     * @return Email address that the receipt for the resulting payment will be sent to.
     */
    val receiptEmail: String?,

    /**
     * @return Status of this PaymentIntent.
     */
    override val status: StripeIntent.Status?,

    private val setupFutureUsage: StripeIntent.Usage?,

    /**
     * @return The payment error encountered in the previous PaymentIntent confirmation.
     */
    val lastPaymentError: Error?
) : StripeModel, StripeIntent {
    @IgnoredOnParcel
    override val nextActionType: StripeIntent.NextActionType? = nextAction?.let {
        StripeIntent.NextActionType.fromCode(it[FIELD_NEXT_ACTION_TYPE] as String?)
    }

    val redirectUrl: Uri?
        get() {
            return redirectData?.url
        }

    override val stripeSdkData: StripeIntent.SdkData?
        get() = if (nextAction == null || StripeIntent.NextActionType.UseStripeSdk !== nextActionType) {
            null
        } else {
            StripeIntent.SdkData(
                nextAction[StripeIntent.NextActionType.UseStripeSdk.code] as Map<String, *>
            )
        }

    override val redirectData: StripeIntent.RedirectData?
        get() {
            if (StripeIntent.NextActionType.RedirectToUrl !== nextActionType) {
                return null
            }

            val nextAction: Map<String, Any?> = (if (StripeIntent.Status.RequiresAction === status) {
                this.nextAction
            } else {
                null
            })
                ?: return null

            val nextActionType = StripeIntent.NextActionType
                .fromCode(nextAction[FIELD_NEXT_ACTION_TYPE] as String?)
            return if (StripeIntent.NextActionType.RedirectToUrl !== nextActionType) {
                null
            } else {
                val redirectToUrl = nextAction[nextActionType.code]
                if (redirectToUrl is Map<*, *>) {
                    StripeIntent.RedirectData.create((redirectToUrl as Map<*, *>?)!!)
                } else {
                    null
                }
            }
        }

    override fun requiresAction(): Boolean {
        return status === StripeIntent.Status.RequiresAction
    }

    override fun requiresConfirmation(): Boolean {
        return status === StripeIntent.Status.RequiresConfirmation
    }

    /**
     * The payment error encountered in the previous PaymentIntent confirmation.
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
            private val PATTERN = Pattern.compile("^pi_([a-zA-Z0-9])+_secret_([a-zA-Z0-9])+$")
        }
    }

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

        internal fun parseIdFromClientSecret(clientSecret: String): String {
            return clientSecret.split("_secret".toRegex())
                .dropLastWhile { it.isEmpty() }.toTypedArray()[0]
        }

        fun fromJson(jsonObject: JSONObject?): PaymentIntent? {
            return jsonObject?.let {
                PaymentIntentJsonParser().parse(it)
            }
        }
    }
}
