package com.stripe.android.shoppay.bridge

import com.google.common.truth.Truth.assertThat
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ShippingCalculationRequestJsonParserTest {

    private val parser = ShippingCalculationRequestJsonParser()

    @Test
    fun `parse returns ShippingCalculationRequest when JSON is valid with all fields`() {
        val json = JSONObject(
            """
            {
                "shippingAddress": {
                    "name": "John Doe",
                    "address": {
                        "city": "San Francisco",
                        "state": "CA",
                        "postal_code": "94105",
                        "country": "US",
                        "phone": "+1-555-123-4567",
                        "organization": "Acme Corp"
                    }
                }
            }
            """.trimIndent()
        )

        val result = parser.parse(json)

        assertThat(result).isNotNull()

        assertThat(result?.shippingAddress?.name).isEqualTo("John Doe")

        val address = result?.shippingAddress?.address
        assertThat(address?.city).isEqualTo("San Francisco")
        assertThat(address?.state).isEqualTo("CA")
        assertThat(address?.postalCode).isEqualTo("94105")
        assertThat(address?.country).isEqualTo("US")
    }

    @Test
    fun `parse returns ShippingCalculationRequest when JSON is valid with minimal fields`() {
        val json = JSONObject(
            """
            {
                "shippingAddress": {
                    "address": {
                        "city": "New York",
                        "country": "US"
                    }
                }
            }
            """.trimIndent()
        )

        val result = parser.parse(json)

        assertThat(result).isNotNull()
        assertThat(result?.shippingAddress?.name).isNull()

        val address = result?.shippingAddress?.address
        assertThat(address?.city).isEqualTo("New York")
        assertThat(address?.country).isEqualTo("US")
        assertThat(address?.state).isNull()
        assertThat(address?.postalCode).isNull()
    }

    @Test
    fun `parse returns ShippingCalculationRequest when name is null but address is present`() {
        val json = JSONObject(
            """
            {
                "shippingAddress": {
                    "name": null,
                    "address": {
                        "city": "Los Angeles",
                        "country": "US"
                    }
                }
            }
            """.trimIndent()
        )

        val result = parser.parse(json)

        assertThat(result).isNotNull()
        assertThat(result?.shippingAddress?.name).isNull()

        val address = result?.shippingAddress?.address
        assertThat(address?.city).isEqualTo("Los Angeles")
        assertThat(address?.country).isEqualTo("US")
    }

    @Test
    fun `parse returns null when address is missing`() {
        val json = JSONObject(
            """
            {
                "shippingAddress": {
                    "name": "John Doe",
                    "otherField": "value"
                }
            }
            """.trimIndent()
        )

        val result = parser.parse(json)

        assertThat(result).isNull()
    }

    @Test
    fun `parse returns null when address is not an object`() {
        val json = JSONObject(
            """
            {
                "shippingAddress": {
                    "name": "John Doe",
                    "address": "notAnObject"
                }
            }
            """.trimIndent()
        )

        val result = parser.parse(json)

        assertThat(result).isNull()
    }

    @Test
    fun `parse returns null when address is null`() {
        val json = JSONObject(
            """
            {
                "shippingAddress": {
                    "name": "John Doe",
                    "address": null
                }
            }
            """.trimIndent()
        )

        val result = parser.parse(json)

        assertThat(result).isNull()
    }

    @Test
    fun `parse handles addressLine as single string`() {
        val json = JSONObject(
            """
            {
                "shippingAddress": {
                    "address": {
                        "city": "Boston",
                        "country": "US"
                    }
                }
            }
            """.trimIndent()
        )

        val result = parser.parse(json)

        assertThat(result).isNotNull()

        val address = result?.shippingAddress?.address
        assertThat(address?.city).isEqualTo("Boston")
        assertThat(address?.country).isEqualTo("US")
    }
}
