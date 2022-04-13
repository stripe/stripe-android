package com.stripe.android.connections.model.serializer

import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonTransformingSerializer

object JsonAsStringSerializer :
    JsonTransformingSerializer<String>(tSerializer = String.serializer()) {
    override fun transformDeserialize(element: JsonElement): JsonElement {
        return JsonPrimitive(value = element.toString())
    }
}
