package com.stripe.android.identity.networking.models

import com.google.common.truth.Truth.assertThat
import com.stripe.android.identity.json
import com.stripe.android.identity.networking.VERIFICATION_PAGE_NOT_REQUIRE_LIVE_CAPTURE_JSON_STRING
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.Test

internal class VerificationPageNetworkingDataTest {
    @Test
    fun `existing page without networking data retains ordinary flow`() {
        val page = json.decodeFromString(
            VerificationPageSerializer,
            VERIFICATION_PAGE_NOT_REQUIRE_LIVE_CAPTURE_JSON_STRING
        )

        assertThat(page.networkingData).isNull()
        assertThat(page.networkedIdentityRoute).isEqualTo(NetworkedIdentityRoute.OrdinaryIdentity)
    }

    @Test
    fun `null networking data retains ordinary flow`() {
        assertThat(pageWithNetworkingData("null").networkedIdentityRoute)
            .isEqualTo(NetworkedIdentityRoute.OrdinaryIdentity)
    }

    @Test
    fun `absent features retains ordinary flow`() {
        val page = pageWithNetworkingData("{}")

        assertThat(page.networkingData?.features).isNull()
        assertThat(page.networkedIdentityRoute).isEqualTo(NetworkedIdentityRoute.OrdinaryIdentity)
    }

    @Test
    fun `absent flags decode as null and retain ordinary flow`() {
        val page = pageWithFeatures("{}")

        assertThat(page.networkingData?.features?.viCompatible).isNull()
        assertThat(page.networkingData?.features?.consumerSaveEnabled).isNull()
        assertThat(page.networkedIdentityRoute).isEqualTo(NetworkedIdentityRoute.OrdinaryIdentity)
    }

    @Test
    fun `missing compatibility prevents reuse and save`() {
        assertThat(pageWithFeatureOverride("vi_compatible", JsonNull).networkedIdentityRoute)
            .isEqualTo(NetworkedIdentityRoute.OrdinaryIdentity)
    }

    @Test
    fun `missing merchant eligibility prevents reuse and save`() {
        assertThat(pageWithFeatureOverride("vi_merchant_eligible", JsonNull).networkedIdentityRoute)
            .isEqualTo(NetworkedIdentityRoute.OrdinaryIdentity)
    }

    @Test
    fun `missing merchant enablement prevents reuse and save`() {
        assertThat(pageWithFeatureOverride("vi_merchant_enabled", JsonNull).networkedIdentityRoute)
            .isEqualTo(NetworkedIdentityRoute.OrdinaryIdentity)
    }

    @Test
    fun `ineligible merchant prevents reuse and save`() {
        assertThat(pageWithFeatureOverride("vi_merchant_eligible", false).networkedIdentityRoute)
            .isEqualTo(NetworkedIdentityRoute.OrdinaryIdentity)
    }

    @Test
    fun `disabled merchant prevents reuse and save`() {
        assertThat(pageWithFeatureOverride("vi_merchant_enabled", false).networkedIdentityRoute)
            .isEqualTo(NetworkedIdentityRoute.OrdinaryIdentity)
    }

    @Test
    fun `incompatible client prevents reuse and save`() {
        assertThat(pageWithFeatureOverride("vi_compatible", false).networkedIdentityRoute)
            .isEqualTo(NetworkedIdentityRoute.OrdinaryIdentity)
    }

    @Test
    fun `reuse takes precedence over save`() {
        assertThat(pageWithFeatures(ALL_ENABLED).networkedIdentityRoute).isEqualTo(NetworkedIdentityRoute.Reuse)
    }

    @Test
    fun `reuse does not require save to be enabled`() {
        assertThat(pageWithFeatureOverride("consumer_save_enabled", false).networkedIdentityRoute)
            .isEqualTo(NetworkedIdentityRoute.Reuse)
    }

    @Test
    fun `save is chosen when reuse is disabled`() {
        assertThat(pageWithFeatureOverride("consumer_reuse_enabled", false).networkedIdentityRoute)
            .isEqualTo(NetworkedIdentityRoute.Save)
    }

    @Test
    fun `save is chosen when reuse is not possible`() {
        assertThat(pageWithFeatureOverride("consumer_reuse_possible", false).networkedIdentityRoute)
            .isEqualTo(NetworkedIdentityRoute.Save)
    }

    @Test
    fun `enabled merchant without consumer capabilities retains ordinary flow`() {
        val page = pageWithFeatures(
            """{"vi_compatible":true,"vi_merchant_eligible":true,"vi_merchant_enabled":true}"""
        )

        assertThat(page.networkedIdentityRoute).isEqualTo(NetworkedIdentityRoute.OrdinaryIdentity)
    }

    @Test
    fun `updating requirements retains bootstrap networking data`() {
        val page = pageWithFeatures(ALL_ENABLED)
        val updated = page.copy(requirements = VerificationPageRequirements(missing = emptyList()))

        assertThat(page.networkedIdentityRoute).isEqualTo(NetworkedIdentityRoute.Reuse)
        assertThat(updated.requirements.missing).isEmpty()
        assertThat(updated.networkingData).isEqualTo(page.networkingData)
        assertThat(updated.networkedIdentityRoute).isEqualTo(NetworkedIdentityRoute.Reuse)
    }

    private fun pageWithFeatureOverride(name: String, value: Any): VerificationPage {
        val features = json.parseToJsonElement(ALL_ENABLED).jsonObject
        return pageWithFeatures(JsonObject(features + (name to json.parseToJsonElement(value.toString()))).toString())
    }

    private fun pageWithFeatures(features: String): VerificationPage =
        pageWithNetworkingData("""{"features":$features}""")

    private fun pageWithNetworkingData(networkingData: String): VerificationPage {
        val original = json.parseToJsonElement(VERIFICATION_PAGE_NOT_REQUIRE_LIVE_CAPTURE_JSON_STRING).jsonObject
        return json.decodeFromString(
            VerificationPageSerializer,
            JsonObject(original + ("networking_data" to json.parseToJsonElement(networkingData))).toString()
        )
    }

    private companion object {
        const val ALL_ENABLED = """{
            "vi_compatible":true,"vi_merchant_eligible":true,"vi_merchant_enabled":true,
            "consumer_save_enabled":true,"consumer_reuse_enabled":true,"consumer_reuse_possible":true
        }"""
    }
}
