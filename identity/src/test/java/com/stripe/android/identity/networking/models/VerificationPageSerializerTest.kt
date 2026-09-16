package com.stripe.android.identity.networking.models

import com.google.common.truth.Truth.assertThat
import com.stripe.android.identity.json
import com.stripe.android.identity.networking.VERIFICATION_PAGE_NOT_REQUIRE_LIVE_CAPTURE_JSON_STRING
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.Test
import kotlin.test.assertFailsWith

internal class VerificationPageSerializerTest {
    @Test
    fun `malformed NI array does not expose contact information in diagnostics`() = assertRedactedFailure(
        pageWithNetworkedIdentity("""[{"email":"$EMAIL","phone_number":"$PHONE"}]""")
    )

    @Test
    fun `malformed contact object does not expose the bootstrap response`() = assertRedactedFailure(
        pageWithNetworkedIdentity("""{"email":{"private":"$EMAIL"},"phone_number":"$PHONE"}""")
    )

    @Test
    fun `malformed NI state does not expose contact information in diagnostics`() = assertRedactedFailure(
        pageWithNetworkedIdentity("""{"state":["$EMAIL","$PHONE"]}""")
    )

    @Test
    fun `missing required Identity fields still fail without exposing the response`() = assertRedactedFailure(
        JsonObject(
            json.parseToJsonElement(
                pageWithNetworkedIdentity("""{"email":"$EMAIL","phone_number":"$PHONE"}""")
            ).jsonObject - "id"
        ).toString()
    )

    private fun assertRedactedFailure(source: String) {
        val error = assertFailsWith<SerializationException> {
            json.decodeFromString(VerificationPageSerializer, source)
        }
        assertThat(error.message).isEqualTo("Invalid Identity verification page response.")
        assertThat(error.cause).isNull()
        assertThat(error.stackTraceToString()).doesNotContain(EMAIL)
        assertThat(error.stackTraceToString()).doesNotContain(PHONE)
    }

    private fun pageWithNetworkedIdentity(networkedIdentity: String): String {
        val original = json.parseToJsonElement(VERIFICATION_PAGE_NOT_REQUIRE_LIVE_CAPTURE_JSON_STRING).jsonObject
        return JsonObject(original + ("networked_identity" to json.parseToJsonElement(networkedIdentity))).toString()
    }

    private companion object {
        const val EMAIL = "private.consumer@example.com"
        const val PHONE = "+12025550100"
    }
}
