package com.stripe.android.model

import com.stripe.android.model.parsers.SetupIntentJsonParser
import org.json.JSONObject

@Suppress("MaxLineLength")
internal object SetupIntentFixtures {
    private val PARSER = SetupIntentJsonParser()

    internal val SI_NEXT_ACTION_REDIRECT_JSON = JSONObject(
        """
        {
            "id": "seti_1EqTSZGMT9dGPIDGVzCUs6dV",
            "object": "setup_intent",
            "cancellation_reason": null,
            "client_secret": "seti_1EqTSZGMT9dGPIDGVzCUs6dV_secret_FL9mS9ILygVyGEOSmVNqHT83rxkqy0Y",
            "created": 1561677666,
            "description": "a description",
            "last_setup_error": null,
            "livemode": false,
            "next_action": {
                "redirect_to_url": {
                    "return_url": "stripe://setup_intent_return",
                    "url": "https://hooks.stripe.com/redirect/authenticate/src_1EqTStGMT9dGPIDGJGPkqE6B?client_secret=src_client_secret_FL9m741mmxtHykDlRTC5aQ02"
                },
                "type": "redirect_to_url"
            },
            "payment_method": "pm_1EqTSoGMT9dGPIDG7dgafX1H",
            "payment_method_types": [
                "card"
            ],
            "status": "requires_action",
            "usage": "off_session"
        }
        """.trimIndent()
    )

    internal val SI_NEXT_ACTION_VERIFY_WITH_MICRODEPOSITS_JSON =
        JSONObject(
            """
        {
            "id": "seti_1Kd5ncLu5o3P18ZpOYpGt5BF",
            "object": "setup_intent",
            "cancellation_reason": null,
            "client_secret": "seti_1Kd5ncLu5o3P18ZpOYpGt5BF_secret_LJjGof4HuzSfxwvNCYP5UhdSQKSC9kS",
            "created": 1647233188,
            "description": "Example SetupIntent",
            "last_setup_error": null,
            "livemode": false,
            "next_action": {
                "type": "verify_with_microdeposits",
                "verify_with_microdeposits": {
                    "arrival_date": 1647327600,
                    "hosted_verification_url": "https://payments.stripe.com/microdeposit/sacs_test_YWNjdF8xSHZUSTdMdTVvM1AxOFpwLHNhX25vbmNlX0xKakc4NzlEYjNZaWxQT09Ma0RaZDROTklPcUVHb2s0000d7kDmkhf",
                    "microdeposit_type": "amounts"
                }
            },
            "payment_method": {
                "id": "pm_1Kd5ndLu5o3P18ZpLVuthxK2",
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
                    "email": "johnny@lawrence.com",
                    "name": "Johnny Lawrence",
                    "phone": null
                },
                "created": 1647233189,
                "customer": null,
                "livemode": false,
                "type": "us_bank_account",
                "us_bank_account": {
                    "account_holder_type": "individual",
                    "account_type": "checking",
                    "bank_name": "STRIPE TEST BANK",
                    "fingerprint": "FFDMA0xfhBjWSZLu",
                    "last4": "6789",
                    "linked_account": null,
                    "networks": {
                        "preferred": "ach",
                        "supported": [
                            "ach"
                        ]
                    },
                    "routing_number": "110000000"
                }
            },
            "payment_method_options": {
                "us_bank_account": {
                    "verification_method": "automatic"
                }
            },
            "payment_method_types": [
                "us_bank_account"
            ],
            "status": "requires_action",
            "usage": "off_session"
        }
            """.trimIndent()
        )

