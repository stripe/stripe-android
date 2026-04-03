package com.stripe.android.core.model.serializers

import androidx.annotation.RestrictTo
import com.stripe.android.core.StripeError
import com.stripe.android.core.model.parsers.StripeErrorJsonParser
import kotlinx.serialization.KSerializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.json.JSONObject

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object StripeErrorSerializer : KSerializer<StripeError> {

    override val descriptor = JsonObject.serializer().descriptor

    override fun deserialize(decoder: Decoder): StripeError {
        check(decoder is JsonDecoder)
        val parser = StripeErrorJsonParser()
        val json = decoder.decodeJsonElement()
        return parser.parse(JSONObject(json.toString()))
    }

    override fun serialize(encoder: Encoder, value: StripeError) {
        check(encoder is JsonEncoder)
        encoder.encodeJsonElement(
            buildJsonObject {
                value.code?.let { put(StripeErrorJsonParser.FIELD_CODE, it) }
                value.message?.let { put(StripeErrorJsonParser.FIELD_MESSAGE, it) }
                value.param?.let { put(StripeErrorJsonParser.FIELD_PARAM, it) }
                value.type?.let { put(StripeErrorJsonParser.FIELD_TYPE, it) }
                value.docUrl?.let { put(StripeErrorJsonParser.FIELD_DOC_URL, it) }
                value.charge?.let { put(StripeErrorJsonParser.FIELD_CHARGE, it) }
                value.declineCode?.let { put(StripeErrorJsonParser.FIELD_DECLINE_CODE, it) }
                value.extraFields?.let { extraFields ->
                    put(
                        StripeErrorJsonParser.FIELD_EXTRA_FIELDS,
                        JsonObject(
                            extraFields
                                .mapValues { JsonPrimitive(it.value) }
                                .toMap()
                        )
                    )
                }
            }
        )
    }
}
