package com.stripe.android.model

import org.json.JSONObject

@Suppress("MaxLineLength")
internal object GooglePayFixtures {
    val GOOGLE_PAY_RESULT_WITH_FULL_BILLING_ADDRESS = JSONObject(
        """
        {
            "apiVersionMinor": 0,
            "apiVersion": 2,
            "paymentMethodData": {
                "description": "Visa •••• 1234",
                "tokenizationData": {
                    "type": "PAYMENT_GATEWAY",
                    "token": "{\n  \"id\": \"tok_1F4VSjBbvEcIpqUbSsbEtBap\",\n  \"object\": \"token\",\n  \"card\": {\n    \"id\": \"card_1F4B7Q\",\n    \"object\": \"card\",\n    \"address_city\": \"San Francisco\",\n    \"address_country\": \"US\",\n    \"address_line1\": \"510 Townsend Street\",\n    \"address_line1_check\": \"unchecked\",\n    \"address_line2\": null,\n    \"address_state\": \"CA\",\n    \"address_zip\": \"20895\",\n    \"address_zip_check\": \"unchecked\",\n    \"brand\": \"Visa\",\n    \"country\": \"US\",\n    \"cvc_check\": null,\n    \"dynamic_last4\": \"4242\",\n    \"exp_month\": 12,\n    \"exp_year\": 2024,\n    \"funding\": \"credit\",\n    \"last4\": \"1234\",\n    \"metadata\": {\n    },\n    \"name\": \"Stripe Johnson\",\n    \"tokenization_method\": \"android_pay\"\n  },\n  \"client_ip\": \"74.125.113.98\",\n  \"created\": 1565030476,\n  \"livemode\": false,\n  \"type\": \"card\",\n  \"used\": false\n}\n"
                },
                "type": "CARD",
                "info": {
                    "cardNetwork": "VISA",
                    "cardDetails": "1234",
                    "billingAddress": {
                        "phoneNumber": "1-888-555-1234",
                        "address3": "",
                        "sortingCode": "",
                        "address2": "",
                        "countryCode": "US",
                        "address1": "510 Townsend St",
                        "postalCode": "94103",
                        "name": "Stripe Johnson",
                        "locality": "San Francisco",
                        "administrativeArea": "CA"
                    }
                }
            },
            "email": "stripe@example.com"
        }
        """.trimIndent()
    )

    val GOOGLE_PAY_RESULT_WITH_NO_BILLING_ADDRESS = JSONObject(
        """
        {
            "apiVersionMinor": 0,
            "apiVersion": 2,
            "paymentMethodData": {
                "description": "Visa •••• 1234",
                "tokenizationData": {
                    "type": "PAYMENT_GATEWAY",
                    "token": "{\n  \"id\": \"tok_1F4ACMCRMbs6FrXf6fPqLnN7\",\n  \"object\": \"token\",\n  \"card\": {\n    \"id\": \"card_1F4AzKCRMbs6FrXf1nX87nde\",\n    \"object\": \"card\",\n    \"address_city\": null,\n    \"address_country\": null,\n    \"address_line1\": null,\n    \"address_line1_check\": null,\n    \"address_line2\": null,\n    \"address_state\": null,\n    \"address_zip\": null,\n    \"address_zip_check\": null,\n    \"brand\": \"Visa\",\n    \"country\": \"US\",\n    \"cvc_check\": null,\n    \"dynamic_last4\": \"4242\",\n    \"exp_month\": 12,\n    \"exp_year\": 2024,\n    \"funding\": \"credit\",\n    \"last4\": \"1234\",\n    \"metadata\": {\n    },\n    \"name\": \"Stripe Johnson\",\n    \"tokenization_method\": \"android_pay\"\n  },\n  \"client_ip\": \"74.125.113.96\",\n  \"created\": 1565029974,\n  \"livemode\": false,\n  \"type\": \"card\",\n  \"used\": false\n}\n"
                },
                "type": "CARD",
                "info": {
                    "cardNetwork": "VISA",
                    "cardDetails": "1234"
                }
            }
        }
        """.trimIndent()
    )

    val RESULT_WITH_SHIPPING_ADDRESS = JSONObject(
        """
        {
            "apiVersionMinor": 0,
            "apiVersion": 2,
            "paymentMethodData": {
                "description": "Visa •••• 1234",
                "tokenizationData": {
                    "type": "PAYMENT_GATEWAY",
                    "token": "{\n  \"id\": \"tok_1GW4JpL6pqDH2fD\",\n  \"object\": \"token\",\n  \"card\": {\n    \"id\": \"card_1GW4JpL6pqDHo\",\n    \"object\": \"card\",\n    \"address_city\": \"San Francisco\",\n    \"address_country\": \"US\",\n    \"address_line1\": \"510 Townsend St\",\n    \"address_line1_check\": \"unchecked\",\n    \"address_line2\": null,\n    \"address_state\": \"MD\",\n    \"address_zip\": \"20895\",\n    \"address_zip_check\": \"unchecked\",\n    \"brand\": \"Visa\",\n    \"country\": \"US\",\n    \"cvc_check\": null,\n    \"dynamic_last4\": \"4242\",\n    \"exp_month\": 12,\n    \"exp_year\": 2045,\n    \"funding\": \"credit\",\n    \"last4\": \"1234\",\n    \"metadata\": {\n    },\n    \"name\": \"Jenny Rosen\",\n    \"tokenization_method\": \"android_pay\"\n  },\n  \"client_ip\": \"74.12.182.19\",\n  \"created\": 1586453857,\n  \"livemode\": false,\n  \"type\": \"card\",\n  \"used\": false\n}\n"
                },
                "type": "CARD",
                "info": {
                    "cardNetwork": "VISA",
                    "cardDetails": "1234",
                    "billingAddress": {
                        "phoneNumber": "1-800-555-1234",
                        "address3": "",
                        "sortingCode": "",
                        "address2": "",
                        "countryCode": "US",
                        "address1": "510 Townsend St",
                        "postalCode": "94103",
                        "name": "Jenny Rosen",
                        "locality": "San Francisco",
                        "administrativeArea": "CA"
                    }
                }
            },
            "shippingAddress": {
                "phoneNumber": "1-800-555-1234",
                "address3": "",
                "sortingCode": "",
                "address2": "",
                "countryCode": "US",
                "address1": "510 Townsend St",
                "postalCode": "94103",
                "name": "Jenny Rosen",
                "locality": "San Francisco",
                "administrativeArea": "CA"
            }
        }
        """.trimIndent()
    )
}
