package com.stripe.android.financialconnections.model.serializer

import com.stripe.android.financialconnections.domain.prepane.Body
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal object BodySerializer :
    JsonContentPolymorphicSerializer<Body>(Body::class) {

    override fun selectDeserializer(element: JsonElement): KSerializer<out Body> {
        return when (element.typeValue) {
            Body.TYPE_TEXT -> Body.Text.serializer()
            Body.TYPE_IMAGE -> Body.Image.serializer()
            else -> throw IllegalArgumentException("Unknown type!")
        }
    }

    /**
     * gets the `type` value from the given [JsonElement]
     */
    private val JsonElement.typeValue: String?
        get() = jsonObject["type"]?.jsonPrimitive?.content
}
