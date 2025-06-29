package com.stripe.android.shoppay.bridge

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ShopPayInitParamsResponseTest {

    @Test
    fun `toJson should serialize all fields correctly`() {
        val response = ShopPayInitParamsResponse(
            shopId = "shop_123456",
            customerSessionClientSecret = "css_test_secret_abc123",
            amountTotal = 2550
        )

        val json = response.toJson()

        assertThat(json.getString("shopId")).isEqualTo("shop_123456")
        assertThat(json.getString("customerSessionClientSecret")).isEqualTo("css_test_secret_abc123")
        assertThat(json.getInt("amountTotal")).isEqualTo(2550)
    }
}
