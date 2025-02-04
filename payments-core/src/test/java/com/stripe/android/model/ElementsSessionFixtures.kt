package com.stripe.android.model

import org.json.JSONObject

@Suppress("LargeClass")
internal object ElementsSessionFixtures {
    val EXPANDED_PAYMENT_INTENT_JSON = JSONObject(
        """
        {
          "business_name": "Mybusiness",
          "link_settings": {
            "link_bank_enabled": false,
            "link_bank_onboarding_enabled": false
          },
          "merchant_country": "US",
          "payment_method_preference": {
            "object": "payment_method_preference",
            "country_code": "US",
            "ordered_payment_method_types": [
              "card",
              "ideal",
              "sepa_debit",
              "bancontact",
              "sofort"
            ],
            "payment_intent": {
              "id": "pi_3JTDhYIyGgrkZxL71IDUGKps",
              "object": "payment_intent",
              "amount": 973,
              "canceled_at": null,
              "cancellation_reason": null,
              "capture_method": "automatic",
              "client_secret": "pi_3JTDhYIyGgrkZxL71IDUGKps_secret_aWuzwD4JvF1HM8XJTdUsXG6Za",
              "confirmation_method": "automatic",
              "created": 1630103948,
              "currency": "eur",
              "description": null,
              "last_payment_error": null,
              "livemode": false,
              "next_action": null,
              "payment_method": null,
              "payment_method_types": [
                "bancontact",
                "card",
                "sepa_debit",
                "sofort",
                "ideal"
              ],
              "receipt_email": null,
              "setup_future_usage": null,
              "shipping": {
                "address": {
                  "city": "San Francisco",
                  "country": "US",
                  "line1": "510 Townsend St",
                  "line2": null,
                  "postal_code": "94102",
                  "state": "California"
                },
                "carrier": null,
                "name": "Bruno",
                "phone": null,
                "tracking_number": null
              },
              "source": null,
              "status": "requires_payment_method"
            },
            "type": "payment_intent"
          }
        }
        """.trimIndent()
    )

    val EXPANDED_PAYMENT_INTENT_WITH_LINK_INCENTIVE_JSON = JSONObject(
        """
        {
          "business_name": "Mybusiness",
          "link_settings": {
            "link_bank_enabled": false,
            "link_bank_onboarding_enabled": false,
            "link_consumer_incentive": {
              incentive_params: {
                payment_method: "link_instant_debits"
              },
              incentive_display_text: "$5"
            }
          },
          "merchant_country": "US",
          "payment_method_preference": {
            "object": "payment_method_preference",
            "country_code": "US",
            "ordered_payment_method_types": [
              "card",
              "ideal",
              "sepa_debit",
              "bancontact",
              "sofort"
            ],
            "payment_intent": {
              "id": "pi_3JTDhYIyGgrkZxL71IDUGKps",
              "object": "payment_intent",
              "amount": 973,
              "canceled_at": null,
              "cancellation_reason": null,
              "capture_method": "automatic",
              "client_secret": "pi_3JTDhYIyGgrkZxL71IDUGKps_secret_aWuzwD4JvF1HM8XJTdUsXG6Za",
              "confirmation_method": "automatic",
              "created": 1630103948,
              "currency": "eur",
              "description": null,
              "last_payment_error": null,
              "livemode": false,
              "next_action": null,
              "payment_method": null,
              "payment_method_types": [
                "bancontact",
                "card",
                "sepa_debit",
                "sofort",
                "ideal"
              ],
              "receipt_email": null,
              "setup_future_usage": null,
              "shipping": {
                "address": {
                  "city": "San Francisco",
                  "country": "US",
                  "line1": "510 Townsend St",
                  "line2": null,
                  "postal_code": "94102",
                  "state": "California"
                },
                "carrier": null,
                "name": "Bruno",
                "phone": null,
                "tracking_number": null
              },
              "source": null,
              "status": "requires_payment_method"
            },
            "type": "payment_intent"
          }
        }
        """.trimIndent()
    )

    val EXPANDED_PAYMENT_INTENT_WITH_NATIVE_LINK_FLAGS_ENABLED_JSON = JSONObject(
        """
        {
          "business_name": "Mybusiness",
          "link_settings": {
            "link_bank_enabled": false,
            "link_bank_onboarding_enabled": false,
            "link_mobile_use_attestation_endpoints": true,
            "link_mobile_suppress_2fa_modal": true
          },
          "merchant_country": "US",
          "payment_method_preference": {
            "object": "payment_method_preference",
            "country_code": "US",
            "ordered_payment_method_types": [
              "card",
              "ideal",
              "sepa_debit",
              "bancontact",
              "sofort"
            ],
            "payment_intent": {
              "id": "pi_3JTDhYIyGgrkZxL71IDUGKps",
              "object": "payment_intent",
              "amount": 973,
              "canceled_at": null,
              "cancellation_reason": null,
              "capture_method": "automatic",
              "client_secret": "pi_3JTDhYIyGgrkZxL71IDUGKps_secret_aWuzwD4JvF1HM8XJTdUsXG6Za",
              "confirmation_method": "automatic",
              "created": 1630103948,
              "currency": "eur",
              "description": null,
              "last_payment_error": null,
              "livemode": false,
              "next_action": null,
              "payment_method": null,
              "payment_method_types": [
                "bancontact",
                "card",
                "sepa_debit",
                "sofort",
                "ideal"
              ],
              "receipt_email": null,
              "setup_future_usage": null,
              "shipping": {
                "address": {
                  "city": "San Francisco",
                  "country": "US",
                  "line1": "510 Townsend St",
                  "line2": null,
                  "postal_code": "94102",
                  "state": "California"
                },
                "carrier": null,
                "name": "Bruno",
                "phone": null,
                "tracking_number": null
              },
              "source": null,
              "status": "requires_payment_method"
            },
            "type": "payment_intent"
          }
        }
        """.trimIndent()
    )

    val EXPANDED_PAYMENT_INTENT_WITH_NATIVE_LINK_FLAGS_DISABLED_JSON = JSONObject(
        """
        {
          "business_name": "Mybusiness",
          "link_settings": {
            "link_bank_enabled": false,
            "link_bank_onboarding_enabled": false,
            "link_mobile_use_attestation_endpoints": false,
            "link_mobile_suppress_2fa_modal": false
          },
          "merchant_country": "US",
          "payment_method_preference": {
            "object": "payment_method_preference",
            "country_code": "US",
            "ordered_payment_method_types": [
              "card",
              "ideal",
              "sepa_debit",
              "bancontact",
              "sofort"
            ],
            "payment_intent": {
              "id": "pi_3JTDhYIyGgrkZxL71IDUGKps",
              "object": "payment_intent",
              "amount": 973,
              "canceled_at": null,
              "cancellation_reason": null,
              "capture_method": "automatic",
              "client_secret": "pi_3JTDhYIyGgrkZxL71IDUGKps_secret_aWuzwD4JvF1HM8XJTdUsXG6Za",
              "confirmation_method": "automatic",
              "created": 1630103948,
              "currency": "eur",
              "description": null,
              "last_payment_error": null,
              "livemode": false,
              "next_action": null,
              "payment_method": null,
              "payment_method_types": [
                "bancontact",
                "card",
                "sepa_debit",
                "sofort",
                "ideal"
              ],
              "receipt_email": null,
              "setup_future_usage": null,
              "shipping": {
                "address": {
                  "city": "San Francisco",
                  "country": "US",
                  "line1": "510 Townsend St",
                  "line2": null,
                  "postal_code": "94102",
                  "state": "California"
                },
                "carrier": null,
                "name": "Bruno",
                "phone": null,
                "tracking_number": null
              },
              "source": null,
              "status": "requires_payment_method"
            },
            "type": "payment_intent"
          }
        }
        """.trimIndent()
    )

