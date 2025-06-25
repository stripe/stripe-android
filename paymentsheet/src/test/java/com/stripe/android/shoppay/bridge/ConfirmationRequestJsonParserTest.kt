package com.stripe.android.shoppay.bridge

import com.google.common.truth.Truth.assertThat
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ConfirmationRequestJsonParserTest {

    private val eceShippingRateParser = FakeECEShippingRateJsonParser()
    private val parser = ConfirmationRequestJsonParser(eceShippingRateParser)

    @Test
    fun `parse returns ConfirmationRequest when JSON is valid`() {
        eceShippingRateParser.willReturn(ShopPayTestFactory.ECE_SHIPPING_RATE)

        val json = JSONObject(
            """
            {
                "paymentDetails": {
                    "billingDetails": {
                        "name": "John Doe"
                    },
                    "shippingRate": {
                        "id": "shipping-rate-id"
                    }
                }
            }
            """.trimIndent()
        )

        val result = parser.parse(json)

        assertThat(result).isNotNull()
        assertThat(result?.paymentDetails?.shippingRate).isEqualTo(ShopPayTestFactory.ECE_SHIPPING_RATE)
        assertThat(result?.paymentDetails?.billingDetails?.name).isEqualTo("John Doe")
    }

    @Test
    fun `parse returns request when shippingRate is null`() {
        val json = JSONObject(
            """
            {
                "paymentDetails": {
                    "billingDetails": {
                        "name": "John Doe"
                    }
                }
            }
            """.trimIndent()
        )

        val result = parser.parse(json)

        assertThat(result).isNotNull()
        assertThat(result?.paymentDetails?.shippingRate).isNull()
    }

    @Test
    fun `parse returns null when paymentDetails is missing`() {
        val json = JSONObject("{}")
        val result = parser.parse(json)
        assertThat(result).isNull()
    }
}
