package com.stripe.android.model.parsers

import androidx.annotation.Size
import com.stripe.android.core.model.StripeJsonUtils
import com.stripe.android.core.model.StripeJsonUtils.optString
import com.stripe.android.core.model.StripeModel
import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.model.Source
import com.stripe.android.model.SourceTypeModel
import org.json.JSONObject

internal class SourceJsonParser : ModelJsonParser<Source> {
    override fun parse(json: JSONObject): Source? {
        return when (json.optString(FIELD_OBJECT)) {
            VALUE_CARD -> fromCardJson(json)
            VALUE_SOURCE -> fromSourceJson(json)
            else -> null
        }
    }

    internal class OwnerJsonParser : ModelJsonParser<Source.Owner> {
        override fun parse(json: JSONObject): Source.Owner {
            return Source.Owner(
                address = json.optJSONObject(FIELD_ADDRESS)?.let {
                    AddressJsonParser().parse(it)
                },
                email = optString(json, FIELD_EMAIL),
                name = optString(json, FIELD_NAME),
                phone = optString(json, FIELD_PHONE),
                verifiedAddress = json.optJSONObject(FIELD_VERIFIED_ADDRESS)?.let {
                    AddressJsonParser().parse(it)
                },
                verifiedEmail = optString(json, FIELD_VERIFIED_EMAIL),
                verifiedName = optString(json, FIELD_VERIFIED_NAME),
                verifiedPhone = optString(json, FIELD_VERIFIED_PHONE)
            )
        }

        private companion object {
            private const val FIELD_ADDRESS = "address"
            private const val FIELD_EMAIL = "email"
            private const val FIELD_NAME = "name"
            private const val FIELD_PHONE = "phone"
            private const val FIELD_VERIFIED_ADDRESS = "verified_address"
            private const val FIELD_VERIFIED_EMAIL = "verified_email"
            private const val FIELD_VERIFIED_NAME = "verified_name"
            private const val FIELD_VERIFIED_PHONE = "verified_phone"
        }
    }

    private companion object {
        private const val VALUE_SOURCE = "source"
        private const val VALUE_CARD = "card"

        private val MODELED_TYPES = setOf(
            Source.SourceType.CARD
        )

        private const val FIELD_ID: String = "id"
        private const val FIELD_OBJECT: String = "object"
        private const val FIELD_AMOUNT: String = "amount"
        private const val FIELD_CLIENT_SECRET: String = "client_secret"
        private const val FIELD_CREATED: String = "created"
        private const val FIELD_CURRENCY: String = "currency"
        private const val FIELD_LIVEMODE: String = "livemode"
        private const val FIELD_OWNER: String = "owner"
        private const val FIELD_SOURCE_ORDER: String = "source_order"
        private const val FIELD_STATEMENT_DESCRIPTOR: String = "statement_descriptor"
        private const val FIELD_STATUS: String = "status"
        private const val FIELD_TYPE: String = "type"
        private const val FIELD_USAGE: String = "usage"

        private fun fromCardJson(jsonObject: JSONObject): Source {
            return Source(
                optString(jsonObject, FIELD_ID),
                sourceTypeModel = SourceCardDataJsonParser().parse(jsonObject),
                type = Source.SourceType.CARD,
                typeRaw = Source.SourceType.CARD
            )
        }

        private fun fromSourceJson(jsonObject: JSONObject): Source {
            @Source.SourceType val typeRaw = optString(jsonObject, FIELD_TYPE)
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
                id = optString(jsonObject, FIELD_ID),
                amount = StripeJsonUtils.optLong(jsonObject, FIELD_AMOUNT),
                clientSecret = optString(jsonObject, FIELD_CLIENT_SECRET),
                created = StripeJsonUtils.optLong(jsonObject, FIELD_CREATED),
                currency = optString(jsonObject, FIELD_CURRENCY),
                isLiveMode = jsonObject.optBoolean(FIELD_LIVEMODE),
                owner = optStripeJsonModel(jsonObject, FIELD_OWNER),
                sourceOrder = jsonObject.optJSONObject(FIELD_SOURCE_ORDER)?.let {
                    SourceOrderJsonParser().parse(it)
                },
                statementDescriptor = optString(jsonObject, FIELD_STATEMENT_DESCRIPTOR),
                status = Source.Status.fromCode(optString(jsonObject, FIELD_STATUS)),
                sourceTypeData = sourceTypeData,
                sourceTypeModel = sourceTypeModel,
                type = type,
                typeRaw = typeRaw,
                usage = Source.Usage.fromCode(optString(jsonObject, FIELD_USAGE))
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
                FIELD_OWNER -> {
                    jsonObject.optJSONObject(FIELD_OWNER)?.let {
                        OwnerJsonParser().parse(it)
                    }
                }
                Source.SourceType.CARD -> {
                    jsonObject.optJSONObject(Source.SourceType.CARD)?.let {
                        SourceCardDataJsonParser().parse(it)
                    }
                }
                else -> {
                    null
                }
            }

            return model as? T
        }

        @Source.SourceType
        private fun asSourceType(sourceType: String?): String {
            return when (sourceType) {
                Source.SourceType.CARD -> Source.SourceType.CARD
                else -> Source.SourceType.UNKNOWN
            }
        }
    }
}
