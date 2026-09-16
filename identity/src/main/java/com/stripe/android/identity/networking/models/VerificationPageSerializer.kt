package com.stripe.android.identity.networking.models

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonTransformingSerializer
import kotlinx.serialization.json.jsonObject

/**
 * Supplies absent optional NI fields without changing Identity's null encoding (used to clear data)
 * or making existing required response fields optional. Internal model construction stays explicit.
 */
internal object VerificationPageSerializer : KSerializer<VerificationPage> {
    override val descriptor = VerificationPageNormalizer.descriptor

    override fun deserialize(decoder: Decoder): VerificationPage = try {
        VerificationPageNormalizer.deserialize(decoder)
    } catch (_: IllegalArgumentException) {
        // The JSON decoder may embed the response in its exception, including NI contact details.
        // Keep malformed/required fields invalid without propagating that response into diagnostics.
        throw SerializationException("Invalid Identity verification page response.")
    }

    override fun serialize(encoder: Encoder, value: VerificationPage) =
        VerificationPageNormalizer.serialize(encoder, value)
}

private object VerificationPageNormalizer :
    JsonTransformingSerializer<VerificationPage>(VerificationPage.serializer()) {
    override fun transformDeserialize(element: JsonElement): JsonElement {
        val page = element.jsonObject
        val networkingData = page["networking_data"] as? JsonObject
        val features = networkingData?.get("features") as? JsonObject
        val normalizedFeatures = features?.let {
            JsonObject(
                optionalFeatureNames.associateWith { JsonNull } + it
            )
        } ?: JsonNull
        val normalizedNetworkingData = networkingData?.let {
            JsonObject(it + ("features" to normalizedFeatures))
        } ?: page["networking_data"] ?: JsonNull
        val providedDetails = page["provided_details"] as? JsonObject
        val normalizedProvidedDetails = providedDetails?.let {
            JsonObject(mapOf("email" to JsonNull) + it)
        } ?: page["provided_details"] ?: JsonNull
        return JsonObject(
            page + mapOf(
                "networking_data" to normalizedNetworkingData,
                "provided_details" to normalizedProvidedDetails,
                "merchant_publishable_key" to (page["merchant_publishable_key"] ?: JsonNull),
                "networked_identity" to normalizeNetworkedIdentity(page["networked_identity"])
            )
        )
    }

    private fun normalizeNetworkedIdentity(element: JsonElement?): JsonElement {
        val networking = element as? JsonObject ?: return element ?: JsonNull
        requireBooleanFields(networking, listOf("save_available", "reuse_available"))
        val state = networking["state"] as? JsonObject
        state?.let { requireBooleanFields(it, listOf("consented", "skipped")) }
        val normalizedState = state?.let {
            JsonObject(listOf("consented", "skipped", "direction").associateWith { JsonNull } + it)
        } ?: networking["state"] ?: JsonNull
        return JsonObject(
            listOf("save_available", "reuse_available", "email", "phone_number").associateWith { JsonNull } +
                networking + ("state" to normalizedState)
        )
    }

    private fun requireBooleanFields(value: JsonObject, fields: List<String>) {
        fields.forEach { field ->
            val element = value[field]
            if (element == null || element == JsonNull) return@forEach
            val primitive = element as? JsonPrimitive
                ?: throw SerializationException("Invalid Networked Identity configuration.")
            if (primitive.isString || primitive.content !in listOf("true", "false")) {
                throw SerializationException("Invalid Networked Identity configuration.")
            }
        }
    }

    private val optionalFeatureNames = listOf(
        "vi_compatible",
        "vi_merchant_eligible",
        "vi_merchant_enabled",
        "consumer_save_enabled",
        "consumer_reuse_enabled",
        "consumer_reuse_possible"
    )
}
