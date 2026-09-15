package com.stripe.android.identity.networking.models

import com.google.common.truth.Truth.assertThat
import com.stripe.android.identity.json
import com.stripe.android.identity.networking.VERIFICATION_PAGE_NOT_REQUIRE_LIVE_CAPTURE_JSON_STRING
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.Test

internal class VerificationPageProvidedDetailsTest {
    @Test
    fun `older responses omit merchant details without changing ordinary Identity`() {
        val page = json.decodeFromString(
            VerificationPageSerializer,
            VERIFICATION_PAGE_NOT_REQUIRE_LIVE_CAPTURE_JSON_STRING
        )

        assertThat(page.providedDetails).isNull()
        assertThat(page.networkedIdentityRoute).isEqualTo(NetworkedIdentityRoute.OrdinaryIdentity)
    }

    @Test
    fun `null merchant details preserve eligibility`() {
        val page = pageWithProvidedDetails("null")

        assertThat(page.providedDetails).isNull()
        assertThat(page.networkedIdentityRoute).isEqualTo(NetworkedIdentityRoute.Reuse)
    }

    @Test
    fun `merchant details without email preserve eligibility`() {
        val page = pageWithProvidedDetails("{}")

        assertThat(page.providedDetails).isNotNull()
        assertThat(page.providedDetails?.email).isNull()
        assertThat(page.networkedIdentityRoute).isEqualTo(NetworkedIdentityRoute.Reuse)
    }

    @Test
    fun `null merchant email preserves eligibility`() {
        val page = pageWithProvidedDetails("""{"email":null}""")

        assertThat(page.providedDetails?.email).isNull()
        assertThat(page.networkedIdentityRoute).isEqualTo(NetworkedIdentityRoute.Reuse)
    }

    @Test
    fun `merchant email survives requirements updates alongside networking data`() {
        val page = pageWithProvidedDetails("""{"email":"merchant@example.com"}""")
        val updated = page.copy(requirements = VerificationPageRequirements(missing = emptyList()))

        assertThat(page.providedDetails?.email).isEqualTo("merchant@example.com")
        assertThat(updated.providedDetails).isEqualTo(page.providedDetails)
        assertThat(updated.networkingData).isEqualTo(page.networkingData)
        assertThat(updated.networkedIdentityRoute).isEqualTo(NetworkedIdentityRoute.Reuse)
        assertThat(updated.requirements.missing).isEmpty()
    }

    @Test
    fun `decoding preserves source email for explicit sanitization decisions in UI`() {
        val page = pageWithProvidedDetails("""{"email":" john doe@example.com "}""")

        assertThat(page.providedDetails?.email).isEqualTo(" john doe@example.com ")
    }

    @Test
    fun `merchant email is redacted from model diagnostics`() {
        val page = pageWithProvidedDetails("""{"email":"merchant@example.com"}""")

        assertThat(page.providedDetails?.email).isEqualTo("merchant@example.com")
        assertThat(page.providedDetails.toString()).doesNotContain("merchant@example.com")
        assertThat(page.toString()).doesNotContain("merchant@example.com")
    }

    private fun pageWithProvidedDetails(providedDetails: String): VerificationPage {
        val original = json.parseToJsonElement(VERIFICATION_PAGE_NOT_REQUIRE_LIVE_CAPTURE_JSON_STRING).jsonObject
        val networkingData = json.parseToJsonElement(
            """{"features":{
                "vi_compatible":true,"vi_merchant_eligible":true,"vi_merchant_enabled":true,
                "consumer_reuse_enabled":true,"consumer_reuse_possible":true
            }}"""
        )
        return json.decodeFromString(
            VerificationPageSerializer,
            JsonObject(
                original + mapOf(
                    "provided_details" to json.parseToJsonElement(providedDetails),
                    "networking_data" to networkingData
                )
            ).toString()
        )
    }
}
