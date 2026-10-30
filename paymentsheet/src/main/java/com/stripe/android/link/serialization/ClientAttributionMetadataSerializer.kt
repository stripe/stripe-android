package com.stripe.android.link.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal object ClientAttributionMetadataSerializer : KSerializer<Map<String, Any>> {
    override val descriptor = JsonObject.serializer().descriptor

    override fun serialize(encoder: Encoder, value: Map<String, Any>) {
        check(encoder is JsonEncoder)
        encoder.encodeJsonElement(
            JsonObject(
                value.mapValues { (_, value) ->
                    when (value) {
                        is List<*> -> JsonArray(value.map { JsonPrimitive(it.toString()) })
                        else -> JsonPrimitive(value.toString())
                    }
                }
            )
        )
    }

    override fun deserialize(decoder: Decoder): Map<String, Any> {
        check(decoder is JsonDecoder)
        return decoder.decodeJsonElement().jsonObject.mapValues { (_, value) ->
            when (value) {
                is JsonArray -> value.map { it.jsonPrimitive.content }
                else -> value.jsonPrimitive.content
            }
        }
    }
}
