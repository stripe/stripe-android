package com.stripe.android.model

import androidx.annotation.StringDef
import com.stripe.android.model.Source.SourceFlow
import com.stripe.android.model.Source.SourceType
import com.stripe.android.model.parsers.SourceJsonParser
import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.RawValue
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
    val codeVerification: SourceCodeVerification? = null,

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
    @param:SourceFlow @field:SourceFlow @get:SourceFlow
    val flow: String? = null,

    /**
     * Has the value true if the object exists in live mode or the value false if the object
     * exists in test mode.
     */
    val isLiveMode: Boolean? = null,

    /**
     * Set of key-value pairs that you can attach to an object. This can be useful for storing
     * additional information about the object in a structured format.
     */
    val metaData: Map<String, String>? = null,

    /**
     * Information about the owner of the payment instrument that may be used or required by
     * particular source types.
     */
    val owner: SourceOwner? = null,

    /**
     * Information related to the receiver flow.
     * Present if the source is a receiver ([flow] is [SourceFlow.RECEIVER]).
     */
    val receiver: SourceReceiver? = null,

    /**
     * Information related to the redirect flow. Present if the source is authenticated by a
     * redirect ([flow] is [SourceFlow.REDIRECT]).
     */
    val redirect: SourceRedirect? = null,

    /**
     * The status of the source, one of `canceled`, `chargeable`, `consumed`, `failed`,
     * or `pending`. Only `chargeable` sources can be used to create a charge.
     */
    @param:SourceStatus @field:SourceStatus @get:SourceStatus
    val status: String? = null,

    val sourceTypeData: Map<String, @RawValue Any?>? = null,

    val sourceTypeModel: SourceTypeModel? = null,

    /**
     * Gets the [SourceType] of this Source, as one of the enumerated values.
     * If a custom source type has been created, this returns [SourceType.UNKNOWN]. To get
     * the raw value of an [SourceType.UNKNOWN] type, use [typeRaw].
     *
     * @return the [SourceType] of this Source
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
    @param:Usage @field:Usage @get:Usage
    val usage: String? = null,

    private val weChatParam: WeChat? = null,

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

            return requireNotNull(weChatParam)
        }

    @Retention(AnnotationRetention.SOURCE)
    @StringDef(SourceType.ALIPAY, SourceType.CARD, SourceType.THREE_D_SECURE, SourceType.GIROPAY,
        SourceType.SEPA_DEBIT, SourceType.IDEAL, SourceType.SOFORT, SourceType.BANCONTACT,
        SourceType.P24, SourceType.EPS, SourceType.MULTIBANCO, SourceType.WECHAT, SourceType.KLARNA,
        SourceType.UNKNOWN)
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

    @Retention(AnnotationRetention.SOURCE)
    @StringDef(SourceStatus.PENDING, SourceStatus.CHARGEABLE, SourceStatus.CONSUMED,
        SourceStatus.CANCELED, SourceStatus.FAILED)
    annotation class SourceStatus {
        companion object {
            const val PENDING: String = "pending"
            const val CHARGEABLE: String = "chargeable"
            const val CONSUMED: String = "consumed"
            const val CANCELED: String = "canceled"
            const val FAILED: String = "failed"
        }
    }

    @Retention(AnnotationRetention.SOURCE)
    @StringDef(Usage.REUSABLE, Usage.SINGLE_USE)
    annotation class Usage {
        companion object {
            const val REUSABLE: String = "reusable"
            const val SINGLE_USE: String = "single_use"
        }
    }

    @Retention(AnnotationRetention.SOURCE)
    @StringDef(SourceFlow.REDIRECT, SourceFlow.RECEIVER, SourceFlow.CODE_VERIFICATION,
        SourceFlow.NONE)
    annotation class SourceFlow {
        companion object {
            const val REDIRECT: String = "redirect"
            const val RECEIVER: String = "receiver"
            const val CODE_VERIFICATION: String = "code_verification"
            const val NONE: String = "none"
        }
    }

    companion object {
        internal const val OBJECT_TYPE = "source"

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
