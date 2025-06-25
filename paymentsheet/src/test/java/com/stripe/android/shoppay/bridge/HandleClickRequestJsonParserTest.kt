package com.stripe.android.shoppay.bridge

import com.google.common.truth.Truth.assertThat
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class HandleClickRequestJsonParserTest {

    private val parser = HandleClickRequestJsonParser()

    @Test
    fun `parse returns HandleClickRequest when JSON is valid`() {
        val json = JSONObject(
            """
            {
                "eventData": {
                    "expressPaymentType": "shopPay"
                }
            }
            """.trimIndent()
        )

        val result = parser.parse(json)

        assertThat(result).isNotNull()
        assertThat(result!!.eventData.expressPaymentType).isEqualTo("shopPay")
    }

    @Test
    fun `parse returns null when eventData is missing`() {
        val json = JSONObject(
            """
            {
                "otherField": "value"
            }
            """.trimIndent()
        )

        val result = parser.parse(json)

        assertThat(result).isNull()
    }

    @Test
    fun `parse returns null when eventData is not an object`() {
        val json = JSONObject(
            """
            {
                "eventData": "notAnObject"
            }
            """.trimIndent()
        )

        val result = parser.parse(json)

        assertThat(result).isNull()
    }

    @Test
    fun `parse returns null when expressPaymentType is missing`() {
        val json = JSONObject(
            """
            {
                "eventData": {
                    "otherField": "value"
                }
            }
            """.trimIndent()
        )

        val result = parser.parse(json)

        assertThat(result).isNull()
    }

    @Test
    fun `parse returns null when expressPaymentType is null`() {
        val json = JSONObject(
            """
            {
                "eventData": {
                    "expressPaymentType": null
                }
            }
            """.trimIndent()
        )

        val result = parser.parse(json)

        assertThat(result).isNull()
    }

    @Test
    fun `parse returns null when expressPaymentType is not a string`() {
        val json = JSONObject(
            """
            {
                "eventData": {
                    "expressPaymentType": 123
                }
            }
            """.trimIndent()
        )

        val result = parser.parse(json)

        assertThat(result).isNull()
    }

    @Test
    fun `parse handles empty expressPaymentType string`() {
        val json = JSONObject(
            """
            {
                "eventData": {
                    "expressPaymentType": ""
                }
            }
            """.trimIndent()
        )

        val result = parser.parse(json)

        assertThat(result).isNotNull()
        assertThat(result!!.eventData.expressPaymentType).isEqualTo("")
    }
}