    internal val SI_WITH_LAST_PAYMENT_ERROR = requireNotNull(
        PARSER.parse(
            JSONObject(
                """
        {
            "id": "seti_1EqTSZGMT9dGPIDGVzCUs6dV",
            "object": "setup_intent",
            "cancellation_reason": null,
            "client_secret": "seti_1EqTSZGMT9dGPIDGVzCUs6dV_secret_FL9mS9ILygVyGEOSmVNqHT83rxkqy0Y",
            "created": 1561677666,
            "description": "a description",
            "last_setup_error": {
                "code": "setup_intent_authentication_failure",
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
            "next_action": {
                "redirect_to_url": {
                    "return_url": "stripe://setup_intent_return",
                    "url": "https://hooks.stripe.com/redirect/authenticate/src_1EqTStGMT9dGPIDGJGPkqE6B?client_secret=src_client_secret_FL9m741mmxtHykDlRTC5aQ02"
                },
                "type": "redirect_to_url"
            },
            "payment_method": "pm_1EqTSoGMT9dGPIDG7dgafX1H",
            "payment_method_types": [
                "card"
            ],
            "status": "requires_action",
            "usage": "off_session"
        }
                """.trimIndent()
            )
        )
    )

    internal val CANCELLED = requireNotNull(
        PARSER.parse(
            JSONObject(
                """
        {
            "id": "seti_1FCoS9CRMbs6FrXfxFQOp8Mm",
            "object": "setup_intent",
            "application": null,
            "cancellation_reason": "abandoned",
            "client_secret": "seti_1FCoS9CRMbs6FrXfxFQOp8Mm_secret_FiEwNDtwMi",
            "created": 1567088301,
            "customer": "cus_FWhpaTLIPWLhpJ",
            "description": null,
            "last_setup_error": null,
            "livemode": false,
            "metadata": {},
            "next_action": null,
            "on_behalf_of": null,
            "payment_method": "pm_1F1wa2CRMbs6FrXfm9XfWrGS",
            "payment_method_options": {
                "card": {
                    "request_three_d_secure": "automatic"
                }
            },
            "payment_method_types": [
                "card"
            ],
            "status": "canceled",
            "usage": "off_session"
        }
                """.trimIndent()
            )
        )
    )

    val SI_WITH_LAST_SETUP_ERROR = PARSER.parse(
        JSONObject(
            """
        {
            "id": "si_1F7J1aCRMbs6FrXfaJcvbxF6",
            "object": "setup_intent",
            "amount": 1000,
            "capture_method": "manual",
            "client_secret": "pi_1F7J1aCRMbs6FrXfaJcvbxF6_secret_mIuDLsSfoo1m6s",
            "confirmation_method": "automatic",
            "created": 1565775850,
            "currency": "usd",
            "description": "Example SetupIntent",
            "last_setup_error": {
                "code": "card_declined",
                "doc_url": "https://stripe.com/docs/error-codes",
                "message": "Error message.",
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
            "payment_method": null,
            "payment_method_types": [
                "card"
            ],
            "receipt_email": null,
            "source": null,
            "status": "requires_payment_method"
        }
            """.trimIndent()
        )
    )!!

    internal val SI_SUCCEEDED = requireNotNull(
        PARSER.parse(
            JSONObject(
                """
        {
            "id": "seti_1FCoS9CRMbs6FrXfxFQOp8Mm",
            "object": "setup_intent",
            "application": null,
            "cancellation_reason": "abandoned",
            "client_secret": "seti_1FCoS9CRMbs6FrXfxFQOp8Mm_secret_FiEwNDtwMi",
            "created": 1567088301,
            "customer": "cus_FWhpaTLIPWLhpJ",
            "description": null,
            "last_setup_error": null,
            "livemode": false,
            "metadata": {},
            "next_action": null,
            "on_behalf_of": null,
            "payment_method": "pm_1F1wa2CRMbs6FrXfm9XfWrGS",
            "payment_method_options": {
                "card": {
                    "request_three_d_secure": "automatic"
                }
            },
            "payment_method_types": [
                "card"
            ],
            "status": "succeeded",
            "usage": "off_session"
        }
                """.trimIndent()
            )
        )
    )

    internal val SI_REQUIRES_PAYMENT_METHOD_JSON =
        JSONObject(
            """
        {
            "id": "seti_1GSmaFCRMbs",
            "object": "setup_intent",
            "cancellation_reason": null,
            "client_secret": "seti_1GSmaFCRMbs6FrXfmjThcHan_secret_H0oC2iSB4FtW4d",
            "created": 1585670699,
            "description": null,
            "last_setup_error": null,
            "livemode": false,
            "payment_method": null,
            "payment_method_types": [
                "card"
            ],
            "status": "requires_payment_method",
            "usage": "off_session"
        }
            """.trimIndent()
        )

