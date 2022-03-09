package com.stripe.android.stripecardscan.framework.util

import android.util.Base64
import com.stripe.android.core.networking.QueryStringFactory
import com.stripe.android.core.networking.toMap
import com.stripe.android.stripecardscan.framework.NetworkConfig
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import java.nio.charset.Charset

internal fun b64Encode(s: String): String =
    Base64.encodeToString(s.toByteArray(Charset.defaultCharset()), Base64.NO_WRAP)

internal fun b64Encode(b: ByteArray): String =
    Base64.encodeToString(b, Base64.NO_WRAP)

/**
 * Encode a serializable object to a x-www-url-encoded string. The source object must convert to a
 * [Map] so that the parameters can be named.
 */
internal fun <T> encodeToXWWWFormUrl(serializer: SerializationStrategy<T>, value: T): String =
    QueryStringFactory.create(NetworkConfig.json.encodeToJsonElement(serializer, value).toMap())

/**
 * Encode a serializable object to a JSON string
 */
internal fun <T> encodeToJson(serializer: SerializationStrategy<T>, value: T): String =
    NetworkConfig.json.encodeToString(serializer, value)

/**
 * Decode an object from a JSON string
 */
internal fun <T> decodeFromJson(deserializer: DeserializationStrategy<T>, value: String): T =
    NetworkConfig.json.decodeFromString(deserializer, value)