    val EXPANDED_PAYMENT_INTENT_WITH_LINK_ATTESTATION_ENDPOINTS_MISSING_JSON = JSONObject(
        """
        {
          "business_name": "Mybusiness",
          "link_settings": {
            "link_bank_enabled": false,
            "link_bank_onboarding_enabled": false
          },
          "merchant_country": "US",
          "payment_method_preference": {
            "object": "payment_method_preference",
            "country_code": "US",
            "ordered_payment_method_types": [
              "card",
              "ideal",
              "sepa_debit",
              "bancontact",
              "sofort"
            ],
            "payment_intent": {
              "id": "pi_3JTDhYIyGgrkZxL71IDUGKps",
              "object": "payment_intent",
              "amount": 973,
              "canceled_at": null,
              "cancellation_reason": null,
              "capture_method": "automatic",
              "client_secret": "pi_3JTDhYIyGgrkZxL71IDUGKps_secret_aWuzwD4JvF1HM8XJTdUsXG6Za",
              "confirmation_method": "automatic",
              "created": 1630103948,
              "currency": "eur",
              "description": null,
              "last_payment_error": null,
              "livemode": false,
              "next_action": null,
              "payment_method": null,
              "payment_method_types": [
                "bancontact",
                "card",
                "sepa_debit",
                "sofort",
                "ideal"
              ],
              "receipt_email": null,
              "setup_future_usage": null,
              "shipping": {
                "address": {
                  "city": "San Francisco",
                  "country": "US",
                  "line1": "510 Townsend St",
                  "line2": null,
                  "postal_code": "94102",
                  "state": "California"
                },
                "carrier": null,
                "name": "Bruno",
                "phone": null,
                "tracking_number": null
              },
              "source": null,
              "status": "requires_payment_method"
            },
            "type": "payment_intent"
          }
        }
        """.trimIndent()
    )

    val EXPANDED_SETUP_INTENT_JSON = JSONObject(
        """
        {
          "business_name": "Mybusiness",
          "link_settings": {
            "link_bank_enabled": false,
            "link_bank_onboarding_enabled": false
          },
          "merchant_country": "US",
          "payment_method_preference": {
            "object": "payment_method_preference",
            "country_code": "US",
            "ordered_payment_method_types": [
              "card",
              "ideal",
              "sepa_debit",
              "bancontact",
              "sofort"
            ],
            "setup_intent": {
              "id": "seti_1JTDqGIyGgrkZxL7reCXkpr5",
              "object": "setup_intent",
              "cancellation_reason": null,
              "client_secret": "seti_1JTDqGIyGgrkZxL7reCXkpr5_secret_K7SlVPncyaU4cdiDyyHfyqNjvCDvYxG",
              "created": 1630104488,
              "description": null,
              "last_setup_error": null,
              "livemode": false,
              "next_action": null,
              "payment_method": null,
              "payment_method_types": [
                "sepa_debit",
                "ideal",
                "bancontact",
                "card",
                "sofort"
              ],
              "status": "requires_payment_method",
              "usage": "off_session"
            },
            "type": "setup_intent"
          }
        }
        """.trimIndent()
    )

    val EXPANDED_SETUP_INTENT_JSON_WITH_CBC_ELIGIBLE_BUT_NO_NETWORKS = JSONObject(
        """
        {
          "business_name": "Mybusiness",
          "card_brand_choice": {
            "eligible": true
          },
          "link_settings": {
            "link_bank_enabled": false,
            "link_bank_onboarding_enabled": false
          },
          "merchant_country": "US",
          "payment_method_preference": {
            "object": "payment_method_preference",
            "country_code": "US",
            "ordered_payment_method_types": [
              "card",
              "ideal",
              "sepa_debit",
              "bancontact",
              "sofort"
            ],
            "setup_intent": {
              "id": "seti_1JTDqGIyGgrkZxL7reCXkpr5",
              "object": "setup_intent",
              "cancellation_reason": null,
              "client_secret": "seti_1JTDqGIyGgrkZxL7reCXkpr5_secret_K7SlVPncyaU4cdiDyyHfyqNjvCDvYxG",
              "created": 1630104488,
              "description": null,
              "last_setup_error": null,
              "livemode": false,
              "next_action": null,
              "payment_method": null,
              "payment_method_types": [
                "sepa_debit",
                "ideal",
                "bancontact",
                "card",
                "sofort"
              ],
              "status": "requires_payment_method",
              "usage": "off_session"
            },
            "type": "setup_intent"
          }
        }
        """.trimIndent()
    )

    fun createPaymentIntentWithCustomerSession(
        allowRedisplay: String? = "limited",
        paymentMethodRemoveFeature: String? = "enabled",
        paymentMethodRemoveLastFeature: String? = "enabled",
        paymentMethodSetAsDefaultFeature: String = "disabled",
        paymentMethodSyncDefaultFeature: String = "disabled",
    ): JSONObject {
        return JSONObject(
            """
            {
              "business_name": "Mybusiness",
              "link_settings": {
                "link_bank_enabled": false,
                "link_bank_onboarding_enabled": false
              },
              "merchant_country": "US",
              "payment_method_preference": {
                "object": "payment_method_preference",
                "country_code": "US",
                "ordered_payment_method_types": [
                  "card",
                  "ideal",
                  "sepa_debit",
                  "bancontact",
                  "sofort"
                ],
                "payment_intent": {
                  "id": "pi_123",
                  "object": "payment_intent",
                  "amount": 973,
                  "canceled_at": null,
                  "cancellation_reason": null,
                  "capture_method": "automatic",
                  "client_secret": "pi_1234567",
                  "confirmation_method": "automatic",
                  "created": 1630103948,
                  "currency": "eur",
                  "description": null,
                  "last_payment_error": null,
                  "livemode": false,
                  "next_action": null,
                  "payment_method": null,
                  "payment_method_types": [
                    "bancontact",
                    "card",
                    "sepa_debit",
                    "sofort",
                    "ideal"
                  ],
                  "receipt_email": null,
                  "setup_future_usage": null,
                  "shipping": {
                    "address": {
                      "city": "San Francisco",
                      "country": "US",
                      "line1": "510 Townsend St",
                      "line2": null,
                      "postal_code": "94102",
                      "state": "California"
                    },
                    "carrier": null,
                    "name": "Bruno",
                    "phone": null,
                    "tracking_number": null
                  },
                  "source": null,
                  "status": "requires_payment_method"
                },
                "type": "payment_intent"
              },
              "customer": {
                "customer_session": {
                  "id": "cuss_123",
                  "object": "customer_session",
                  "api_key": "ek_test_1234",
                  "api_key_expiry": 1713890664,
                  "components": {
                    "buy_button": {
                      "enabled": false
                    },
                    "payment_element": {
                      "enabled": false,
                      "features": null
                    },
                    "mobile_payment_element": {
                      "enabled": true,
                      "features": {
                        "payment_method_remove": ${paymentMethodRemoveFeature ?: "enabled"},
                        "payment_method_save": "disabled",
                        "payment_method_remove_last": ${paymentMethodRemoveLastFeature ?: "enabled"},
                        "payment_method_save_allow_redisplay_override": ${allowRedisplay?.let { "\"$it\""} ?: "null"},
                        "payment_method_set_as_default": $paymentMethodSetAsDefaultFeature,
                      }
                    },
                    "customer_sheet": {
                      "enabled": true,
                      "features": {
                        "payment_method_remove": ${paymentMethodRemoveFeature ?: "enabled"},
                        "payment_method_remove_last": ${paymentMethodRemoveLastFeature ?: "enabled"},
                        "payment_method_sync_default": $paymentMethodSyncDefaultFeature,
                      }
                    },
                    "pricing_table": {
                      "enabled": false
                    }
                  },
                  "customer": "cus_1",
                  "livemode": false
                },
                "default_payment_method": "pm_123",
                "payment_methods": [
                  {
                    "id": "pm_123",
                    "created": 1550757934255,
                    "customer": "cus_1",
                    "livemode": false,
                    "metadata": null,
                    "type": "card",
                    "billing_details": null,
                    "card": {
                      "brand": "visa",
                      "checks": {
                        "address_line1_check": "unchecked",
                        "cvc_check": "unchecked"
                      },
                      "country": "US",
                      "exp_month": 8,
                      "exp_year": 2032,
                      "funding": "credit",
                      "fingerprint": "fingerprint123",
                      "last4": "4242",
                      "three_d_secure_usage": {
                        "supported": true
                      }
                    }
                  }
                ],
                "payment_methods_with_link_details": []
              }
            }
            """.trimIndent()
        )
    }

