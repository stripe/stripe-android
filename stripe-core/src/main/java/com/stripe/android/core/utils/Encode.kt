package com.stripe.android.core.utils

import android.util.Base64
import androidx.annotation.RestrictTo
import com.stripe.android.core.networking.QueryStringFactory
import com.stripe.android.core.networking.toMap
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json
import java.net.URLEncoder
import java.nio.charset.Charset

private val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = true
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun b64Encode(s: String): String =
    Base64.encodeToString(s.toByteArray(Charset.defaultCharset()), Base64.NO_WRAP)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun b64Encode(b: ByteArray): String =
    Base64.encodeToString(b, Base64.NO_WRAP)

/**
 * Encode a serializable object to a x-www-url-encoded string. The source object must convert to a
 * [Map] so that the parameters can be named.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun <T> encodeToXWWWFormUrl(serializer: SerializationStrategy<T>, value: T): String =
    QueryStringFactory.createFromParamsWithEmptyValues(
        json.encodeToJsonElement(serializer, value).toMap()
    )

/**
 * Encode a serializable object to a JSON string
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun <T> encodeToJson(serializer: SerializationStrategy<T>, value: T): String =
    json.encodeToString(serializer, value)

/**
 * Decode an object from a JSON string
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun <T> decodeFromJson(deserializer: DeserializationStrategy<T>, value: String): T =
    json.decodeFromString(deserializer, value)

/**
 * URL-encode a string. This is useful for sanitizing untrusted data for use in URLs.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun urlEncode(value: String): String = URLEncoder.encode(value, Charsets.UTF_8.name())
