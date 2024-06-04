package com.stripe.android.financialconnections.model.serializer

import com.stripe.android.financialconnections.model.Entry
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal object EntrySerializer :
    JsonContentPolymorphicSerializer<Entry>(Entry::class) {

    override fun selectDeserializer(element: JsonElement): KSerializer<out Entry> {
        return when (element.typeValue) {
            Entry.TYPE_TEXT -> Entry.Text.serializer()
            Entry.TYPE_IMAGE -> Entry.Image.serializer()
            else -> throw IllegalArgumentException("Unknown type! ${element.typeValue}")
        }
    }

    /**
     * gets the `type` value from the given [JsonElement]
     */
    private val JsonElement.typeValue: String?
        get() = jsonObject["type"]?.jsonPrimitive?.content
}
