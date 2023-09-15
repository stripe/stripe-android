package com.stripe.android.ui.core.elements

import android.util.Log
import androidx.annotation.WorkerThread
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

internal object LpmSerializer {

    private val format = Json {
        ignoreUnknownKeys = true
        classDiscriminator = "#class"

        // needed so that null values in the spec are parsed correctly
        coerceInputValues = true
    }

    /**
     * Any error in parsing an LPM (say a missing required field) will result in none of the
     * LPMs being read.
     */
    @WorkerThread
    fun deserializeList(str: String): Result<List<SharedDataSpec>> {
        return runCatching<LpmSerializer, List<SharedDataSpec>> {
            format.decodeFromString(serializer(), str)
        }.onFailure { error ->
            Log.w("STRIPE", "Error parsing LPMs", error)
        }
    }
}
