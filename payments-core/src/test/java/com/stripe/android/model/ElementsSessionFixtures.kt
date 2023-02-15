package com.stripe.android.model

import org.json.JSONObject

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
            "link_only_for_payment_method_types_enabled": false
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
            "link_only_for_payment_method_types_enabled": false
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
}
