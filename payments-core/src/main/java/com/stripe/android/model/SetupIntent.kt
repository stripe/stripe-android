package com.stripe.android.model

import androidx.annotation.RestrictTo
import com.stripe.android.core.model.StripeJsonUtils
import com.stripe.android.core.model.StripeModel
import com.stripe.android.model.parsers.SetupIntentJsonParser
import kotlinx.parcelize.Parcelize
import org.json.JSONObject
import java.util.regex.Pattern

/**
 * A [SetupIntent] guides you through the process of setting up a customer's payment credentials for
 * future payments.
 *
 * - [Setup Intents Overview](https://stripe.com/docs/payments/setup-intents)
 * - [SetupIntents API Reference](https://stripe.com/docs/api/setup_intents)
 */
@Parcelize
data class SetupIntent
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
constructor(
    /**
     * Unique identifier for the object.
     */
    override val id: String?,

    /**
     * Reason for cancellation of this [SetupIntent].
     */
    val cancellationReason: CancellationReason?,

    /**
     * Time at which the object was created. Measured in seconds since the Unix epoch.
     */
    override val created: Long,

    /**
     * Country code of the user.
     */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override val countryCode: String?,

    /**
     * The client secret of this SetupIntent. Used for client-side retrieval using a
     * publishable key.
     *
     * The client secret can be used to complete payment setup from your frontend. It should not
     * be stored, logged, embedded in URLs, or exposed to anyone other than the customer. Make
     * sure that you have TLS enabled on any page that includes the client secret.
     */
    override val clientSecret: String?,

    /**
     * An arbitrary string attached to the object. Often useful for displaying to users.
     */
    override val description: String?,

    /**
     * Has the value `true` if the object exists in live mode or the value
     * `false` if the object exists in test mode.
     */
    override val isLiveMode: Boolean,

    /**
     * The expanded [PaymentMethod] represented by [paymentMethodId].
     */
    override val paymentMethod: PaymentMethod? = null,

    /**
     * ID of the payment method used with this [SetupIntent].
     */
    override val paymentMethodId: String?,

    /**
     * The list of payment method types (e.g. card) that this [SetupIntent] is allowed to set up.
     */
    override val paymentMethodTypes: List<String>,

    /**
     * [Status](https://stripe.com/docs/payments/intents#intent-statuses) of this [SetupIntent].
     */
    override val status: StripeIntent.Status?,

    /**
     * Indicates how the payment method is intended to be used in the future.
     *
     * Use [StripeIntent.Usage.OnSession] if you intend to only reuse the payment method when the
     * customer is in your checkout flow. Use [StripeIntent.Usage.OffSession] if your customer may
     * or may not be in your checkout flow. If not provided, this value defaults to
     * [StripeIntent.Usage.OffSession].
     */
    val usage: StripeIntent.Usage?,

    /**
     * The error encountered in the previous [SetupIntent] confirmation.
     */
    val lastSetupError: Error? = null,

    /**
     * Payment types that have not been activated in livemode, but have been activated in testmode.
     */
    override val unactivatedPaymentMethods: List<String>,

    /**
     * Payment types that are accepted when paying with Link.
     */
    override val linkFundingSources: List<String>,

    override val nextActionData: StripeIntent.NextActionData?,

    private val paymentMethodOptionsJsonString: String? = null,
) : StripeIntent {

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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
            is StripeIntent.NextActionData.CashAppRedirect -> {
                StripeIntent.NextActionType.CashAppRedirect
            }
            is StripeIntent.NextActionData.AlipayRedirect,
            is StripeIntent.NextActionData.BlikAuthorize,
            is StripeIntent.NextActionData.WeChatPayRedirect,
            is StripeIntent.NextActionData.UpiAwaitNotification,
            is StripeIntent.NextActionData.SwishRedirect,
            null -> {
                null
            }
        }

    override val isConfirmed: Boolean
        get() = setOf(
            StripeIntent.Status.Processing,
            StripeIntent.Status.Succeeded
        ).contains(status)

    override val lastErrorMessage: String?
        get() = lastSetupError?.message

    override fun requiresAction(): Boolean {
        return status === StripeIntent.Status.RequiresAction
    }

    override fun requiresConfirmation(): Boolean {
        return status === StripeIntent.Status.RequiresConfirmation
    }

    /**
     * The error encountered in the previous [SetupIntent] confirmation.
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
                    return entries.firstOrNull { it.code == typeCode }
                }
            }
        }

        internal companion object {
            internal const val CODE_AUTHENTICATION_ERROR = "setup_intent_authentication_failure"
        }
    }

    internal data class ClientSecret(internal val value: String) {
        internal val setupIntentId: String =
            value.split("_secret".toRegex())
                .dropLastWhile { it.isEmpty() }.toTypedArray()[0]

        init {
            require(isMatch(value)) {
                "Invalid Setup Intent client secret: $value"
            }
        }

        internal companion object {
            private val PATTERN = Pattern.compile("^seti_[^_]+_secret_[^_]+$")

            fun isMatch(value: String) = PATTERN.matcher(value).matches()
        }
    }

    /**
     * Reason for cancellation of a [SetupIntent].
     */
    enum class CancellationReason(private val code: String) {
        Duplicate("duplicate"),
        RequestedByCustomer("requested_by_customer"),
        Abandoned("abandoned");

        internal companion object {
            internal fun fromCode(code: String?): CancellationReason? {
                return entries.firstOrNull { it.code == code }
            }
        }
    }

    companion object {
        @JvmStatic
        fun fromJson(jsonObject: JSONObject?): SetupIntent? {
            return jsonObject?.let {
                SetupIntentJsonParser().parse(it)
            }
        }
    }
}