    internal val SI_REQUIRES_PAYMENT_METHOD =
        requireNotNull(PARSER.parse(SI_REQUIRES_PAYMENT_METHOD_JSON))

    internal val EXPANDED_PAYMENT_METHOD = JSONObject(
        """
        {
            "id": "seti_1GSmaFCRMbs",
            "object": "setup_intent",
            "cancellation_reason": null,
            "client_secret": "seti_1GSmaFCRMbs6FrXfmjThcHan_secret_H0oC2iSB4FtW4d",
            "created": 1585670699,
            "description": null,
            "last_setup_error": null,
            "livemode": false,
            "payment_method": {
                "id": "pm_1GSmaGCRMbs6F",
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
                "created": 1585670700,
                "customer": null,
                "livemode": false,
                "metadata": {},
                "type": "card"
            },
            "payment_method_types": ["card"],
            "status": "requires_action",
            "usage": "off_session"
        }
        """.trimIndent()
    )

    val SI_NEXT_ACTION_REDIRECT = requireNotNull(
        PARSER.parse(SI_NEXT_ACTION_REDIRECT_JSON)
    )

    val SI_NEXT_ACTION_VERIFY_WITH_MICRODEPOSITS = requireNotNull(
        PARSER.parse(SI_NEXT_ACTION_VERIFY_WITH_MICRODEPOSITS_JSON)
    )

    val SI_WITH_US_BANK_ACCOUNT_VERIFY_COMPLETED_JSON = JSONObject(
        """
            {
                "id": "seti_3KbV27Lu5o3P18Zp1e7NOonG",
                "object": "setup_intent",
                "client_secret": "seti_3KbV27Lu5o3P18Zp1e7NOonG_secret_9PqgvdhyLNub0UvzjFf9bQKqd",
                "last_payment_error": null,
                "livemode": false,
                "next_action": null,
                "status": "processing",
                "amount": 6099,
                "automatic_payment_methods": null,
                "canceled_at": null,
                "cancellation_reason": null,
                "capture_method": "automatic",
                "confirmation_method": "automatic",
                "created": 1646853531,
                "currency": "usd",
                "description": "Example SetupIntent",
                "payment_method": "pm_1KbV27Lu5o3P18ZpAEIGorq8",
                "payment_method_options": { },
                "payment_method_types": [
                    "us_bank_account"
                ],
                "processing": null,
                "receipt_email": null,
                "setup_future_usage": "off_session",
                "shipping": null,
                "source": null
            }
        """.trimIndent()
    )

    val SI_WITH_US_BANK_ACCOUNT_VERIFY_COMPLETED = PARSER.parse(SI_WITH_US_BANK_ACCOUNT_VERIFY_COMPLETED_JSON)!!

