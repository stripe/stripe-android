package com.stripe.android.model

import androidx.annotation.Size
import androidx.annotation.StringDef
import com.stripe.android.model.Source.SourceType
import com.stripe.android.model.StripeJsonUtils.optLong
import com.stripe.android.model.StripeJsonUtils.optString
import org.json.JSONException
import org.json.JSONObject

/**
 * Model for a [Sources API](https://stripe.com/docs/sources) object.
 *
 * See [Sources API Reference](https://stripe.com/docs/api/sources/object).
 */
data class Source private constructor(
    override val id: String?,
    val amount: Long?,
    val clientSecret: String?,
    val codeVerification: SourceCodeVerification?,
    val created: Long?,
    val currency: String?,

    @param:SourceFlow @field:SourceFlow @get:SourceFlow
    val flow: String? = null,

    val isLiveMode: Boolean?,
    val metaData: Map<String, String>?,
    val owner: SourceOwner?,
    val receiver: SourceReceiver? = null,
    val redirect: SourceRedirect? = null,

    @param:SourceStatus @field:SourceStatus @get:SourceStatus
    val status: String?,

    val sourceTypeData: Map<String, Any?>?,
    val sourceTypeModel: StripeSourceTypeModel?,

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

    @param:Usage @field:Usage @get:Usage
    val usage: String?,

    private val weChatParam: WeChat?
) : StripeModel(), StripePaymentSource {

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
        SourceType.P24, SourceType.EPS, SourceType.MULTIBANCO, SourceType.WECHAT,
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
        internal const val VALUE_SOURCE = "source"
        private const val VALUE_CARD = "card"

        private val MODELED_TYPES = setOf(SourceType.CARD, SourceType.SEPA_DEBIT)

        internal const val EURO: String = "eur"
        internal const val USD: String = "usd"

        private const val FIELD_ID: String = "id"
        private const val FIELD_OBJECT: String = "object"
        private const val FIELD_AMOUNT: String = "amount"
        private const val FIELD_CLIENT_SECRET: String = "client_secret"
        private const val FIELD_CODE_VERIFICATION: String = "code_verification"
        private const val FIELD_CREATED: String = "created"
        private const val FIELD_CURRENCY: String = "currency"
        private const val FIELD_FLOW: String = "flow"
        private const val FIELD_LIVEMODE: String = "livemode"
        private const val FIELD_METADATA: String = "metadata"
        private const val FIELD_OWNER: String = "owner"
        private const val FIELD_RECEIVER: String = "receiver"
        private const val FIELD_REDIRECT: String = "redirect"
        private const val FIELD_STATUS: String = "status"
        private const val FIELD_TYPE: String = "type"
        private const val FIELD_USAGE: String = "usage"
        private const val FIELD_WECHAT: String = "wechat"

        @JvmStatic
        fun fromString(jsonString: String?): Source? {
            if (jsonString == null) {
                return null
            }

            return try {
                fromJson(JSONObject(jsonString))
            } catch (ignored: JSONException) {
                null
            }
        }

        @JvmStatic
        fun fromJson(jsonObject: JSONObject?): Source? {
            return when (jsonObject?.optString(FIELD_OBJECT)) {
                VALUE_CARD -> fromCardJson(jsonObject)
                VALUE_SOURCE -> fromSourceJson(jsonObject)
                else -> null
            }
        }

        private fun fromCardJson(jsonObject: JSONObject): Source {
            val id = optString(jsonObject, FIELD_ID)
            val sourceTypeModel = SourceCardData.fromJson(jsonObject)

            return Source(
                id, null, null, null,
                null, null, null, null,
                null, null, null, null,
                null, null, sourceTypeModel, SourceType.CARD,
                SourceType.CARD, null, null
            )
        }

        private fun fromSourceJson(jsonObject: JSONObject): Source {
            @SourceType val typeRaw = optString(jsonObject, FIELD_TYPE) ?: SourceType.UNKNOWN
            @SourceType val type = asSourceType(typeRaw)

            // Until we have models for all types, keep the original hash and the
            // model object. The customType variable can be any field, and is not altered by
            // trying to force it to be a type that we know of.
            val sourceTypeData = StripeJsonUtils.jsonObjectToMap(
                jsonObject.optJSONObject(typeRaw)
            )
            val sourceTypeModel = if (MODELED_TYPES.contains(typeRaw)) {
                optStripeJsonModel(jsonObject, typeRaw, StripeSourceTypeModel::class.java)
            } else {
                null
            }

            return Source(
                id = optString(jsonObject, FIELD_ID),
                amount = optLong(jsonObject, FIELD_AMOUNT),
                clientSecret = optString(jsonObject, FIELD_CLIENT_SECRET),
                codeVerification = optStripeJsonModel(
                    jsonObject,
                    FIELD_CODE_VERIFICATION,
                    SourceCodeVerification::class.java
                ),
                created = optLong(jsonObject, FIELD_CREATED),
                currency = optString(jsonObject, FIELD_CURRENCY),
                flow = asSourceFlow(optString(jsonObject, FIELD_FLOW)),
                isLiveMode = jsonObject.optBoolean(FIELD_LIVEMODE),
                metaData = StripeJsonUtils.jsonObjectToStringMap(
                    jsonObject.optJSONObject(FIELD_METADATA)
                ),
                owner = optStripeJsonModel(jsonObject, FIELD_OWNER, SourceOwner::class.java),
                receiver = optStripeJsonModel(
                    jsonObject,
                    FIELD_RECEIVER,
                    SourceReceiver::class.java
                ),
                redirect = optStripeJsonModel(
                    jsonObject, FIELD_REDIRECT, SourceRedirect::class.java
                ),
                status = asSourceStatus(optString(jsonObject, FIELD_STATUS)),
                sourceTypeData = sourceTypeData,
                sourceTypeModel = sourceTypeModel,
                type = type,
                typeRaw = typeRaw,
                usage = asUsage(optString(jsonObject, FIELD_USAGE)),
                weChatParam = if (SourceType.WECHAT == type) {
                    WeChat.fromJson(jsonObject.optJSONObject(FIELD_WECHAT) ?: JSONObject())
                } else {
                    null
                }
            )
        }

        private fun <T : StripeModel> optStripeJsonModel(
            jsonObject: JSONObject,
            @Size(min = 1) key: String,
            type: Class<T>
        ): T? {
            if (!jsonObject.has(key)) {
                return null
            }

            when (key) {
                FIELD_CODE_VERIFICATION -> {
                    return type.cast(SourceCodeVerification.fromJson(
                        jsonObject.optJSONObject(FIELD_CODE_VERIFICATION)
                    ))
                }
                FIELD_OWNER -> {
                    return type.cast(
                        SourceOwner.fromJson(jsonObject.optJSONObject(FIELD_OWNER)))
                }
                FIELD_RECEIVER -> {
                    return type.cast(
                        SourceReceiver.fromJson(jsonObject.optJSONObject(FIELD_RECEIVER)))
                }
                FIELD_REDIRECT -> {
                    return type.cast(
                        SourceRedirect.fromJson(jsonObject.optJSONObject(FIELD_REDIRECT)))
                }
                SourceType.CARD -> {
                    return type.cast(
                        SourceCardData.fromJson(jsonObject.optJSONObject(SourceType.CARD)))
                }
                SourceType.SEPA_DEBIT -> {
                    return type.cast(SourceSepaDebitData.fromJson(
                        jsonObject.optJSONObject(SourceType.SEPA_DEBIT)
                    ))
                }
                else -> {
                    return null
                }
            }
        }

        @SourceStatus
        private fun asSourceStatus(sourceStatus: String?): String? {
            return when (sourceStatus) {
                SourceStatus.PENDING -> SourceStatus.PENDING
                SourceStatus.CHARGEABLE -> SourceStatus.CHARGEABLE
                SourceStatus.CONSUMED -> SourceStatus.CONSUMED
                SourceStatus.CANCELED -> SourceStatus.CANCELED
                SourceStatus.FAILED -> SourceStatus.FAILED
                else -> null
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

        @Usage
        private fun asUsage(usage: String?): String? {
            return when (usage) {
                Usage.REUSABLE -> Usage.REUSABLE
                Usage.SINGLE_USE -> Usage.SINGLE_USE
                else -> null
            }
        }

        @SourceFlow
        private fun asSourceFlow(sourceFlow: String?): String? {
            return when (sourceFlow) {
                SourceFlow.REDIRECT -> SourceFlow.REDIRECT
                SourceFlow.RECEIVER -> SourceFlow.RECEIVER
                SourceFlow.CODE_VERIFICATION -> SourceFlow.CODE_VERIFICATION
                SourceFlow.NONE -> SourceFlow.NONE
                else -> null
            }
        }
    }
}
