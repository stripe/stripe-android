package com.stripe.android.stripecardscan.framework.util

import android.util.Base64
import com.stripe.android.stripecardscan.framework.NetworkConfig
import com.stripe.android.core.networking.QueryStringFactory
import com.stripe.android.stripecardscan.framework.exception.InvalidSerializationException
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
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
 * Convert a [JsonElement] to a [Map<String, *>] so it's compatible with [QueryStringFactory]. Note
 * that this only supports [JsonObject]s currently. Other types will result in an
 * [InvalidSerializationException].
 */
private fun JsonElement.toMap(): Map<String, *> = when (this) {
    is JsonObject -> toMap()
    else -> throw InvalidSerializationException(this::class.java.simpleName)
}

/**
 * Recursively convert a [JsonElement] to its equivalent primitive kotlin values.
 */
private fun JsonElement.toPrimitives(): Any? = when (this) {
    JsonNull -> null
    is JsonArray -> toPrimitives()
    is JsonObject -> toMap()
    is JsonPrimitive -> content.replace(Regex("^\"|\"$"), "") // remove "" around strings
}

/**
 * Convert all elements of an array to their equivalent kotlin primitives.
 */
private fun JsonArray.toPrimitives(): List<*> = map { it.toPrimitives() }

/**
 * Convert a [JsonObject] to a [Map<String, *>] so it's compatible with [QueryStringFactory].
 *
 * Adapted from https://stackoverflow.com/questions/44870961/how-to-map-a-json-string-to-kotlin-map
 */
private fun JsonObject.toMap(): Map<String, *> = map { it.key to it.value.toPrimitives() }.toMap()

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