    val EXPANDED_PAYMENT_INTENT_WITH_CUSTOMER_SESSION_AND_CUSTOMER_SHEET_COMPONENT = JSONObject(
        """
        {
          "business_name": "Mybusiness",
          "link_settings": {
            "link_bank_enabled": false,
            "link_bank_onboarding_enabled": false
          },
          "merchant_country": "US",
          "payment_method_preference": {
            "object": "payment_method_preference",
            "country_code": "US",
            "ordered_payment_method_types": [
              "card",
              "ideal",
              "sepa_debit",
              "bancontact",
              "sofort"
            ],
            "payment_intent": {
              "id": "pi_123",
              "object": "payment_intent",
              "amount": 973,
              "canceled_at": null,
              "cancellation_reason": null,
              "capture_method": "automatic",
              "client_secret": "pi_1234567",
              "confirmation_method": "automatic",
              "created": 1630103948,
              "currency": "eur",
              "description": null,
              "last_payment_error": null,
              "livemode": false,
              "next_action": null,
              "payment_method": null,
              "payment_method_types": [
                "bancontact",
                "card",
                "sepa_debit",
                "sofort",
                "ideal"
              ],
              "receipt_email": null,
              "setup_future_usage": null,
              "shipping": {
                "address": {
                  "city": "San Francisco",
                  "country": "US",
                  "line1": "510 Townsend St",
                  "line2": null,
                  "postal_code": "94102",
                  "state": "California"
                },
                "carrier": null,
                "name": "Bruno",
                "phone": null,
                "tracking_number": null
              },
              "source": null,
              "status": "requires_payment_method"
            },
            "type": "payment_intent"
          },
          "customer": {
            "customer_session": {
              "id": "cuss_123",
              "object": "customer_session",
              "api_key": "ek_test_1234",
              "api_key_expiry": 1713890664,
              "components": {
                "buy_button": {
                  "enabled": false
                },
                "payment_element": {
                  "enabled": false,
                  "features": null
                },
                "mobile_payment_element": {
                  "enabled": false,
                  "features": null
                },
                "customer_sheet": {
                  "enabled": true,
                  "features": {
                    "payment_method_remove": "enabled",
                    "payment_method_remove_last": "enabled"
                  }
                },
                "pricing_table": {
                  "enabled": false
                }
              },
              "customer": "cus_1",
              "livemode": false
            },
            "default_payment_method": "pm_123",
            "payment_methods": [
              {
                "id": "pm_123",
                "created": 1550757934255,
                "customer": "cus_1",
                "livemode": false,
                "metadata": null,
                "type": "card",
                "billing_details": null,
                "card": {
                  "brand": "visa",
                  "checks": {
                    "address_line1_check": "unchecked",
                    "cvc_check": "unchecked"
                  },
                  "country": "US",
                  "exp_month": 8,
                  "exp_year": 2032,
                  "funding": "credit",
                  "fingerprint": "fingerprint123",
                  "last4": "4242",
                  "three_d_secure_usage": {
                    "supported": true
                  }
                }
              }
            ],
            "payment_methods_with_link_details": []
          }
        }
        """.trimIndent()
    )

    val EXPANDED_PAYMENT_INTENT_JSON_WITH_CBC_ELIGIBLE = JSONObject(
        """
        {
          "business_name": "Mybusiness",
          "card_brand_choice": {
            "eligible": true,
            "preferred_networks": ["cartes_bancaires"]
          },
          "link_settings": {
            "link_bank_enabled": false,
            "link_bank_onboarding_enabled": false
          },
          "merchant_country": "US",
          "payment_method_preference": {
            "object": "payment_method_preference",
            "country_code": "US",
            "ordered_payment_method_types": [
              "card",
              "ideal",
              "sepa_debit",
              "bancontact",
              "sofort"
            ],
            "payment_intent": {
              "id": "pi_3JTDhYIyGgrkZxL71IDUGKps",
              "object": "payment_intent",
              "amount": 973,
              "canceled_at": null,
              "cancellation_reason": null,
              "capture_method": "automatic",
              "client_secret": "pi_3JTDhYIyGgrkZxL71IDUGKps_secret_aWuzwD4JvF1HM8XJTdUsXG6Za",
              "confirmation_method": "automatic",
              "created": 1630103948,
              "currency": "eur",
              "description": null,
              "last_payment_error": null,
              "livemode": false,
              "next_action": null,
              "payment_method": null,
              "payment_method_types": [
                "bancontact",
                "card",
                "sepa_debit",
                "sofort",
                "ideal"
              ],
              "receipt_email": null,
              "setup_future_usage": null,
              "shipping": {
                "address": {
                  "city": "San Francisco",
                  "country": "US",
                  "line1": "510 Townsend St",
                  "line2": null,
                  "postal_code": "94102",
                  "state": "California"
                },
                "carrier": null,
                "name": "Bruno",
                "phone": null,
                "tracking_number": null
              },
              "source": null,
              "status": "requires_payment_method"
            },
            "type": "payment_intent"
          }
        }
        """.trimIndent()
    )

    val EXPANDED_PAYMENT_INTENT_JSON_WITH_CBC_NOT_ELIGIBLE = JSONObject(
        """
        {
          "business_name": "Mybusiness",
          "card_brand_choice": {
            "eligible": false,
            "preferred_networks": ["cartes_bancaires"]
          },
          "link_settings": {
            "link_bank_enabled": false,
            "link_bank_onboarding_enabled": false
          },
          "merchant_country": "US",
          "payment_method_preference": {
            "object": "payment_method_preference",
            "country_code": "US",
            "ordered_payment_method_types": [
              "card",
              "ideal",
              "sepa_debit",
              "bancontact",
              "sofort"
            ],
            "payment_intent": {
              "id": "pi_3JTDhYIyGgrkZxL71IDUGKps",
              "object": "payment_intent",
              "amount": 973,
              "canceled_at": null,
              "cancellation_reason": null,
              "capture_method": "automatic",
              "client_secret": "pi_3JTDhYIyGgrkZxL71IDUGKps_secret_aWuzwD4JvF1HM8XJTdUsXG6Za",
              "confirmation_method": "automatic",
              "created": 1630103948,
              "currency": "eur",
              "description": null,
              "last_payment_error": null,
              "livemode": false,
              "next_action": null,
              "payment_method": null,
              "payment_method_types": [
                "bancontact",
                "card",
                "sepa_debit",
                "sofort",
                "ideal"
              ],
              "receipt_email": null,
              "setup_future_usage": null,
              "shipping": {
                "address": {
                  "city": "San Francisco",
                  "country": "US",
                  "line1": "510 Townsend St",
                  "line2": null,
                  "postal_code": "94102",
                  "state": "California"
                },
                "carrier": null,
                "name": "Bruno",
                "phone": null,
                "tracking_number": null
              },
              "source": null,
              "status": "requires_payment_method"
            },
            "type": "payment_intent"
          }
        }
        """.trimIndent()
    )

    val EXPANDED_PAYMENT_INTENT_WITH_LINK_FUNDING_SOURCES_JSON = JSONObject(
        """
        {
          "business_name": "Mybusiness",
          "link_settings": {
            "link_authenticated_change_event_enabled": false,
            "link_bank_incentives_enabled": false,
            "link_bank_onboarding_enabled": false,
            "link_email_verification_login_enabled": false,
            "link_financial_incentives_experiment_enabled": false,
            "link_funding_sources": [
              "CARD", "BANK_ACCOUNT"
            ],
            "link_local_storage_login_enabled": true,
            "link_only_for_payment_method_types_enabled": false,
            "link_passthrough_mode_enabled": true
          },
          "merchant_country": "US",
          "payment_method_preference": {
            "object": "payment_method_preference",
            "country_code": "US",
            "ordered_payment_method_types": [
              "card",
              "ideal",
              "sepa_debit",
              "bancontact",
              "sofort"
            ],
            "payment_intent": {
              "id": "pi_3JTDhYIyGgrkZxL71IDUGKps",
              "object": "payment_intent",
              "amount": 973,
              "canceled_at": null,
              "cancellation_reason": null,
              "capture_method": "automatic",
              "client_secret": "pi_3JTDhYIyGgrkZxL71IDUGKps_secret_aWuzwD4JvF1HM8XJTdUsXG6Za",
              "confirmation_method": "automatic",
              "created": 1630103948,
              "currency": "eur",
              "description": null,
              "last_payment_error": null,
              "livemode": false,
              "next_action": null,
              "payment_method": null,
              "payment_method_types": [
                "bancontact",
                "card",
                "sepa_debit",
                "sofort",
                "ideal"
              ],
              "receipt_email": null,
              "setup_future_usage": null,
              "shipping": {
                "address": {
                  "city": "San Francisco",
                  "country": "US",
                  "line1": "510 Townsend St",
                  "line2": null,
                  "postal_code": "94102",
                  "state": "California"
                },
                "carrier": null,
                "name": "Bruno",
                "phone": null,
                "tracking_number": null
              },
              "source": null,
              "status": "requires_payment_method"
            },
            "type": "payment_intent"
          }
        }
        """.trimIndent()
    )

