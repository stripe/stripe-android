package com.stripe.android.stripecardscan.framework.util

import android.util.Base64
import com.stripe.android.stripecardscan.framework.NetworkConfig
import com.stripe.android.core.networking.QueryStringFactory
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
import java.nio.charset.Charset

fun b64Encode(s: String): String =
    Base64.encodeToString(s.toByteArray(Charset.defaultCharset()), Base64.NO_WRAP)

fun b64Encode(b: ByteArray): String =
    Base64.encodeToString(b, Base64.NO_WRAP)

/**
 * Encode a serializable object to a x-www-url-encoded string
 */
@ExperimentalSerializationApi
fun <T> encodeToXWWWFormUrl(serializer: SerializationStrategy<T>, value: T): String =
    QueryStringFactory.create(NetworkConfig.form.encodeToMap(serializer, value))

/**
 * Encode a serializable object to a JSON string
 */
fun <T> encodeToJson(serializer: SerializationStrategy<T>, value: T): String =
    NetworkConfig.json.encodeToString(serializer, value)

/**
 * Decode an object from a JSON string
 */
fun <T> decodeFromJson(deserializer: DeserializationStrategy<T>, value: String): T =
    NetworkConfig.json.decodeFromString(deserializer, value)
