package com.stripe.android.ui.core.elements

import android.util.Log
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

internal class LpmSerializer {
    private val format = Json {
        ignoreUnknownKeys = true
        classDiscriminator = "#class"
    }

    fun serialize(data: SharedDataSpec) =
        format.encodeToJsonElement(serializer(), data)

    fun deserialize(str: String) = runCatching {
        format.decodeFromString<SharedDataSpec>(serializer(), str)
    }.onFailure { }

    /**
     * Any error in parsing an LPM (say a missing required field) will result in none of the
     * LPMs being read.
     */
    fun deserializeList(str: String) = if (str.isEmpty()) {
        emptyList()
    } else {
        try {
            format.decodeFromString<List<SharedDataSpec>>(serializer(), str)
        }
        catch(e: Exception){
            Log.w("STRIPE", "Error parsing LPMs", e)
            emptyList()
        }
    }
}
