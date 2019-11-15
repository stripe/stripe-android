package com.stripe.android.model

import org.json.JSONObject

internal object SourceOrderFixtures {

    val SOURCE_ORDER_JSON = JSONObject(
        """
        {
            "amount": 1000,
            "currency": "eur",
            "email": "jrosen@example.com",
            "items": [{
                    "amount": 1000,
                    "currency": "eur",
                    "description": "shoes",
                    "quantity": 1,
                    "type": "sku"
                },
                {
                    "amount": 1000,
                    "currency": "eur",
                    "description": "socks",
                    "quantity": 1,
                    "type": "sku"
                },
                {
                    "amount": 499,
                    "currency": "eur",
                    "description": "ground shipping",
                    "type": "shipping"
                },
                {
                    "amount": 299,
                    "currency": "eur",
                    "description": "sales tax",
                    "type": "tax"
                }
            ],
            "shipping": {
                "address": {
                    "city": "San Francisco",
                    "country": "US",
                    "line1": "123 Market St",
                    "line2": "#345",
                    "postal_code": "94107",
                    "state": "CA"
                },
                "carrier": "UPS",
                "name": "Jenny Rosen",
                "phone": "1-800-555-1234",
                "tracking_number": "tracking_12345"
            }
        }
        """.trimIndent()
    )

    val SOURCE_ORDER = SourceOrder(
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
}
