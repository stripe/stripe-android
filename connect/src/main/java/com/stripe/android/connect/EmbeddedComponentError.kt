package com.stripe.android.connect

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive

/**
 * An error that can occur loading a Connect embedded component.
 */
class EmbeddedComponentError(
    /**
     * The type of error.
     */
    val type: ErrorType,
    /**
     * A description of the error.
     */
    override val message: String?
) : RuntimeException(message) {

    /**
     * Types of errors that can occur when loading a Connect embedded component.
     */
    @Serializable(with = ErrorTypeSerializer::class)
    enum class ErrorType(val value: String) {
        /**
         * Failure to connect to Stripe's API.
         */
        API_CONNECTION_ERROR("api_connection_error"),

        /**
         * Failure to perform the authentication flow within Connect Embedded Components.
         */
        AUTHENTICATION_ERROR("authentication_error"),

        /**
         * Account session create failed.
         */
        ACCOUNT_SESSION_CREATE_ERROR("account_session_create_error"),

        /**
         * Request failed with a 4xx status code, typically caused by platform configuration issues.
         */
        INVALID_REQUEST_ERROR("invalid_request_error"),

        /**
         * Too many requests hit the API too quickly.
         */
        RATE_LIMIT_ERROR("rate_limit_error"),

        /**
         * Failure to render the component, typically caused by browser extensions or network issues.
         */
        RENDER_ERROR("render_error"),

        /**
         * API errors covering any other type of problem (e.g., a temporary problem with Stripe's
         * servers), and are extremely uncommon. Also used as a fallback for unknown error types.
         */
        API_ERROR("api_error");

        internal companion object {
            fun fromValue(value: String?): ErrorType =
                entries.find { it.value == value } ?: API_ERROR
        }
    }

    override fun toString(): String = "${type.value}: $message"
}

/**
 * Custom serializer for ErrorType that handles unknown and null values by falling back to API_ERROR.
 */
internal object ErrorTypeSerializer : KSerializer<EmbeddedComponentError.ErrorType> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("EmbeddedComponentError.ErrorType", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: EmbeddedComponentError.ErrorType) {
        encoder.encodeString(value.value)
    }

    override fun deserialize(decoder: Decoder): EmbeddedComponentError.ErrorType {
        return if (decoder is JsonDecoder) {
            val element = decoder.decodeJsonElement()
            val rawValue = (element as? JsonPrimitive)?.takeIf { !it.isString || it.content != "null" }?.content
            EmbeddedComponentError.ErrorType.fromValue(rawValue)
        } else {
            EmbeddedComponentError.ErrorType.fromValue(decoder.decodeString())
        }
    }
}
