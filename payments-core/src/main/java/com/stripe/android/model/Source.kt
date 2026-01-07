package com.stripe.android.model

import androidx.annotation.Keep
import androidx.annotation.StringDef
import com.stripe.android.core.model.StripeModel
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

    @Retention(AnnotationRetention.SOURCE)
    @StringDef(SourceType.CARD)
    annotation class SourceType {
        companion object {
            const val CARD: String = "card"
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
            fun fromCode(code: String?) = entries.firstOrNull { it.code == code }
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
            fun fromCode(code: String?) = entries.firstOrNull { it.code == code }
        }
    }

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
                else -> SourceType.UNKNOWN
            }
        }
    }
}
