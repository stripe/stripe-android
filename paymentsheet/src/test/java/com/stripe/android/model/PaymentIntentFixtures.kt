package com.stripe.android.model

internal object PaymentIntentFixtures {
    private val PARSER = com.stripe.android.model.parsers.PaymentIntentJsonParser()

    private val PI_SUCCEEDED_JSON = org.json.JSONObject(
        """
        {
            "id": "pi_1IRg6VCRMbs6F",
            "object": "payment_intent",
            "amount": 1099,
            "canceled_at": null,
            "cancellation_reason": null,
            "capture_method": "automatic",
            "client_secret": "pi_1IRg6VCRMbs6F_secret_7oH5g4v8GaCrHfsGYS6kiSnwF",
            "confirmation_method": "automatic",
            "created": 1614960135,
            "currency": "usd",
            "description": "Example PaymentIntent",
            "last_payment_error": null,
            "livemode": false,
            "next_action": null,
            "payment_method": "pm_1IJs3ZCRMbs",
            "payment_method_types": ["card"],
            "receipt_email": null,
            "setup_future_usage": null,
            "shipping": null,
            "source": null,
            "status": "succeeded"
        }
        """.trimIndent()
    )
    val PI_SUCCEEDED = requireNotNull(PARSER.parse(PI_SUCCEEDED_JSON))

