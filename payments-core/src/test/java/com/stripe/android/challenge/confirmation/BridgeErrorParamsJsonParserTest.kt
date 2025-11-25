package com.stripe.android.challenge.confirmation

import com.google.common.truth.Truth.assertThat
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class BridgeErrorParamsJsonParserTest {

    private val parser = BridgeErrorParamsJsonParser()

    @Test
    fun `parse returns BridgeErrorParams when all fields are present`() {
        val json = JSONObject(
            """
            {
                "message": "Payment failed",
                "type": "card_error",
                "code": "card_declined"
            }
            """.trimIndent()
        )

        val result = parser.parse(json)

        assertThat(result).isNotNull()
        assertThat(result?.message).isEqualTo("Payment failed")
        assertThat(result?.type).isEqualTo("card_error")
        assertThat(result?.code).isEqualTo("card_declined")
    }

    @Test
    fun `parse returns BridgeErrorParams when only message is present`() {
        val json = JSONObject(
            """
            {
                "message": "Payment failed"
            }
            """.trimIndent()
        )

        val result = parser.parse(json)

        assertThat(result).isNotNull()
        assertThat(result?.message).isEqualTo("Payment failed")
        assertThat(result?.type).isNull()
        assertThat(result?.code).isNull()
    }

    @Test
    fun `parse returns BridgeErrorParams when only type is present`() {
        val json = JSONObject(
            """
            {
                "type": "card_error"
            }
            """.trimIndent()
        )

        val result = parser.parse(json)

        assertThat(result).isNotNull()
        assertThat(result?.message).isNull()
        assertThat(result?.type).isEqualTo("card_error")
        assertThat(result?.code).isNull()
    }

    @Test
    fun `parse returns BridgeErrorParams when only code is present`() {
        val json = JSONObject(
            """
            {
                "code": "card_declined"
            }
            """.trimIndent()
        )

        val result = parser.parse(json)

        assertThat(result).isNotNull()
        assertThat(result?.message).isNull()
        assertThat(result?.type).isNull()
        assertThat(result?.code).isEqualTo("card_declined")
    }

    @Test
    fun `parse returns BridgeErrorParams with null fields when fields are empty strings`() {
        val json = JSONObject(
            """
            {
                "message": "",
                "type": "",
                "code": ""
            }
            """.trimIndent()
        )

        val result = parser.parse(json)

        assertThat(result).isNotNull()
        assertThat(result?.message).isNull()
        assertThat(result?.type).isNull()
        assertThat(result?.code).isNull()
    }

    @Test
    fun `parse returns BridgeErrorParams with null fields when JSON is empty`() {
        val json = JSONObject("{}")

        val result = parser.parse(json)

        assertThat(result).isNotNull()
        assertThat(result?.message).isNull()
        assertThat(result?.type).isNull()
        assertThat(result?.code).isNull()
    }
}
