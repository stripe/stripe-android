package com.stripe.android.financialconnections.model.serializer

import com.stripe.android.financialconnections.domain.AuthSessionChallenge
import com.stripe.android.financialconnections.domain.AuthSessionChallengeType
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive


object AuthSessionChallengeSerializer : KSerializer<AuthSessionChallenge?> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("AuthSessionChallenge") {
        element<String>("id")
        element<AuthSessionChallengeType>("type")
    }

    override fun serialize(encoder: Encoder, value: AuthSessionChallenge?) {
        TODO("Not yet implemented")
    }

    override fun deserialize(decoder: Decoder): AuthSessionChallenge? {
        val jsonElement: JsonElement = decoder.decodeSerializableValue(JsonElement.serializer())
        val jsonObject = jsonElement.jsonObject


        val id = jsonObject["id"]?.jsonPrimitive?.content
        val type = jsonObject["type"]?.jsonPrimitive?.content

        if (id == null || type == null) return null

        val challenge = when (type) {
            "username_password" -> Json.decodeFromJsonElement(
                AuthSessionChallengeType.UsernamePassword.serializer(),
                jsonObject.getValue("username_password")
            )

            "tokenized_text" -> Json.decodeFromJsonElement(
                AuthSessionChallengeType.TokenizedText.serializer(),
                jsonObject.getValue("tokenized_text")
            )

            "text" -> Json.decodeFromJsonElement(
                AuthSessionChallengeType.Text.serializer(),
                jsonObject.getValue("text")
            )

            "options" -> Json.decodeFromJsonElement(
                AuthSessionChallengeType.Options.serializer(),
                jsonObject.getValue("options")
            )

            else -> error("No known type $type found")
        }

        return AuthSessionChallenge(
            id = id,
            type = type,
            challengeType = challenge
        )
    }
}