    val PI_REQUIRES_MASTERCARD_3DS2_JSON = org.json.JSONObject(
        """
        {
            "id": "pi_1ExkUeAWhjPjYwPiXph9ouXa",
            "object": "payment_intent",
            "amount": 2000,
            "amount_capturable": 0,
            "amount_received": 0,
            "application": null,
            "application_fee_amount": null,
            "canceled_at": null,
            "cancellation_reason": null,
            "capture_method": "automatic",
            "charges": {
                "object": "list",
                "data": [],
                "has_more": false,
                "total_count": 0,
                "url": "/v1/charges?payment_intent=pi_1ExkUeAWhjPjYwPiXph9ouXa"
            },
            "client_secret": "pi_1ExkUeAWhjPjYwPiXph9ouXa_secret_nGTdfGlzL9Uop59wN55LraiC7",
            "confirmation_method": "manual",
            "created": 1563498160,
            "currency": "usd",
            "customer": "cus_FSfpeeEUO3TDOJ",
            "description": "Example PaymentIntent",
            "invoice": null,
            "last_payment_error": null,
            "livemode": true,
            "next_action": {
                "type": "use_stripe_sdk",
                "use_stripe_sdk": {
                    "type": "stripe_3ds2_fingerprint",
                    "three_d_secure_2_source": "src_1ExkUeAWhjPjYwPiLWUvXrSA",
                    "directory_server_name": "mastercard",
                    "server_transaction_id": "34b16ea1-1206-4ee8-84d2-d292bc73c2ae",
                    "three_ds_method_url": "https://secure5.arcot.com/content-server/api/tds2/txn/browser/v1/tds-method",
                    "three_ds_optimizations": "",
                    "directory_server_encryption": {
                        "directory_server_id": "A000000004",
                        "key_id": "7c4debe3f4af7f9d1569a2ffea4343c2566826ee",
                        "algorithm": "RSA",
                        "certificate": "-----BEGIN CERTIFICATE-----\nMIIFtTCCA52gAwIBAgIQJqSRaPua/6cpablmVDHWUDANBgkqhkiG9w0BAQsFADB6\nMQswCQYDVQQGEwJVUzETMBEGA1UEChMKTWFzdGVyQ2FyZDEoMCYGA1UECxMfTWFz\ndGVyQ2FyZCBJZGVudGl0eSBDaGVjayBHZW4gMzEsMCoGA1UEAxMjUFJEIE1hc3Rl\nckNhcmQgM0RTMiBBY3F1aXJlciBTdWIgQ0EwHhcNMTgxMTIwMTQ1MzIzWhcNMjEx\nMTIwMTQ1MzIzWjBxMQswCQYDVQQGEwJVUzEdMBsGA1UEChMUTWFzdGVyQ2FyZCBX\nb3JsZHdpZGUxGzAZBgNVBAsTEmdhdGV3YXktZW5jcnlwdGlvbjEmMCQGA1UEAxMd\nM2RzMi5kaXJlY3RvcnkubWFzdGVyY2FyZC5jb20wggEiMA0GCSqGSIb3DQEBAQUA\nA4IBDwAwggEKAoIBAQCFlZjqbbL9bDKOzZFawdbyfQcezVEUSDCWWsYKw/V6co9A\nGaPBUsGgzxF6+EDgVj3vYytgSl8xFvVPsb4ZJ6BJGvimda8QiIyrX7WUxQMB3hyS\nBOPf4OB72CP+UkaFNR6hdlO5ofzTmB2oj1FdLGZmTN/sj6ZoHkn2Zzums8QAHFjv\nFjspKUYCmms91gpNpJPUUztn0N1YMWVFpFMytahHIlpiGqTDt4314F7sFABLxzFr\nDmcqhf623SPV3kwQiLVWOvewO62ItYUFgHwle2dq76YiKrUv1C7vADSk2Am4gqwv\n7dcCnFeM2AHbBFBa1ZBRQXosuXVw8ZcQqfY8m4iNAgMBAAGjggE+MIIBOjAOBgNV\nHQ8BAf8EBAMCAygwCQYDVR0TBAIwADAfBgNVHSMEGDAWgBSakqJUx4CN/s5W4wMU\n/17uSLhFuzBIBggrBgEFBQcBAQQ8MDowOAYIKwYBBQUHMAGGLGh0dHA6Ly9vY3Nw\nLnBraS5pZGVudGl0eWNoZWNrLm1hc3RlcmNhcmQuY29tMCgGA1UdEQQhMB+CHTNk\nczIuZGlyZWN0b3J5Lm1hc3RlcmNhcmQuY29tMGkGA1UdHwRiMGAwXqBcoFqGWGh0\ndHA6Ly9jcmwucGtpLmlkZW50aXR5Y2hlY2subWFzdGVyY2FyZC5jb20vOWE5MmEy\nNTRjNzgwOGRmZWNlNTZlMzAzMTRmZjVlZWU0OGI4NDViYi5jcmwwHQYDVR0OBBYE\nFHxN6+P0r3+dFWmi/+pDQ8JWaCbuMA0GCSqGSIb3DQEBCwUAA4ICAQAtwW8siyCi\nmhon1WUAUmufZ7bbegf3cTOafQh77NvA0xgVeloELUNCwsSSZgcOIa4Zgpsa0xi5\nfYxXsPLgVPLM0mBhTOD1DnPu1AAm32QVelHe6oB98XxbkQlHGXeOLs62PLtDZd94\n7pm08QMVb+MoCnHLaBLV6eKhKK+SNrfcxr33m0h3v2EMoiJ6zCvp8HgIHEhVpleU\n8H2Uo5YObatb/KUHgtp2z0vEfyGhZR7hrr48vUQpfVGBABsCV0aqUkPxtAXWfQo9\n1N9B7H3EIcSjbiUz5vkj9YeDSyJIi0Y/IZbzuNMsz2cRi1CWLl37w2fe128qWxYq\nY/k+Y4HX7uYchB8xPaZR4JczCvg1FV2JrkOcFvElVXWSMpBbe2PS6OMr3XxrHjzp\nDyM9qvzge0Ai9+rq8AyGoG1dP2Ay83Ndlgi42X3yl1uEUW2feGojCQQCFFArazEj\nLUkSlrB2kA12SWAhsqqQwnBLGSTp7PqPZeWkluQVXS0sbj0878kTra6TjG3U+KqO\nJCj8v6G380qIkAXe1xMHHNQ6GS59HZMeBPYkK2y5hmh/JVo4bRfK7Ya3blBSBfB8\nAVWQ5GqVWklvXZsQLN7FH/fMIT3y8iE1W19Ua4whlhvn7o/aYWOkHr1G2xyh8BHj\n7H63A2hjcPlW/ZAJSTuBZUClAhsNohH2Jg==\n-----END CERTIFICATE-----\n",
                        "root_certificate_authorities": ["-----BEGIN CERTIFICATE-----\n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\n-----END CERTIFICATE-----\n\n"]
                    }
                }
            },
            "on_behalf_of": null,
            "payment_method": "pm_1ExkUWAWhjPjYwPiBMVId8xT",
            "payment_method_options": {
                "card": {
                    "request_three_d_secure": "automatic"
                }
            },
            "payment_method_types": ["card"],
            "receipt_email": "jenny@example.com",
            "review": null,
            "setup_future_usage": null,
            "shipping": {
                "address": {
                    "city": "San Francisco",
                    "country": "US",
                    "line1": "123 Market St",
                    "line2": "#345",
                    "postal_code": "94107",
                    "state": "CA"
                },
                "carrier": null,
                "name": "Fake Name",
                "phone": "(555) 555-5555",
                "tracking_number": null
            },
            "source": null,
            "statement_descriptor": null,
            "status": "requires_action",
            "transfer_data": null,
            "transfer_group": null
        }
        """.trimIndent()
    )

