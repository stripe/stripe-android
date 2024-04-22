package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import androidx.annotation.WorkerThread
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal object ExternalPaymentMethodsSerializer {

    private val format = Json {
        ignoreUnknownKeys = true
    }

    @WorkerThread
    fun deserializeList(str: String): Result<List<ExternalPaymentMethodSpec>> {
        return runCatching {
            format.decodeFromString(serializer(), str)
        }
    }
}
