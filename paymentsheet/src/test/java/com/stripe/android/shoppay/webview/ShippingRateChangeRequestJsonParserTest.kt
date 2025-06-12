package com.stripe.android.shoppay.webview

import com.google.common.truth.Truth.assertThat
import org.json.JSONObject
import org.junit.Test

class ShippingRateChangeRequestJsonParserTest {

    private val parser = ShippingRateChangeRequestJsonParser()

    @Test
    fun `parse should correctly parse valid shipping rate change request`() {
        val jsonString = """
            {
                "shippingRate": {
                    "id": "test-rate",
                    "displayName": "Test Rate",
                    "amount": 799,
                    "deliveryEstimate": "1-2 days"
                },
                "currentAmount": 1045,
                "timestamp": 1749687762035,
                "requestId": "req_1749687762035_qvqkedo"
            }
        """.trimIndent()

        val json = JSONObject(jsonString)
        val result = parser.parse(json)

        assertThat(result).isNotNull()
        result!!

        // Test top-level fields
        assertThat(result.requestId).isEqualTo("req_1749687762035_qvqkedo")
        assertThat(result.timestamp).isEqualTo(1749687762035L)
        assertThat(result.currentAmount).isEqualTo(1045L)

        // Test shipping rate fields
        val shippingRate = result.shippingRate
        assertThat(shippingRate.id).isEqualTo("test-rate")
        assertThat(shippingRate.displayName).isEqualTo("Test Rate")
        assertThat(shippingRate.amount).isEqualTo(799L)
        assertThat(shippingRate.deliveryEstimate).isEqualTo("1-2 days")
    }

    @Test
    fun `parse should handle missing optional deliveryEstimate field`() {
        val jsonString = """
            {
                "shippingRate": {
                    "id": "fast-rate",
                    "displayName": "Fast Shipping",
                    "amount": 1299
                },
                "currentAmount": 2000,
                "timestamp": 1749687762036,
                "requestId": "req_1749687762036_xyz"
            }
        """.trimIndent()

        val json = JSONObject(jsonString)
        val result = parser.parse(json)

        assertThat(result).isNotNull()
        result!!

        assertThat(result.requestId).isEqualTo("req_1749687762036_xyz")
        assertThat(result.timestamp).isEqualTo(1749687762036L)
        assertThat(result.currentAmount).isEqualTo(2000L)

        val shippingRate = result.shippingRate
        assertThat(shippingRate.id).isEqualTo("fast-rate")
        assertThat(shippingRate.displayName).isEqualTo("Fast Shipping")
        assertThat(shippingRate.amount).isEqualTo(1299L)
        assertThat(shippingRate.deliveryEstimate).isNull()
    }

    @Test
    fun `parse should return null when requestId is missing`() {
        val jsonString = """
            {
                "shippingRate": {
                    "id": "test-rate",
                    "displayName": "Test Rate",
                    "amount": 799
                },
                "currentAmount": 1045,
                "timestamp": 1749687762035
            }
        """.trimIndent()

        val json = JSONObject(jsonString)
        val result = parser.parse(json)

        assertThat(result).isNull()
    }

    @Test
    fun `parse should return null when shippingRate is missing`() {
        val jsonString = """
            {
                "currentAmount": 1045,
                "timestamp": 1749687762035,
                "requestId": "req_1749687762035_qvqkedo"
            }
        """.trimIndent()

        val json = JSONObject(jsonString)
        val result = parser.parse(json)

        assertThat(result).isNull()
    }

    @Test
    fun `parse should return null when shipping rate id is missing`() {
        val jsonString = """
            {
                "shippingRate": {
                    "displayName": "Test Rate",
                    "amount": 799,
                    "deliveryEstimate": "1-2 days"
                },
                "currentAmount": 1045,
                "timestamp": 1749687762035,
                "requestId": "req_1749687762035_qvqkedo"
            }
        """.trimIndent()

        val json = JSONObject(jsonString)
        val result = parser.parse(json)

        assertThat(result).isNull()
    }

    @Test
    fun `parse should return null when shipping rate displayName is missing`() {
        val jsonString = """
            {
                "shippingRate": {
                    "id": "test-rate",
                    "amount": 799,
                    "deliveryEstimate": "1-2 days"
                },
                "currentAmount": 1045,
                "timestamp": 1749687762035,
                "requestId": "req_1749687762035_qvqkedo"
            }
        """.trimIndent()

        val json = JSONObject(jsonString)
        val result = parser.parse(json)

        assertThat(result).isNull()
    }

    @Test
    fun `parse should handle zero timestamp`() {
        val jsonString = """
            {
                "shippingRate": {
                    "id": "test-rate",
                    "displayName": "Test Rate",
                    "amount": 799
                },
                "currentAmount": 1045,
                "requestId": "req_1749687762035_qvqkedo"
            }
        """.trimIndent()

        val json = JSONObject(jsonString)
        val result = parser.parse(json)

        assertThat(result).isNotNull()
        result!!
        assertThat(result.timestamp).isEqualTo(0L)
    }

    @Test
    fun `parse should handle zero currentAmount`() {
        val jsonString = """
            {
                "shippingRate": {
                    "id": "free-shipping",
                    "displayName": "Free Shipping",
                    "amount": 0
                },
                "timestamp": 1749687762035,
                "requestId": "req_1749687762035_free"
            }
        """.trimIndent()

        val json = JSONObject(jsonString)
        val result = parser.parse(json)

        assertThat(result).isNotNull()
        result!!
        assertThat(result.currentAmount).isEqualTo(0L)
        assertThat(result.shippingRate.amount).isEqualTo(0L)
    }
}
