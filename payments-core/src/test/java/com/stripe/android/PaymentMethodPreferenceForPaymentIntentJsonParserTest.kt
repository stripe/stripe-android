package com.stripe.android

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.parsers.PaymentMethodPreferenceForPaymentIntentJsonParser
import org.json.JSONObject
import org.junit.Test

class PaymentMethodPreferenceForPaymentIntentJsonParserTest {

    @Test
    fun `Test ordered payment methods returned in PI payment_method_type variable`() {
        val parsedData = PaymentMethodPreferenceForPaymentIntentJsonParser().parse(
            JSONObject(
                PI_WITH_CARD_AFTERPAY_AU_BECS
            )
        )
        assertThat(parsedData?.intent?.paymentMethodTypes).isEqualTo(
            listOf(
                "au_becs_debit",
                "afterpay_clearpay",
                "card"
            )
        )
    }

    companion object {
        val PI_WITH_CARD_AFTERPAY_AU_BECS = """
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
}
