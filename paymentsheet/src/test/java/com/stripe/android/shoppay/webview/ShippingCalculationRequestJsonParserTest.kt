package com.stripe.android.shoppay.webview

import com.google.common.truth.Truth.assertThat
import org.json.JSONObject
import org.junit.Test

class ShippingCalculationRequestJsonParserTest {

    private val parser = ShippingCalculationRequestJsonParser()

    @Test
    fun `parse should correctly parse valid shipping calculation request`() {
        val jsonString = """
            {
                "requestId": "1749656439239_0.4450066427864713",
                "shippingAddress": {
                    "address1": "101 Polk St",
                    "address2": "Unit 1405",
                    "city": "San Francisco",
                    "companyName": "",
                    "countryCode": "US",
                    "email": "test@example.com",
                    "firstName": "Test",
                    "lastName": "User",
                    "phone": "+14155551234",
                    "postalCode": "94102-4763",
                    "provinceCode": "CA"
                },
                "timestamp": 1749656439240
            }
        """.trimIndent()

        val json = JSONObject(jsonString)
        val result = parser.parse(json)

        assertThat(result).isNotNull()
        result!!

        // Test top-level fields
        assertThat(result.requestId).isEqualTo("1749656439239_0.4450066427864713")
        assertThat(result.timestamp).isEqualTo(1749656439240L)

        // Test shipping address fields
        val address = result.shippingAddress
        assertThat(address.address1).isEqualTo("101 Polk St")
        assertThat(address.address2).isEqualTo("Unit 1405")
        assertThat(address.city).isEqualTo("San Francisco")
        assertThat(address.companyName).isNull()
        assertThat(address.countryCode).isEqualTo("US")
        assertThat(address.email).isEqualTo("test@example.com")
        assertThat(address.firstName).isEqualTo("Test")
        assertThat(address.lastName).isEqualTo("User")
        assertThat(address.phone).isEqualTo("+14155551234")
        assertThat(address.postalCode).isEqualTo("94102-4763")
        assertThat(address.provinceCode).isEqualTo("CA")
    }

    @Test
    fun `parse should handle missing optional fields`() {
        val jsonString = """
            {
                "requestId": "test_request_123",
                "shippingAddress": {
                    "address1": "123 Main St",
                    "city": "New York",
                    "countryCode": "US"
                },
                "timestamp": 1234567890
            }
        """.trimIndent()

        val json = JSONObject(jsonString)
        val result = parser.parse(json)

        assertThat(result).isNotNull()
        result!!

        assertThat(result.requestId).isEqualTo("test_request_123")
        assertThat(result.timestamp).isEqualTo(1234567890L)

        val address = result.shippingAddress
        assertThat(address.address1).isEqualTo("123 Main St")
        assertThat(address.city).isEqualTo("New York")
        assertThat(address.countryCode).isEqualTo("US")
        assertThat(address.address2).isNull()
        assertThat(address.companyName).isNull()
        assertThat(address.email).isNull()
        assertThat(address.firstName).isNull()
        assertThat(address.lastName).isNull()
        assertThat(address.phone).isNull()
        assertThat(address.postalCode).isNull()
        assertThat(address.provinceCode).isNull()
    }

    @Test
    fun `parse should return null when requestId is missing`() {
        val jsonString = """
            {
                "shippingAddress": {
                    "address1": "123 Main St",
                    "city": "New York",
                    "countryCode": "US"
                },
                "timestamp": 1234567890
            }
        """.trimIndent()

        val json = JSONObject(jsonString)
        val result = parser.parse(json)

        assertThat(result).isNull()
    }

    @Test
    fun `parse should return null when shippingAddress is missing`() {
        val jsonString = """
            {
                "requestId": "test_request_123",
                "timestamp": 1234567890
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
                "requestId": "test_request_123",
                "shippingAddress": {
                    "address1": "123 Main St",
                    "city": "New York",
                    "countryCode": "US"
                }
            }
        """.trimIndent()

        val json = JSONObject(jsonString)
        val result = parser.parse(json)

        assertThat(result).isNotNull()
        result!!
        assertThat(result.timestamp).isEqualTo(0L)
    }
}
