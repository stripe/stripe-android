package com.stripe.android.model

import org.json.JSONObject

/**
 * Fixtures for checkout session responses from `/v1/payment_pages/{id}/init` API.
 */
internal object CheckoutSessionFixtures {

    val CHECKOUT_SESSION_RESPONSE_JSON = JSONObject(
        """
        {
          "session_id": "cs_test_a1vLTpmgcJO40ZjQpd3GUNHwlwtkT1bejjhpfd0nN05iqoVuJziixjNYIh",
          "object": "checkout.session",
          "currency": "usd",
          "mode": "payment",
          "status": "open",
          "livemode": false,
          "line_item_group": {
            "currency": "usd",
            "total": 999,
            "subtotal": 999,
            "line_items": [
              {
                "id": "li_1SrjAuLu5o3P18ZpVBMMs98l",
                "object": "item",
                "name": "Llama Figure",
                "quantity": 1,
                "subtotal": 999,
                "total": 999
              }
            ]
          },
          "total_summary": {
            "due": 999,
            "subtotal": 999,
            "total": 999
          },
          "elements_session": {
            "account_id": "acct_1HvTI7Lu5o3P18Zp",
            "apple_pay_merchant_token_webhook_url": "https://pm-hooks.stripe.com/apple_pay/merchant_token/pDq7tf9uieoQWMVJixFwuOve/acct_1HvTI7Lu5o3P18Zp/",
            "apple_pay_preference": "enabled",
            "business_name": "Mobile Example Account",
            "capability_enabled_card_networks": [
              "cartes_bancaires",
              "jcb",
              "diners",
              "discover"
            ],
            "card_brand_choice": {
              "eligible": false,
              "preferred_networks": [
                "cartes_bancaires"
              ],
              "supported_cobranded_networks": {
                "cartes_bancaires": false
              }
            },
            "google_pay_preference": "enabled",
            "link_settings": {
              "link_authenticated_change_event_enabled": false,
              "link_bank_onboarding_enabled": false,
              "link_consumer_incentive": null,
              "link_default_opt_in": "FULL",
              "link_funding_sources": [
                "CARD",
                "BANK_ACCOUNT"
              ],
              "link_mode": "LINK_PAYMENT_METHOD",
              "link_mobile_disable_signup": false,
              "link_passthrough_mode_enabled": false
            },
            "merchant_country": "US",
            "merchant_currency": "usd",
            "merchant_id": "acct_1HvTI7Lu5o3P18Zp",
            "ordered_payment_method_types_and_wallets": [
              "card",
              "link",
              "cashapp",
              "google_pay",
              "apple_pay",
              "alipay",
              "wechat_pay",
              "us_bank_account",
              "amazon_pay",
              "afterpay_clearpay",
              "klarna",
              "crypto"
            ],
            "payment_method_preference": {
              "object": "payment_method_preference",
              "country_code": "US",
              "ordered_payment_method_types": [
                "card",
                "link",
                "cashapp",
                "alipay",
                "wechat_pay",
                "us_bank_account",
                "amazon_pay",
                "afterpay_clearpay",
                "klarna",
                "crypto"
              ],
              "type": "deferred_intent"
            },
            "payment_method_specs": [
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
                  "light_theme_png": "https://js.stripe.com/v3/fingerprinted/img/payment-methods/icon-pm-alipay@3x-d216a94882c3c5422274faaec75a3c81.png",
                  "light_theme_svg": "https://js.stripe.com/v3/fingerprinted/img/payment-methods/alipay-22c167d415e209c71b2ac68b7fbc9f43.svg"
                },
                "type": "alipay"
              },
              {
                "async": false,
                "fields": [],
                "selector_icon": {
                  "light_theme_png": "https://js.stripe.com/v3/fingerprinted/img/payment-methods/icon-pm-wechat-pay@3x-ce6c167dcedb7faa3510e7f912518ead.png",
                  "light_theme_svg": "https://js.stripe.com/v3/fingerprinted/img/payment-methods/icon-pm-wechat-pay-f62a5a27f646cb5f596c610475d14444.svg"
                },
                "type": "wechat_pay"
              }
            ],
            "session_id": "elements_session_1nWWJQ3A6yS",
            "unactivated_payment_method_types": [],
            "unverified_payment_methods_on_domain": [
              "apple_pay"
            ]
          },
          "server_built_elements_session_params": {
            "type": "deferred_intent",
            "locale": "en-US",
            "deferred_intent": {
              "mode": "payment",
              "amount": 999,
              "currency": "usd",
              "capture_method": "automatic_async",
              "payment_method_types": [
                "card",
                "afterpay_clearpay",
                "alipay",
                "klarna",
                "link",
                "us_bank_account",
                "wechat_pay",
                "cashapp",
                "amazon_pay",
                "crypto"
              ]
            }
          },
          "payment_method_types": [
            "card",
            "afterpay_clearpay",
            "alipay",
            "klarna",
            "link",
            "us_bank_account",
            "wechat_pay",
            "cashapp",
            "amazon_pay",
            "crypto"
          ]
        }
        """.trimIndent()
    )

    /**
     * Minimal elements_session JSON for edge case tests.
     */
    const val MINIMAL_ELEMENTS_SESSION_JSON = """
        {
            "session_id": "es_123",
            "merchant_country": "US",
            "google_pay_preference": "enabled",
            "payment_method_preference": {
                "object": "payment_method_preference",
                "country_code": "US",
                "ordered_payment_method_types": ["card"],
                "type": "deferred_intent"
            }
        }
    """

