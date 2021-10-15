package com.stripe.android.model

import com.stripe.android.model.parsers.PaymentIntentJsonParser
import org.json.JSONObject

internal object PaymentIntentFixtures {
    private val PARSER = PaymentIntentJsonParser()

    const val KEY_ID = "7c4debe3f4af7f9d1569a2ffea4343c2566826ee"

    private val PI_SUCCEEDED_JSON = JSONObject(
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

    val PI_REQUIRES_MASTERCARD_3DS2_JSON = JSONObject(
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

    val PI_REQUIRES_AMEX_3DS2 = PARSER.parse(
        JSONObject(
            """
        {
            "id": "pi_1EceMnCRMbs6FrXfCXdF8dnx",
            "object": "payment_intent",
            "amount": 1000,
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
                "url": "/v1/charges?payment_intent=pi_1EceMnCRMbs6FrXfCXdF8dnx"
            },
            "client_secret": "pi_1EceMnCRMbs6FrXfCXdF8dnx_secret_vew0L3IGaO0x9o0eyRMGzKr0k",
            "confirmation_method": "automatic",
            "created": 1558469721,
            "currency": "usd",
            "customer": null,
            "description": null,
            "invoice": null,
            "last_payment_error": null,
            "livemode": false,
            "metadata": {},
            "next_action": {
                "type": "use_stripe_sdk",
                "use_stripe_sdk": {
                    "type": "stripe_3ds2_fingerprint",
                    "three_d_secure_2_source": "src_1EceOlCRMbs6FrXf2hqrI1g5",
                    "directory_server_name": "american_express",
                    "server_transaction_id": "e64bb72f-60ac-4845-b8b6-47cfdb0f73aa",
                    "three_ds_method_url": "",
                    "directory_server_encryption": {
                        "directory_server_id": "A000000025",
                        "certificate": "-----BEGIN CERTIFICATE-----\nMIIE0TCCA7mgAwIBAgIUXbeqM1duFcHk4dDBwT8o7Ln5wX8wDQYJKoZIhvcNAQEL\nBQAwXjELMAkGA1UEBhMCVVMxITAfBgNVBAoTGEFtZXJpY2FuIEV4cHJlc3MgQ29t\ncGFueTEsMCoGA1UEAxMjQW1lcmljYW4gRXhwcmVzcyBTYWZla2V5IElzc3Vpbmcg\nQ0EwHhcNMTgwMjIxMjM0OTMxWhcNMjAwMjIxMjM0OTMwWjCB0DELMAkGA1UEBhMC\nVVMxETAPBgNVBAgTCE5ldyBZb3JrMREwDwYDVQQHEwhOZXcgWW9yazE\/MD0GA1UE\nChM2QW1lcmljYW4gRXhwcmVzcyBUcmF2ZWwgUmVsYXRlZCBTZXJ2aWNlcyBDb21w\nYW55LCBJbmMuMTkwNwYDVQQLEzBHbG9iYWwgTmV0d29yayBUZWNobm9sb2d5IC0g\nTmV0d29yayBBUEkgUGxhdGZvcm0xHzAdBgNVBAMTFlNESy5TYWZlS2V5LkVuY3J5\ncHRLZXkwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQDSFF9kTYbwRrxX\nC6WcJJYio5TZDM62+CnjQRfggV3GMI+xIDtMIN8LL\/jbWBTycu97vrNjNNv+UPhI\nWzhFDdUqyRfrY337A39uE8k1xhdDI3dNeZz6xgq8r9hn2NBou78YPBKidpN5oiHn\nTxcFq1zudut2fmaldaa9a4ZKgIQo+02heiJfJ8XNWkoWJ17GcjJ59UU8C1KF\/y1G\nymYO5ha2QRsVZYI17+ZFsqnpcXwK4Mr6RQKV6UimmO0nr5++CgvXfekcWAlLV6Xq\njuACWi3kw0haepaX\/9qHRu1OSyjzWNcSVZ0On6plB5Lq6Y9ylgmxDrv+zltz3MrT\nK7txIAFFAgMBAAGjggESMIIBDjAMBgNVHRMBAf8EAjAAMCEGA1UdEQQaMBiCFlNE\nSy5TYWZlS2V5LkVuY3J5cHRLZXkwRQYJKwYBBAGCNxQCBDgeNgBBAE0ARQBYAF8A\nUwBBAEYARQBLAEUAWQAyAF8ARABTAF8ARQBOAEMAUgBZAFAAVABJAE8ATjAOBgNV\nHQ8BAf8EBAMCBJAwHwYDVR0jBBgwFoAU7k\/rXuVMhTBxB1zSftPgmLFuDIgwRAYD\nVR0fBD0wOzA5oDegNYYzaHR0cDovL2FtZXhzay5jcmwuY29tLXN0cm9uZy1pZC5u\nZXQvYW1leHNhZmVrZXkuY3JsMB0GA1UdDgQWBBQHclVTo5nwZGH8labJ2F2P45xi\nfDANBgkqhkiG9w0BAQsFAAOCAQEAWY6b77VBoGLs3k5vOqSU7QRqT+4v6y77T8LA\nBKrSZ58DiVZWVyDSxyftQUiRRgFHt2gTN0yfJTP50Fyp84nCEWC0tugZ4iIhgPss\nHzL+4\/u4eG\/MTzK2ESxvPgr6YHajyuU+GXA89u8+bsFrFmojOjhTgFKli7YUeV\/0\nxoiYZf2utlns800ofJrcrfiFoqE6PvK4Od0jpeMgfSKv71nK5ihA1+wTk76ge1fs\nPxL23hEdRpWW11ofaLfJGkLFXMM3\/LHSXWy7HhsBgDELdzLSHU4VkSv8yTOZxsRO\nByxdC5v3tXGcK56iQdtKVPhFGOOEBugw7AcuRzv3f1GhvzAQZg==\n-----END CERTIFICATE-----",
                        "key_id": "7c4debe3f4af7f9d1569a2ffea4343c2566826ee"
                    }
                }
            },
            "on_behalf_of": null,
            "payment_method": "pm_1EceOkCRMbs6FrXft9sFxCTG",
            "payment_method_types": [
                "card"
            ],
            "receipt_email": null,
            "review": null,
            "shipping": null,
            "source": null,
            "statement_descriptor": null,
            "status": "requires_action",
            "transfer_data": null,
            "transfer_group": null
        }
            """.trimIndent()
        )
    )!!

    val PI_REQUIRES_3DS1 = PARSER.parse(
        JSONObject(
            """
        {
            "id": "pi_1EceMnCRMbs6FrXfCXdF8dnx",
            "object": "payment_intent",
            "amount": 1000,
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
                "url": "/v1/charges?payment_intent=pi_1EceMnCRMbs6FrXfCXdF8dnx"
            },
            "client_secret": "pi_1EceMnCRMbs6FrXfCXdF8dnx_secret_vew0L3IGaO0x9o0eyRMGzKr0k",
            "confirmation_method": "automatic",
            "created": 1558469721,
            "currency": "usd",
            "customer": null,
            "description": null,
            "invoice": null,
            "last_payment_error": null,
            "livemode": false,
            "metadata": {},
            "next_action": {
                "type": "use_stripe_sdk",
                "use_stripe_sdk": {
                    "type": "three_d_secure_redirect",
                    "stripe_js": "https://hooks.stripe.com/3d_secure_2_eap/begin_test/src_1Ecve7CRMbs6FrXfm8AxXMIh/src_client_secret_F79yszOBAiuaZTuIhbn3LPUW"
                }
            },
            "on_behalf_of": null,
            "payment_method": "pm_1Ecve6CRMbs6FrXf08xsGeHv",
            "payment_method_types": [
                "card"
            ],
            "receipt_email": null,
            "review": null,
            "shipping": null,
            "source": null,
            "statement_descriptor": null,
            "status": "requires_action",
            "transfer_data": null,
            "transfer_group": null
        }
            """.trimIndent()
        )
    )!!

    val PI_REQUIRES_REDIRECT = PARSER.parse(
        JSONObject(
            """
        {
            "id": "pi_1EZlvVCRMbs6FrXfKpq2xMmy",
            "object": "payment_intent",
            "amount": 1000,
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
                "url": "/v1/charges?payment_intent=pi_1EZlvVCRMbs6FrXfKpq2xMmy"
            },
            "client_secret": "pi_1EZlvVCRMbs6FrXfKpq2xMmy_secret_cmhLfbSA54n4",
            "confirmation_method": "automatic",
            "created": 1557783797,
            "currency": "usd",
            "customer": null,
            "description": null,
            "invoice": null,
            "last_payment_error": null,
            "livemode": false,
            "metadata": {},
            "next_action": {
                "redirect_to_url": {
                    "return_url": "stripe://deeplink",
                    "url": "https://hooks.stripe.com/3d_secure_2_eap/begin_test/src_1Ecaz6CRMbs6FrXfuYKBRSUG/src_client_secret_F6octeOshkgxT47dr0ZxSZiv"
                },
                "type": "redirect_to_url"
            },
            "on_behalf_of": null,
            "payment_method": "pm_1Ecaz6CRMbs6FrXfQs3wNGwB",
            "payment_method_types": [
                "card"
            ],
            "receipt_email": null,
            "review": null,
            "shipping": null,
            "source": null,
            "statement_descriptor": null,
            "status": "requires_action",
            "transfer_data": null,
            "transfer_group": null
        }
            """.trimIndent()
        )
    )!!

    val PI_REQUIRES_PAYMENT_METHOD = PARSER.parse(
        JSONObject(
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

    val PI_WITH_LAST_PAYMENT_ERROR = PARSER.parse(
        JSONObject(
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

    val CANCELLED = PARSER.parse(
        JSONObject(
            """
        {
            "id": "pi_1FCpMECRMbs6FrXfVulorSf5",
            "object": "payment_intent",
            "amount": 4000,
            "amount_capturable": 0,
            "amount_received": 0,
            "application": null,
            "application_fee_amount": null,
            "canceled_at": 1567091866,
            "cancellation_reason": "abandoned",
            "capture_method": "automatic",
            "charges": {
                "object": "list",
                "data": [],
                "has_more": false,
                "total_count": 0,
                "url": "/v1/charges?payment_intent=pi_1FCpMECRMbs6FrXfVulorSf5"
            },
            "client_secret": "pi_1FCpMECRMbs6FrXfVulorSf5_secret_oSppt5A",
            "confirmation_method": "manual",
            "created": 1567091778,
            "currency": "usd",
            "customer": "cus_FWhpaTLIPWLhpJ",
            "description": "Example PaymentIntent",
            "invoice": null,
            "last_payment_error": null,
            "livemode": false,
            "metadata": null,
            "next_action": null,
            "on_behalf_of": null,
            "payment_method": null,
            "payment_method_options": {
                "card": {
                    "request_three_d_secure": "automatic"
                }
            },
            "payment_method_types": [
                "card"
            ],
            "receipt_email": null,
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
            "statement_descriptor_suffix": null,
            "status": "canceled",
            "transfer_data": null,
            "transfer_group": null
        }
            """.trimIndent()
        )
    )!!

    val PAYMENT_INTENT_WITH_CANCELED_3DS1_SOURCE = PARSER.parse(
        JSONObject(
            """
        {
            "id": "pi_1FeqH9CRMbs6FrXfcqqpoC2H",
            "object": "payment_intent",
            "amount": 1099,
            "canceled_at": null,
            "cancellation_reason": null,
            "capture_method": "manual",
            "client_secret": "pi_1FeqH9CRMbs6FrXfcqqpoC2H_secret_DHYkuHTGq",
            "confirmation_method": "automatic",
            "created": 1573768491,
            "currency": "usd",
            "description": "Example PaymentIntent",
            "last_payment_error": {
                "message": "The PaymentMethod on this PaymentIntent was previously used without being attached to a Customer or was detached from a Customer, and may not be used again. You can try confirming again with a new PaymentMethod.",
                "payment_method": {
                    "id": "pm_1FeqHBCRMbs6FrXfsDE5NFJH",
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
                        "country": "US",
                        "exp_month": 1,
                        "exp_year": 2025,
                        "funding": "credit",
                        "generated_from": null,
                        "last4": "3063",
                        "three_d_secure_usage": {
                            "supported": true
                        },
                        "wallet": null
                    },
                    "created": 1573768493,
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
    )

    val EXPANDED_PAYMENT_METHOD_JSON = JSONObject(
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
                    "exp_year": 2025,
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

    val PI_WITH_SHIPPING_JSON = JSONObject(
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
                    "exp_year": 2025,
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

    val OXXO_REQUIRES_ACTION_JSON = JSONObject(
        """
        {
            "id": "pi_1IcuwoL32KlRo",
            "object": "payment_intent",
            "amount": 1099,
            "canceled_at": null,
            "cancellation_reason": null,
            "capture_method": "automatic",
            "client_secret": "pi_1IcuwoL32KlRo_secret_KC0YoHfna465TDVW",
            "confirmation_method": "automatic",
            "created": 1617638802,
            "currency": "mxn",
            "description": "Example PaymentIntent",
            "last_payment_error": null,
            "livemode": false,
            "next_action": {
                "oxxo_display_details": {
                    "expires_after": 1617944399,
                    "hosted_voucher_url": "https:\/\/payments.stripe.com\/oxxo\/voucher\/test_YWNjdF8xSWN1c1VMMzJLbFJvdDAxLF9KRlBtckVBMERWM0lBZEUyb",
                    "number": "12345678901234657890123456789012"
                },
                "type": "oxxo_display_details"
            },
            "payment_method": {
                "id": "pm_1IcuwoL32KlRot01",
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
                    "email": "jrosen@example.com",
                    "name": "Jenny Rosen",
                    "phone": null
                },
                "created": 1617638802,
                "customer": null,
                "livemode": false,
                "oxxo": {},
                "type": "oxxo"
            },
            "payment_method_types": ["card", "oxxo"],
            "receipt_email": null,
            "setup_future_usage": null,
            "shipping": null,
            "source": null,
            "status": "requires_action"
        }
        """.trimIndent()
    )
    val OXXO_REQUIES_ACTION = requireNotNull(PARSER.parse(OXXO_REQUIRES_ACTION_JSON))

    val ALIPAY_REQUIRES_ACTION_JSON = JSONObject(
        """
        {
          "id": "pi_1HDEFVKlwPmebFhpCobFP55H",
          "object": "payment_intent",
          "amount": 100,
          "canceled_at": null,
          "cancellation_reason": null,
          "capture_method": "automatic",
          "client_secret": "pi_1HDEFVKlwPmebFhpCobFP55H_secret_XW8sADccCxtusewAwn5z9kAiw",
          "confirmation_method": "automatic",
          "created": 1596740133,
          "currency": "usd",
          "description": "Example PaymentIntent",
          "last_payment_error": null,
          "livemode": true,
          "next_action": {
            "alipay_handle_redirect": {
              "native_data": "_input_charset=utf-8&app_pay=Y&currency=USD&forex_biz=FP&notify_url=https%3A%2F%2Fhooks.stripe.com%2Falipay%2Falipay%2Fhook%2F6255d30b067c8f7a162c79c654483646%2Fsrc_1HDEFWKlwPmebFhp6tcpln8T&out_trade_no=src_1HDEFWKlwPmebFhp6tcpln8T&partner=2088621828244481&payment_type=1&product_code=NEW_WAP_OVERSEAS_SELLER&return_url=https%3A%2F%2Fhooks.stripe.com%2Fadapter%2Falipay%2Fredirect%2Fcomplete%2Fsrc_1HDEFWKlwPmebFhp6tcpln8T%2Fsrc_client_secret_S6H9mVMKK6qxk9YxsUvbH55K&secondary_merchant_id=acct_1EqOyCKlwPmebFhp&secondary_merchant_industry=5734&secondary_merchant_name=Yuki-Test&sendFormat=normal&service=create_forex_trade_wap&sign=b691876a7f0bd889530f54a271d314d5&sign_type=MD5&subject=Yuki-Test&supplier=Yuki-Test&timeout_rule=20m&total_fee=1.00",
              "native_url": null,
              "return_url": "example://return_url",
              "url": "https://hooks.stripe.com/redirect/authenticate/src_1HDEFWKlwPmebFhp6tcpln8T?client_secret=src_client_secret_S6H9mVMKK6qxk9YxsUvbH55K"
            },
            "type": "alipay_handle_redirect"
          },
          "payment_method": {
            "id": "pm_1HDEFVKlwPmebFhpKYYkSm8H",
            "object": "payment_method",
            "alipay": {},
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
            "created": 1596740133,
            "customer": null,
            "livemode": true,
            "type": "alipay"
          },
          "payment_method_types": [
            "alipay"
          ],
          "receipt_email": null,
          "setup_future_usage": null,
          "shipping": null,
          "source": null,
          "status": "requires_action"
        }
        """.trimIndent()
    )

    val ALIPAY_REQUIRES_ACTION = PARSER.parse(ALIPAY_REQUIRES_ACTION_JSON)!!

    val ALIPAY_TEST_MODE_JSON = JSONObject(
        """
        {
          "id": "pi_1HDEFVKlwPmebFhpCobFP55H",
          "object": "payment_intent",
          "amount": 100,
          "canceled_at": null,
          "cancellation_reason": null,
          "capture_method": "automatic",
          "client_secret": "pi_1HDEFVKlwPmebFhpCobFP55H_secret_XW8sADccCxtusewAwn5z9kAiw",
          "confirmation_method": "automatic",
          "created": 1596740133,
          "currency": "usd",
          "description": "Example PaymentIntent",
          "last_payment_error": null,
          "livemode": true,
          "next_action": {
            "alipay_handle_redirect": {
              "native_data": null,
              "native_url": null,
              "return_url": "example://return_url",
              "url": "https://hooks.stripe.com/redirect/authenticate/src_1HDEFWKlwPmebFhp6tcpln8T?client_secret=src_client_secret_S6H9mVMKK6qxk9YxsUvbH55K"
            },
            "type": "alipay_handle_redirect"
          },
          "payment_method": {
            "id": "pm_1HDEFVKlwPmebFhpKYYkSm8H",
            "object": "payment_method",
            "alipay": {},
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
            "created": 1596740133,
            "customer": null,
            "livemode": false,
            "type": "alipay"
          },
          "payment_method_types": [
            "alipay"
          ],
          "receipt_email": null,
          "setup_future_usage": null,
          "shipping": null,
          "source": null,
          "status": "requires_action"
        }
        """.trimIndent()
    )

    val ALIPAY_TEST_MODE = PARSER.parse(ALIPAY_TEST_MODE_JSON)!!

    val PI_REQUIRES_BLIK_AUTHORIZE_JSON = JSONObject(
        """
        {
          "id": "pi_1IVmwXFY0qyl6XeWwxGWA04D",
          "object": "payment_intent",
          "amount": 1099,
          "amount_capturable": 0,
          "amount_received": 0,
          "amount_subtotal": 1099,
          "application": null,
          "application_fee_amount": null,
          "canceled_at": null,
          "cancellation_reason": null,
          "capture_method": "automatic",
          "charges": {
            "object": "list",
            "data": [
        
            ],
            "has_more": false,
            "total_count": 0,
            "url": "/v1/charges?payment_intent=pi_1IVmwXFY0qyl6XeWwxGWA04D"
          },
          "client_secret": "pi_1IVmwXFY0qyl6XeWwxGWA04D_secret_4U8cSCdPefr8LHtPsKvA3mcQz",
          "confirmation_method": "automatic",
          "created": 1615939737,
          "currency": "pln",
          "customer": null,
          "description": null,
          "invoice": null,
          "last_payment_error": null,
          "livemode": false,
          "metadata": {
          },
          "next_action": {
            "type": "blik_authorize"
          },
          "on_behalf_of": null,
          "payment_method": "pm_1IVnI3FY0qyl6XeWxJFdBh2g",
          "payment_method_options": {
            "blik": {
            }
          },
          "payment_method_types": [
            "blik"
          ],
          "receipt_email": null,
          "review": null,
          "setup_future_usage": null,
          "shipping": null,
          "source": null,
          "statement_descriptor": null,
          "statement_descriptor_suffix": null,
          "status": "requires_action",
          "total_details": {
            "amount_discount": 0,
            "amount_tax": 0
          },
          "transfer_data": null,
          "transfer_group": null
        }
        """.trimIndent()
    )

    val PI_REQUIRES_BLIK_AUTHORIZE = PARSER.parse(PI_REQUIRES_BLIK_AUTHORIZE_JSON)!!

    private val PI_REQUIRES_WECHAT_PAY_AUTHORIZE_JSON = JSONObject(
        """
        {
          "id": "pi_1IlJH7BNJ02ErVOjm37T3OUt",
          "object": "payment_intent",
          "amount": 1099,
          "amount_capturable": 0,
          "amount_received": 0,
          "application": null,
          "application_fee_amount": null,
          "canceled_at": null,
          "cancellation_reason": null,
          "capture_method": "automatic",
          "charges": {
            "object": "list",
            "data": [
        
            ],
            "has_more": false,
            "total_count": 0,
            "url": "/v1/charges?payment_intent=pi_1IlJH7BNJ02ErVOjm37T3OUt"
          },
          "client_secret": "pi_1IlJH7BNJ02ErVOjm37T3OUt_secret_vgMExmjvESdtPqddHOSSSDip2",
          "confirmation_method": "automatic",
          "created": 1619638941,
          "currency": "usd",
          "customer": null,
          "description": null,
          "invoice": null,
          "last_payment_error": null,
          "livemode": false,
          "metadata": {
          },
          "next_action": {
            "type": "wechat_pay_redirect_to_android_app",
            "wechat_pay_redirect_to_android_app": {
              "app_id": "wx65997d6307c3827d",
              "nonce_str": "some_random_string",
              "package": "Sign=WXPay",
              "partner_id": "wx65997d6307c3827d",
              "prepay_id": "test_transaction",
              "sign": "8B26124BABC816D7140034DDDC7D3B2F1036CCB2D910E52592687F6A44790D5E",
              "timestamp": "1619638941"
            }
          },
          "on_behalf_of": null,
          "payment_method": {
            "id": "pm_1IlJH7BNJ02ErVOjxKQu1wfH",
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
            "created": 1634242405,
            "customer": null,
            "livemode": false,
            "type": "wechat_pay",
            "wechat_pay": {
            }
          },
          "payment_method_options": {
            "wechat_pay": {
            }
          },
          "payment_method_types": [
            "wechat_pay"
          ],
          "receipt_email": null,
          "review": null,
          "setup_future_usage": null,
          "shipping": null,
          "source": null,
          "statement_descriptor": null,
          "statement_descriptor_suffix": null,
          "status": "requires_action",
          "transfer_data": null,
          "transfer_group": null
        }
        """.trimIndent()
    )

    val PI_REQUIRES_WECHAT_PAY_AUTHORIZE = PARSER.parse(PI_REQUIRES_WECHAT_PAY_AUTHORIZE_JSON)!!

    /**
     * A sample response of PI from refresh endpoint that has status 'requires_action'
     */
    val PI_REFRESH_RESPONSE_REQUIRES_WECHAT_PAY_AUTHORIZE_JSON = JSONObject(
        """
        {
          "id": "pi_3JkCxKBNJ02ErVOj0kNqBMAZ",
          "object": "payment_intent",
          "allowed_source_types": [
            "wechat_pay"
          ],
          "amount": 50,
          "canceled_at": null,
          "cancellation_reason": null,
          "capture_method": "automatic",
          "client_secret": "pi_3JkCxKBNJ02ErVOj0kNqBMAZ_secret_bC6oXqo976LFM06Z9rlhmzUQq",
          "confirmation_method": "automatic",
          "created": 1634152658,
          "currency": "usd",
          "description": "Example PaymentIntent",
          "last_payment_error": null,
          "livemode": true,
          "next_action": {
            "type": "wechat_pay_redirect_to_android_app",
            "wechat_pay_redirect_to_android_app": {
              "app_id": "wx65997d6307c3827d",
              "nonce_str": "osR3zctjDetBvsBN",
              "package": "Sign=WXPay",
              "partner_id": "268716457",
              "prepay_id": "wx140509412586868522820b12f205690000",
              "sign": "7A5981D3347941F4F9AECACF36E467EAE4B7734E47CE4B058E8B2299DAF19E8A",
              "timestamp": "1634159381"
            }
          },
          "next_source_action": {
            "type": "wechat_pay_redirect_to_android_app",
            "wechat_pay_redirect_to_android_app": {
              "app_id": "wx65997d6307c3827d",
              "nonce_str": "osR3zctjDetBvsBN",
              "package": "Sign=WXPay",
              "partner_id": "268716457",
              "prepay_id": "wx140509412586868522820b12f205690000",
              "sign": "7A5981D3347941F4F9AECACF36E467EAE4B7734E47CE4B058E8B2299DAF19E8A",
              "timestamp": "1634159381"
            }
          },
          "payment_method": "pm_1JkEhjBNJ02ErVOjUZ3ekLUL",
          "payment_method_types": [
            "wechat_pay"
          ],
          "receipt_email": null,
          "setup_future_usage": null,
          "shipping": null,
          "source": null,
          "status": "requires_action"
        }
        """.trimIndent()
    )

    val PI_REFRESH_RESPONSE_REQUIRES_WECHAT_PAY_AUTHORIZE =
        PARSER.parse(PI_REFRESH_RESPONSE_REQUIRES_WECHAT_PAY_AUTHORIZE_JSON)!!

    /**
     * A sample response of PI from refresh endpoint that has status 'succeeded'
     */
    val PI_REFRESH_RESPONSE_WECHAT_PAY_SUCCESS_JSON = JSONObject(
        """
        {
          "id": "pi_3JkCxKBNJ02ErVOj0kNqBMAZ",
          "object": "payment_intent",
          "allowed_source_types": [
            "wechat_pay"
          ],
          "amount": 50,
          "canceled_at": null,
          "cancellation_reason": null,
          "capture_method": "automatic",
          "client_secret": "pi_3JkCxKBNJ02ErVOj0kNqBMAZ_secret_bC6oXqo976LFM06Z9rlhmzUQq",
          "confirmation_method": "automatic",
          "created": 1634152658,
          "currency": "usd",
          "description": "Example PaymentIntent",
          "last_payment_error": null,
          "livemode": true,
          "next_action": {
            "type": "wechat_pay_redirect_to_android_app",
            "wechat_pay_redirect_to_android_app": {
              "app_id": "wx65997d6307c3827d",
              "nonce_str": "osR3zctjDetBvsBN",
              "package": "Sign=WXPay",
              "partner_id": "268716457",
              "prepay_id": "wx140509412586868522820b12f205690000",
              "sign": "7A5981D3347941F4F9AECACF36E467EAE4B7734E47CE4B058E8B2299DAF19E8A",
              "timestamp": "1634159381"
            }
          },
          "next_source_action": {
            "type": "wechat_pay_redirect_to_android_app",
            "wechat_pay_redirect_to_android_app": {
              "app_id": "wx65997d6307c3827d",
              "nonce_str": "osR3zctjDetBvsBN",
              "package": "Sign=WXPay",
              "partner_id": "268716457",
              "prepay_id": "wx140509412586868522820b12f205690000",
              "sign": "7A5981D3347941F4F9AECACF36E467EAE4B7734E47CE4B058E8B2299DAF19E8A",
              "timestamp": "1634159381"
            }
          },
          "payment_method": "pm_1JkEhjBNJ02ErVOjUZ3ekLUL",
          "payment_method_types": [
            "wechat_pay"
          ],
          "receipt_email": null,
          "setup_future_usage": null,
          "shipping": null,
          "source": null,
          "status": "succeeded"
        }
        """.trimIndent()
    )

    val PI_REFRESH_RESPONSE_WECHAT_PAY_SUCCESS =
        PARSER.parse(PI_REFRESH_RESPONSE_WECHAT_PAY_SUCCESS_JSON)!!
}
