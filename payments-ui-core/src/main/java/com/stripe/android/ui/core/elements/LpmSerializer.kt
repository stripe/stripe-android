package com.stripe.android.ui.core.elements

import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

class LpmSerializer {
    private val format = Json {
        ignoreUnknownKeys = true
        classDiscriminator = "#class"
    }

    fun serialize(data: SharedDataSpec) =
        format.encodeToJsonElement(serializer(), data)

    fun deserialize(str: String) = runCatching {
        format.decodeFromString<SharedDataSpec>(serializer(), str)
    }.onFailure { }

    fun deserializeList(str: String) =
        format.decodeFromString<List<SharedDataSpec>>(serializer(), str)
}
