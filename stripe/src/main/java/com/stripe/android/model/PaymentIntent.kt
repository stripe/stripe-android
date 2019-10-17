package com.stripe.android.model

import android.net.Uri
import com.stripe.android.model.StripeJsonUtils.optBoolean
import com.stripe.android.model.StripeJsonUtils.optCurrency
import com.stripe.android.model.StripeJsonUtils.optLong
import com.stripe.android.model.StripeJsonUtils.optMap
import com.stripe.android.model.StripeJsonUtils.optString
import org.json.JSONException
import org.json.JSONObject

/**
 * A PaymentIntent tracks the process of collecting a payment from your customer.
 *
 * - [Payment Intents Overview](https://stripe.com/docs/payments/payment-intents)
 * - [PaymentIntents API](https://stripe.com/docs/api/payment_intents)
 */
data class PaymentIntent private constructor(
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
    val nextAction: Map<String, Any?>?,

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
) : StripeModel(), StripeIntent {
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
    data class Error private constructor(

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
    ) {
        enum class Type(val code: String) {
            ApiConnectionError("api_connection_error"),
            ApiError("api_error"),
            AuthenticationError("authentication_error"),
            CardError("card_error"),
            IdempotencyError("idempotency_error"),
            InvalidRequestError("invalid_request_error"),
            RateLimitError("rate_limit_error");

            companion object {
                internal fun fromCode(typeCode: String?): Type? {
                    return values().firstOrNull { it.code == typeCode }
                }
            }
        }

        companion object {
            private const val FIELD_CHARGE = "charge"
            private const val FIELD_CODE = "code"
            private const val FIELD_DECLINE_CODE = "decline_code"
            private const val FIELD_DOC_URL = "doc_url"
            private const val FIELD_MESSAGE = "message"
            private const val FIELD_PARAM = "param"
            private const val FIELD_PAYMENT_METHOD = "payment_method"
            private const val FIELD_TYPE = "type"

            internal fun fromJson(errorJson: JSONObject?): Error? {
                return if (errorJson == null) {
                    null
                } else {
                    Error(
                        charge = optString(errorJson, FIELD_CHARGE),
                        code = optString(errorJson, FIELD_CODE),
                        declineCode = optString(errorJson, FIELD_DECLINE_CODE),
                        docUrl = optString(errorJson, FIELD_DOC_URL),
                        message = optString(errorJson, FIELD_MESSAGE),
                        param = optString(errorJson, FIELD_PARAM),
                        paymentMethod = PaymentMethod.fromJson(
                            errorJson.optJSONObject(FIELD_PAYMENT_METHOD)
                        ),
                        type = Type.fromCode(optString(errorJson, FIELD_TYPE))
                    )
                }
            }
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

        companion object {
            internal fun fromCode(code: String?): CancellationReason? {
                return values().firstOrNull { it.code == code }
            }
        }
    }

    companion object {
        private const val VALUE_PAYMENT_INTENT = "payment_intent"

        private const val FIELD_ID = "id"
        private const val FIELD_OBJECT = "object"
        private const val FIELD_AMOUNT = "amount"
        private const val FIELD_CREATED = "created"
        private const val FIELD_CANCELED_AT = "canceled_at"
        private const val FIELD_CANCELLATION_REASON = "cancellation_reason"
        private const val FIELD_CAPTURE_METHOD = "capture_method"
        private const val FIELD_CLIENT_SECRET = "client_secret"
        private const val FIELD_CONFIRMATION_METHOD = "confirmation_method"
        private const val FIELD_CURRENCY = "currency"
        private const val FIELD_DESCRIPTION = "description"
        private const val FIELD_LAST_PAYMENT_ERROR = "last_payment_error"
        private const val FIELD_LIVEMODE = "livemode"
        private const val FIELD_NEXT_ACTION = "next_action"
        private const val FIELD_PAYMENT_METHOD_ID = "payment_method_id"
        private const val FIELD_PAYMENT_METHOD_TYPES = "payment_method_types"
        private const val FIELD_RECEIPT_EMAIL = "receipt_email"
        private const val FIELD_STATUS = "status"
        private const val FIELD_SETUP_FUTURE_USAGE = "setup_future_usage"

        private const val FIELD_NEXT_ACTION_TYPE = "type"

        internal fun parseIdFromClientSecret(clientSecret: String): String {
            return clientSecret.split("_secret".toRegex())
                .dropLastWhile { it.isEmpty() }.toTypedArray()[0]
        }

        fun fromString(jsonString: String?): PaymentIntent? {
            return if (jsonString == null) {
                null
            } else {
                try {
                    fromJson(JSONObject(jsonString))
                } catch (ignored: JSONException) {
                    null
                }
            }
        }

        fun fromJson(jsonObject: JSONObject?): PaymentIntent? {
            if (jsonObject == null || VALUE_PAYMENT_INTENT != jsonObject.optString(FIELD_OBJECT)) {
                return null
            }

            val id = optString(jsonObject, FIELD_ID)
            val objectType = optString(jsonObject, FIELD_OBJECT)
            val paymentMethodTypes = jsonArrayToList(
                jsonObject.optJSONArray(FIELD_PAYMENT_METHOD_TYPES))
            val amount = optLong(jsonObject, FIELD_AMOUNT)
            val canceledAt = jsonObject.optLong(FIELD_CANCELED_AT)
            val cancellationReason = CancellationReason.fromCode(
                optString(jsonObject, FIELD_CANCELLATION_REASON)
            )
            val captureMethod = optString(jsonObject, FIELD_CAPTURE_METHOD)
            val clientSecret = optString(jsonObject, FIELD_CLIENT_SECRET)
            val confirmationMethod = optString(jsonObject, FIELD_CONFIRMATION_METHOD)
            val created = jsonObject.optLong(FIELD_CREATED)
            val currency = optCurrency(jsonObject, FIELD_CURRENCY)
            val description = optString(jsonObject, FIELD_DESCRIPTION)
            val livemode = java.lang.Boolean.TRUE == optBoolean(jsonObject, FIELD_LIVEMODE)
            val paymentMethodId = optString(jsonObject, FIELD_PAYMENT_METHOD_ID)
            val receiptEmail = optString(jsonObject, FIELD_RECEIPT_EMAIL)
            val status = StripeIntent.Status.fromCode(
                optString(jsonObject, FIELD_STATUS)
            )
            val setupFutureUsage = StripeIntent.Usage.fromCode(
                optString(jsonObject, FIELD_SETUP_FUTURE_USAGE)
            )
            val nextAction = optMap(jsonObject, FIELD_NEXT_ACTION)
            val lastPaymentError = Error.fromJson(
                jsonObject.optJSONObject(FIELD_LAST_PAYMENT_ERROR)
            )

            return PaymentIntent(
                id,
                objectType,
                paymentMethodTypes,
                amount,
                canceledAt,
                cancellationReason,
                captureMethod,
                clientSecret,
                confirmationMethod,
                created,
                currency,
                description,
                livemode,
                nextAction,
                paymentMethodId,
                receiptEmail,
                status,
                setupFutureUsage,
                lastPaymentError
            )
        }
    }
}
