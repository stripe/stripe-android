package com.stripe.android.challenge.confirmation

import com.google.common.truth.Truth.assertThat
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class BridgeSuccessParamsJsonParserTest {

    private val parser = BridgeSuccessParamsJsonParser()

    @Test
    fun `parse returns BridgeSuccessParams when client_secret is present`() {
        val json = JSONObject(
            """
            {
                "client_secret": "pi_123_secret_456"
            }
            """.trimIndent()
        )

        val result = parser.parse(json)

        assertThat(result).isNotNull()
        assertThat(result?.clientSecret).isEqualTo("pi_123_secret_456")
    }

    @Test
    fun `parse returns null when client_secret is missing`() {
        val json = JSONObject("{}")

        val result = parser.parse(json)

        assertThat(result).isNull()
    }

    @Test
    fun `parse returns null when client_secret is empty string`() {
        val json = JSONObject(
            """
            {
                "client_secret": ""
            }
            """.trimIndent()
        )

        val result = parser.parse(json)

        assertThat(result).isNull()
    }

    @Test
    fun `parse returns null when client_secret is blank string`() {
        val json = JSONObject(
            """
            {
                "client_secret": "   "
            }
            """.trimIndent()
        )

        val result = parser.parse(json)

        assertThat(result).isNull()
    }
}