    val PI_REQUIRES_MASTERCARD_3DS2 = PARSER.parse(PI_REQUIRES_MASTERCARD_3DS2_JSON)!!

    val PI_REQUIRES_PAYMENT_METHOD = PARSER.parse(
        org.json.JSONObject(
            """
        {
            "id": "pi_1F7J1aCRMbs6FrXfaJcvbxF6",
            "object": "payment_intent",
            "amount": 1099,
            "canceled_at": null,
            "cancellation_reason": null,
            "capture_method": "manual",
            "client_secret": "pi_1F7J1aCRMbs6FrXfaJcvbxF6_secret_mIuDLsSfoo1m6s",
            "confirmation_method": "automatic",
            "created": 1565775850,
            "currency": "usd",
            "description": "Example PaymentIntent",
            "livemode": false,
            "next_action": null,
            "payment_method": null,
            "payment_method_types": [
                "card",
                "link"
            ],
            "receipt_email": null,
            "setup_future_usage": null,
            "shipping": null,
            "source": null,
            "status": "requires_payment_method",
            "link_funding_sources": [
              "CARD", "BANK_ACCOUNT"
            ]
        }
            """.trimIndent()
        )
    )!!

    val PI_REQUIRES_PAYMENT_METHOD_CVC_RECOLLECTION = PARSER.parse(
        org.json.JSONObject(
            """
        {
            "id": "pi_1F7J1aCRMbs6FrXfaJcvbxF6",
            "object": "payment_intent",
            "amount": 1099,
            "canceled_at": null,
            "cancellation_reason": null,
            "capture_method": "manual",
            "client_secret": "pi_1F7J1aCRMbs6FrXfaJcvbxF6_secret_mIuDLsSfoo1m6s",
            "confirmation_method": "automatic",
            "created": 1565775850,
            "currency": "usd",
            "description": "Example PaymentIntent",
            "livemode": false,
            "next_action": null,
            "payment_method": null,
            "payment_method_types": [
                "card",
                "link"
            ],
            "receipt_email": null,
            "setup_future_usage": null,
            "shipping": null,
            "source": null,
            "payment_method_options": {
                "card": {
                    "require_cvc_recollection": true
                }
            },
            "status": "requires_payment_method",
            "link_funding_sources": [
              "CARD", "BANK_ACCOUNT"
            ]
        }
            """.trimIndent()
        )
    )!!

