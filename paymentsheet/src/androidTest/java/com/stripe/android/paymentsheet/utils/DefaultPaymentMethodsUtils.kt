package com.stripe.android.paymentsheet.utils

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeTestRule
import com.stripe.android.model.PaymentMethod
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers
import com.stripe.android.paymentsheet.ExperimentalCustomerSessionApi
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.ui.SAVED_PAYMENT_OPTION_TAB_LAYOUT_TEST_TAG
import com.stripe.android.testing.PaymentMethodFactory
import org.json.JSONArray

internal object DefaultPaymentMethodsUtils {
    fun enqueueElementsSessionResponse(
        networkRule: NetworkRule,
        isDeferredIntent: Boolean = false,
        cards: List<PaymentMethod> = emptyList(),
        defaultPaymentMethod: String? = null,
    ) {
        networkRule.enqueue(
            RequestMatchers.host("api.stripe.com"),
            RequestMatchers.method("GET"),
            RequestMatchers.path("/v1/elements/sessions"),
        ) { response ->
            response.setBody(
                createElementsSessionResponse(
                    cards = cards,
                    defaultPaymentMethod = defaultPaymentMethod,
                    isDeferredIntent = isDeferredIntent,
                )
            )
        }
    }

    @OptIn(ExperimentalCustomerSessionApi::class)
    fun launch(
        testContext: ProductIntegrationTestRunnerContext,
        composeTestRule: ComposeTestRule,
        paymentMethodLayout: PaymentSheet.PaymentMethodLayout,
        paymentMethodType: PaymentMethodType = PaymentMethodType.Card,
        hasSavedPaymentMethods: Boolean = true,
        isDeferredIntent: Boolean = false,
    ) {
        paymentMethodType.paymentMethodSetup()

        testContext.launch(
            configuration = PaymentSheet.Configuration(
                merchantDisplayName = "Example, Inc.",
                paymentMethodLayout = paymentMethodLayout,
                customer = PaymentSheet.CustomerConfiguration.createWithCustomerSession(
                    id = "cus_1",
                    clientSecret = "cuss_1",
                ),
                allowsDelayedPaymentMethods = true,
            ),
            isDeferredIntent = isDeferredIntent,
        )

        if (paymentMethodLayout == PaymentSheet.PaymentMethodLayout.Horizontal && hasSavedPaymentMethods) {
            composeTestRule.waitUntil(timeoutMillis = 5_000) {
                composeTestRule.onAllNodes(hasTestTag(SAVED_PAYMENT_OPTION_TAB_LAYOUT_TEST_TAG)).fetchSemanticsNodes()
                    .isNotEmpty()
            }
        }
    }

    private fun createElementsSessionResponse(
        cards: List<PaymentMethod>,
        defaultPaymentMethod: String?,
        isDeferredIntent: Boolean,
    ): String {
        val cardsArray = JSONArray()

        cards.forEach { card ->
            cardsArray.put(PaymentMethodFactory.convertCardToJson(card))
        }

        val cardsStringified = cardsArray.toString(2)

        return if (isDeferredIntent) {
            deferredIntentElementsSessionResponse(
                paymentMethods = cardsStringified,
                defaultPaymentMethod = defaultPaymentMethod,
            )
        } else {
            intentFirstElementsSessionResponse(
                paymentMethods = cardsStringified,
                defaultPaymentMethod = defaultPaymentMethod,
            )
        }
    }

    private fun intentFirstElementsSessionResponse(
        paymentMethods: String,
        defaultPaymentMethod: String?,
    ): String {
        return """
                {
                  "business_name": "Mobile Example Account",
                  "google_pay_preference": "enabled",
                  "merchant_country": "US",
                  "merchant_currency": "usd",
                  "merchant_id": "acct_1HvTI7Lu5o3P18Zp",
                  "meta_pay_signed_container_context": null,
                  "order": null,
                  "ordered_payment_method_types_and_wallets": [
                    "card",
                    "us_bank_account"
                  ],
                  "customer": {
                    "payment_methods": $paymentMethods,
                    "customer_session": {
                      "id": "cuss_654321",
                      "livemode": false,
                      "api_key": "ek_12345",
                      "api_key_expiry": 1899787184,
                      "customer": "cus_1",
                      "components": {
                        "mobile_payment_element": {
                          "enabled": true,
                          "features": {
                            "payment_method_save": "enabled",
                            "payment_method_remove": "enabled",
                            "payment_method_remove_last": "enabled",
                            "payment_method_save_allow_redisplay_override": null,
                            "payment_method_set_as_default": "enabled"
                          }
                        },
                        "customer_sheet": {
                          "enabled": false,
                          "features": null
                        }
                      }
                    },
                    "default_payment_method": $defaultPaymentMethod
                  },
                  "payment_method_preference": {
                    "object": "payment_method_preference",
                    "country_code": "US",
                    "ordered_payment_method_types": [
                      "card",
                       "us_bank_account"
                    ],
                    "payment_intent": {
                      "id": "pi_example",
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
                      "client_secret": "pi_example_secret_example",
                      "confirmation_method": "automatic",
                      "created": 1674750417,
                      "currency": "usd",
                      "description": null,
                      "last_payment_error": null,
                      "livemode": false,
                      "next_action": null,
                      "payment_method": null,
                      "payment_method_options": {
                        "us_bank_account": {
                          "verification_method": "automatic"
                        }
                      },
                      "payment_method_types": [
                        "card",
                        "us_bank_account"
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
                  "paypal_express_config": {
                    "client_id": null,
                    "paypal_merchant_id": null
                  },
                  "shipping_address_settings": {
                    "autocomplete_allowed": true
                  },
                  "unactivated_payment_method_types": []
                }
            """.trimIndent()
    }

    private fun deferredIntentElementsSessionResponse(
        paymentMethods: String,
        defaultPaymentMethod: String?,
    ): String {
        return """
                {
                  "business_name": "Mobile Example Account",
                  "google_pay_preference": "enabled",
                  "merchant_country": "US",
                  "merchant_currency": "usd",
                  "merchant_id": "acct_1HvTI7Lu5o3P18Zp",
                  "meta_pay_signed_container_context": null,
                  "order": null,
                  "ordered_payment_method_types_and_wallets": [
                    "card",
                    "us_bank_account"
                  ],
                  "customer": {
                    "payment_methods": $paymentMethods,
                    "customer_session": {
                      "id": "cuss_654321",
                      "livemode": false,
                      "api_key": "ek_12345",
                      "api_key_expiry": 1899787184,
                      "customer": "cus_1",
                      "components": {
                        "mobile_payment_element": {
                          "enabled": true,
                          "features": {
                            "payment_method_save": "enabled",
                            "payment_method_remove": "enabled",
                            "payment_method_remove_last": "enabled",
                            "payment_method_save_allow_redisplay_override": null,
                            "payment_method_set_as_default": "enabled"
                          }
                        },
                        "customer_sheet": {
                          "enabled": false,
                          "features": null
                        }
                      }
                    },
                    "default_payment_method": $defaultPaymentMethod
                  },
                  "payment_method_preference": {
                    "object": "payment_method_preference",
                    "country_code": "US",
                    "ordered_payment_method_types": [
                      "card",
                      "us_bank_account"
                    ],
                    "type": "deferred_intent"
                  },
                  "paypal_express_config": {
                    "client_id": null,
                    "paypal_merchant_id": null
                  },
                  "shipping_address_settings": {
                    "autocomplete_allowed": true
                  },
                  "unactivated_payment_method_types": []
                }
            """.trimIndent()
    }
}
