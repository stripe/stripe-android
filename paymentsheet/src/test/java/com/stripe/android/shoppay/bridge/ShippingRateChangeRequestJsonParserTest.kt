package com.stripe.android.shoppay.bridge

import com.google.common.truth.Truth.assertThat
import com.stripe.android.shoppay.ShopPayTestFactory
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ShippingRateChangeRequestJsonParserTest {

    private val eceShippingRateParser = FakeECEShippingRateJsonParser()
    private val parser = ShippingRateChangeRequestJsonParser(eceShippingRateParser)

    @Test
    fun `parse returns ShippingRateChangeRequest when JSON is valid`() {
        eceShippingRateParser.willReturn(ShopPayTestFactory.ECE_SHIPPING_RATE)

        val json = JSONObject(
            """
            {
                "shippingRate": {
                    "id": "express"
                }
            }
            """.trimIndent()
        )

        val result = parser.parse(json)

        assertThat(result).isNotNull()
        assertThat(result?.shippingRate).isEqualTo(ShopPayTestFactory.ECE_SHIPPING_RATE)
    }

    @Test
    fun `parse returns null when shippingRate is missing`() {
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
    fun `parse returns null when shippingRate is not an object`() {
        val json = JSONObject(
            """
            {
                "shippingRate": "notAnObject"
            }
            """.trimIndent()
        )

        val result = parser.parse(json)

        assertThat(result).isNull()
    }

    @Test
    fun `parse returns null when nested parser returns null`() {
        eceShippingRateParser.willReturn(null)
        val json = JSONObject(
            """
            {
                "shippingRate": {
                    "id": "express"
                }
            }
            """.trimIndent()
        )

        val result = parser.parse(json)

        assertThat(result).isNull()
    }
}
