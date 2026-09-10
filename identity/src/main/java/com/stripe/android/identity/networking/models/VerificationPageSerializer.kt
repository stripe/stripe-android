package com.stripe.android.identity.networking.models

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonTransformingSerializer
import kotlinx.serialization.json.jsonObject

/**
 * Supplies absent optional NI fields without changing Identity's null encoding (used to clear data)
 * or making existing required response fields optional. Internal model construction stays explicit.
 */
internal object VerificationPageSerializer :
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
        return JsonObject(page + ("networking_data" to normalizedNetworkingData))
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
