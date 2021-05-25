package com.stripe.android.model

import kotlin.test.Test
import kotlin.test.assertEquals

class SourceOrderParamsTest {

    @Test
    fun toParamMap() {
        val params = SourceOrderParams(
            items = listOf(
                SourceOrderParams.Item(
                    type = SourceOrderParams.Item.Type.Sku,
                    amount = 1000,
                    currency = "eur",
                    description = "shoes",
                    quantity = 1
                )
            ),
            shipping = SourceOrderParams.Shipping(
                address = AddressFixtures.ADDRESS,
                carrier = "UPS",
                name = "Jenny Rosen"
            )
        )

        val expectedParamsMap = mapOf(
            "items" to listOf(
                mapOf(
                    "type" to "sku",
                    "amount" to 1000,
                    "currency" to "eur",
                    "description" to "shoes",
                    "quantity" to 1
                )
            ),
            "shipping" to mapOf(
                "address" to mapOf(
                    "city" to "San Francisco",
                    "country" to "US",
                    "line1" to "123 Market St",
                    "line2" to "#345",
                    "postal_code" to "94107",
                    "state" to "CA"
                ),
                "carrier" to "UPS",
                "name" to "Jenny Rosen"
            )
        )

        assertEquals(expectedParamsMap, params.toParamMap())
    }
}
