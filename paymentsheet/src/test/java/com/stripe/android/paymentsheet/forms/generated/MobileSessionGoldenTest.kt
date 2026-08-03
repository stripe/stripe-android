package com.stripe.android.paymentsheet.forms.generated

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONObject
import org.junit.Test
import kotlin.test.assertFailsWith

class MobileSessionGoldenTest {
    @Test
    fun `generated request fixtures decode and re-encode`() {
        listOf(
            "minimum_request",
            "fully_populated_request",
            "omitted_optional_request_field",
            "explicit_null_request_field",
        ).forEach { fixture ->
            val decoded = json.decodeFromString<PaymentSheetConfigV1>(fixture(fixture))
            val reencoded = json.encodeToString(decoded)

            assertThat(json.decodeFromString<PaymentSheetConfigV1>(reencoded)).isEqualTo(decoded)
        }
    }

    @Test
    fun `generated response fixtures decode and re-encode`() {
        listOf(
            "minimum_response",
            "fully_populated_response",
            "unknown_optional_response_field",
            "unknown_extensible_value_response",
        ).forEach { fixture ->
            val decoded = json.decodeFromString<MobilePaymentElementV1>(fixture(fixture))
            val reencoded = json.encodeToString(decoded)

            assertThat(json.decodeFromString<MobilePaymentElementV1>(reencoded)).isEqualTo(decoded)
        }
    }

    @Test
    fun `invalid required response fixture fails explicitly`() {
        assertFailsWith<SerializationException> {
            json.decodeFromString<MobilePaymentElementV1>(fixture("invalid_required_response_field"))
        }
    }

    @Test
    fun `generated response decodes server required form screen`() {
        val decoded = json.decodeFromString<MobilePaymentElementV1>(fixture("fully_populated_response"))

        assertThat(decoded.formSpecs.first().requiresFormScreen).isTrue()
    }

    @Test
    fun `generated manifest matches compiled contract`() {
        val manifest = JSONObject(resource("/mobile_session/v1/MobileSession.manifest.json"))

        assertThat(manifest.getInt("contract_major")).isEqualTo(MobileSessionContractV1.CONTRACT_MAJOR)
        assertThat(manifest.getString("contract_revision"))
            .isEqualTo(MobileSessionContractV1.CONTRACT_REVISION)
        assertThat(manifest.getString("contract_digest")).isEqualTo(MobileSessionContractV1.CONTRACT_DIGEST)
        assertThat(manifest.getString("generator_digest")).isEqualTo(MobileSessionContractV1.GENERATOR_DIGEST)
        assertThat(manifest.getString("mint_commit")).isEqualTo(MobileSessionContractV1.MINT_COMMIT)
    }

    @Test
    fun `v2 fixtures decode independently from v1`() {
        listOf(
            "minimum_request",
            "fully_populated_request",
            "omitted_optional_request_field",
            "explicit_null_request_field",
        ).forEach { fixture ->
            val decoded = json.decodeFromString<PaymentSheetConfigV2>(fixture(fixture, major = 2))
            val reencoded = json.encodeToString(decoded)

            assertThat(json.decodeFromString<PaymentSheetConfigV2>(reencoded)).isEqualTo(decoded)
        }

        listOf(
            "minimum_response",
            "fully_populated_response",
            "unknown_optional_response_field",
            "unknown_extensible_value_response",
        ).forEach { fixture ->
            val decoded = json.decodeFromString<MobilePaymentElementV2>(fixture(fixture, major = 2))
            val reencoded = json.encodeToString(decoded)

            assertThat(json.decodeFromString<MobilePaymentElementV2>(reencoded)).isEqualTo(decoded)
        }

        assertFailsWith<SerializationException> {
            json.decodeFromString<MobilePaymentElementV2>(
                fixture("invalid_required_response_field", major = 2)
            )
        }

        val v2 = json.decodeFromString<MobilePaymentElementV2>(fixture("fully_populated_response", major = 2))
        val v2Manifest = JSONObject(resource("/mobile_session/v2/MobileSession.manifest.json"))

        assertThat(v2.contract.major).isEqualTo(2)
        assertThat(v2.paymentMethodAvailability.entries).isNotEmpty()
        assertThat(v2Manifest.getInt("contract_major")).isEqualTo(MobileSessionContractV2.CONTRACT_MAJOR)
        assertThat(v2Manifest.getString("contract_revision"))
            .isEqualTo(MobileSessionContractV2.CONTRACT_REVISION)
        assertThat(v2Manifest.getString("contract_digest")).isEqualTo(MobileSessionContractV2.CONTRACT_DIGEST)
        assertThat(v2Manifest.getString("generator_digest")).isEqualTo(MobileSessionContractV2.GENERATOR_DIGEST)
        assertThat(v2Manifest.getString("mint_commit")).isEqualTo(MobileSessionContractV2.MINT_COMMIT)
    }

    private fun fixture(name: String, major: Int = 1): String {
        return resource("/mobile_session/v$major/$name.json")
    }

    private fun resource(path: String): String {
        return checkNotNull(javaClass.getResource(path)).readText()
    }

    private companion object {
        val json = Json {
            encodeDefaults = true
            ignoreUnknownKeys = true
        }
    }
}
