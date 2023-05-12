package com.stripe.android.model

import androidx.annotation.Keep
import androidx.annotation.StringDef
import com.stripe.android.core.model.StripeModel
import com.stripe.android.model.Source.Flow
import com.stripe.android.model.Source.SourceType
import com.stripe.android.model.parsers.SourceJsonParser
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue
import org.json.JSONObject

/**
 * Model for a [Sources API](https://stripe.com/docs/sources) object.
 *
 * See [Sources API Reference](https://stripe.com/docs/api/sources/object).
 */
@Parcelize
data class Source internal constructor(
    /**
     * Unique identifier for the object.
     */
    override val id: String?,

    /**
     * A positive integer in the smallest currency unit (that is, 100 cents for $1.00, or 1 for ¥1,
     * Japanese Yen being a zero-decimal currency) representing the total amount associated with
     * the source. This is the amount for which the source will be chargeable once ready.
     * Required for `single_use` sources.
     */
    val amount: Long? = null,

    /**
     * The client secret of the source. Used for client-side retrieval using a publishable key.
     */
    val clientSecret: String? = null,

    /**
     * Information related to the code verification flow. Present if the source is authenticated
     * by a verification code (`flow` is `code_verification`).
     */
    val codeVerification: CodeVerification? = null,

    /**
     * Time at which the object was created. Measured in seconds since the Unix epoch.
     */
    val created: Long? = null,

    /**
     * Three-letter [ISO code for the currency](https://stripe.com/docs/currencies) associated with
     * the source. This is the currency for which the source will be chargeable once ready.
     * Required for `single_use` sources.
     */
    val currency: String? = null,

    /**
     * The authentication `flow` of the source.
     * `flow` is one of `redirect`, `receiver`, `code_verification`, `none`.
     */
    val flow: Flow? = null,

    /**
     * Has the value true if the object exists in live mode or the value false if the object
     * exists in test mode.
     */
    val isLiveMode: Boolean? = null,

    /**
     * Information about the owner of the payment instrument that may be used or required by
     * particular source types.
     */
    val owner: Owner? = null,

    /**
     * Information related to the receiver flow.
     * Present if the source is a receiver ([flow] is [Flow.Receiver]).
     */
    val receiver: Receiver? = null,

    /**
     * Information related to the redirect flow. Present if the source is authenticated by a
     * redirect ([flow] is [Flow.REDIRECT]).
     */
    val redirect: Redirect? = null,

    /**
     * The status of the source, one of `canceled`, `chargeable`, `consumed`, `failed`,
     * or `pending`. Only `chargeable` sources can be used to create a charge.
     */
    val status: Status? = null,

    val sourceTypeData: Map<String, @RawValue Any?>? = null,

    val sourceTypeModel: SourceTypeModel? = null,

    /**
     * The [SourceType] of this Source, as one of the enumerated values.
     * If a custom source type has been created, this returns [SourceType.UNKNOWN]. To get
     * the raw value of an [SourceType.UNKNOWN] type, use [typeRaw].
     */
    @param:SourceType @field:SourceType @get:SourceType
    val type: String,

    /**
     * Gets the type of this source as a String. If it is a known type, this will return
     * a string equal to the [SourceType] returned from [type]. This
     * method is not restricted to known types
     *
     * @return the type of this Source as a string
     */
    val typeRaw: String,

    /**
     * Either `reusable` or `single_use`. Whether this source should be reusable or not. Some source
     * types may or may not be reusable by construction, while others may leave the option at
     * creation. If an incompatible value is passed, an error will be returned.
     */
    val usage: Usage? = null,

    private val _weChat: WeChat? = null,

    private val _klarna: Klarna? = null,

    /**
     * Information about the items and shipping associated with the source. Required for
     * transactional credit (for example Klarna) sources before you can charge it.
     */
    val sourceOrder: SourceOrder? = null,

    /**
     * Extra information about a source. This will appear on your customer’s statement
     * every time you charge the source.
     */
    val statementDescriptor: String? = null
) : StripeModel, StripePaymentSource {

    val weChat: WeChat
        get() {
            check(SourceType.WECHAT == type) {
                "Source type must be '${SourceType.WECHAT}'"
            }

            return requireNotNull(_weChat)
        }

    val klarna: Klarna
        get() {
            check(SourceType.KLARNA == type) {
                "Source type must be '${SourceType.KLARNA}'"
            }

            return requireNotNull(_klarna)
        }

    @Retention(AnnotationRetention.SOURCE)
    @StringDef(
        SourceType.ALIPAY, SourceType.CARD, SourceType.THREE_D_SECURE, SourceType.GIROPAY,
        SourceType.SEPA_DEBIT, SourceType.IDEAL, SourceType.SOFORT, SourceType.BANCONTACT,
        SourceType.P24, SourceType.EPS, SourceType.MULTIBANCO, SourceType.WECHAT, SourceType.KLARNA,
        SourceType.UNKNOWN
    )
    annotation class SourceType {
        companion object {
            const val ALIPAY: String = "alipay"
            const val CARD: String = "card"
            const val THREE_D_SECURE: String = "three_d_secure"
            const val GIROPAY: String = "giropay"
            const val SEPA_DEBIT: String = "sepa_debit"
            const val IDEAL: String = "ideal"
            const val SOFORT: String = "sofort"
            const val BANCONTACT: String = "bancontact"
            const val P24: String = "p24"
            const val EPS: String = "eps"
            const val MULTIBANCO: String = "multibanco"
            const val WECHAT: String = "wechat"
            const val KLARNA: String = "klarna"
            const val UNKNOWN: String = "unknown"
        }
    }

    /**
     * The status of the source, one of `canceled`, `chargeable`, `consumed`, `failed`,
     * or `pending`. Only `chargeable` sources can be used to create a charge.
     */
    enum class Status(private val code: String) {
        Canceled("canceled"),
        Chargeable("chargeable"),
        Consumed("consumed"),
        Failed("failed"),
        Pending("pending");

        @Keep
        override fun toString(): String = code

        internal companion object {
            fun fromCode(code: String?) = values().firstOrNull { it.code == code }
        }
    }

    /**
     * Either `reusable` or `single_use`. Whether this source should be reusable or not.
     * Some source types may or may not be reusable by construction, while others may leave the
     * option at creation. If an incompatible value is passed, an error will be returned.
     */
    enum class Usage(internal val code: String) {
        Reusable("reusable"),
        SingleUse("single_use");

        @Keep
        override fun toString(): String = code

        internal companion object {
            fun fromCode(code: String?) = values().firstOrNull { it.code == code }
        }
    }

    /**
     * The authentication `flow` of the source.
     */
    enum class Flow(internal val code: String) {
        Redirect("redirect"),
        Receiver("receiver"),
        CodeVerification("code_verification"),
        None("none");

        @Keep
        override fun toString(): String = code

        internal companion object {
            fun fromCode(code: String?) = values().firstOrNull { it.code == code }
        }
    }

    /**
     * Information related to the redirect flow. Present if the source is authenticated by a
     * redirect ([flow] is [Flow.Redirect]).
     */
    @Parcelize
    data class Redirect(
        /**
         * The URL you provide to redirect the customer to after they authenticated their payment.
         */
        val returnUrl: String?,

        /**
         * The status of the redirect, either
         * `pending` (ready to be used by your customer to authenticate the transaction),
         * `succeeded` (succesful authentication, cannot be reused) or
         * `not_required` (redirect should not be used) or
         * `failed` (failed authentication, cannot be reused).
         */
        val status: Status?,

        /**
         * The URL provided to you to redirect a customer to as part of a `redirect`
         * authentication flow.
         */
        val url: String?
    ) : StripeModel {

        enum class Status(private val code: String) {
            Pending("pending"),
            Succeeded("succeeded"),
            NotRequired("not_required"),
            Failed("failed");

            @Keep
            override fun toString(): String = code

            internal companion object {
                fun fromCode(code: String?) = values().firstOrNull { it.code == code }
            }
        }
    }

    /**
     * Information related to the code verification flow. Present if the source is authenticated
     * by a verification code ([flow] is [Flow.CodeVerification]).
     */
    @Parcelize
    data class CodeVerification internal constructor(
        /**
         * The number of attempts remaining to authenticate the source object with a verification
         * code.
         */
        val attemptsRemaining: Int,

        /**
         * The status of the code verification, either
         * `pending` (awaiting verification, `attempts_remaining` should be greater than 0),
         * `succeeded` (successful verification) or
         * `failed` (failed verification, cannot be verified anymore as `attempts_remaining` should be 0).
         */
        val status: Status?
    ) : StripeModel {

        enum class Status(private val code: String) {
            Pending("pending"),
            Succeeded("succeeded"),
            Failed("failed");

            @Keep
            override fun toString(): String = code

            internal companion object {
                fun fromCode(code: String?) = values().firstOrNull { it.code == code }
            }
        }
    }

    /**
     * Information related to the receiver flow. Present if [flow] is [Source.Flow.Receiver].
     */
    @Parcelize
    data class Receiver internal constructor(
        /**
         * The address of the receiver source. This is the value that should be communicated to the
         * customer to send their funds to.
         */
        val address: String?,

        /**
         * The total amount that was moved to your balance. This is almost always equal to the amount
         * charged. In rare cases when customers deposit excess funds and we are unable to refund
         * those, those funds get moved to your balance and show up in amount_charged as well.
         * The amount charged is expressed in the source’s currency.
         */
        val amountCharged: Long,

        /**
         * The total amount received by the receiver source.
         * `amount_received = amount_returned + amount_charged` should be true for consumed sources
         * unless customers deposit excess funds. The amount received is expressed in the source’s
         * currency.
         */
        val amountReceived: Long,

        /**
         * The total amount that was returned to the customer. The amount returned is expressed in
         * the source’s currency.
         */
        val amountReturned: Long
    ) : StripeModel

    /**
     * Information about the owner of the payment instrument that may be used or required by
     * particular source types.
     */
    @Parcelize
    data class Owner internal constructor(
        /**
         * Owner’s address.
         */
        val address: Address?,

        /**
         * Owner’s email address.
         */
        val email: String?,

        /**
         * Owner’s full name.
         */
        val name: String?,

        /**
         * Owner’s phone number (including extension).
         */
        val phone: String?,

        /**
         * Verified owner’s address. Verified values are verified or provided by the payment
         * method directly (and if supported) at the time of authorization or settlement.
         * They cannot be set or mutated.
         */
        val verifiedAddress: Address?,

        /**
         * Verified owner’s email address. Verified values are verified or provided by the
         * payment method directly (and if supported) at the time of authorization or settlement.
         * They cannot be set or mutated.
         */
        val verifiedEmail: String?,

        /**
         * Verified owner’s full name. Verified values are verified or provided by the payment
         * method directly (and if supported) at the time of authorization or settlement.
         * They cannot be set or mutated.
         */
        val verifiedName: String?,

        /**
         * Verified owner’s phone number (including extension). Verified values are verified or
         * provided by the payment method directly (and if supported) at the time of authorization
         * or settlement. They cannot be set or mutated.
         */
        val verifiedPhone: String?
    ) : StripeModel

    @Parcelize
    data class Klarna(
        val firstName: String?,
        val lastName: String?,
        val purchaseCountry: String?,
        val clientToken: String?,
        val payNowAssetUrlsDescriptive: String?,
        val payNowAssetUrlsStandard: String?,
        val payNowName: String?,
        val payNowRedirectUrl: String?,
        val payLaterAssetUrlsDescriptive: String?,
        val payLaterAssetUrlsStandard: String?,
        val payLaterName: String?,
        val payLaterRedirectUrl: String?,
        val payOverTimeAssetUrlsDescriptive: String?,
        val payOverTimeAssetUrlsStandard: String?,
        val payOverTimeName: String?,
        val payOverTimeRedirectUrl: String?,
        val paymentMethodCategories: Set<String>,
        val customPaymentMethods: Set<String>
    ) : StripeModel

    companion object {
        internal const val EURO: String = "eur"
        internal const val USD: String = "usd"

        @JvmStatic
        fun fromJson(jsonObject: JSONObject?): Source? {
            return jsonObject?.let {
                SourceJsonParser().parse(it)
            }
        }

        @SourceType
        @JvmStatic
        fun asSourceType(sourceType: String?): String {
            return when (sourceType) {
                SourceType.CARD -> SourceType.CARD
                SourceType.THREE_D_SECURE -> SourceType.THREE_D_SECURE
                SourceType.GIROPAY -> SourceType.GIROPAY
                SourceType.SEPA_DEBIT -> SourceType.SEPA_DEBIT
                SourceType.IDEAL -> SourceType.IDEAL
                SourceType.SOFORT -> SourceType.SOFORT
                SourceType.BANCONTACT -> SourceType.BANCONTACT
                SourceType.ALIPAY -> SourceType.ALIPAY
                SourceType.EPS -> SourceType.EPS
                SourceType.P24 -> SourceType.P24
                SourceType.MULTIBANCO -> SourceType.MULTIBANCO
                SourceType.WECHAT -> SourceType.WECHAT
                SourceType.UNKNOWN -> SourceType.UNKNOWN
                else -> SourceType.UNKNOWN
            }
        }
    }
}
