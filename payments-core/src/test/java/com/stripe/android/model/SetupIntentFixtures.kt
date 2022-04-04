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
                    "exp_year": 2025,
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
}