    val PI_REQUIRES_PAYMENT_METHOD_WITHOUT_LINK = PARSER.parse(
        org.json.JSONObject(
            """
        {
            "id": "pi_1F7J1aCRMbs6FrXfaJcvbxF6",
            "object": "payment_intent",
            "amount": 1099,
            "canceled_at": null,
            "cancellation_reason": null,
            "capture_method": "manual",
            "client_secret": "pi_1F7J1aCRMbs6FrXfaJcvbxF6_secret_mIuDLsSfoo1m6s",
            "confirmation_method": "automatic",
            "created": 1565775850,
            "currency": "usd",
            "description": "Example PaymentIntent",
            "livemode": false,
            "next_action": null,
            "payment_method": null,
            "payment_method_types": [
                "card"
            ],
            "receipt_email": null,
            "setup_future_usage": null,
            "shipping": null,
            "source": null,
            "status": "requires_payment_method"
        }
            """.trimIndent()
        )
    )!!

    val PI_REQUIRES_PAYMENT_METHOD_CARD_SFU_SET = PARSER.parse(
        org.json.JSONObject(
            """
        {
            "id": "pi_1F7J1aCRMbs6FrXfaJcvbxF6",
            "object": "payment_intent",
            "amount": 1099,
            "canceled_at": null,
            "cancellation_reason": null,
            "capture_method": "manual",
            "client_secret": "pi_1F7J1aCRMbs6FrXfaJcvbxF6_secret_mIuDLsSfoo1m6s",
            "confirmation_method": "automatic",
            "created": 1565775850,
            "currency": "usd",
            "description": "Example PaymentIntent",
            "livemode": false,
            "next_action": null,
            "payment_method": null,
            "payment_method_types": [
                "card"
            ],
            "payment_method_options": {
               "card": {
                "setup_future_usage": null
               }
            },
            "receipt_email": null,
            "setup_future_usage": null,
            "shipping": null,
            "source": null,
            "status": "requires_payment_method"
        }
            """.trimIndent()
        )
    )!!

    val PI_WITH_LAST_PAYMENT_ERROR = PARSER.parse(
        org.json.JSONObject(
            """
        {
            "id": "pi_1F7J1aCRMbs6FrXfaJcvbxF6",
            "object": "payment_intent",
            "amount": 1000,
            "canceled_at": null,
            "cancellation_reason": null,
            "capture_method": "manual",
            "client_secret": "pi_1F7J1aCRMbs6FrXfaJcvbxF6_secret_mIuDLsSfoo1m6s",
            "confirmation_method": "automatic",
            "created": 1565775850,
            "currency": "usd",
            "description": "Example PaymentIntent",
            "last_payment_error": {
                "code": "payment_intent_authentication_failure",
                "doc_url": "https://stripe.com/docs/error-codes/payment-intent-authentication-failure",
                "message": "The provided PaymentMethod has failed authentication. You can provide payment_method_data or a new PaymentMethod to attempt to fulfill this PaymentIntent again.",
                "payment_method": {
                    "id": "pm_1F7J1bCRMbs6FrXfQKsYwO3U",
                    "object": "payment_method",
                    "billing_details": {
                        "address": {
                            "city": null,
                            "country": null,
                            "line1": null,
                            "line2": null,
                            "postal_code": null,
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
                            "address_postal_code_check": null,
                            "cvc_check": null
                        },
                        "country": null,
                        "exp_month": 8,
                        "exp_year": 2020,
                        "funding": "credit",
                        "generated_from": null,
                        "last4": "3220",
                        "three_d_secure_usage": {
                            "supported": true
                        },
                        "wallet": null
                    },
                    "created": 1565775851,
                    "customer": null,
                    "livemode": false,
                    "metadata": {},
                    "type": "card"
                },
                "type": "invalid_request_error"
            },
            "livemode": false,
            "next_action": null,
            "payment_method": null,
            "payment_method_types": [
                "card"
            ],
            "receipt_email": null,
            "setup_future_usage": null,
            "shipping": null,
            "source": null,
            "status": "requires_payment_method"
        }
            """.trimIndent()
        )
    )!!