    val SI_3DS2_SUCCEEDED_JSON = JSONObject(
        """
            {
                "id": "seti_1L9F4bLu5o3P18Zp0IGLYrNZ",
                "object": "setup_intent",
                "application": null,
                "cancellation_reason": null,
                "client_secret": "seti_1L9F4bLu5o3P18Zp0IGLYrNZ_secret_Lqwy13NMvtzx88afOLNndJRei6rkl8H",
                "created": 1654895333,
                "customer": "cus_LqvlPQBk2QUmXl",
                "description": null,
                "flow_directions": null,
                "last_setup_error": null,
                "latest_attempt": "setatt_1L9F4oLu5o3P18Zp8eQa9Mn2",
                "livemode": false,
                "mandate": null,
                "metadata": {},
                "next_action": null,
                "on_behalf_of": null,
                "payment_method": {
                    "id": "pm_1L9F4oLu5o3P18ZpahIpZYFz",
                    "object": "payment_method",
                    "billing_details": {
                        "address": {
                            "city": null,
                            "country": "IE",
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
                            "cvc_check": "pass"
                        },
                        "country": "IE",
                        "exp_month": 6,
                        "exp_year": 2066,
                        "fingerprint": "UrRvw6ZlmgRswqMC",
                        "funding": "credit",
                        "generated_from": null,
                        "last4": "3220",
                        "networks": {
                            "available": [
                                "visa"
                            ],
                            "preferred": null
                        },
                        "three_d_secure_usage": {
                            "supported": true
                        },
                        "wallet": null
                    },
                    "created": 1654895346,
                    "customer": "cus_LqvlPQBk2QUmXl",
                    "livemode": false,
                    "metadata": {},
                    "type": "card"
                },
                "payment_method_options": {
                    "card": {
                        "mandate_options": null,
                        "network": null,
                        "request_three_d_secure": "automatic"
                    },
                    "us_bank_account": {
                        "verification_method": "automatic"
                    }
                },
                "payment_method_types": [
                    "card",
                    "us_bank_account",
                    "link"
                ],
                "single_use_mandate": null,
                "status": "succeeded",
                "usage": "off_session"
            }
        """.trimIndent()
    )

    val SI_3DS2_SUCCEEDED = PARSER.parse(SI_3DS2_SUCCEEDED_JSON)!!

