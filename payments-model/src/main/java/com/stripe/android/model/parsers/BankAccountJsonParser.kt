package com.stripe.android.model.parsers

import androidx.annotation.RestrictTo
import com.stripe.android.core.model.StripeJsonUtils
import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.model.BankAccount
import kotlinx.serialization.KSerializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.json.JSONObject

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class BankAccountJsonParser : ModelJsonParser<BankAccount> {
    override fun parse(json: JSONObject): BankAccount {
        return BankAccount(
            id = StripeJsonUtils.optString(json, FIELD_ID),
            accountHolderName = StripeJsonUtils.optString(json, FIELD_ACCOUNT_HOLDER_NAME),
            accountHolderType = BankAccount.Type.fromCode(
                StripeJsonUtils.optString(json, FIELD_ACCOUNT_HOLDER_TYPE)
            ),
            bankName = StripeJsonUtils.optString(json, FIELD_BANK_NAME),
            countryCode = StripeJsonUtils.optCountryCode(json, FIELD_COUNTRY),
            currency = StripeJsonUtils.optCurrency(json, FIELD_CURRENCY),
            fingerprint = StripeJsonUtils.optString(json, FIELD_FINGERPRINT),
            last4 = StripeJsonUtils.optString(json, FIELD_LAST4),
            routingNumber = StripeJsonUtils.optString(json, FIELD_ROUTING_NUMBER),
            status = BankAccount.Status.fromCode(
                StripeJsonUtils.optString(json, FIELD_STATUS)
            )
        )
    }

    internal companion object {
        internal const val FIELD_OBJECT = "object"
        internal const val FIELD_ID = "id"
        internal const val FIELD_ACCOUNT_HOLDER_NAME = "account_holder_name"
        internal const val FIELD_ACCOUNT_HOLDER_TYPE = "account_holder_type"
        internal const val FIELD_BANK_NAME = "bank_name"
        internal const val FIELD_COUNTRY = "country"
        internal const val FIELD_CURRENCY = "currency"
        internal const val FIELD_FINGERPRINT = "fingerprint"
        internal const val FIELD_LAST4 = "last4"
        internal const val FIELD_ROUTING_NUMBER = "routing_number"
        internal const val FIELD_STATUS = "status"
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object BankAccountSerializer : KSerializer<BankAccount> {
    override val descriptor = JsonObject.serializer().descriptor

    override fun deserialize(decoder: Decoder): BankAccount {
        check(decoder is JsonDecoder)
        val json = decoder.decodeJsonElement()
        return BankAccountJsonParser().parse(JSONObject(json.toString()))
    }

    override fun serialize(encoder: Encoder, value: BankAccount) {
        check(encoder is JsonEncoder)
        encoder.encodeJsonElement(
            buildJsonObject {
                put(BankAccountJsonParser.FIELD_OBJECT, JsonPrimitive("bank_account"))
                put(BankAccountJsonParser.FIELD_ID, JsonPrimitive(value.id))
                put(BankAccountJsonParser.FIELD_ACCOUNT_HOLDER_NAME, JsonPrimitive(value.accountHolderName))
                put(
                    BankAccountJsonParser.FIELD_ACCOUNT_HOLDER_TYPE,
                    JsonPrimitive(value.accountHolderType?.code)
                )
                put(BankAccountJsonParser.FIELD_BANK_NAME, JsonPrimitive(value.bankName))
                put(BankAccountJsonParser.FIELD_COUNTRY, JsonPrimitive(value.countryCode))
                put(BankAccountJsonParser.FIELD_CURRENCY, JsonPrimitive(value.currency))
                put(BankAccountJsonParser.FIELD_FINGERPRINT, JsonPrimitive(value.fingerprint))
                put(BankAccountJsonParser.FIELD_LAST4, JsonPrimitive(value.last4))
                put(BankAccountJsonParser.FIELD_ROUTING_NUMBER, JsonPrimitive(value.routingNumber))
                put(BankAccountJsonParser.FIELD_STATUS, JsonPrimitive(value.status?.code))
            }
        )
    }
}
