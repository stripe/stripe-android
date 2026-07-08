package com.stripe.android.model.parsers

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.LinkPaymentDetails
import org.json.JSONObject
import org.junit.Test

class PaymentMethodWithLinkDetailsJsonParserTest {

    @Test
    fun `Supports payment method that has no Link payment details`() {
        val json = JSONObject().apply {
            put("payment_method", PAYMENT_METHOD_JSON)
        }
        val paymentMethod = PaymentMethodWithLinkDetailsJsonParser.parse(json)

        assertThat(paymentMethod.linkPaymentDetails).isNull()
        assertThat(paymentMethod.isLinkPassthroughMode).isFalse()
    }

    @Test
    fun `Marks payment method as Link passthrough when it has Link origin and no Link payment details`() {
        val json = JSONObject().apply {
            put("payment_method", CARD_PAYMENT_METHOD_JSON)
            put("is_link_origin", true)
        }
        val paymentMethod = PaymentMethodWithLinkDetailsJsonParser.parse(json)

        assertThat(paymentMethod.linkPaymentDetails).isNull()
        assertThat(paymentMethod.isLinkPassthroughMode).isTrue()
    }

    @Test
    fun `Supports payment method that has Link payment details of type CARD`() {
        val linkPaymentDetails = CONSUMER_PAYMENT_DETAILS_JSON
            .getJSONArray("redacted_payment_details").getJSONObject(0)
        val json = JSONObject().apply {
            put("payment_method", PAYMENT_METHOD_JSON)
            put("link_payment_details", linkPaymentDetails)
            put("is_link_origin", true)
        }
        val paymentMethod = PaymentMethodWithLinkDetailsJsonParser.parse(json)

        assertThat(paymentMethod.linkPaymentDetails).isNotNull()
        assertThat(paymentMethod.isLinkPassthroughMode).isFalse()
    }

    @Test
    fun `Supports payment method that has Link payment details of type BANK_ACCOUNT`() {
        val linkPaymentDetails = CONSUMER_PAYMENT_DETAILS_JSON
            .getJSONArray("redacted_payment_details").getJSONObject(2)
        val json = JSONObject().apply {
            put("payment_method", PAYMENT_METHOD_JSON)
            put("link_payment_details", linkPaymentDetails)
        }
        val paymentMethod = PaymentMethodWithLinkDetailsJsonParser.parse(json)

        assertThat(paymentMethod).isNotNull()
        assertThat(paymentMethod.linkPaymentDetails).isNotNull()
    }

    @Test
    fun `Supports payment method that has Link payment details of unknown type`() {
        val linkPaymentDetails = JSONObject().apply {
            put("id", "UNKNOWN123")
            put("type", "Crypto")
            put("is_default", false)
            put("next_action_types", org.json.JSONArray())
            put(
                "display",
                JSONObject().apply {
                    put("label", "Klarna")
                    put("last4", "1234")
                }
            )
        }
        val json = JSONObject().apply {
            put("payment_method", PAYMENT_METHOD_JSON)
            put("link_payment_details", linkPaymentDetails)
        }
        val paymentMethod = PaymentMethodWithLinkDetailsJsonParser.parse(json)

        assertThat(paymentMethod).isNotNull()
        assertThat(paymentMethod.linkPaymentDetails).isInstanceOf(LinkPaymentDetails.Generic::class.java as Class<*>)
    }

    @Test
    fun `Does not support method that has Link payment details of unsupported type`() {
        val linkPaymentDetails = JSONObject().apply {
            put("id", "UNKNOWN123")
            put("type", "KLARNA")
            put("is_default", false)
        }
        val json = JSONObject().apply {
            put("payment_method", PAYMENT_METHOD_JSON)
            put("link_payment_details", linkPaymentDetails)
        }
        val paymentMethod = PaymentMethodWithLinkDetailsJsonParser.parse(json)

        assertThat(paymentMethod).isNotNull()
        assertThat(paymentMethod.linkPaymentDetails).isNull()
    }

