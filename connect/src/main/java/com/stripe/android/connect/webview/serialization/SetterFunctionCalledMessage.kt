package com.stripe.android.connect.webview.serialization

import com.stripe.android.connect.BuildConfig
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
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
 * Types of errors that can occur when loading a Connect embedded component.
 * Matches the error types defined in Connect.js.
 */
@Serializable(with = EmbeddedErrorTypeSerializer::class)
enum class EmbeddedErrorType(val value: String) {
    /** Failure to connect to Stripe's API */
    API_CONNECTION_ERROR("api_connection_error"),

    /** Failure to perform the authentication flow within Connect Embedded Components */
    AUTHENTICATION_ERROR("authentication_error"),

    /** Account session create failed */
    ACCOUNT_SESSION_CREATE_ERROR("account_session_create_error"),

    /** Request failed with a 4xx status code, typically caused by platform configuration issues */
    INVALID_REQUEST_ERROR("invalid_request_error"),

    /** Too many requests hit the API too quickly */
    RATE_LIMIT_ERROR("rate_limit_error"),

    /** Failure to render the component, typically caused by browser extensions or network issues */
    RENDER_ERROR("render_error"),

    /**
     * API errors covering any other type of problem (e.g., a temporary problem with Stripe's servers),
     * and are extremely uncommon. Also used as a fallback for unknown error types.
     */
    API_ERROR("api_error");

    companion object {
        /**
         * Creates an EmbeddedErrorType from a string value, falling back to API_ERROR for unknown types.
         *
         * @param value The raw string value from Connect.js
         * @return The corresponding EmbeddedErrorType, or API_ERROR if unknown
         */
        fun fromValue(value: String?): EmbeddedErrorType {
            if (value == null) return API_ERROR
            return entries.find { it.value == value } ?: API_ERROR
        }
    }
}

/**
 * Custom serializer for EmbeddedErrorType that handles unknown and null values by falling back to API_ERROR.
 */
internal object EmbeddedErrorTypeSerializer : KSerializer<EmbeddedErrorType> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("EmbeddedErrorType", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: EmbeddedErrorType) {
        encoder.encodeString(value.value)
    }

    override fun deserialize(decoder: Decoder): EmbeddedErrorType {
        return if (decoder is JsonDecoder) {
            val element = decoder.decodeJsonElement()
            val rawValue = (element as? JsonPrimitive)?.takeIf { !it.isString || it.content != "null" }?.content
            EmbeddedErrorType.fromValue(rawValue)
        } else {
            EmbeddedErrorType.fromValue(decoder.decodeString())
        }
    }
}

/**
 * The component executes this callback function when a load failure occurs.
 */
@Serializable
internal data class SetOnLoadError(
    val error: LoadError
) : SetterFunctionCalledMessage.Value {

    @Serializable
    data class LoadError(
        val type: EmbeddedErrorType,
        val message: String?,
    )
}

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
