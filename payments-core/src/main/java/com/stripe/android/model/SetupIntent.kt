package com.stripe.android.model

import androidx.annotation.RestrictTo
import com.stripe.android.StripeCashAppPayBetaApi
import com.stripe.android.core.model.StripeModel
import com.stripe.android.model.parsers.SetupIntentJsonParser
import kotlinx.parcelize.Parcelize
import org.json.JSONObject
import java.util.Objects
import java.util.regex.Pattern

/**
 * A [SetupIntent] guides you through the process of setting up a customer's payment credentials for
 * future payments.
 *
 * - [Setup Intents Overview](https://stripe.com/docs/payments/setup-intents)
 * - [SetupIntents API Reference](https://stripe.com/docs/api/setup_intents)
 */
@OptIn(StripeCashAppPayBetaApi::class)
@Parcelize
class SetupIntent internal constructor(
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
    val countryCode: String?,

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
) : StripeIntent {

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

    override fun hashCode(): Int {
        return Objects.hash(
            id,
            cancellationReason,
            created,
            countryCode,
            clientSecret,
            description,
            isLiveMode,
            paymentMethod,
            paymentMethodId,
            paymentMethodTypes,
            status,
            usage,
            lastSetupError,
            unactivatedPaymentMethods,
            linkFundingSources,
            nextActionData,
        )
    }

    override fun equals(other: Any?): Boolean {
        return other is SetupIntent &&
            id == other.id &&
            cancellationReason == other.cancellationReason &&
            created == other.created &&
            countryCode == other.countryCode &&
            clientSecret == other.clientSecret &&
            description == other.description &&
            isLiveMode == other.isLiveMode &&
            paymentMethod == other.paymentMethod &&
            paymentMethodId == other.paymentMethodId &&
            paymentMethodTypes == other.paymentMethodTypes &&
            status == other.status &&
            usage == other.usage &&
            lastSetupError == other.lastSetupError &&
            unactivatedPaymentMethods == other.unactivatedPaymentMethods &&
            linkFundingSources == other.linkFundingSources &&
            nextActionData == other.nextActionData
    }

    override fun toString(): String {
        return "SetupIntent(" +
            "id=$id, " +
            "cancellationReason=$cancellationReason, " +
            "created=$created, " +
            "countryCode=$countryCode, " +
            "clientSecret=$clientSecret, " +
            "description=$description, " +
            "isLiveMode=$isLiveMode, " +
            "paymentMethod=$paymentMethod, " +
            "paymentMethodId=$paymentMethodId, " +
            "paymentMethodTypes=$paymentMethodTypes, " +
            "status=$status, " +
            "usage=$usage, " +
            "lastSetupError=$lastSetupError, " +
            "unactivatedPaymentMethods=$unactivatedPaymentMethods, " +
            "linkFundingSources=$linkFundingSources, " +
            "nextActionData=$nextActionData)"
    }

    @Deprecated(
        message = "This isn't meant for public usage and will be removed in a future release",
    )
    fun copy(
        id: String? = this.id,
        cancellationReason: CancellationReason? = this.cancellationReason,
        created: Long = this.created,
        countryCode: String? = this.countryCode,
        clientSecret: String? = this.clientSecret,
        description: String? = this.description,
        isLiveMode: Boolean = this.isLiveMode,
        paymentMethod: PaymentMethod? = this.paymentMethod,
        paymentMethodId: String? = this.paymentMethodId,
        paymentMethodTypes: List<String> = this.paymentMethodTypes,
        status: StripeIntent.Status? = this.status,
        usage: StripeIntent.Usage? = this.usage,
        lastSetupError: Error? = this.lastSetupError,
        unactivatedPaymentMethods: List<String> = this.unactivatedPaymentMethods,
        linkFundingSources: List<String> = this.linkFundingSources,
        nextActionData: StripeIntent.NextActionData? = this.nextActionData,
    ): SetupIntent {
        return SetupIntent(
            id = id,
            cancellationReason = cancellationReason,
            created = created,
            countryCode = countryCode,
            clientSecret = clientSecret,
            description = description,
            isLiveMode = isLiveMode,
            paymentMethod = paymentMethod,
            paymentMethodId = paymentMethodId,
            paymentMethodTypes = paymentMethodTypes,
            status = status,
            usage = usage,
            lastSetupError = lastSetupError,
            unactivatedPaymentMethods = unactivatedPaymentMethods,
            linkFundingSources = linkFundingSources,
            nextActionData = nextActionData,
        )
    }

    @Deprecated(
        message = "This isn't meant for public usage and will be removed in a future release",
    )
    fun component1(): String? = id

    @Deprecated(
        message = "This isn't meant for public usage and will be removed in a future release",
    )
    fun component2(): CancellationReason? = cancellationReason

    @Deprecated(
        message = "This isn't meant for public usage and will be removed in a future release",
    )
    fun component3(): Long = created

    @Deprecated(
        message = "This isn't meant for public usage and will be removed in a future release",
    )
    fun component4(): String? = countryCode

    @Deprecated(
        message = "This isn't meant for public usage and will be removed in a future release",
    )
    fun component5(): String? = clientSecret

    @Deprecated(
        message = "This isn't meant for public usage and will be removed in a future release",
    )
    fun component6(): String? = description

    @Deprecated(
        message = "This isn't meant for public usage and will be removed in a future release",
    )
    fun component7(): Boolean = isLiveMode

    @Deprecated(
        message = "This isn't meant for public usage and will be removed in a future release",
    )
    fun component8(): PaymentMethod? = paymentMethod

    @Deprecated(
        message = "This isn't meant for public usage and will be removed in a future release",
    )
    fun component9(): String? = paymentMethodId

    @Deprecated(
        message = "This isn't meant for public usage and will be removed in a future release",
    )
    fun component10(): List<String> = paymentMethodTypes

    @Deprecated(
        message = "This isn't meant for public usage and will be removed in a future release",
    )
    fun component11(): StripeIntent.Status? = status

    @Deprecated(
        message = "This isn't meant for public usage and will be removed in a future release",
    )
    fun component12(): StripeIntent.Usage? = usage

    @Deprecated(
        message = "This isn't meant for public usage and will be removed in a future release",
    )
    fun component13(): SetupIntent.Error? = lastSetupError

    @Deprecated(
        message = "This isn't meant for public usage and will be removed in a future release",
    )
    fun component14(): List<String> = unactivatedPaymentMethods

    @Deprecated(
        message = "This isn't meant for public usage and will be removed in a future release",
    )
    fun component15(): List<String> = linkFundingSources

    @Deprecated(
        message = "This isn't meant for public usage and will be removed in a future release",
    )
    fun component16(): StripeIntent.NextActionData? = nextActionData

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
                    return values().firstOrNull { it.code == typeCode }
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
                return values().firstOrNull { it.code == code }
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