    val EXPANDED_SETUP_INTENT_WITH_LINK_FUNDING_SOURCES_JSON = JSONObject(
        """
        {
          "business_name": "Mybusiness",
          "link_settings": {
            "link_authenticated_change_event_enabled": false,
            "link_bank_incentives_enabled": false,
            "link_bank_onboarding_enabled": false,
            "link_email_verification_login_enabled": false,
            "link_financial_incentives_experiment_enabled": false,
            "link_funding_sources": [
              "CARD", "BANK_ACCOUNT"
            ],
            "link_local_storage_login_enabled": true,
            "link_only_for_payment_method_types_enabled": false,
            "link_passthrough_mode_enabled": false
          },
          "merchant_country": "US",
          "payment_method_preference": {
            "object": "payment_method_preference",
            "country_code": "US",
            "ordered_payment_method_types": [
              "card",
              "ideal",
              "sepa_debit",
              "bancontact",
              "sofort"
            ],
            "setup_intent": {
              "id": "seti_1JTDqGIyGgrkZxL7reCXkpr5",
              "object": "setup_intent",
              "cancellation_reason": null,
              "client_secret": "seti_1JTDqGIyGgrkZxL7reCXkpr5_secret_K7SlVPncyaU4cdiDyyHfyqNjvCDvYxG",
              "created": 1630104488,
              "description": null,
              "last_setup_error": null,
              "livemode": false,
              "next_action": null,
              "payment_method": null,
              "payment_method_types": [
                "sepa_debit",
                "ideal",
                "bancontact",
                "card",
                "sofort"
              ],
              "status": "requires_payment_method",
              "usage": "off_session"
            },
            "type": "setup_intent"
          }
        }
        """.trimIndent()
    )

    val EXPANDED_SETUP_INTENT_WITH_LINK_SIGNUP_DISABLED_JSON = JSONObject(
        """
        {
          "business_name": "Mybusiness",
          "link_settings": {
            "link_authenticated_change_event_enabled": false,
            "link_bank_incentives_enabled": false,
            "link_bank_onboarding_enabled": false,
            "link_email_verification_login_enabled": false,
            "link_financial_incentives_experiment_enabled": false,
            "link_funding_sources": [
              "CARD", "BANK_ACCOUNT"
            ],
            "link_local_storage_login_enabled": true,
            "link_mobile_disable_signup": true,
            "link_only_for_payment_method_types_enabled": false,
            "link_passthrough_mode_enabled": true
          },
          "merchant_country": "US",
          "payment_method_preference": {
            "object": "payment_method_preference",
            "country_code": "US",
            "ordered_payment_method_types": [
              "card",
              "ideal",
              "sepa_debit",
              "bancontact",
              "sofort"
            ],
            "payment_intent": {
              "id": "pi_3JTDhYIyGgrkZxL71IDUGKps",
              "object": "payment_intent",
              "amount": 973,
              "canceled_at": null,
              "cancellation_reason": null,
              "capture_method": "automatic",
              "client_secret": "pi_3JTDhYIyGgrkZxL71IDUGKps_secret_aWuzwD4JvF1HM8XJTdUsXG6Za",
              "confirmation_method": "automatic",
              "created": 1630103948,
              "currency": "eur",
              "description": null,
              "last_payment_error": null,
              "livemode": false,
              "next_action": null,
              "payment_method": null,
              "payment_method_types": [
                "bancontact",
                "card",
                "sepa_debit",
                "sofort",
                "ideal"
              ],
              "receipt_email": null,
              "setup_future_usage": null,
              "shipping": {
                "address": {
                  "city": "San Francisco",
                  "country": "US",
                  "line1": "510 Townsend St",
                  "line2": null,
                  "postal_code": "94102",
                  "state": "California"
                },
                "carrier": null,
                "name": "Bruno",
                "phone": null,
                "tracking_number": null
              },
              "source": null,
              "status": "requires_payment_method"
            },
            "type": "payment_intent"
          }
        }
        """.trimIndent()
    )

    val EXPANDED_SETUP_INTENT_WITH_LINK_SIGNUP_DISABLED_FLAG_FALSE_JSON = JSONObject(
        """
        {
          "business_name": "Mybusiness",
          "link_settings": {
            "link_authenticated_change_event_enabled": false,
            "link_bank_incentives_enabled": false,
            "link_bank_onboarding_enabled": false,
            "link_email_verification_login_enabled": false,
            "link_financial_incentives_experiment_enabled": false,
            "link_funding_sources": [
              "CARD", "BANK_ACCOUNT"
            ],
            "link_local_storage_login_enabled": true,
            "link_mobile_disable_signup": false,
            "link_only_for_payment_method_types_enabled": false,
            "link_passthrough_mode_enabled": true
          },
          "merchant_country": "US",
          "payment_method_preference": {
            "object": "payment_method_preference",
            "country_code": "US",
            "ordered_payment_method_types": [
              "card",
              "ideal",
              "sepa_debit",
              "bancontact",
              "sofort"
            ],
            "payment_intent": {
              "id": "pi_3JTDhYIyGgrkZxL71IDUGKps",
              "object": "payment_intent",
              "amount": 973,
              "canceled_at": null,
              "cancellation_reason": null,
              "capture_method": "automatic",
              "client_secret": "pi_3JTDhYIyGgrkZxL71IDUGKps_secret_aWuzwD4JvF1HM8XJTdUsXG6Za",
              "confirmation_method": "automatic",
              "created": 1630103948,
              "currency": "eur",
              "description": null,
              "last_payment_error": null,
              "livemode": false,
              "next_action": null,
              "payment_method": null,
              "payment_method_types": [
                "bancontact",
                "card",
                "sepa_debit",
                "sofort",
                "ideal"
              ],
              "receipt_email": null,
              "setup_future_usage": null,
              "shipping": {
                "address": {
                  "city": "San Francisco",
                  "country": "US",
                  "line1": "510 Townsend St",
                  "line2": null,
                  "postal_code": "94102",
                  "state": "California"
                },
                "carrier": null,
                "name": "Bruno",
                "phone": null,
                "tracking_number": null
              },
              "source": null,
              "status": "requires_payment_method"
            },
            "type": "payment_intent"
          }
        }
        """.trimIndent()
    )

