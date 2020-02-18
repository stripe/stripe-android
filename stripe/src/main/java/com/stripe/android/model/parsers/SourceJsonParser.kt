package com.stripe.android.model.parsers

import androidx.annotation.Size
import com.stripe.android.model.Source
import com.stripe.android.model.SourceTypeModel
import com.stripe.android.model.StripeJsonUtils
import com.stripe.android.model.StripeModel
import org.json.JSONObject

internal class SourceJsonParser : ModelJsonParser<Source> {
    override fun parse(json: JSONObject): Source? {
        return when (json.optString(FIELD_OBJECT)) {
            VALUE_CARD -> fromCardJson(json)
            VALUE_SOURCE -> fromSourceJson(json)
            else -> null
        }
    }

    private companion object {
        private const val VALUE_SOURCE = "source"
        private const val VALUE_CARD = "card"

        private val MODELED_TYPES = setOf(
            Source.SourceType.CARD, Source.SourceType.SEPA_DEBIT
        )

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
        private const val FIELD_SOURCE_ORDER: String = "source_order"
        private const val FIELD_STATEMENT_DESCRIPTOR: String = "statement_descriptor"
        private const val FIELD_STATUS: String = "status"
        private const val FIELD_TYPE: String = "type"
        private const val FIELD_USAGE: String = "usage"
        private const val FIELD_WECHAT: String = "wechat"

        private fun fromCardJson(jsonObject: JSONObject): Source {
            return Source(
                StripeJsonUtils.optString(jsonObject, FIELD_ID),
                sourceTypeModel = SourceCardDataJsonParser().parse(jsonObject),
                type = Source.SourceType.CARD,
                typeRaw = Source.SourceType.CARD
            )
        }

        private fun fromSourceJson(jsonObject: JSONObject): Source {
            @Source.SourceType val typeRaw = StripeJsonUtils.optString(jsonObject, FIELD_TYPE)
                ?: Source.SourceType.UNKNOWN
            @Source.SourceType val type = asSourceType(typeRaw)

            // Until we have models for all types, keep the original hash and the
            // model object. The customType variable can be any field, and is not altered by
            // trying to force it to be a type that we know of.
            val sourceTypeData = StripeJsonUtils.jsonObjectToMap(
                jsonObject.optJSONObject(typeRaw)
            )
            val sourceTypeModel = if (MODELED_TYPES.contains(typeRaw)) {
                optStripeJsonModel<SourceTypeModel>(jsonObject, typeRaw)
            } else {
                null
            }

            return Source(
                id = StripeJsonUtils.optString(jsonObject, FIELD_ID),
                amount = StripeJsonUtils.optLong(jsonObject, FIELD_AMOUNT),
                clientSecret = StripeJsonUtils.optString(jsonObject, FIELD_CLIENT_SECRET),
                codeVerification = optStripeJsonModel(
                    jsonObject,
                    FIELD_CODE_VERIFICATION
                ),
                created = StripeJsonUtils.optLong(jsonObject, FIELD_CREATED),
                currency = StripeJsonUtils.optString(jsonObject, FIELD_CURRENCY),
                flow = asSourceFlow(StripeJsonUtils.optString(jsonObject, FIELD_FLOW)),
                isLiveMode = jsonObject.optBoolean(FIELD_LIVEMODE),
                metaData = StripeJsonUtils.jsonObjectToStringMap(
                    jsonObject.optJSONObject(FIELD_METADATA)
                ),
                owner = optStripeJsonModel(jsonObject, FIELD_OWNER),
                receiver = optStripeJsonModel(jsonObject, FIELD_RECEIVER),
                redirect = optStripeJsonModel(jsonObject, FIELD_REDIRECT),
                sourceOrder = jsonObject.optJSONObject(FIELD_SOURCE_ORDER)?.let {
                    SourceOrderJsonParser().parse(it)
                },
                statementDescriptor = StripeJsonUtils.optString(jsonObject, FIELD_STATEMENT_DESCRIPTOR),
                status = asSourceStatus(StripeJsonUtils.optString(jsonObject, FIELD_STATUS)),
                sourceTypeData = sourceTypeData,
                sourceTypeModel = sourceTypeModel,
                type = type,
                typeRaw = typeRaw,
                usage = asUsage(StripeJsonUtils.optString(jsonObject, FIELD_USAGE)),
                weChatParam = if (Source.SourceType.WECHAT == type) {
                    WeChatJsonParser().parse(
                        jsonObject.optJSONObject(FIELD_WECHAT) ?: JSONObject()
                    )
                } else {
                    null
                }
            )
        }

        private inline fun <reified T : StripeModel> optStripeJsonModel(
            jsonObject: JSONObject,
            @Size(min = 1) key: String
        ): T? {
            if (!jsonObject.has(key)) {
                return null
            }

            val model: StripeModel? = when (key) {
                FIELD_CODE_VERIFICATION -> {
                    jsonObject.optJSONObject(FIELD_CODE_VERIFICATION)?.let {
                        SourceCodeVerificationJsonParser().parse(it)
                    }
                }
                FIELD_OWNER -> {
                    jsonObject.optJSONObject(FIELD_OWNER)?.let {
                        SourceOwnerJsonParser().parse(it)
                    }
                }
                FIELD_RECEIVER -> {
                    jsonObject.optJSONObject(FIELD_RECEIVER)?.let {
                        SourceReceiverJsonParser().parse(it)
                    }
                }
                FIELD_REDIRECT -> {
                    jsonObject.optJSONObject(FIELD_REDIRECT)?.let {
                        SourceRedirectJsonParser().parse(it)
                    }
                }
                Source.SourceType.CARD -> {
                    jsonObject.optJSONObject(Source.SourceType.CARD)?.let {
                        SourceCardDataJsonParser().parse(it)
                    }
                }
                Source.SourceType.SEPA_DEBIT -> {
                    jsonObject.optJSONObject(Source.SourceType.SEPA_DEBIT)?.let {
                        SourceSepaDebitDataJsonParser().parse(it)
                    }
                }
                else -> {
                    null
                }
            }

            return model as? T
        }

        @Source.SourceStatus
        private fun asSourceStatus(sourceStatus: String?): String? {
            return when (sourceStatus) {
                Source.SourceStatus.PENDING -> Source.SourceStatus.PENDING
                Source.SourceStatus.CHARGEABLE -> Source.SourceStatus.CHARGEABLE
                Source.SourceStatus.CONSUMED -> Source.SourceStatus.CONSUMED
                Source.SourceStatus.CANCELED -> Source.SourceStatus.CANCELED
                Source.SourceStatus.FAILED -> Source.SourceStatus.FAILED
                else -> null
            }
        }

        @Source.SourceType
        @JvmStatic
        fun asSourceType(sourceType: String?): String {
            return when (sourceType) {
                Source.SourceType.CARD -> Source.SourceType.CARD
                Source.SourceType.THREE_D_SECURE -> Source.SourceType.THREE_D_SECURE
                Source.SourceType.GIROPAY -> Source.SourceType.GIROPAY
                Source.SourceType.SEPA_DEBIT -> Source.SourceType.SEPA_DEBIT
                Source.SourceType.IDEAL -> Source.SourceType.IDEAL
                Source.SourceType.SOFORT -> Source.SourceType.SOFORT
                Source.SourceType.BANCONTACT -> Source.SourceType.BANCONTACT
                Source.SourceType.ALIPAY -> Source.SourceType.ALIPAY
                Source.SourceType.EPS -> Source.SourceType.EPS
                Source.SourceType.P24 -> Source.SourceType.P24
                Source.SourceType.MULTIBANCO -> Source.SourceType.MULTIBANCO
                Source.SourceType.WECHAT -> Source.SourceType.WECHAT
                Source.SourceType.UNKNOWN -> Source.SourceType.UNKNOWN
                else -> Source.SourceType.UNKNOWN
            }
        }

        @Source.Usage
        private fun asUsage(usage: String?): String? {
            return when (usage) {
                Source.Usage.REUSABLE -> Source.Usage.REUSABLE
                Source.Usage.SINGLE_USE -> Source.Usage.SINGLE_USE
                else -> null
            }
        }

        @Source.SourceFlow
        private fun asSourceFlow(sourceFlow: String?): String? {
            return when (sourceFlow) {
                Source.SourceFlow.REDIRECT -> Source.SourceFlow.REDIRECT
                Source.SourceFlow.RECEIVER -> Source.SourceFlow.RECEIVER
                Source.SourceFlow.CODE_VERIFICATION -> Source.SourceFlow.CODE_VERIFICATION
                Source.SourceFlow.NONE -> Source.SourceFlow.NONE
                else -> null
            }
        }
    }
}
