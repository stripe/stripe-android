package com.stripe.hcaptcha.encode

import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json

@VisibleForTesting
internal val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = true
}

/**
 * Encode a serializable object to a JSON string
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun <T> encodeToJson(serializer: SerializationStrategy<T>, value: T): String =
    json.encodeToString(serializer, value)
