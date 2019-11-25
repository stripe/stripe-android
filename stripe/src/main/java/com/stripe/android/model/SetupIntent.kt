package com.stripe.android.model

import android.net.Uri
import android.os.Parcelable
import com.stripe.android.model.StripeJsonUtils.optMap
import com.stripe.android.model.StripeJsonUtils.optString
import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.RawValue
import org.json.JSONException
import org.json.JSONObject

/**
 * A SetupIntent guides you through the process of setting up a customer's payment credentials for
 * future payments.
 */
@Parcelize
data class SetupIntent internal constructor(

    /**
     * @return Unique identifier for the object.
     */
    override val id: String?,

    private val objectType: String?,

    /**
     * @return Reason for cancellation of this SetupIntent.
     */
    val cancellationReason: CancellationReason?,

    /**
     * @return Time at which the object was created. Measured in seconds since the Unix epoch.
     */
    override val created: Long,

    /**
     * @return The client secret of this SetupIntent. Used for client-side retrieval using a
     * publishable key.
     *
     * The client secret can be used to complete payment setup from your frontend. It should not
     * be stored, logged, embedded in URLs, or exposed to anyone other than the customer. Make
     * sure that you have TLS enabled on any page that includes the client secret.
     */
    override val clientSecret: String?,

    /**
     * @return An arbitrary string attached to the object. Often useful for displaying to users.
     */
    override val description: String?,

    /**
     * @return Has the value `true` if the object exists in live mode or the value
     * `false` if the object exists in test mode.
     */
    override val isLiveMode: Boolean,

    private val nextAction: Map<String, @RawValue Any?>?,

    override val nextActionType: StripeIntent.NextActionType? = null,

    /**
     * @return ID of the payment method used with this SetupIntent.
     */
    override val paymentMethodId: String?,

    /**
     * @return The list of payment method types (e.g. card) that this SetupIntent is allowed to
     * set up.
     */
    override val paymentMethodTypes: List<String>,

    /**
     * @return Status of this SetupIntent.
     */
    override val status: StripeIntent.Status?,

    /**
     * @return Indicates how the payment method is intended to be used in the future.
     *
     * Use `on_session` if you intend to only reuse the payment method when the
     * customer is in your checkout flow. Use `off_session` if your customer may or
     * may not be in your checkout flow. If not provided, this value defaults to
     * `off_session`.
     */
    val usage: StripeIntent.Usage?,

    /**
     * @return The error encountered in the previous SetupIntent confirmation.
     */
    val lastSetupError: Error?
) : StripeModel(), StripeIntent {

    override val redirectData: StripeIntent.RedirectData?
        get() {
            if (StripeIntent.NextActionType.RedirectToUrl !== nextActionType) {
                return null
            }

            val nextAction: Map<String, Any?> = this.nextAction.takeIf {
                StripeIntent.Status.RequiresAction === status
            } ?: return null

            val nextActionType = StripeIntent.NextActionType
                .fromCode(nextAction[FIELD_NEXT_ACTION_TYPE] as String?)
            return if (StripeIntent.NextActionType.RedirectToUrl === nextActionType) {
                val redirectToUrl = nextAction[nextActionType.code]
                if (redirectToUrl is Map<*, *>) {
                    StripeIntent.RedirectData.create(redirectToUrl)
                } else {
                    null
                }
            } else {
                null
            }
        }

    val redirectUrl: Uri?
        get() {
            return redirectData?.url
        }

    override val stripeSdkData: StripeIntent.SdkData?
        get() = nextAction?.takeIf {
            StripeIntent.NextActionType.UseStripeSdk == nextActionType
        }?.let {
            StripeIntent.SdkData(
                it[StripeIntent.NextActionType.UseStripeSdk.code] as Map<String, *>
            )
        }

    override fun requiresAction(): Boolean {
        return status === StripeIntent.Status.RequiresAction
    }

    override fun requiresConfirmation(): Boolean {
        return status === StripeIntent.Status.RequiresConfirmation
    }

    /**
     * The error encountered in the previous SetupIntent confirmation.
     *
     * See [last_setup_error](https://stripe.com/docs/api/setup_intents/object#setup_intent_object-last_setup_error).
     */
    @Parcelize
    data class Error internal constructor(

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
    ) : Parcelable {
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

        companion object {
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
        RequestedByCustomer("requested_by_customer"),
        Abandoned("abandoned");

        internal companion object {
            internal fun fromCode(code: String?): CancellationReason? {
                return values().firstOrNull { it.code == code }
            }
        }
    }

    companion object {
        private const val VALUE_SETUP_INTENT = "setup_intent"

        private const val FIELD_ID = "id"
        private const val FIELD_OBJECT = "object"
        private const val FIELD_CANCELLATION_REASON = "cancellation_reason"
        private const val FIELD_CREATED = "created"
        private const val FIELD_CLIENT_SECRET = "client_secret"
        private const val FIELD_DESCRIPTION = "description"
        private const val FIELD_LAST_SETUP_ERROR = "last_setup_error"
        private const val FIELD_LIVEMODE = "livemode"
        private const val FIELD_NEXT_ACTION = "next_action"
        private const val FIELD_PAYMENT_METHOD_TYPES = "payment_method_types"
        private const val FIELD_STATUS = "status"
        private const val FIELD_USAGE = "usage"
        private const val FIELD_PAYMENT_METHOD = "payment_method"

        private const val FIELD_NEXT_ACTION_TYPE = "type"

        internal fun parseIdFromClientSecret(clientSecret: String): String {
            return clientSecret.split("_secret".toRegex())
                .dropLastWhile { it.isEmpty() }.toTypedArray()[0]
        }

        @JvmStatic
        fun fromString(jsonString: String?): SetupIntent? {
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

        @JvmStatic
        fun fromJson(jsonObject: JSONObject?): SetupIntent? {
            val objectType = optString(jsonObject, FIELD_OBJECT)
            if (jsonObject == null || VALUE_SETUP_INTENT != objectType) {
                return null
            }

            val nextAction = optMap(jsonObject, FIELD_NEXT_ACTION)
            val nextActionType = nextAction?.let {
                StripeIntent.NextActionType.fromCode(it[FIELD_NEXT_ACTION_TYPE] as String?)
            }
            return SetupIntent(
                id = optString(jsonObject, FIELD_ID),
                objectType = objectType,
                created = jsonObject.optLong(FIELD_CREATED),
                clientSecret = optString(jsonObject, FIELD_CLIENT_SECRET),
                cancellationReason = CancellationReason.fromCode(
                    optString(jsonObject, FIELD_CANCELLATION_REASON)
                ),
                description = optString(jsonObject, FIELD_DESCRIPTION),
                isLiveMode = jsonObject.optBoolean(FIELD_LIVEMODE),
                paymentMethodId = optString(jsonObject, FIELD_PAYMENT_METHOD),
                paymentMethodTypes = jsonArrayToList(
                    jsonObject.optJSONArray(FIELD_PAYMENT_METHOD_TYPES)
                ),
                status = StripeIntent.Status.fromCode(optString(jsonObject, FIELD_STATUS)),
                usage = StripeIntent.Usage.fromCode(optString(jsonObject, FIELD_USAGE)),
                nextAction = nextAction,
                nextActionType = nextActionType,
                lastSetupError = Error.fromJson(
                    jsonObject.optJSONObject(FIELD_LAST_SETUP_ERROR)
                )
            )
        }
    }
}