    val EXPANDED_PAYMENT_METHOD_JSON = org.json.JSONObject(
        """
        {
            "id": "pi_1GSTxJCRMbs",
            "object": "payment_intent",
            "amount": 1099,
            "canceled_at": null,
            "cancellation_reason": null,
            "capture_method": "automatic",
            "client_secret": "pi_1GSTxJCRMbs_secret_NqmhRfE9f",
            "confirmation_method": "automatic",
            "created": 1585599093,
            "currency": "usd",
            "description": "Example PaymentIntent",
            "last_payment_error": null,
            "livemode": false,
            "payment_method": {
                "id": "pm_1GSTxOCRMbs6FrXfYCosDqyr",
                "object": "payment_method",
                "billing_details": {
                    "address": {
                        "city": null,
                        "country": null,
                        "line1": null,
                        "line2": null,
                        "postal_code": null,
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
                        "address_postal_code_check": null,
                        "cvc_check": null
                    },
                    "country": "IE",
                    "exp_month": 1,
                    "exp_year": 2045,
                    "funding": "credit",
                    "generated_from": null,
                    "last4": "3238",
                    "three_d_secure_usage": {
                        "supported": true
                    },
                    "wallet": null
                },
                "created": 1585599098,
                "customer": null,
                "livemode": false,
                "metadata": {},
                "type": "card"
            },
            "payment_method_types": ["card"],
            "receipt_email": null,
            "setup_future_usage": null,
            "shipping": {
                "address": {
                    "city": "San Francisco",
                    "country": "US",
                    "line1": "123 Market St",
                    "line2": "#345",
                    "postal_code": "94107",
                    "state": "CA"
                },
                "carrier": "Fedex",
                "name": "Jenny Rosen",
                "phone": null,
                "tracking_number": "12345"
            },
            "source": null,
            "status": "requires_action"
        }
        """.trimIndent()
    )

    val PI_WITH_PAYMENT_METHOD = PARSER.parse(EXPANDED_PAYMENT_METHOD_JSON)

    val PI_WITH_SHIPPING_JSON = org.json.JSONObject(
        """
        {
            "id": "pi_1GYda2CRMbs",
            "object": "payment_intent",
            "amount": 1099,
            "canceled_at": null,
            "cancellation_reason": null,
            "capture_method": "automatic",
            "client_secret": "pi_1GYda2CRMbs_secret_Z2zduomY0",
            "confirmation_method": "automatic",
            "created": 1587066058,
            "currency": "usd",
            "description": "Example PaymentIntent",
            "last_payment_error": null,
            "livemode": false,
            "next_action": null,
            "payment_method": {
                "id": "pm_1GYda7CRMbs6FrX",
                "object": "payment_method",
                "billing_details": {
                    "address": {
                        "city": "San Francisco",
                        "country": "US",
                        "line1": "123 Market St",
                        "line2": "#345",
                        "postal_code": "94107",
                        "state": "CA"
                    },
                    "email": null,
                    "name": "Jenny Rosen",
                    "phone": null
                },
                "card": {
                    "brand": "visa",
                    "checks": {
                        "address_line1_check": null,
                        "address_postal_code_check": null,
                        "cvc_check": null
                    },
                    "country": "US",
                    "exp_month": 12,
                    "exp_year": 2045,
                    "funding": "credit",
                    "generated_from": null,
                    "last4": "4242",
                    "three_d_secure_usage": {
                        "supported": true
                    },
                    "wallet": {
                        "dynamic_last4": "4242",
                        "google_pay": {},
                        "type": "google_pay"
                    }
                },
                "created": 1587066063,
                "customer": null,
                "livemode": false,
                "metadata": {},
                "type": "card"
            },
            "payment_method_types": ["card"],
            "receipt_email": null,
            "setup_future_usage": null,
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
                "tracking_number": "12345"
            },
            "source": null,
            "status": "succeeded"
        }
        """.trimIndent()
    )
    val PI_WITH_SHIPPING = PARSER.parse(PI_WITH_SHIPPING_JSON)!!

    val PI_OFF_SESSION = PARSER.parse(
        PI_WITH_SHIPPING_JSON
    )!!.copy(
        setupFutureUsage = StripeIntent.Usage.OffSession
    )
}
