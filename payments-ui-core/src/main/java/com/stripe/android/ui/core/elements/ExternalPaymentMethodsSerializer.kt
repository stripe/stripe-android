package com.stripe.android.ui.core.elements

import android.util.Log
import androidx.annotation.RestrictTo
import androidx.annotation.WorkerThread
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object ExternalPaymentMethodsSerializer {

    private val format = Json {
        ignoreUnknownKeys = true
        classDiscriminator = "#class"

        // needed so that null values in the spec are parsed correctly
        coerceInputValues = true
    }

    @WorkerThread
    fun deserializeList(str: String): Result<List<ExternalPaymentMethodSpec>> {
        return runCatching<ExternalPaymentMethodsSerializer, List<ExternalPaymentMethodSpec>> {
            format.decodeFromString(serializer(), str)
        }.onFailure { error ->
            Log.w("xkcd", "Error parsing EPMs", error)
        }.onSuccess {
            Log.i("xkcd", "decoded!")
        }
    }
}