    private companion object {
        val PAYMENT_METHOD_JSON = JSONObject(
            """
            {
              "id": "pm_1FSQaJCR",
              "object": "payment_method",
              "billing_details": null,
              "created": 1570809799,
              "customer": null,
              "livemode": false,
              "metadata": null,
              "allow_redisplay": "unspecified",
              "link": {
                "email": "email@email.com"
              },
              "type": "link"
            }
            """.trimIndent()
        )

        val CARD_PAYMENT_METHOD_JSON = JSONObject(
            """
            {
              "id": "pm_1QoYo1Esh8quxL21bpIFTRrP",
              "object": "payment_method",
              "allow_redisplay": "always",
              "billing_details": {
                "address": {
                  "city": null,
                  "country": "US",
                  "line1": null,
                  "line2": null,
                  "postal_code": "12345",
                  "state": null
                },
                "email": null,
                "name": null,
                "phone": null,
                "tax_id": null
              },
              "card": {
                "brand": "visa",
                "checks": {
                  "address_line1_check": null,
                  "address_postal_code_check": null,
                  "cvc_check": null
                },
                "country": "US",
                "display_brand": "visa",
                "exp_month": 4,
                "exp_year": 2026,
                "funding": "credit",
                "generated_from": null,
                "last4": "4242",
                "networks": {
                  "available": [
                    "visa"
                  ],
                  "preferred": null
                },
                "regulated_status": "unregulated",
                "three_d_secure_usage": {
                  "supported": true
                },
                "wallet": null
              },
              "created": 1738624313,
              "customer": "cus_Qurj8RK03RchBB",
              "livemode": false,
              "radar_options": {},
              "type": "card"
            }
            """.trimIndent()
        )

        val CONSUMER_PAYMENT_DETAILS_JSON = JSONObject(
            """
            {
              "redacted_payment_details": [
                {
                  "id": "QAAAKJ6",
                  "bank_account_details": null,
                  "billing_address": {
                    "administrative_area": null,
                    "country_code": "US",
                    "dependent_locality": null,
                    "line_1": null,
                    "line_2": null,
                    "locality": null,
                    "name": null,
                    "postal_code": "12312",
                    "sorting_code": null
                  },
                  "billing_email_address": "",
                  "card_details": {
                    "brand": "MASTERCARD",
                    "checks": {
                      "address_line1_check": "STATE_INVALID",
                      "address_postal_code_check": "PASS",
                      "cvc_check": "PASS"
                    },
                    "funding": "CREDIT",
                    "exp_month": 12,
                    "exp_year": 2023,
                    "last4": "4444"
                  },
                  "is_default": true,
                  "type": "CARD"
                },
                {
                  "id": "QAAAKIL",
                  "bank_account_details": null,
                  "billing_address": {
                    "administrative_area": null,
                    "country_code": "US",
                    "dependent_locality": null,
                    "line_1": null,
                    "line_2": null,
                    "locality": null,
                    "name": null,
                    "postal_code": "42424",
                    "sorting_code": null
                  },
                  "billing_email_address": "",
                  "card_details": {
                    "brand": "VISA",
                    "checks": {
                      "address_line1_check": "STATE_INVALID",
                      "address_postal_code_check": "PASS",
                      "cvc_check": "FAIL"
                    },
                    "funding": "CREDIT",
                    "exp_month": 4,
                    "exp_year": 2024,
                    "last4": "4242"
                  },
                  "is_default": false,
                  "type": "CARD"
                },
                {
                  "id": "wAAACGA",
                  "bank_account_details": {
                    "bank_icon_code": null,
                    "bank_account_name": "STRIPE TEST BANK ACCOUNT",
                    "last4": "6789"
                  },
                  "billing_address": {
                    "administrative_area": null,
                    "country_code": null,
                    "dependent_locality": null,
                    "line_1": null,
                    "line_2": null,
                    "locality": null,
                    "name": null,
                    "postal_code": null,
                    "sorting_code": null
                  },
                  "billing_email_address": "",
                  "card_details": null,
                  "is_default": false,
                  "type": "BANK_ACCOUNT"
                }
              ]
            }
            """.trimIndent()
        )
    }
}
