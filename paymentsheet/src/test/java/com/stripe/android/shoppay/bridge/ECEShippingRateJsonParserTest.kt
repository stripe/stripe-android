package com.stripe.android.shoppay.bridge

import com.google.common.truth.Truth.assertThat
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ECEShippingRateJsonParserTest {

    private val parser = ECEShippingRateJsonParser()

    @Test
    fun `parse returns ECEShippingRate when JSON is valid with text deliveryEstimate`() {
        val json = JSONObject(
            """
            {
                "id": "rate_1",
                "displayName": "Standard Shipping",
                "amount": 500,
                "deliveryEstimate": "5-7 business days"
            }
            """.trimIndent()
        )

        val result = parser.parse(json)

        assertThat(result).isNotNull()
        assertThat(result?.id).isEqualTo("rate_1")
        assertThat(result?.displayName).isEqualTo("Standard Shipping")
        assertThat(result?.amount).isEqualTo(500)
        assertThat(result?.deliveryEstimate).isInstanceOf(ECEDeliveryEstimate.Text::class.java)
        val deliveryEstimate = result?.deliveryEstimate as ECEDeliveryEstimate.Text
        assertThat(deliveryEstimate.value).isEqualTo("5-7 business days")
    }

    @Test
    fun `parse returns ECEShippingRate when JSON is valid with range deliveryEstimate`() {
        val json = JSONObject(
            """
            {
                "id": "rate_2",
                "displayName": "Express Shipping",
                "amount": 1500,
                "deliveryEstimate": {
                    "minimum": {
                        "unit": "DAY",
                        "value": 1
                    },
                    "maximum": {
                        "unit": "DAY",
                        "value": 3
                    }
                }
            }
            """.trimIndent()
        )

        val result = parser.parse(json)

        assertThat(result).isNotNull()
        assertThat(result?.id).isEqualTo("rate_2")
        assertThat(result?.displayName).isEqualTo("Express Shipping")
        assertThat(result?.amount).isEqualTo(1500)
        assertThat(result?.deliveryEstimate).isInstanceOf(ECEDeliveryEstimate.Range::class.java)
        val deliveryEstimate = result?.deliveryEstimate as ECEDeliveryEstimate.Range
        assertThat(deliveryEstimate.value.minimum?.unit).isEqualTo(DeliveryTimeUnit.DAY)
        assertThat(deliveryEstimate.value.minimum?.value).isEqualTo(1)
        assertThat(deliveryEstimate.value.maximum?.unit).isEqualTo(DeliveryTimeUnit.DAY)
        assertThat(deliveryEstimate.value.maximum?.value).isEqualTo(3)
    }

    @Test
    fun `parse returns ECEShippingRate when JSON has minimal fields`() {
        val json = JSONObject(
            """
            {
                "id": "rate_3",
                "displayName": "Free Shipping"
            }
            """.trimIndent()
        )

        val result = parser.parse(json)

        assertThat(result).isNotNull()
        assertThat(result?.id).isEqualTo("rate_3")
        assertThat(result?.displayName).isEqualTo("Free Shipping")
        assertThat(result?.amount).isEqualTo(0)
        assertThat(result?.deliveryEstimate).isNull()
    }

    @Test
    fun `parse returns null when id is missing`() {
        val json = JSONObject(
            """
            {
                "displayName": "Standard Shipping",
                "amount": 500
            }
            """.trimIndent()
        )

        val result = parser.parse(json)

        assertThat(result).isNull()
    }

    @Test
    fun `parse returns null when displayName is missing`() {
        val json = JSONObject(
            """
            {
                "id": "rate_1",
                "amount": 500
            }
            """.trimIndent()
        )

        val result = parser.parse(json)

        assertThat(result).isNull()
    }

    @Test
    fun `parse returns null deliveryEstimate when range is malformed`() {
        val json = JSONObject(
            """
            {
                "id": "rate_4",
                "displayName": "Malformed Shipping",
                "amount": 1000,
                "deliveryEstimate": {
                    "maximum": {
                        "unit": "DAY",
                        "value": 3
                    }
                }
            }
            """.trimIndent()
        )

        val result = parser.parse(json)

        assertThat(result).isNotNull()
        assertThat(result?.deliveryEstimate).isNull()
    }

    @Test
    fun `parse returns null deliveryEstimate when delivery unit is malformed`() {
        val json = JSONObject(
            """
            {
                "id": "rate_5",
                "displayName": "Another Malformed Shipping",
                "amount": 2000,
                "deliveryEstimate": {
                    "minimum": {
                        "unit": "DAY"
                    },
                    "maximum": {
                        "unit": "DAY",
                        "value": 3
                    }
                }
            }
            """.trimIndent()
        )

        val result = parser.parse(json)

        assertThat(result).isNotNull()
        assertThat(result?.deliveryEstimate).isNull()
    }
}
