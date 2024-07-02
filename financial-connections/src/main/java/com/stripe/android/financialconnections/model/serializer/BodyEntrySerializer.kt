package com.stripe.android.financialconnections.model.serializer

import FinancialConnectionsGenericInfoScreen.Body.BodyEntry
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal object BodyEntrySerializer : JsonContentPolymorphicSerializer<BodyEntry>(
    BodyEntry::class
) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<BodyEntry> {
        val json = element.jsonObject
        return when (json["type"]?.jsonPrimitive?.content) {
            "text" -> BodyEntry.BodyText.serializer()
            "image" -> BodyEntry.BodyImage.serializer()
            "bullets" -> BodyEntry.BodyBullets.serializer()
            else -> BodyEntry.Unknown.serializer()
        }
    }
}
