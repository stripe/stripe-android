package com.stripe.android.model.parsers

import com.google.common.truth.Truth.assertThat
import org.json.JSONObject
import org.junit.Test

class PaymentMethodsListJsonParserTest {

    @Test
    fun `parse() should return expected object`() {
        assertThat(
            PaymentMethodsListJsonParser().parse(JSON).paymentMethods
        ).hasSize(4)
    }

    private companion object {
        val JSON = JSONObject(
            """
            {
                "object": "list",
                "data": [{
                    "id": "pm_1Hi37tCRMbs6FrXfHKguY48J",
                    "object": "payment_method",
                    "billing_details": {
                        "address": {
                            "city": null,
                            "country": null,
                            "line1": null,
                            "line2": null,
                            "postal_code": "42424244",
                            "state": null
                        },
                        "email": null,
                        "name": null,
                        "phone": null
                    },
                    "card": {
                        "brand": "visa",
                        "checks": {
                            "address_line1_check": null,
                            "address_postal_code_check": "pass",
                            "cvc_check": "pass"
                        },
                        "country": "US",
                        "exp_month": 4,
                        "exp_year": 2024,
                        "fingerprint": "atmHgDo9nxHpQJiw",
                        "funding": "credit",
                        "generated_from": null,
                        "last4": "4242",
                        "networks": {
                            "available": ["visa"],
                            "preferred": null
                        },
                        "three_d_secure_usage": {
                            "supported": true
                        },
                        "wallet": null
                    },
                    "created": 1604085545,
                    "customer": "cus_IHnhWPbnPfSidi",
                    "livemode": false,
                    "type": "card"
                }, {
                    "id": "pm_1HhhEACRMbs6FrXftkeI7zWI",
                    "object": "payment_method",
                    "billing_details": {
                        "address": {
                            "city": null,
                            "country": null,
                            "line1": null,
                            "line2": null,
                            "postal_code": "424242",
                            "state": null
                        },
                        "email": null,
                        "name": null,
                        "phone": null
                    },
                    "card": {
                        "brand": "visa",
                        "checks": {
                            "address_line1_check": null,
                            "address_postal_code_check": "pass",
                            "cvc_check": "pass"
                        },
                        "country": "US",
                        "exp_month": 4,
                        "exp_year": 2024,
                        "fingerprint": "atmHgDo9nxHpQJiw",
                        "funding": "credit",
                        "generated_from": null,
                        "last4": "4242",
                        "networks": {
                            "available": ["visa"],
                            "preferred": null
                        },
                        "three_d_secure_usage": {
                            "supported": true
                        },
                        "wallet": null
                    },
                    "created": 1604001367,
                    "customer": "cus_IHnhWPbnPfSidi",
                    "livemode": false,
                    "type": "card"
                }, {
                    "id": "pm_1Hhh2RCRMbs6FrXfw1CnAri4",
                    "object": "payment_method",
                    "billing_details": {
                        "address": {
                            "city": null,
                            "country": null,
                            "line1": null,
                            "line2": null,
                            "postal_code": "42424",
                            "state": null
                        },
                        "email": null,
                        "name": null,
                        "phone": null
                    },
                    "card": {
                        "brand": "visa",
                        "checks": {
                            "address_line1_check": null,
                            "address_postal_code_check": "pass",
                            "cvc_check": "pass"
                        },
                        "country": "US",
                        "exp_month": 4,
                        "exp_year": 2024,
                        "fingerprint": "atmHgDo9nxHpQJiw",
                        "funding": "credit",
                        "generated_from": null,
                        "last4": "4242",
                        "networks": {
                            "available": ["visa"],
                            "preferred": null
                        },
                        "three_d_secure_usage": {
                            "supported": true
                        },
                        "wallet": null
                    },
                    "created": 1604000639,
                    "customer": "cus_IHnhWPbnPfSidi",
                    "livemode": false,
                    "type": "card"
                }, {
                    "id": "pm_1HhEHFCRMbs6FrXfefODJMiH",
                    "object": "payment_method",
                    "billing_details": {
                        "address": {
                            "city": null,
                            "country": null,
                            "line1": null,
                            "line2": null,
                            "postal_code": "4242424",
                            "state": null
                        },
                        "email": null,
                        "name": null,
                        "phone": null
                    },
                    "card": {
                        "brand": "visa",
                        "checks": {
                            "address_line1_check": null,
                            "address_postal_code_check": "pass",
                            "cvc_check": "pass"
                        },
                        "country": "US",
                        "exp_month": 4,
                        "exp_year": 2024,
                        "fingerprint": "atmHgDo9nxHpQJiw",
                        "funding": "credit",
                        "generated_from": null,
                        "last4": "4242",
                        "networks": {
                            "available": ["visa"],
                            "preferred": null
                        },
                        "three_d_secure_usage": {
                            "supported": true
                        },
                        "wallet": null
                    },
                    "created": 1603890081,
                    "customer": "cus_IHnhWPbnPfSidi",
                    "livemode": false,
                    "type": "card"
                }],
                "has_more": false,
                "url": "\/v1\/payment_methods"
            }
            """.trimIndent()
        )
    }
}
