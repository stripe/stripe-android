package com.stripe.android.connect.webview.serialization

import com.stripe.android.connect.BuildConfig
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.serializer

@Serializable(with = SetterFunctionCalledMessageSerializer::class)
internal data class SetterFunctionCalledMessage(
    val setter: String,
    val value: Value
) {
    constructor(value: Value) : this(
        setter = value.expectedSetterName,
        value = value
    )

    init {
        require(!BuildConfig.DEBUG || value is UnknownValue || setter == value.expectedSetterName) {
            "Setter does not match value type: setter=$setter, value=$value"
        }
    }

    sealed interface Value

    @Serializable
    data class UnknownValue(
        val value: JsonElement
    ) : Value

    companion object {
        val Value.expectedSetterName: String
            get() {
                require(this !is UnknownValue)
                return javaClass.simpleName.replaceFirstChar { it.lowercaseChar() }
            }
    }
}

// Values

/**
 * Emitted when Connect JS has initialized and the component renders a loading state.
 */
@Serializable
internal data class SetOnLoaderStart(
    val elementTagName: String
) : SetterFunctionCalledMessage.Value

/**
 * The component executes this callback function when a load failure occurs.
 */
@Serializable
internal data class SetOnLoadError(
    val type: String, // TODO - possibly use an enum or sealed class here.
    val message: String?,
) : SetterFunctionCalledMessage.Value

/**
 * The connected account has exited the onboarding process.
 */
@Serializable
internal data object SetOnExit : SetterFunctionCalledMessage.Value

// Serialization

internal object SetterFunctionCalledMessageSerializer : KSerializer<SetterFunctionCalledMessage> {
    override val descriptor: SerialDescriptor = JsonObject.serializer().descriptor

    override fun deserialize(decoder: Decoder): SetterFunctionCalledMessage {
        check(decoder is JsonDecoder)
        val jsonObject = decoder.decodeJsonElement().jsonObject
        val setter = jsonObject.getValue("setter").jsonPrimitive.content
        val valueJson = jsonObject.getValue("value")
        val valueSerializer = decoder.json.valueSerializerForSetter(setter)
        val value = valueSerializer
            ?.let { decoder.json.decodeFromJsonElement(it, valueJson) }
            ?: SetterFunctionCalledMessage.UnknownValue(
                value = valueJson
            )
        return SetterFunctionCalledMessage(setter = setter, value = value)
    }

    override fun serialize(encoder: Encoder, value: SetterFunctionCalledMessage) {
        check(encoder is JsonEncoder)
        val resultValue =
            if (value.value is SetterFunctionCalledMessage.UnknownValue) {
                value.value.value // lol
            } else {
                val valueSerializer = encoder.json.valueSerializerForSetter(value.setter)!!
                encoder.json.encodeToJsonElement(valueSerializer, value.value)
            }
        encoder.encodeJsonElement(
            buildJsonObject {
                put("setter", JsonPrimitive(value.setter))
                put("value", resultValue)
            }
        )
    }

    private fun Json.valueSerializerForSetter(setter: String): KSerializer<SetterFunctionCalledMessage.Value>? {
        val className = buildString {
            append(SetterFunctionCalledMessage::class.java.`package`!!.name)
            append(".")
            append(setter.replaceFirstChar { it.uppercaseChar() })
        }
        return runCatching {
            @Suppress("UNCHECKED_CAST")
            serializersModule.serializer(Class.forName(className)) as KSerializer<SetterFunctionCalledMessage.Value>
        }.getOrNull()
    }
}
