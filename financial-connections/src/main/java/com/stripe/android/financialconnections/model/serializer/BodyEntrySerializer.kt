package com.stripe.android.financialconnections.model.serializer

import FinancialConnectionsGenericInfoScreen
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal object BodyEntrySerializer : JsonContentPolymorphicSerializer<FinancialConnectionsGenericInfoScreen.Body.BodyEntry>(
    FinancialConnectionsGenericInfoScreen.Body.BodyEntry::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<FinancialConnectionsGenericInfoScreen.Body.BodyEntry> {
        val json = element.jsonObject
        return when (json["type"]?.jsonPrimitive?.content) {
            "text" -> FinancialConnectionsGenericInfoScreen.Body.BodyEntry.BodyText.serializer()
            "image" -> FinancialConnectionsGenericInfoScreen.Body.BodyEntry.BodyImage.serializer()
            "bullets" -> FinancialConnectionsGenericInfoScreen.Body.BodyEntry.BodyBullets.serializer()
            else -> throw SerializationException("Unknown type for BodyEntry")
        }
    }
}