    val SI_3DS2_PROCESSING_JSON = JSONObject(
        """
            {
                "id": "seti_1L9F4bLu5o3P18Zp0IGLYrNZ",
                "object": "setup_intent",
                "application": null,
                "cancellation_reason": null,
                "client_secret": "seti_1L9F4bLu5o3P18Zp0IGLYrNZ_secret_Lqwy13NMvtzx88afOLNndJRei6rkl8H",
                "created": 1654895333,
                "customer": "cus_LqvlPQBk2QUmXl",
                "description": null,
                "flow_directions": null,
                "last_setup_error": null,
                "latest_attempt": "setatt_1L9F4oLu5o3P18Zp8eQa9Mn2",
                "livemode": false,
                "mandate": null,
                "metadata": {},
                "next_action": {
                  "type": "use_stripe_sdk",
                  "use_stripe_sdk": {
                    "type": "stripe_3ds2_fingerprint",
                    "merchant": "acct_1G4B0ILXscDhLXgF",
                    "three_d_secure_2_source": "src_1LOQYGLXscDhLXgFfh401OjG",
                    "directory_server_name": "visa",
                    "server_transaction_id": "525ba092-6758-45f2-8155-01078d8985f7",
                    "three_ds_method_url": "",
                    "three_ds_optimizations": "kf",
                    "directory_server_encryption": {
                      "directory_server_id": "A000000003",
                      "algorithm": "RSA",
                      "certificate": "-----BEGIN CERTIFICATE-----\nMIIGAzCCA+ugAwIBAgIQDaAlB1IbPwgx5esGu9tLIjANBgkqhkiG9w0BAQsFADB2\nMQswCQYDVQQGEwJVUzENMAsGA1UECgwEVklTQTEvMC0GA1UECwwmVmlzYSBJbnRl\ncm5hdGlvbmFsIFNlcnZpY2UgQXNzb2NpYXRpb24xJzAlBgNVBAMMHlZpc2EgZUNv\nbW1lcmNlIElzc3VpbmcgQ0EgLSBHMjAeFw0yMTA4MjMxNTMyMzNaFw0yNDA4MjIx\nNTMyMzNaMIGhMRgwFgYDVQQHDA9IaWdobGFuZHMgUmFuY2gxETAPBgNVBAgMCENv\nbG9yYWRvMQswCQYDVQQGEwJVUzENMAsGA1UECgwEVklTQTEvMC0GA1UECwwmVmlz\nYSBJbnRlcm5hdGlvbmFsIFNlcnZpY2UgQXNzb2NpYXRpb24xJTAjBgNVBAMMHDNk\nczIucnNhLmVuY3J5cHRpb24udmlzYS5jb20wggEiMA0GCSqGSIb3DQEBAQUAA4IB\nDwAwggEKAoIBAQCy34cZ88+xfenoccRD1jOi6uVCPXo2xyabXcKntxl7h1kHahac\nmpnuiH+kSgSg4DEHDXHg0WBcpMp0cB67dUE1XDxLAxN0gL5fXpVX7dUjI9tS8lcW\nndChHxZTA8HcXUtv1IwU1L3luhgNkog509bRw/V1GLukW6CwFRkMI/8fecV8EUcw\nIGiBr4/cAcaPnLxFWm/SFL2NoixiNf6LnwHrU4YIHsPQCIAM1km4XPDb7Gk2S3o0\nkkXroU87yoiHzFHbEZUN/tO0Juyz8K6AtGBKoppv1hEHz9MFNzLlvGPo7wcPpovb\nMYtwxj10KhtfEKh0sS0yMl1Uvw36JmuwjaC3AgMBAAGjggFfMIIBWzAMBgNVHRMB\nAf8EAjAAMB8GA1UdIwQYMBaAFL0nYyikrlS3yCO3wTVCF+nGeF+FMGcGCCsGAQUF\nBwEBBFswWTAwBggrBgEFBQcwAoYkaHR0cDovL2Vucm9sbC52aXNhY2EuY29tL2VD\nb21tRzIuY3J0MCUGCCsGAQUFBzABhhlodHRwOi8vb2NzcC52aXNhLmNvbS9vY3Nw\nMEYGA1UdIAQ/MD0wMQYIKwYBBQUHAgEwJTAjBggrBgEFBQcCARYXaHR0cDovL3d3\ndy52aXNhLmNvbS9wa2kwCAYGZ4EDAQEBMBMGA1UdJQQMMAoGCCsGAQUFBwMCMDUG\nA1UdHwQuMCwwKqAooCaGJGh0dHA6Ly9lbnJvbGwudmlzYWNhLmNvbS9lQ29tbUcy\nLmNybDAdBgNVHQ4EFgQU/JtqQ7VLWNd3/9zQjpnsR2rz+cwwDgYDVR0PAQH/BAQD\nAgSwMA0GCSqGSIb3DQEBCwUAA4ICAQBYOGCI/bYG2gmLgh7UXg5qrt4xeDYe4RXe\n5xSjFkTelNvdf+KykB+oQzw8ZobIY+pKsPihM6IrtoJQuzOLXPV5L9U4j1qa/NZB\nGZTXFMwKGN/v0/tAj3h8wefcLPWb15RsXEpZmA87ollezpXeEHXPhFIit7cHoG5P\nfem9yMuDISI97qbnIKNtFENJr+fMkWIykQ0QnkM1rt99Yv2ZE4GWZN7VJ0zXFqOF\nNF2IVwnTIZ21eDiCOjQr6ohq7bChDMelB5XvEuhfe400DqDP+e5pPHo81ecXkjJK\ngS5grYYZIbeDBdQL1Cgs1mGu6On8ecr0rcpRlQh++BySg9MKkzJdLt1vsYmxfrfb\nkUaLglTdYAU2nYaOEDR4NvkRxfzegXyXkOqfPTmfkrg+OB0LeuICITJGJ0cuZD5W\nGUNaT9WruEANBRJNVjSX1UeJUnCpz4nitT1ml069ONjEowyWUcKvTr4/nrargv2R\npOD4RPJMti6kG+bm9OeATiSgVNmO5lkAS4AkOop2IcbRFcVKJUTOhx2Q37L4nuAH\nTCXQ9vwT4yWz6fVaCfL/FTvCGMilLPzXC/00OPA2ZtWvClvFh/uHJBjRUnj6WXp3\nO9p9uHfdV9eKJH37k94GUSMjBKQ6aIru1VUvSOmUPrDz5JbQB7bP+IzUaFHeweZX\nOWumZmyGDw==\n-----END CERTIFICATE-----\n",
                      "root_certificate_authorities": [
                        "-----BEGIN CERTIFICATE-----\nMIIDojCCAoqgAwIBAgIQE4Y1TR0/BvLB+WUF1ZAcYjANBgkqhkiG9w0BAQUFADBr\nMQswCQYDVQQGEwJVUzENMAsGA1UEChMEVklTQTEvMC0GA1UECxMmVmlzYSBJbnRl\ncm5hdGlvbmFsIFNlcnZpY2UgQXNzb2NpYXRpb24xHDAaBgNVBAMTE1Zpc2EgZUNv\nbW1lcmNlIFJvb3QwHhcNMDIwNjI2MDIxODM2WhcNMjIwNjI0MDAxNjEyWjBrMQsw\nCQYDVQQGEwJVUzENMAsGA1UEChMEVklTQTEvMC0GA1UECxMmVmlzYSBJbnRlcm5h\ndGlvbmFsIFNlcnZpY2UgQXNzb2NpYXRpb24xHDAaBgNVBAMTE1Zpc2EgZUNvbW1l\ncmNlIFJvb3QwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCvV95WHm6h\n2mCxlCfLF9sHP4CFT8icttD0b0/Pmdjh28JIXDqsOTPHH2qLJj0rNfVIsZHBAk4E\nlpF7sDPwsRROEW+1QK8bRaVK7362rPKgH1g/EkZgPI2h4H3PVz4zHvtH8aoVlwdV\nZqW1LS7YgFmypw23RuwhY/81q6UCzyr0TP579ZRdhE2o8mCP2w4lPJ9zcc+U30rq\n299yOIzzlr3xF7zSujtFWsan9sYXiwGd/BmoKoMWuDpI/k4+oKsGGelT84ATB+0t\nvz8KPFUgOSwsAGl0lUq8ILKpeeUYiZGo3BxN77t+Nwtd/jmliFKMAGzsGHxBvfaL\ndXe6YJ2E5/4tAgMBAAGjQjBAMA8GA1UdEwEB/wQFMAMBAf8wDgYDVR0PAQH/BAQD\nAgEGMB0GA1UdDgQWBBQVOIMPPyw/cDMezUb+B4wg4NfDtzANBgkqhkiG9w0BAQUF\nAAOCAQEAX/FBfXxcCLkr4NWSR/pnXKUTwwMhmytMiUbPWU3J/qVAtmPN3XEolWcR\nzCSs00Rsca4BIGsDoo8Ytyk6feUWYFN4PMCvFYP3j1IzJL1kk5fui/fbGKhtcbP3\nLBfQdCVp9/5rPJS+TUtBjE7ic9DjkCJzQ83z7+pzzkWKsKZJ/0x9nXGIxHYdkFsd\n7v3M9+79YKWxehZx0RbQfBI8bGmX265fOZpwLwU8GUYEmSA20GBuYQa7FkKMcPcw\n++DbZqMAAb3mLNqRX6BGi01qnD093QVG/na/oAo85ADmJ7f/hC3euiInlhBx6yLt\n398znM/jra6O1I7mT1GvFpLgXPYHDw==\n-----END CERTIFICATE-----\n",
                        "-----BEGIN CERTIFICATE-----\nMIIFqTCCA5GgAwIBAgIPUT6WAAAA20Qn7qzgvuFIMA0GCSqGSIb3DQEBCwUAMG8x\nCzAJBgNVBAYTAlVTMQ0wCwYDVQQKDARWSVNBMS8wLQYDVQQLDCZWaXNhIEludGVy\nbmF0aW9uYWwgU2VydmljZSBBc3NvY2lhdGlvbjEgMB4GA1UEAwwXVmlzYSBQdWJs\naWMgUlNBIFJvb3QgQ0EwHhcNMjEwMzE2MDAwMDAwWhcNNDEwMzE1MDAwMDAwWjBv\nMQswCQYDVQQGEwJVUzENMAsGA1UECgwEVklTQTEvMC0GA1UECwwmVmlzYSBJbnRl\ncm5hdGlvbmFsIFNlcnZpY2UgQXNzb2NpYXRpb24xIDAeBgNVBAMMF1Zpc2EgUHVi\nbGljIFJTQSBSb290IENBMIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEA\n2WEbXLS3gI6LOY93bP7Kz6EO9L1QXlr8l+fTkJWZldJ6QuwZ1cv4369tfjeJ8O5w\nSJiDcVw7eNdOP73LfAtwHlTnUnb0e9ILTTipc5bkNnAevocrJACsrpiQ8jBI9ttp\ncqKUeJgzW4Ie25ypirKroVD42b4E0iICK2cZ5QfD4BSzUnftp4Bqh8AfpGvG1lre\nCaD53qrsy5SUadY/NaeUGOkqdPvDSNoDIdrbExwnZaSFUmjQT1svKwMqGo2GFrgJ\n4cULEp4NNj5rga8YTTZ7Xo5MblHrLpSPOmJev30KWi/BcbvtCNYNWBTg7UMzP3cK\nMQ1pGLvG2PgvFTZSRvH3QzngJRgrDYYOJ6kj9ave+6yOOFqj80ZCuH0Nugt2mMS3\nc3+Nksaw+6H3cQPsE/Gv5zjfsKleRhEFtE1gyrdUg1DMgu8o/YhKM7FAqkXUn74z\nwoRFgx3Mi5OaGTQbg+NlwJgR4sVHXCV4s9b8PjneLhzWMn353SFARF9dnO7LDBqq\ntT6WltJu1z9x2Ze0UVNZvxKGcyCkLody29O8j9/MGZ8SOSUu4U6NHrebKuuf9Fht\nn6PqQ4ppkhy6sReXeV5NVGfVpDYY5ZAKEWqTYgMULWpQ2Py4BGpFzBe07jXkyulR\npoKvz14iXeA0oq16c94DrFYX0jmrWLeU4a/TCZQLFIsCAwEAAaNCMEAwHQYDVR0O\nBBYEFEtNpg77oBHorQvi8PMKAC+sixb7MA8GA1UdEwEB/wQFMAMBAf8wDgYDVR0P\nAQH/BAQDAgEGMA0GCSqGSIb3DQEBCwUAA4ICAQC5BU9qQSZYPcgCp2x0Juq59kMm\nXuBly094DaEnPqvtCgwwAirkv8x8/QSOxiWWiu+nveyuR+j6Gz/fJaV4u+J5QEDy\ncfk605Mw3HIcJOeZvDgk1eyOmQwUP6Z/BdQTNJmZ92Z8dcG5yWCxLBrqPH7ro3Ss\njhYq9duIJU7jfizCJCN4W8tp0D2pWBe1/CYNswP4GMs5jQ5+ZQKN/L5JFdwVTu7X\nPt8b5zfgbmmQpVmUn0oFwm3OI++Z6gEpNmW5bd/2oUIZoG96Qff2fauVMAYiWQvN\nnL3y1gkRguTOSMVUCCiGfdvwu5ygowillvV2nHb7+YibQ9N5Z2spP0o9Zlfzoat2\n7WFpyK47TiUdu/4toarLKGZP+hbA/F4xlnM/8EfZkE1DeTTI0lhN3O8yEsHrtRl1\nOuQZ/IexHO8UGU6jvn4TWo10HYeXzrGckL7oIXfGTrjPzfY62T5HDW/BAEZS+9Tk\nijz25YM0fPPz7IdlEG+k4q4YwZ82j73Y9kDEM5423mrWorq/Bq7I5Y8v0LTY9GWH\nYrpElYf0WdOXAbsfwQiT6qnRio+p82VyqlY8Jt6VVA6CDy/iHKwcj1ELEnDQfVv9\nhedoxmnQ6xe/nK8czclu9hQJRv5Lh9gk9Q8DKK2nmgzZ8SSQ+lr3mSSeY8JOMRlE\n+RKdOQIChWthTJKh7w==\n-----END CERTIFICATE-----\n"
                      ]
                    },
                    "one_click_authn": null
                  }
                },
                "on_behalf_of": null,
                "payment_method": {
                    "id": "pm_1L9F4oLu5o3P18ZpahIpZYFz",
                    "object": "payment_method",
                    "billing_details": {
                        "address": {
                            "city": null,
                            "country": "IE",
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
                            "cvc_check": "pass"
                        },
                        "country": "IE",
                        "exp_month": 6,
                        "exp_year": 2066,
                        "fingerprint": "UrRvw6ZlmgRswqMC",
                        "funding": "credit",
                        "generated_from": null,
                        "last4": "3220",
                        "networks": {
                            "available": [
                                "visa"
                            ],
                            "preferred": null
                        },
                        "three_d_secure_usage": {
                            "supported": true
                        },
                        "wallet": null
                    },
                    "created": 1654895346,
                    "customer": "cus_LqvlPQBk2QUmXl",
                    "livemode": false,
                    "metadata": {},
                    "type": "card"
                },
                "payment_method_options": {
                    "card": {
                        "mandate_options": null,
                        "network": null,
                        "request_three_d_secure": "automatic"
                    },
                    "us_bank_account": {
                        "verification_method": "automatic"
                    }
                },
                "payment_method_types": [
                    "card",
                    "us_bank_account",
                    "link"
                ],
                "single_use_mandate": null,
                "status": "processing",
                "usage": "off_session"
            }
        """.trimIndent()
    )

