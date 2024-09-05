package com.stripe.android.financialconnections.model.serializer

import FinancialConnectionsGenericInfoScreen.Body.Entry
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal object BodyEntrySerializer : JsonContentPolymorphicSerializer<Entry>(Entry::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<Entry> {
        val json = element.jsonObject
        return when (json["type"]?.jsonPrimitive?.content) {
            "text" -> Entry.Text.serializer()
            "image" -> Entry.Image.serializer()
            "bullets" -> Entry.Bullets.serializer()
            else -> Entry.Unknown.serializer()
        }
    }
}
