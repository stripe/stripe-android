package com.stripe.android.identity.networking.models

import com.google.common.truth.Truth.assertThat
import com.stripe.android.identity.json
import com.stripe.android.identity.networking.VERIFICATION_PAGE_NOT_REQUIRE_LIVE_CAPTURE_JSON_STRING
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
        assertThat(page.networkedIdentity).isNull()
        assertThat(page.merchantPublishableKey).isNull()
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
    fun `absent legacy flags decode as null`() {
        val page = pageWithNetworkingData("""{"features":{}}""")

        assertThat(page.networkingData?.features?.viCompatible).isNull()
        assertThat(page.networkingData?.features?.consumerSaveEnabled).isNull()
        assertThat(page.networkedIdentityRoute).isEqualTo(NetworkedIdentityRoute.OrdinaryIdentity)
    }

    @Test
    fun `legacy eligibility flags remain decodable but cannot enable v8 routing`() {
        val page = pageWithNetworkingData(
            """{"features":{
                "vi_compatible":true,"vi_merchant_eligible":true,"vi_merchant_enabled":true,
                "consumer_save_enabled":true,"consumer_reuse_enabled":true,"consumer_reuse_possible":true
            }}"""
        )
        val updated = page.copy(requirements = VerificationPageRequirements(missing = emptyList()))

        assertThat(page.networkingData?.features?.consumerReuseEnabled).isTrue()
        assertThat(page.networkedIdentityRoute).isEqualTo(NetworkedIdentityRoute.OrdinaryIdentity)
        assertThat(updated.networkingData).isEqualTo(page.networkingData)
        assertThat(updated.networkedIdentityRoute).isEqualTo(NetworkedIdentityRoute.OrdinaryIdentity)
    }

    private fun pageWithNetworkingData(networkingData: String): VerificationPage {
        val original = json.parseToJsonElement(VERIFICATION_PAGE_NOT_REQUIRE_LIVE_CAPTURE_JSON_STRING).jsonObject
        return json.decodeFromString(
            VerificationPageSerializer,
            JsonObject(original + ("networking_data" to json.parseToJsonElement(networkingData))).toString()
        )
    }
}
