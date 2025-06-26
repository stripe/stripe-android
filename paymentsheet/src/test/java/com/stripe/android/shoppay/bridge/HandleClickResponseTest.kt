package com.stripe.android.shoppay.bridge

import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.PaymentSheet
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class HandleClickResponseTest {

    @Test
    fun `toJson should handle multiple line items and shipping rates`() {
        val lineItems = listOf(
            PaymentSheet.ShopPayConfiguration.LineItem("Item 1", 1000),
            PaymentSheet.ShopPayConfiguration.LineItem("Item 2", 2000),
            PaymentSheet.ShopPayConfiguration.LineItem("Discount", -500)
        )

        val shippingRates = listOf(
            PaymentSheet.ShopPayConfiguration.ShippingRate("rate_1", 500, "Express", null),
            PaymentSheet.ShopPayConfiguration.ShippingRate("rate_2", 0, "Free", null)
        )

        val response = HandleClickResponse(
            lineItems = lineItems,
            shippingRates = shippingRates,
            billingAddressRequired = true,
            emailRequired = true,
            phoneNumberRequired = false,
            shippingAddressRequired = true,
            allowedShippingCountries = listOf("US", "CA", "MX", "GB", "DE"),
            businessName = "Multi-Item Business",
            shopId = "shop_multi"
        )

        val json = response.toJson()

        assertThat(json.getString("shopId")).isEqualTo("shop_multi")

        val businessJson = json.getJSONObject("business")
        assertThat(businessJson.getString("name")).isEqualTo("Multi-Item Business")

        val lineItemsArray = json.getJSONArray("lineItems")
        assertThat(lineItemsArray.length()).isEqualTo(3)

        val shippingRatesArray = json.getJSONArray("shippingRates")
        assertThat(shippingRatesArray.length()).isEqualTo(2)

        val countriesArray = json.getJSONArray("allowedShippingCountries")
        assertThat(countriesArray.length()).isEqualTo(5)

        assertThat(json.getBoolean("billingAddressRequired")).isTrue()
        assertThat(json.getBoolean("emailRequired")).isTrue()
        assertThat(json.getBoolean("phoneNumberRequired")).isFalse()
        assertThat(json.getBoolean("shippingAddressRequired")).isTrue()
    }
}