    /**
     * Confirm response with a succeeded PaymentIntent.
     */
    val CHECKOUT_SESSION_CONFIRM_SUCCEEDED_JSON = JSONObject(
        """
        {
            "session_id": "cs_test_a1vLTpmgcJO40ZjQpd3GUNHwlwtkT1bejjhpfd0nN05iqoVuJziixjNYIh",
            "currency": "usd",
            "total_summary": {
                "due": 999
            },
            "payment_intent": {
                "id": "pi_3QWK2VIyGgrkZxL71xfPBWG5",
                "object": "payment_intent",
                "amount": 999,
                "currency": "usd",
                "status": "succeeded",
                "client_secret": "pi_3QWK2VIyGgrkZxL71xfPBWG5_secret_abc123",
                "payment_method": "pm_1234",
                "payment_method_types": ["card"],
                "livemode": false,
                "created": 1734000000
            }
        }
        """.trimIndent()
    )

    /**
     * Confirm response with a PaymentIntent that requires action (3DS).
     */
    val CHECKOUT_SESSION_CONFIRM_REQUIRES_ACTION_JSON = JSONObject(
        """
        {
            "session_id": "cs_test_a1vLTpmgcJO40ZjQpd3GUNHwlwtkT1bejjhpfd0nN05iqoVuJziixjNYIh",
            "currency": "usd",
            "total_summary": {
                "due": 999
            },
            "payment_intent": {
                "id": "pi_3QWK2VIyGgrkZxL71xfPBWG5",
                "object": "payment_intent",
                "amount": 999,
                "currency": "usd",
                "status": "requires_action",
                "client_secret": "pi_3QWK2VIyGgrkZxL71xfPBWG5_secret_abc123",
                "payment_method": "pm_1234",
                "payment_method_types": ["card"],
                "livemode": false,
                "created": 1734000000,
                "next_action": {
                    "redirect_to_url": {
                        "return_url": "stripesdk://payment_return_url",
                        "url": "https://hooks.stripe.com/3d_secure_2_eap/begin_test/src_1Ecaz6CRMbs6FrXfuYKBRSUG/src_client_secret_test"
                    },
                    "type": "redirect_to_url"
                }
            }
        }
        """.trimIndent()
    )

    /**
     * Init response with customer data (for SPM support).
     * Customer is at the top level, not inside elements_session.
     */
    val CHECKOUT_SESSION_WITH_CUSTOMER_JSON = JSONObject(
        """
        {
            "session_id": "cs_test_abc123",
            "currency": "usd",
            "total_summary": {
                "due": 1000
            },
            "customer": {
                "id": "cus_test_customer",
                "payment_methods": [
                    {
                        "id": "pm_card_visa",
                        "object": "payment_method",
                        "type": "card",
                        "card": {
                            "brand": "visa",
                            "last4": "4242",
                            "exp_month": 12,
                            "exp_year": 2030
                        },
                        "livemode": false,
                        "created": 1734000000
                    },
                    {
                        "id": "pm_card_mastercard",
                        "object": "payment_method",
                        "type": "card",
                        "card": {
                            "brand": "mastercard",
                            "last4": "5555",
                            "exp_month": 6,
                            "exp_year": 2028
                        },
                        "livemode": false,
                        "created": 1734000000
                    }
                ]
            },
            "elements_session": $MINIMAL_ELEMENTS_SESSION_JSON
        }
        """.trimIndent()
    )

    /**
     * Init response with customer but no saved payment methods.
     */
    val CHECKOUT_SESSION_WITH_EMPTY_CUSTOMER_JSON = JSONObject(
        """
        {
            "session_id": "cs_test_abc123",
            "currency": "usd",
            "total_summary": {
                "due": 1000
            },
            "customer": {
                "id": "cus_test_empty_customer",
                "payment_methods": []
            },
            "elements_session": $MINIMAL_ELEMENTS_SESSION_JSON
        }
        """.trimIndent()
    )

    /**
     * Init response without customer (guest checkout).
     */
    val CHECKOUT_SESSION_WITHOUT_CUSTOMER_JSON = JSONObject(
        """
        {
            "session_id": "cs_test_abc123",
            "currency": "usd",
            "total_summary": {
                "due": 1000
            },
            "elements_session": $MINIMAL_ELEMENTS_SESSION_JSON
        }
        """.trimIndent()
    )

    /**
     * Init response with save offer enabled and status not_accepted.
     */
    val CHECKOUT_SESSION_WITH_SAVE_ENABLED_JSON = JSONObject(
        """
        {
            "session_id": "cs_test_abc123",
            "currency": "usd",
            "total_summary": {
                "due": 1000
            },
            "customer_managed_saved_payment_methods_offer_save": {
                "enabled": true,
                "status": "not_accepted"
            },
            "elements_session": $MINIMAL_ELEMENTS_SESSION_JSON
        }
        """.trimIndent()
    )

    /**
     * Init response with save offer enabled and status accepted.
     */
    val CHECKOUT_SESSION_WITH_SAVE_ACCEPTED_JSON = JSONObject(
        """
        {
            "session_id": "cs_test_abc123",
            "currency": "usd",
            "total_summary": {
                "due": 1000
            },
            "customer_managed_saved_payment_methods_offer_save": {
                "enabled": true,
                "status": "accepted"
            },
            "elements_session": $MINIMAL_ELEMENTS_SESSION_JSON
        }
        """.trimIndent()
    )

    /**
     * Init response with save offer disabled.
     */
    val CHECKOUT_SESSION_WITH_SAVE_DISABLED_JSON = JSONObject(
        """
        {
            "session_id": "cs_test_abc123",
            "currency": "usd",
            "total_summary": {
                "due": 1000
            },
            "customer_managed_saved_payment_methods_offer_save": {
                "enabled": false,
                "status": "not_accepted"
            },
            "elements_session": $MINIMAL_ELEMENTS_SESSION_JSON
        }
        """.trimIndent()
    )
}