    val SI_3DS2_PROCESSING = PARSER.parse(SI_3DS2_PROCESSING_JSON)!!

    internal val SI_WITH_COUNTRY_CODE = JSONObject(
        """
        {
            "id": "seti_1EqTSZGMT9dGPIDGVzCUs6dV",
            "object": "setup_intent",
            "cancellation_reason": null,
            "client_secret": "seti_1EqTSZGMT9dGPIDGVzCUs6dV_secret_FL9mS9ILygVyGEOSmVNqHT83rxkqy0Y",
            "created": 1561677666,
            "description": "a description",
            "last_setup_error": null,
            "livemode": false,
            "payment_method": "pm_1EqTSoGMT9dGPIDG7dgafX1H",
            "payment_method_types": [
                "card"
            ],
            "status": "requires_action",
            "usage": "off_session",
            "country_code": "CA"
        }
        """.trimIndent()
    )

    val CASH_APP_PAY_REQUIRES_ACTION_JSON by lazy {
        JSONObject(
            """
            {
              "id": "seti_1234",
              "object": "setup_intent",
              "cancellation_reason": null,
              "client_secret": "seti_1234_secret_5678",
              "created": 1561677666,
              "description": "a description",
              "last_setup_error": null,
              "livemode": false,
              "next_action": {
                "cashapp_handle_redirect_or_display_qr_code": {
                  "hosted_instructions_url": "https://payments.stripe.com/cashapp/instructions/CCUaFwoVYWNjdF8xSHZUSTdMdTVvM1AxOFpwKJaloJ4GMgZcsN7zB3I6L2wvyPfW8B6gy0_BsHb7Q21FYoKjIGxNvVsVYjJ6pbAIw_28VE2MVWcJQHMaEObM",
                  "mobile_auth_url": "https://pm-redirects.stripe.com/authorize/acct_1HvTI7Lu5o3P18Zp/pa_nonce_NC1mezV544wpYmFaXyJpnleeurKO3TZ",
                  "qr_code": {
                    "expires_at": 1674056362,
                    "image_url_png": "https://qr.stripe.com/test_YWNjdF8xSHZUSTdMdTVvM1AxOFpwLF9OQzFtSUpKYkMwaVBZTldRMW5BelF0OWNiWkk3a25o0100dYmSjOJt.png",
                    "image_url_svg": "https://qr.stripe.com/test_YWNjdF8xSHZUSTdMdTVvM1AxOFpwLF9OQzFtSUpKYkMwaVBZTldRMW5BelF0OWNiWkk3a25o0100dYmSjOJt.svg"
                  }
                },
                "type": "cashapp_handle_redirect_or_display_qr_code"
              },
              "payment_method": "pm_1MRdj7Lu5o3P18Zp41wd191i",
              "payment_method_types": [
                "cashapp"
              ],
              "status": "requires_action",
              "usage": "off_session"
            }
            """.trimIndent()
        )
    }
}
