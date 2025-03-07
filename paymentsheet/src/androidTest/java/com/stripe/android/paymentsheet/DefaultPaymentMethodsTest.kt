package com.stripe.android.paymentsheet

import android.content.Context
import androidx.compose.ui.test.hasTestTag
import androidx.test.core.app.ApplicationProvider
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.stripe.android.core.utils.urlEncode
import com.stripe.android.model.PaymentMethod
import com.stripe.android.networktesting.RequestMatchers
import com.stripe.android.networktesting.RequestMatchers.bodyPart
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentsheet.ui.SAVED_PAYMENT_OPTION_TAB_LAYOUT_TEST_TAG
import com.stripe.android.paymentsheet.utils.PaymentSheetLayoutType
import com.stripe.android.paymentsheet.utils.PaymentSheetLayoutTypeProvider
import com.stripe.android.paymentsheet.utils.ProductIntegrationTestRunnerContext
import com.stripe.android.paymentsheet.utils.ProductIntegrationType
import com.stripe.android.paymentsheet.utils.ProductIntegrationTypeProvider
import com.stripe.android.paymentsheet.utils.TestRules
import com.stripe.android.paymentsheet.utils.assertCompleted
import com.stripe.android.paymentsheet.utils.runProductIntegrationTest
import com.stripe.android.testing.PaymentMethodFactory
import org.json.JSONArray
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCustomerSessionApi::class)
@RunWith(TestParameterInjector::class)
internal class DefaultPaymentMethodsTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @get:Rule
    val testRules: TestRules = TestRules.create()

    private val composeTestRule = testRules.compose
    private val networkRule = testRules.networkRule

    @TestParameter(valuesProvider = ProductIntegrationTypeProvider::class)
    lateinit var integrationType: ProductIntegrationType

    @TestParameter(valuesProvider = PaymentSheetLayoutTypeProvider::class)
    lateinit var layoutType: PaymentSheetLayoutType

    @Test
    fun setDefaultCard_selectsCard() = runProductIntegrationTest(
        networkRule = networkRule,
        integrationType = integrationType,
        resultCallback = {},
    ) { testContext ->
        val cards = listOf(
            PaymentMethodFactory.card(last4 = "4242", id = "pm_1"),
            PaymentMethodFactory.card(last4 = "1001", id = "pm_2")
        )

        enqueueElementsSessionResponse(
            cards = cards,
        )

        launch(
            testContext = testContext,
            paymentMethodLayout = layoutType.paymentMethodLayout,
        )

        val originallySelectedPaymentMethod = cards[0]
        val newDefaultPaymentMethod = cards[1]

        layoutType.assertHasSelectedPaymentMethod(
            composeTestRule = composeTestRule,
            context = context,
            paymentMethod = originallySelectedPaymentMethod,
        )

        enqueueSetDefaultPaymentMethodRequest()

        layoutType.setDefaultPaymentMethod(
            composeTestRule = composeTestRule,
            newDefaultPaymentMethod = newDefaultPaymentMethod,
        )

        layoutType.assertHasSelectedPaymentMethod(
            composeTestRule = composeTestRule,
            context = context,
            paymentMethod = newDefaultPaymentMethod,
        )

        testContext.markTestSucceeded()
    }

    private fun enqueueSetDefaultPaymentMethodRequest() {
        networkRule.enqueue(
            RequestMatchers.host("api.stripe.com"),
            RequestMatchers.method("POST"),
            RequestMatchers.path("/v1/elements/customers/cus_1/set_default_payment_method"),
        ) { response ->
            response.setResponseCode(200)
            response.setBody(
                """{
                          "id": "cus_1",
                          "object": "customer",
                          "created": 1739227546,
                          "default_source": null,
                          "description": null,
                          "email": null,
                          "livemode": false,
                          "shipping": null
                        }
                    """.trimIndent()
            )
        }
    }

    @Test
    fun defaultPaymentMethod_displayedWithDefaultBadge() = runProductIntegrationTest(
        networkRule = networkRule,
        integrationType = integrationType,
        resultCallback = {},
    ) { testContext ->
        val cards = listOf(
            PaymentMethodFactory.card(last4 = "4242", id = "pm_1"),
            PaymentMethodFactory.card(last4 = "1001", id = "pm_2")
        )

        enqueueElementsSessionResponse(
            cards = cards,
            defaultPaymentMethod = cards.first().id
        )

        launch(
            testContext = testContext,
            paymentMethodLayout = layoutType.paymentMethodLayout,
        )

        layoutType.assertDefaultPaymentMethodBadgeDisplayed(composeTestRule)

        testContext.markTestSucceeded()
    }
    
    @Test
    fun defaultPaymentMethod_isSelected() = runProductIntegrationTest(
        networkRule = networkRule,
        integrationType = integrationType,
        resultCallback = {},
    ) { testContext ->
        val cards = listOf(
            PaymentMethodFactory.card(last4 = "4242", id = "pm_1"),
            PaymentMethodFactory.card(last4 = "1001", id = "pm_2")
        )
        val defaultPaymentMethod = cards[1]

        enqueueElementsSessionResponse(
            cards = cards,
            defaultPaymentMethod = defaultPaymentMethod.id
        )

        launch(
            testContext = testContext,
            paymentMethodLayout = layoutType.paymentMethodLayout,
        )

        layoutType.assertHasSelectedPaymentMethod(
            composeTestRule = composeTestRule,
            context = context,
            paymentMethod = defaultPaymentMethod,
        )

        testContext.markTestSucceeded()
    }

    @Test
    fun setNewCardAsDefault_sendsSetAsDefaultParamInConfirmCall() = runProductIntegrationTest(
        networkRule = networkRule,
        integrationType = integrationType,
        resultCallback = ::assertCompleted,
    ) { testContext ->
        val paymentSheetPage = PaymentSheetPage(composeTestRule)

        enqueueElementsSessionResponse()

        launch(
            testContext = testContext,
            paymentMethodLayout = layoutType.paymentMethodLayout,
            hasSavedPaymentMethods = false,
        )

        enqueueGetCardMetadata()

        paymentSheetPage.fillOutCardDetails()
        paymentSheetPage.checkSaveForFuture()
        paymentSheetPage.checkSetAsDefaultCheckbox()

        enqueuePaymentIntentConfirmWithExpectedSetAsDefault(setAsDefaultValue = true)

        paymentSheetPage.clickPrimaryButton()
    }

    @Test
    fun payWithNewCard_dontCheckSetAsDefault_sendsSetAsDefaultAsFalseParamInConfirmCall() = runProductIntegrationTest(
        networkRule = networkRule,
        integrationType = integrationType,
        resultCallback = ::assertCompleted,
    ) { testContext ->
        val paymentSheetPage = PaymentSheetPage(composeTestRule)

        enqueueElementsSessionResponse()

        launch(
            testContext = testContext,
            paymentMethodLayout = layoutType.paymentMethodLayout,
            hasSavedPaymentMethods = false,
        )

        enqueueGetCardMetadata()

        paymentSheetPage.fillOutCardDetails()
        paymentSheetPage.checkSaveForFuture()

        enqueuePaymentIntentConfirmWithExpectedSetAsDefault(setAsDefaultValue = false)

        paymentSheetPage.clickPrimaryButton()
    }

    @Test
    fun payWithNewCard_uncheckSaveForFuture_doesNotSendSetAsDefaultInConfirmCall() = runProductIntegrationTest(
        networkRule = networkRule,
        integrationType = integrationType,
        resultCallback = ::assertCompleted,
    ) { testContext ->
        val paymentSheetPage = PaymentSheetPage(composeTestRule)

        enqueueElementsSessionResponse()

        launch(
            testContext = testContext,
            paymentMethodLayout = layoutType.paymentMethodLayout,
            hasSavedPaymentMethods = false,
        )

        enqueueGetCardMetadata()

        paymentSheetPage.fillOutCardDetails()
        paymentSheetPage.checkSaveForFuture()
        paymentSheetPage.checkSetAsDefaultCheckbox()

        // Un-check save for future -- this will hide the set as default checkbox
        paymentSheetPage.checkSaveForFuture()

        enqueuePaymentIntentConfirmWithoutSetAsDefault()

        paymentSheetPage.clickPrimaryButton()
    }

    private fun enqueueGetCardMetadata() {
        networkRule.enqueue(
            method("GET"),
            path("edge-internal/card-metadata")
        ) { response ->
            response.testBodyFromFile("card-metadata-get.json")
        }
    }

    private fun enqueuePaymentIntentConfirmWithoutSetAsDefault() {
        return networkRule.enqueue(
            method("POST"),
            path("/v1/payment_intents/pi_example/confirm"),
            bodyPart(urlEncode("payment_method_data[allow_redisplay]"), "unspecified"),
        ) { response ->
            response.testBodyFromFile("payment-intent-confirm.json")
        }
    }

    private fun enqueuePaymentIntentConfirmWithExpectedSetAsDefault(setAsDefaultValue: Boolean) {
        return networkRule.enqueue(
            method("POST"),
            path("/v1/payment_intents/pi_example/confirm"),
            bodyPart(urlEncode("payment_method_data[allow_redisplay]"), "always"),
            bodyPart(urlEncode("set_as_default_payment_method"), setAsDefaultValue.toString())
        ) { response ->
            response.testBodyFromFile("payment-intent-confirm.json")
        }

    private fun enqueueElementsSessionResponse(
        cards: List<PaymentMethod> = emptyList(),
        setAsDefaultFeatureEnabled: Boolean = true,
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
                    setAsDefaultFeatureEnabled = setAsDefaultFeatureEnabled,
                    defaultPaymentMethod = defaultPaymentMethod,
                )
            )
        }
    }

    private fun launch(
        testContext: ProductIntegrationTestRunnerContext,
        paymentMethodLayout: PaymentSheet.PaymentMethodLayout,
        hasSavedPaymentMethods: Boolean = true,
    ) {
        testContext.launch(
            configuration = PaymentSheet.Configuration(
                merchantDisplayName = "Example, Inc.",
                paymentMethodLayout = paymentMethodLayout,
                customer = PaymentSheet.CustomerConfiguration.createWithCustomerSession(
                    id = "cus_1",
                    clientSecret = "cuss_1",
                )
            ),
        )

        if (
            paymentMethodLayout == PaymentSheet.PaymentMethodLayout.Horizontal &&
            hasSavedPaymentMethods
        ) {
            composeTestRule.waitUntil(timeoutMillis = 5_000) {
                composeTestRule
                    .onAllNodes(hasTestTag(SAVED_PAYMENT_OPTION_TAB_LAYOUT_TEST_TAG))
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
        }
    }

    private companion object {
        @Suppress("LongMethod")
        fun createElementsSessionResponse(
            cards: List<PaymentMethod>,
            setAsDefaultFeatureEnabled: Boolean,
            defaultPaymentMethod: String?,
        ): String {
            val cardsArray = JSONArray()

            cards.forEach { card ->
                cardsArray.put(PaymentMethodFactory.convertCardToJson(card))
            }

            val cardsStringified = cardsArray.toString(2)

            val setAsDefaultFeatureEnabledStringified = setAsDefaultFeatureEnabled.toFeatureState()

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
                    "card"
                  ],
                  "card_brand_choice": {
                    "eligible": true,
                    "preferred_networks": ["cartes_bancaires"]
                  },
                  "customer": {
                    "payment_methods": $cardsStringified,
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
                            "payment_method_set_as_default": $setAsDefaultFeatureEnabledStringified
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
                      "card"
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
                        "card"
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
                      "fields": [],
                      "type": "card"
                    }
                  ],
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

        private fun Boolean.toFeatureState(): String {
            return if (this) {
                "enabled"
            } else {
                "disabled"
            }
        }
    }
}