    val PI_WITH_CARD_AFTERPAY_AU_BECS by lazy {
        """
            {
              "apple_pay_preference": "enabled",
              "business_name": "AU Mobile Example Account",
              "experiments": {
                "miui_payment_element_aa_experiment": "control"
              },
              "flags": {
                "elements_include_payment_intent_id_in_analytics_events": true,
                "elements_enable_mx_card_installments": false,
                "elements_enable_br_card_installments": false,
                "elements_enable_bacs_debit_pm": false
              },
              "google_pay_preference": "enabled",
              "link_consumer_info": null,
              "link_settings": {
                "instant_debits_inline_institution": true,
                "link_bank_onboarding_enabled": false,
                "link_email_verification_login_enabled": false,
                "link_financial_incentives_experiment_enabled": true,
                "link_local_storage_login_enabled": true
              },
              "merchant_country": "AU",
              "merchant_currency": "aud",
              "merchant_id": "acct_1KaoFxCPXw4rvZpf",
              "order": null,
              "ordered_payment_method_types_and_wallets": [
                "card",
                "apple_pay",
                "google_pay",
                "afterpay_clearpay",
                "au_becs_debit"
              ],
              "payment_method_preference": {
                "object": "payment_method_preference",
                "country_code": "US",
                "ordered_payment_method_types": [
                  "au_becs_debit",
                  "afterpay_clearpay",
                  "card"
                ],
                "payment_intent": {
                  "id": "pi_3LABhECPXw4rvZpf0x3iNYAf",
                  "object": "payment_intent",
                  "amount": 5099,
                  "amount_details": {
                    "tip": {}
                  },
                  "automatic_payment_methods": {
                    "enabled": true
                  },
                  "canceled_at": null,
                  "cancellation_reason": null,
                  "capture_method": "automatic",
                  "client_secret": "pi_3LABhECPXw4rvZpf0x3iNYAf_secret_Gmfvx5MS15mcv0jPOF4CAuLV4",
                  "confirmation_method": "automatic",
                  "created": 1655120680,
                  "currency": "aud",
                  "description": null,
                  "last_payment_error": null,
                  "livemode": true,
                  "next_action": null,
                  "payment_method": null,
                  "payment_method_types": [
                    "card",
                    "afterpay_clearpay",
                    "au_becs_debit"
                  ],
                  "processing": null,
                  "receipt_email": null,
                  "setup_future_usage": null,
                  "shipping": {
                    "address": {
                      "city": "San Francisco",
                      "country": "US",
                      "line1": "510 Townsend St",
                      "line2": null,
                      "postal_code": "94102",
                      "state": "California"
                    },
                    "carrier": null,
                    "name": "John Doe",
                    "phone": null,
                    "tracking_number": null
                  },
                  "source": null,
                  "status": "requires_payment_method"
                },
                "type": "payment_intent"
              },
              "shipping_address_settings": {
                "autocomplete_allowed": false
              },
              "unactivated_payment_method_types": [
                "au_becs_debit"
              ]
            }
        """.trimIndent()
    }

    val PI_WITH_CARD_AFTERPAY_AU_BECS_NO_ORDERED_LPMS by lazy {
        """
            {
              "apple_pay_preference": "enabled",
              "business_name": "AU Mobile Example Account",
              "experiments": {
                "miui_payment_element_aa_experiment": "control"
              },
              "flags": {
                "elements_include_payment_intent_id_in_analytics_events": true,
                "elements_enable_mx_card_installments": false,
                "elements_enable_br_card_installments": false,
                "elements_enable_bacs_debit_pm": false
              },
              "google_pay_preference": "enabled",
              "link_consumer_info": null,
              "link_settings": {
                "instant_debits_inline_institution": true,
                "link_bank_onboarding_enabled": false,
                "link_email_verification_login_enabled": false,
                "link_financial_incentives_experiment_enabled": true,
                "link_local_storage_login_enabled": true
              },
              "merchant_country": "AU",
              "merchant_currency": "aud",
              "merchant_id": "acct_1KaoFxCPXw4rvZpf",
              "order": null,
              "ordered_payment_method_types_and_wallets": [
                "card",
                "apple_pay",
                "google_pay",
                "afterpay_clearpay",
                "au_becs_debit"
              ],
              "payment_method_preference": {
                "object": "payment_method_preference",
                "country_code": "US",
                "payment_intent": {
                  "id": "pi_3LABhECPXw4rvZpf0x3iNYAf",
                  "object": "payment_intent",
                  "amount": 5099,
                  "amount_details": {
                    "tip": {}
                  },
                  "automatic_payment_methods": {
                    "enabled": true
                  },
                  "canceled_at": null,
                  "cancellation_reason": null,
                  "capture_method": "automatic",
                  "client_secret": "pi_3LABhECPXw4rvZpf0x3iNYAf_secret_Gmfvx5MS15mcv0jPOF4CAuLV4",
                  "confirmation_method": "automatic",
                  "created": 1655120680,
                  "currency": "aud",
                  "description": null,
                  "last_payment_error": null,
                  "livemode": false,
                  "next_action": null,
                  "payment_method": null,
                  "payment_method_types": [
                    "card",
                    "afterpay_clearpay",
                    "au_becs_debit"
                  ],
                  "processing": null,
                  "receipt_email": null,
                  "setup_future_usage": null,
                  "shipping": {
                    "address": {
                      "city": "San Francisco",
                      "country": "US",
                      "line1": "510 Townsend St",
                      "line2": null,
                      "postal_code": "94102",
                      "state": "California"
                    },
                    "carrier": null,
                    "name": "John Doe",
                    "phone": null,
                    "tracking_number": null
                  },
                  "source": null,
                  "status": "requires_payment_method"
                },
                "type": "payment_intent"
              },
              "shipping_address_settings": {
                "autocomplete_allowed": false
              },
              "unactivated_payment_method_types": [
                "au_becs_debit"
              ]
            }
        """.trimIndent()
    }

    val DEFERRED_INTENT_JSON = JSONObject(
        """
            {
              "account_id": null,
              "apple_pay_preference": "enabled",
              "business_name": "Mobile Example Account",
              "experiments": {
                "element_link_autofill_in_link_authentication_element": "control_test",
                "element_link_autofill_in_payment_element": "control_test",
                "elements_link_aa": "control_test",
                "elements_link_in_payment_element_only": "treatment",
                "elements_link_in_payment_element_only_holdback": "control",
                "elements_link_longterm_holdback": "control",
                "lpm_discoverability_upe_experiment_1": "downward_arrow_treatment"
              },
              "experiments_data": {
                "arb_id": "232dd033-0b45-4456-b834-ecdcb02ab1fb",
                "experiment_assignments": {
                  "element_link_autofill_in_link_authentication_element": "control_test",
                  "element_link_autofill_in_payment_element": "control_test",
                  "elements_link_aa": "control_test",
                  "elements_link_in_payment_element_only": "treatment",
                  "elements_link_in_payment_element_only_holdback": "control",
                  "elements_link_longterm_holdback": "control",
                  "lpm_discoverability_upe_experiment_1": "downward_arrow_treatment"
                }
              },
              "flags": {
                "elements_disable_paypal_express": true,
                "elements_enable_blik": true,
                "elements_enable_br_card_installments": false,
                "elements_enable_deferred_intent": false,
                "elements_enable_demo_pay": false,
                "elements_enable_express_checkout": false,
                "elements_enable_external_payment_method_paypal": false,
                "elements_enable_external_payment_method_venmo": false,
                "elements_enable_mobilepay": false,
                "elements_enable_mx_card_installments": false,
                "elements_enable_revolut_pay": false,
                "elements_link_enable_email_domain_correction": false,
                "elements_lpm_discoverability_downward_arrow": false,
                "elements_lpm_discoverability_rotating_cycle": false,
                "elements_web_lpm_server_driven_ui": true,
                "financial_connections_enable_deferred_intent_flow": false,
                "merchant_success_log_element_is_visible": true
              },
              "google_pay_preference": "enabled",
              "link_consumer_info": null,
              "link_settings": {
                "link_authenticated_change_event_enabled": false,
                "link_bank_incentives_enabled": false,
                "link_bank_onboarding_enabled": false,
                "link_crypto_onramp_bank_upsell": false,
                "link_crypto_onramp_elements_logout_disabled": false,
                "link_crypto_onramp_force_cvc_reverification": false,
                "link_elements_is_crypto_onramp": false,
                "link_elements_pageload_sign_up_disabled": false,
                "link_email_verification_login_enabled": false,
                "link_financial_incentives_experiment_enabled": false,
                "link_funding_sources": [
                  "CARD"
                ],
                "link_instant_debits_create_link_account_session_on_instantiation": false,
                "link_local_storage_login_enabled": false,
                "link_m2_default_integration_enabled": true,
                "link_only_for_payment_method_types_enabled": false,
                "link_passthrough_mode_enabled": false,
                "link_pay_button_element_enabled": true,
                "link_session_storage_login_enabled": true
              },
              "merchant_country": "US",
              "merchant_currency": "usd",
              "merchant_id": "acct_1HvTI7Lu5o3P18Zp",
              "meta_pay_signed_container_context": null,
              "order": null,
              "ordered_payment_method_types_and_wallets": [
                "card",
                "link",
                "apple_pay",
                "google_pay",
                "cashapp",
                "us_bank_account",
                "klarna",
                "afterpay_clearpay",
                "alipay",
                "wechat_pay"
              ],
              "payment_method_preference": {
                "object": "payment_method_preference",
                "country_code": "CA",
                "ordered_payment_method_types": [
                  "card",
                  "link",
                  "cashapp"
                ],
                "type": "deferred_intent"
              },
              "payment_method_specs": [
                {
                  "async": false,
                  "fields": [
                    {
                      "type": "afterpay_header"
                    },
                    {
                      "api_path": {
                        "v1": "billing_details[name]"
                      },
                      "type": "name"
                    },
                    {
                      "api_path": {
                        "v1": "billing_details[email]"
                      },
                      "type": "email"
                    },
                    {
                      "allowed_country_codes": null,
                      "type": "billing_address"
                    }
                  ],
                  "selector_icon": {
                    "light_theme_png": "https://js.stripe.com/v3/fingerprinted/img/payment-methods/icon-pm-afterpay@3x-6776ded2b20306c85d02639aea1e7dc5.png",
                    "light_theme_svg": "https://js.stripe.com/v3/fingerprinted/img/payment-methods/icon-pm-afterpay-abedc6b87e4e9f917e22bbe6648ba809.svg"
                  },
                  "type": "afterpay_clearpay"
                },
                {
                  "async": false,
                  "fields": [],
                  "type": "card"
                },
                {
                  "async": false,
                  "fields": [],
                  "selector_icon": {
                    "light_theme_png": "https://js.stripe.com/v3/fingerprinted/img/payment-methods/icon-pm-cashapp@3x-a89c5d8d0651cae2a511bb49a6be1cfc.png",
                    "light_theme_svg": "https://js.stripe.com/v3/fingerprinted/img/payment-methods/icon-pm-cashapp-981164a833e417d28a8ac2684fda2324.svg"
                  },
                  "type": "cashapp"
                },
                {
                  "async": false,
                  "fields": [],
                  "selector_icon": {
                    "light_theme_png": "https://js.stripe.com/v3/fingerprinted/img/payment-methods/wechat_pay.png",
                    "light_theme_svg": "https://js.stripe.com/v3/fingerprinted/img/payment-methods/icon-pm-wechat-pay-f62a5a27f646cb5f596c610475d14444.svg"
                  },
                  "type": "wechat_pay"
                }
              ],
              "paypal_express_config": {
                "client_id": null,
                "paypal_merchant_id": null
              },
              "session_id": "elements_session_1t6ejApXCS5",
              "shipping_address_settings": {
                "autocomplete_allowed": true
              },
              "unactivated_payment_method_types": []
            }
        """.trimIndent()
    )

