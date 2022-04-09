package com.stripe.android.connections.model.serializer

import com.stripe.android.model.Token
import com.stripe.android.model.parsers.TokenJsonParser
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.json.JSONObject

/**
 * [Token] serialization is manually handled on :payments-model.
 *
 * :connections uses [kotlinx.serialization], this [KSerializer] bridges the existing
 * manual serialization when [Token] exist as a field of a [Serializable] model.
 */
internal object TokenSerializer : KSerializer<Token> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("com.stripe.android.model.Token", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Token) {
        throw SerializationException("serializing Token is not yet supported.")
    }

    override fun deserialize(decoder: Decoder): Token {
        val json = JSONObject(decoder.decodeString())
        return requireNotNull(TokenJsonParser().parse(json))
    }
}
