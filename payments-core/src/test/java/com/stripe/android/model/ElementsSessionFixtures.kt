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

    val EXPANDED_SETUP_INTENT_JSON_WITH_CBC_ELIGIBLE = JSONObject(
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

    val EXPANDED_PAYMENT_INTENT_JSON_WITH_CBC_ELIGIBLE = JSONObject(
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
            "eligible": false
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
                "elements_enable_payment_element_vertical_layout": false,
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
}
