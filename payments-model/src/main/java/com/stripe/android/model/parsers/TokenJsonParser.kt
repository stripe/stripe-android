package com.stripe.android.model.parsers

import androidx.annotation.RestrictTo
import com.stripe.android.core.model.StripeJsonUtils
import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.model.Token
import com.stripe.android.model.Token.Type
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.json.JSONObject
import java.util.Date
import java.util.concurrent.TimeUnit

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class TokenJsonParser : ModelJsonParser<Token> {
    override fun parse(json: JSONObject): Token? {
        val tokenId = StripeJsonUtils.optString(json, FIELD_ID)
        val createdTimeStamp = StripeJsonUtils.optLong(json, FIELD_CREATED)
        val tokenType = Type.fromCode(StripeJsonUtils.optString(json, FIELD_TYPE))
        if (tokenType == null || tokenId == null || createdTimeStamp == null) {
            return null
        }

        val used = StripeJsonUtils.optBoolean(json, FIELD_USED)
        val liveMode = StripeJsonUtils.optBoolean(json, FIELD_LIVEMODE)
        val date = Date(TimeUnit.SECONDS.toMillis(createdTimeStamp))

        return when (tokenType) {
            Type.Card -> {
                json.optJSONObject(Type.Card.code)?.let {
                    Token(
                        id = tokenId,
                        livemode = liveMode,
                        created = date,
                        used = used,
                        type = Type.Card,
                        card = CardJsonParser().parse(it)
                    )
                }
            }
            Type.BankAccount -> {
                json.optJSONObject(Type.BankAccount.code)?.let {
                    Token(
                        id = tokenId,
                        livemode = liveMode,
                        created = date,
                        used = used,
                        type = Type.BankAccount,
                        bankAccount = BankAccountJsonParser().parse(it)
                    )
                }
            }
            else -> {
                Token(
                    id = tokenId,
                    type = tokenType,
                    livemode = liveMode,
                    created = date,
                    used = used
                )
            }
        }
    }

    internal companion object {
        internal const val FIELD_OBJECT = "object"
        internal const val FIELD_CREATED = "created"
        internal const val FIELD_ID = "id"
        internal const val FIELD_LIVEMODE = "livemode"

        internal const val FIELD_TYPE = "type"
        internal const val FIELD_USED = "used"

        internal const val FIELD_BANK_ACCOUNT = "bank_account"
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object TokenSerializer : KSerializer<Token?> {
    override val descriptor = JsonObject.serializer().descriptor

    override fun deserialize(decoder: Decoder): Token? {
        check(decoder is JsonDecoder)
        val json = decoder.decodeJsonElement()
        return TokenJsonParser().parse(JSONObject(json.toString()))
    }

    override fun serialize(encoder: Encoder, value: Token?) {
        check(encoder is JsonEncoder)
        if (value == null) {
            @OptIn(ExperimentalSerializationApi::class)
            encoder.encodeNull()
            return
        }
        encoder.encodeJsonElement(
            buildJsonObject {
                put(TokenJsonParser.FIELD_OBJECT, JsonPrimitive("token"))
                put(TokenJsonParser.FIELD_CREATED, JsonPrimitive(TimeUnit.MILLISECONDS.toSeconds(value.created.time)))
                put(TokenJsonParser.FIELD_ID, JsonPrimitive(value.id))
                put(TokenJsonParser.FIELD_LIVEMODE, JsonPrimitive(value.livemode))
                put(TokenJsonParser.FIELD_TYPE, JsonPrimitive(value.type.code))
                put(TokenJsonParser.FIELD_USED, JsonPrimitive(value.used))
                if (value.bankAccount != null) {
                    put(
                        TokenJsonParser.FIELD_BANK_ACCOUNT,
                        encoder.json.encodeToJsonElement(BankAccountSerializer, value.bankAccount),
                    )
                }
                // Card serialization is not (yet) supported.
            }
        )
    }
}
