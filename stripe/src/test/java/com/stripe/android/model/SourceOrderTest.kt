package com.stripe.android.model

import kotlin.test.Test
import kotlin.test.assertEquals

class SourceOrderTest {

    @Test
    fun testParse() {
        val expected = SourceOrder(
            amount = 1000,
            currency = "eur",
            email = "jrosen@example.com",
            items = listOf(
                SourceOrder.Item(
                    type = SourceOrder.Item.Type.Sku,
                    amount = 1000,
                    currency = "eur",
                    description = "shoes",
                    quantity = 1
                ),
                SourceOrder.Item(
                    type = SourceOrder.Item.Type.Sku,
                    amount = 1000,
                    currency = "eur",
                    description = "socks",
                    quantity = 1
                ),
                SourceOrder.Item(
                    type = SourceOrder.Item.Type.Shipping,
                    amount = 499,
                    currency = "eur",
                    description = "ground shipping"
                ),
                SourceOrder.Item(
                    type = SourceOrder.Item.Type.Tax,
                    amount = 299,
                    currency = "eur",
                    description = "sales tax"
                )
            ),
            shipping = SourceOrder.Shipping(
                address = AddressFixtures.ADDRESS,
                carrier = "UPS",
                name = "Jenny Rosen",
                phone = "1-800-555-1234",
                trackingNumber = "tracking_12345"
            )
        )
        assertEquals(expected, SourceOrderFixtures.SOURCE_ORDER)
    }
}
