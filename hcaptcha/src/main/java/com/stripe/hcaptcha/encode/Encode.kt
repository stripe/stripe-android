@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package com.stripe.hcaptcha.encode

import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlin.time.Duration

@VisibleForTesting
internal val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = true
//    serializersModule = module
}

/**
 * Encode a serializable object to a JSON string
 */
fun <T> encodeToJson(serializer: SerializationStrategy<T>, value: T): String =
    json.encodeToString(serializer, value)