    val PAYMENT_INTENT_WITH_EXTERNAL_VENMO_JSON = JSONObject(
        """
            {
                "account_id": "acct_1HvTI7Lu5o3P18Zp",
                "apple_pay_merchant_token_webhook_url": "https:\/\/pm-hooks.stripe.com\/apple_pay\/merchant_token\/pDq7tf9uieoQWMVJixFwuOve\/acct_1HvTI7Lu5o3P18Zp\/",
                "apple_pay_preference": "enabled",
                "business_name": "Mobile Example Account",
                "card_brand_choice": {
                    "eligible": false,
                    "preferred_networks": [
                        "cartes_bancaires"
                    ],
                    "supported_cobranded_networks": {
                        "cartes_bancaires": false
                    }
                },
                "customer": null,
                "customer_error": null,
                "experiments_data": {
                    "arb_id": "4397e65b-d3eb-44e0-afd7-e5054f23c4a2",
                    "experiment_assignments": {
                        "elements_card_mulberry_purchase_protections": "control",
                        "elements_debit_mulberry_purchase_protections": "control",
                        "elements_link_longterm_holdback_v2": "control",
                        "elements_merchant_ui_api_srv": "control",
                        "lape_redesign_v0_aa": "control_test",
                        "link_popup_webview_option_ios": "control",
                        "no_code_default_values": "control",
                        "no_code_default_values_aa": "control"
                    }
                },
                "external_payment_method_data": [
                    {
                        "dark_image_url": null,
                        "label": "Venmo",
                        "light_image_url": "https:\/\/js.stripe.com\/v3\/fingerprinted\/img\/payment-methods\/icon-epm-venmo-162b3cf0020c8fe2ce4bde7ec3845941.png",
                        "type": "external_venmo"
                    }
                ],
                "flags": {
                    "cbc_in_link_popup": false,
                    "disable_cbc_in_link_popup": false,
                    "ece_apple_pay_payment_request_passthrough": false,
                    "elements_disable_express_checkout_button_amazon_pay": false,
                    "elements_disable_link_email_otp": false,
                    "elements_disable_payment_element_card_country_zip_validations": false,
                    "elements_disable_paypal_express": false,
                    "elements_disable_recurring_express_checkout_button_amazon_pay": false,
                    "elements_enable_affirm_unified_offer": true,
                    "elements_enable_afterpay_clearpay_unified_offer": true,
                    "elements_enable_blik": true,
                    "elements_enable_br_card_installments": false,
                    "elements_enable_card_brand_choice_payment_element_link": false,
                    "elements_enable_card_brand_choice_payment_element_payment_method_data": true,
                    "elements_enable_client_attribution_metadata": true,
                    "elements_enable_express_checkout_button_demo_pay": false,
                    "elements_enable_external_payment_method_alipay_mobile": false,
                    "elements_enable_external_payment_method_amazon_pay": false,
                    "elements_enable_external_payment_method_aplazame": false,
                    "elements_enable_external_payment_method_aplazo": false,
                    "elements_enable_external_payment_method_atome": false,
                    "elements_enable_external_payment_method_atone": false,
                    "elements_enable_external_payment_method_au_pay": false,
                    "elements_enable_external_payment_method_azupay": false,
                    "elements_enable_external_payment_method_bankaxept": false,
                    "elements_enable_external_payment_method_benefit": false,
                    "elements_enable_external_payment_method_billie": false,
                    "elements_enable_external_payment_method_bizum": false,
                    "elements_enable_external_payment_method_bluecode": false,
                    "elements_enable_external_payment_method_bpay": false,
                    "elements_enable_external_payment_method_catch": false,
                    "elements_enable_external_payment_method_check": false,
                    "elements_enable_external_payment_method_coinbase_pay": false,
                    "elements_enable_external_payment_method_dankort": false,
                    "elements_enable_external_payment_method_dapp": false,
                    "elements_enable_external_payment_method_dbarai": false,
                    "elements_enable_external_payment_method_divido": false,
                    "elements_enable_external_payment_method_ebt_snap": false,
                    "elements_enable_external_payment_method_eftpos_australia": false,
                    "elements_enable_external_payment_method_famipay": false,
                    "elements_enable_external_payment_method_fawry": false,
                    "elements_enable_external_payment_method_fonix": false,
                    "elements_enable_external_payment_method_gcash": false,
                    "elements_enable_external_payment_method_girocard": false,
                    "elements_enable_external_payment_method_gopay": false,
                    "elements_enable_external_payment_method_grabpay_later": false,
                    "elements_enable_external_payment_method_hands_in": false,
                    "elements_enable_external_payment_method_humm": false,
                    "elements_enable_external_payment_method_interac": false,
                    "elements_enable_external_payment_method_iwocapay": false,
                    "elements_enable_external_payment_method_kbc": false,
                    "elements_enable_external_payment_method_knet": false,
                    "elements_enable_external_payment_method_kriya": false,
                    "elements_enable_external_payment_method_laybuy": false,
                    "elements_enable_external_payment_method_line_pay": false,
                    "elements_enable_external_payment_method_mb_way": false,
                    "elements_enable_external_payment_method_mercado_pago": false,
                    "elements_enable_external_payment_method_merpay": false,
                    "elements_enable_external_payment_method_momo": false,
                    "elements_enable_external_payment_method_mondu": false,
                    "elements_enable_external_payment_method_mybank": false,
                    "elements_enable_external_payment_method_netbanking": false,
                    "elements_enable_external_payment_method_nexi_pay": false,
                    "elements_enable_external_payment_method_oney": false,
                    "elements_enable_external_payment_method_online_banking_czech_republic": false,
                    "elements_enable_external_payment_method_online_banking_finland": false,
                    "elements_enable_external_payment_method_online_banking_poland": false,
                    "elements_enable_external_payment_method_online_banking_slovakia": false,
                    "elements_enable_external_payment_method_online_banking_thailand": false,
                    "elements_enable_external_payment_method_paidy": false,
                    "elements_enable_external_payment_method_pay_easy": false,
                    "elements_enable_external_payment_method_paybright": false,
                    "elements_enable_external_payment_method_payconiq": false,
                    "elements_enable_external_payment_method_paydirekt": false,
                    "elements_enable_external_payment_method_payit": false,
                    "elements_enable_external_payment_method_paypal": true,
                    "elements_enable_external_payment_method_paypay": false,
                    "elements_enable_external_payment_method_paypo": false,
                    "elements_enable_external_payment_method_payrexx": false,
                    "elements_enable_external_payment_method_paysafecard": false,
                    "elements_enable_external_payment_method_paytm": false,
                    "elements_enable_external_payment_method_payu": false,
                    "elements_enable_external_payment_method_picpay": false,
                    "elements_enable_external_payment_method_pix_international": false,
                    "elements_enable_external_payment_method_planpay": false,
                    "elements_enable_external_payment_method_pledg": false,
                    "elements_enable_external_payment_method_poli": false,
                    "elements_enable_external_payment_method_postepay": false,
                    "elements_enable_external_payment_method_postfinance": false,
                    "elements_enable_external_payment_method_rabbitline_pay": false,
                    "elements_enable_external_payment_method_rakuten_pay": false,
                    "elements_enable_external_payment_method_ratepay": false,
                    "elements_enable_external_payment_method_samsung_pay": false,
                    "elements_enable_external_payment_method_satispay": false,
                    "elements_enable_external_payment_method_scalapay": false,
                    "elements_enable_external_payment_method_sequra": false,
                    "elements_enable_external_payment_method_sezzle": false,
                    "elements_enable_external_payment_method_skrill": false,
                    "elements_enable_external_payment_method_swish": false,
                    "elements_enable_external_payment_method_tabby": false,
                    "elements_enable_external_payment_method_titres_restaurant": false,
                    "elements_enable_external_payment_method_tng": false,
                    "elements_enable_external_payment_method_truelayer": false,
                    "elements_enable_external_payment_method_trustly": false,
                    "elements_enable_external_payment_method_twint": false,
                    "elements_enable_external_payment_method_v_pay": false,
                    "elements_enable_external_payment_method_venmo": false,
                    "elements_enable_external_payment_method_vipps": false,
                    "elements_enable_external_payment_method_wallets_india": false,
                    "elements_enable_external_payment_method_walley": false,
                    "elements_enable_external_payment_method_wechat_mobile": false,
                    "elements_enable_external_payment_method_younitedpay": false,
                    "elements_enable_klarna_unified_offer": true,
                    "elements_enable_link_spm": true,
                    "elements_enable_mobilepay": false,
                    "elements_enable_mx_card_installments": false,
                    "elements_enable_passive_captcha": true,
                    "elements_enable_passive_hcaptcha_in_payment_method_creation": true,
                    "elements_enable_payment_element_postal_validation": true,
                    "elements_enable_read_allow_redisplay": false,
                    "elements_enable_save_for_future_payments_pre_check": false,
                    "elements_enable_save_last_used_payment_method": true,
                    "elements_enable_south_korea_market_underlying_pms": false,
                    "elements_enable_use_last_used_payment_method": false,
                    "elements_enable_write_allow_redisplay": false,
                    "elements_saved_payment_methods": true,
                    "elements_stop_move_focus_to_first_errored_field": true,
                    "elements_web_lpm_server_driven_ui": true,
                    "elements_write_sfu_into_confirm_request": false,
                    "enable_ece_session_id_confirmation_token": false,
                    "enable_third_party_recurring_express_checkout_element": false,
                    "financial_connections_enable_deferred_intent_flow": true,
                    "legacy_confirmation_tokens": false,
                    "link_enable_card_brand_choice": true,
                    "link_purchase_protections_enabled": false,
                    "link_share_expand_payment_method": true,
                    "payment_method_allow_redisplay": true,
                    "show_swish_redirect_and_qr_code_auth_flows": true,
                    "use_link_views": false
                },
                "google_pay_preference": "enabled",
                "legacy_customer": null,
                "link_purchase_protections_data": {
                    "is_eligible": true,
                    "type": "shopping"
                },
                "link_settings": {
                    "link_authenticated_change_event_enabled": false,
                    "link_bank_incentives_enabled": false,
                    "link_bank_onboarding_enabled": false,
                    "link_crypto_onramp_bank_upsell": false,
                    "link_crypto_onramp_elements_logout_disabled": false,
                    "link_crypto_onramp_force_cvc_reverification": false,
                    "link_disable_email_otp": false,
                    "link_disable_postal_code_autocomplete": false,
                    "link_disable_postal_code_strategy_evaluation": false,
                    "link_disabled_reasons": {
                        "payment_element_passthrough_mode": [
                            "gated_into_elements_saved_payment_methods"
                        ],
                        "payment_element_payment_method_mode": [
                            "gated_into_enable_m2_passthrough_mode"
                        ]
                    },
                    "link_elements_is_crypto_onramp": false,
                    "link_elements_pageload_sign_up_disabled": false,
                    "link_email_verification_login_enabled": false,
                    "link_enable_email_otp_for_link_popup": true,
                    "link_enable_instant_debits_in_testmode": false,
                    "link_enable_prefill_data_collection": true,
                    "link_enable_prefill_data_collection_dryrun": false,
                    "link_enable_webauthn_for_link_popup": true,
                    "link_financial_incentives_experiment_enabled": false,
                    "link_funding_sources": [
                        "CARD"
                    ],
                    "link_funding_sources_onboarding_enabled": [
                        "CARD"
                    ],
                    "link_funding_sources_onboarding_unavailable_from_holdback": [],
                    "link_global_holdback_on": false,
                    "link_hcaptcha_site_key": null,
                    "link_local_storage_login_enabled": true,
                    "link_m2_default_integration_enabled": false,
                    "link_no_code_default_values_dashboard_setting": false,
                    "link_no_code_default_values_identification": true,
                    "link_no_code_default_values_recall": true,
                    "link_no_code_default_values_usage": false,
                    "link_only_for_payment_method_types_enabled": false,
                    "link_opt_out_email_input_refactor": true,
                    "link_passthrough_mode_enabled": true,
                    "link_pay_button_element_enabled": true,
                    "link_payment_element_disabled_by_targeting": false,
                    "link_payment_element_enable_webauthn_login": true,
                    "link_pm_killswitch_on_in_elements": false,
                    "link_popup_webview_option": "shared",
                    "link_session_storage_login_enabled": true,
                    "link_targeting_results": {
                        "payment_element_passthrough_mode": null
                    }
                },
                "lpm_promotions": null,
                "merchant_country": "US",
                "merchant_currency": "usd",
                "merchant_id": "acct_1HvTI7Lu5o3P18Zp",
                "merchant_logo_url": null,
                "meta_pay_signed_container_context": null,
                "order": null,
                "ordered_payment_method_types_and_wallets": [
                    "card",
                    "apple_pay",
                    "afterpay_clearpay",
                    "cashapp",
                    "amazon_pay",
                    "acss_debit",
                    "google_pay",
                    "us_bank_account",
                    "klarna",
                    "affirm",
                    "alipay",
                    "wechat_pay"
                ],
                "passive_captcha": {
                    "site_key": "20000000-ffff-ffff-ffff-000000000002"
                },
                "payment_method_preference": {
                    "object": "payment_method_preference",
                    "country_code": "US",
                    "ordered_payment_method_types": [
                        "card",
                        "afterpay_clearpay",
                        "cashapp",
                        "amazon_pay",
                        "acss_debit",
                        "us_bank_account",
                        "klarna",
                        "affirm",
                        "alipay",
                        "wechat_pay"
                    ],
                    "payment_intent": {
                        "id": "pi_3P8S7cLu5o3P18Zp14AjsL3v",
                        "object": "payment_intent",
                        "amount": 5099,
                        "amount_details": {
                            "tip": {}
                        },
                        "automatic_payment_methods": {
                            "allow_redirects": "always",
                            "enabled": true
                        },
                        "canceled_at": null,
                        "cancellation_reason": null,
                        "capture_method": "automatic",
                        "client_secret": "pi_3P8S7cLu5o3P18Zp14AjsL3v_secret_Wtzl733f6p5NBZS15Bnzn4TRl",
                        "confirmation_method": "automatic",
                        "created": 1713812508,
                        "currency": "usd",
                        "description": null,
                        "last_payment_error": null,
                        "livemode": false,
                        "next_action": null,
                        "payment_method": null,
                        "payment_method_configuration_details": {
                            "id": "pmc_1JcZxfLu5o3P18Zp6oYzNhiP",
                            "parent": null
                        },
                        "payment_method_options": {
                            "us_bank_account": {
                                "verification_method": "automatic"
                            }
                        },
                        "payment_method_types": [
                            "card",
                            "acss_debit",
                            "afterpay_clearpay",
                            "alipay",
                            "klarna",
                            "link",
                            "us_bank_account",
                            "wechat_pay",
                            "affirm",
                            "cashapp",
                            "amazon_pay"
                        ],
                        "processing": null,
                        "receipt_email": null,
                        "setup_future_usage": null,
                        "shipping": null,
                        "source": null,
                        "status": "requires_payment_method"
                    },
                    "type": "payment_intent"
                },
                "payment_method_specs": [
                    {
                        "async": false,
                        "fields": [
                            {
                                "type": "affirm_header"
                            }
                        ],
                        "next_action_spec": {
                            "confirm_response_status_specs": {
                                "requires_action": {
                                    "type": "redirect_to_url"
                                }
                            },
                            "post_confirm_handling_pi_status_specs": {
                                "requires_action": {
                                    "type": "canceled"
                                },
                                "succeeded": {
                                    "type": "finished"
                                }
                            }
                        },
                        "selector_icon": {
                            "light_theme_png": "https:\/\/js.stripe.com\/v3\/fingerprinted\/img\/payment-methods\/icon-pm-affirm@3x-d2623d995950761883fca048ce6e0550.png",
                            "light_theme_svg": "https:\/\/js.stripe.com\/v3\/fingerprinted\/img\/payment-methods\/icon-pm-affirm-cd0d27fdc6cb5ca18c77645c577c8b9b.svg"
                        },
                        "type": "affirm"
                    },
                    {
                        "async": false,
                        "fields": [
                            {
                                "type": "afterpay_header"
                            },
                            {
                                "api_path": {
                                    "v1": "billing_details[name]"
                                },
                                "type": "name"
                            },
                            {
                                "api_path": {
                                    "v1": "billing_details[email]"
                                },
                                "type": "email"
                            },
                            {
                                "for": "phone",
                                "type": "placeholder"
                            },
                            {
                                "allowed_country_codes": null,
                                "type": "billing_address"
                            }
                        ],
                        "next_action_spec": {
                            "confirm_response_status_specs": {
                                "requires_action": {
                                    "type": "redirect_to_url"
                                }
                            },
                            "post_confirm_handling_pi_status_specs": {
                                "requires_action": {
                                    "type": "canceled"
                                },
                                "succeeded": {
                                    "type": "finished"
                                }
                            }
                        },
                        "selector_icon": {
                            "light_theme_png": "https:\/\/js.stripe.com\/v3\/fingerprinted\/img\/payment-methods\/icon-pm-afterpay@3x-6776ded2b20306c85d02639aea1e7dc5.png",
                            "light_theme_svg": "https:\/\/js.stripe.com\/v3\/fingerprinted\/img\/payment-methods\/icon-pm-afterpay-abedc6b87e4e9f917e22bbe6648ba809.svg"
                        },
                        "type": "afterpay_clearpay"
                    },
                    {
                        "async": false,
                        "fields": [],
                        "selector_icon": {
                            "light_theme_png": "https:\/\/js.stripe.com\/v3\/fingerprinted\/img\/payment-methods\/icon-pm-alipay@3x-d216a94882c3c5422274faaec75a3c81.png",
                            "light_theme_svg": "https:\/\/js.stripe.com\/v3\/fingerprinted\/img\/payment-methods\/alipay-22c167d415e209c71b2ac68b7fbc9f43.svg"
                        },
                        "type": "alipay"
                    },
                    {
                        "async": false,
                        "fields": [],
                        "next_action_spec": {
                            "confirm_response_status_specs": {
                                "requires_action": {
                                    "type": "redirect_to_url"
                                }
                            },
                            "post_confirm_handling_pi_status_specs": {
                                "requires_action": {
                                    "type": "canceled"
                                },
                                "succeeded": {
                                    "type": "finished"
                                }
                            }
                        },
                        "selector_icon": {
                            "dark_theme_png": "https:\/\/js.stripe.com\/v3\/fingerprinted\/img\/payment-methods\/icon-pm-amazonpay_dark-9e42e4498e0bee515c423ef66010852f.png",
                            "dark_theme_svg": "https:\/\/js.stripe.com\/v3\/fingerprinted\/img\/payment-methods\/icon-pm-amazonpay_dark-d3ef2eeefed43cea1f969c3a2a4118d8.svg",
                            "light_theme_png": "https:\/\/js.stripe.com\/v3\/fingerprinted\/img\/payment-methods\/icon-pm-amazonpay_light-5694f2030b236ad5410b5b7e52bb538c.png",
                            "light_theme_svg": "https:\/\/js.stripe.com\/v3\/fingerprinted\/img\/payment-methods\/icon-pm-amazonpay_light-f932bc37514fd6dd1f66006767287085.svg"
                        },
                        "type": "amazon_pay"
                    },
                    {
                        "async": false,
                        "fields": [],
                        "type": "card"
                    },
                    {
                        "async": false,
                        "fields": [],
                        "selector_icon": {
                            "light_theme_png": "https:\/\/js.stripe.com\/v3\/fingerprinted\/img\/payment-methods\/icon-pm-cashapp@3x-a89c5d8d0651cae2a511bb49a6be1cfc.png",
                            "light_theme_svg": "https:\/\/js.stripe.com\/v3\/fingerprinted\/img\/payment-methods\/icon-pm-cashapp-981164a833e417d28a8ac2684fda2324.svg"
                        },
                        "type": "cashapp"
                    },
                    {
                        "async": false,
                        "fields": [
                            {
                                "type": "klarna_header"
                            },
                            {
                                "for": "name",
                                "type": "placeholder"
                            },
                            {
                                "api_path": {
                                    "v1": "billing_details[email]"
                                },
                                "type": "email"
                            },
                            {
                                "for": "phone",
                                "type": "placeholder"
                            },
                            {
                                "api_path": {
                                    "v1": "billing_details[address][country]"
                                },
                                "type": "klarna_country"
                            },
                            {
                                "for": "billing_address_without_country",
                                "type": "placeholder"
                            }
                        ],
                        "next_action_spec": {
                            "confirm_response_status_specs": {
                                "requires_action": {
                                    "type": "redirect_to_url"
                                }
                            },
                            "post_confirm_handling_pi_status_specs": {
                                "requires_action": {
                                    "type": "canceled"
                                },
                                "succeeded": {
                                    "type": "finished"
                                }
                            }
                        },
                        "selector_icon": {
                            "light_theme_png": "https:\/\/js.stripe.com\/v3\/fingerprinted\/img\/payment-methods\/icon-pm-klarna@3x-d8624aa9a5662d719a44d16b9fcca0be.png",
                            "light_theme_svg": "https:\/\/js.stripe.com\/v3\/fingerprinted\/img\/payment-methods\/icon-pm-klarna-bb91aa8f173a3c72931696b0f752ec73.svg"
                        },
                        "type": "klarna"
                    },
                    {
                        "async": false,
                        "fields": [],
                        "selector_icon": {
                            "light_theme_png": "https:\/\/js.stripe.com\/v3\/fingerprinted\/img\/payment-methods\/icon-pm-wechat-pay@3x-ce6c167dcedb7faa3510e7f912518ead.png",
                            "light_theme_svg": "https:\/\/js.stripe.com\/v3\/fingerprinted\/img\/payment-methods\/icon-pm-wechat-pay-f62a5a27f646cb5f596c610475d14444.svg"
                        },
                        "type": "wechat_pay"
                    }
                ],
                "paypal_express_config": {
                    "client_id": null,
                    "paypal_merchant_id": null
                },
                "prefill_selectors": {
                    "default_values": {
                        "email": []
                    }
                },
                "session_id": "elements_session_0lFa7ifluGm",
                "shipping_address_settings": {
                    "autocomplete_allowed": true
                },
                "unactivated_payment_method_types": [],
                "unverified_payment_methods_on_domain": [
                    "apple_pay"
                ]
            }
        """.trimIndent()
    )
}
