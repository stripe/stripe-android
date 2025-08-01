package com.stripe.android.shoppay.bridge

import com.google.common.truth.Truth.assertThat
import com.stripe.android.elements.payment.ShopPayConfiguration
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ShopPayMapperTest {

    @Test
    fun `LineItem toJSON should convert name and amount correctly`() {
        val lineItem = ShopPayConfiguration.LineItem(
            name = "Test Product",
            amount = 1050
        )

        val json = lineItem.toJSON()

        assertThat(json.getString("name")).isEqualTo("Test Product")
        assertThat(json.getInt("amount")).isEqualTo(1050)
    }

    @Test
    fun `ShippingRate toJSON should convert all fields correctly with deliveryEstimate`() {
        val deliveryEstimate = ShopPayConfiguration.DeliveryEstimate.Text("2-3 business days")
        val shippingRate = ShopPayConfiguration.ShippingRate(
            id = "express_shipping",
            amount = 1299,
            displayName = "Express Shipping",
            deliveryEstimate = deliveryEstimate
        )

        val json = shippingRate.toJSON()

        assertThat(json.getString("id")).isEqualTo("express_shipping")
        assertThat(json.getInt("amount")).isEqualTo(1299)
        assertThat(json.getString("displayName")).isEqualTo("Express Shipping")
        assertThat(json.getString("deliveryEstimate")).isEqualTo("2-3 business days")
    }

    @Test
    fun `ShippingRate toJSON should handle null deliveryEstimate`() {
        val shippingRate = ShopPayConfiguration.ShippingRate(
            id = "standard",
            amount = 0,
            displayName = "Free Shipping",
            deliveryEstimate = null
        )

        val json = shippingRate.toJSON()

        assertThat(json.getString("id")).isEqualTo("standard")
        assertThat(json.getInt("amount")).isEqualTo(0)
        assertThat(json.getString("displayName")).isEqualTo("Free Shipping")
        assertThat(json.has("deliveryEstimate")).isFalse()
    }

    @Test
    fun `DeliveryEstimate Text toJSON should return string value`() {
        val estimate = ShopPayConfiguration.DeliveryEstimate.Text("Next day delivery")

        val result = estimate.toJSON()

        assertThat(result).isEqualTo("Next day delivery")
    }

    @Test
    fun `DeliveryEstimate Range toJSON should convert both minimum and maximum`() {
        val minUnit = ShopPayConfiguration.DeliveryEstimate.DeliveryEstimateUnit(
            value = 2,
            unit = ShopPayConfiguration.DeliveryEstimate.DeliveryEstimateUnit.TimeUnit.DAY
        )
        val maxUnit = ShopPayConfiguration.DeliveryEstimate.DeliveryEstimateUnit(
            value = 5,
            unit = ShopPayConfiguration.DeliveryEstimate.DeliveryEstimateUnit.TimeUnit.DAY
        )
        val estimate = ShopPayConfiguration.DeliveryEstimate.Range(
            minimum = minUnit,
            maximum = maxUnit
        )

        val result = estimate.toJSON() as JSONObject

        val minimumJson = result.getJSONObject("minimum")
        assertThat(minimumJson.getString("unit")).isEqualTo("day")
        assertThat(minimumJson.getInt("value")).isEqualTo(2)

        val maximumJson = result.getJSONObject("maximum")
        assertThat(maximumJson.getString("unit")).isEqualTo("day")
        assertThat(maximumJson.getInt("value")).isEqualTo(5)
    }
}
