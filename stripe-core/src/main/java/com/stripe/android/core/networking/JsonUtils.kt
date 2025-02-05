package com.stripe.android.core.networking

import androidx.annotation.RestrictTo
import com.stripe.android.core.exception.InvalidSerializationException
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Convert a [JsonElement] to a [Map<String, *>] so it's compatible with [QueryStringFactory]. Note
 * that this only supports [JsonObject]s currently. Other types will result in an
 * [InvalidSerializationException].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun JsonElement.toMap(): Map<String, *> = when (this) {
    is JsonObject -> toMap()
    else -> throw InvalidSerializationException(this::class.java.simpleName)
}

/**
 * Recursively convert a [JsonElement] to its equivalent primitive kotlin values.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun JsonElement.toPrimitives(): Any? = when (this) {
    JsonNull -> null
    is JsonArray -> toPrimitives()
    is JsonObject -> toMap()
    is JsonPrimitive -> content.replace(Regex("^\"|\"$"), "") // remove "" around strings
}

/**
 * Convert all elements of an array to their equivalent kotlin primitives.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun JsonArray.toPrimitives(): List<*> = map { it.toPrimitives() }

/**
 * Convert a [JsonObject] to a [Map<String, *>] so it's compatible with [QueryStringFactory].
 *
 * Adapted from https://stackoverflow.com/questions/44870961/how-to-map-a-json-string-to-kotlin-map
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun JsonObject.toMap(): Map<String, *> = map { it.key to it.value.toPrimitives() }.toMap()

/**
 * Convert a string to snake_case.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun String.toSnakeCase(): String {
    if (isEmpty()) return this
    return buildString {
        var lastChar: Char? = null
        for (char in this@toSnakeCase) {
            when {
                char.isUpperCase() -> {
                    if (lastChar != null && lastChar != '_') {
                        append('_')
                    }
                    append(char.lowercase())
                }
                else -> {
                    append(char)
                }
            }
            lastChar = char
        }
    }
}
