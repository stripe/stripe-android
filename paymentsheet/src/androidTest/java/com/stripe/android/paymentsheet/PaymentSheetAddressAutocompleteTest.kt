package com.stripe.android.paymentsheet

import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.stripe.android.networktesting.RequestMatchers.bodyPart
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.networktesting.elementsSession
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentelement.AddressAutocompletePreview
import com.stripe.android.paymentsheet.utils.ApiConfigurationTestType
import com.stripe.android.paymentsheet.utils.ApiConfigurationTestTypeProvider
import com.stripe.android.paymentsheet.utils.TestRules
import com.stripe.android.paymentsheet.utils.assertCompleted
import com.stripe.android.paymentsheet.utils.runPaymentSheetTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(AddressAutocompletePreview::class)
@RunWith(TestParameterInjector::class)
internal class PaymentSheetAddressAutocompleteTest(
    @TestParameter(valuesProvider = ApiConfigurationTestTypeProvider::class)
    private val apiConfigurationTestType: ApiConfigurationTestType,
) {
    @get:Rule
    val testRules: TestRules = TestRules.create()

    private val composeTestRule = testRules.compose
    private val networkRule = testRules.networkRule

    private val paymentSheetPage = PaymentSheetPage(composeTestRule)

    @Suppress("DEPRECATION")
    @Test
    fun testUnfilled() = runPaymentSheetTest(
        apiConfigurationTestType = apiConfigurationTestType,
        networkRule = networkRule,
        composeTestRule = testRules.compose,
        resultCallback = ::assertCompleted,
    ) { context ->
        enqueueElementsSession()

        context.presentPaymentSheet {
            presentWithPaymentIntent(
                paymentIntentClientSecret = "pi_123_secret_123",
                configuration = apiConfigurationTestType.applyTo(
                    PaymentSheet.Configuration.Builder(merchantDisplayName = "Example, Inc.")
                        .billingDetailsCollectionConfiguration(
                            PaymentSheet.BillingDetailsCollectionConfiguration(
                                address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
                                attachDefaultsToPaymentMethod = true,
                            ),
                        )
                        .googlePlacesApiKey("gp_123")
                        .build()
                ),
            )
        }

        clickAndFillOutCard()

        paymentSheetPage.waitForText(text = "Address")
        paymentSheetPage.clickViewWithText(text = "Address")

        fillOutAutocompletePage()

        enqueueConfirm()

        paymentSheetPage.clickPrimaryButton()
    }

    @Suppress("DEPRECATION")
    @Test
    fun testPrefilled() = runPaymentSheetTest(
        apiConfigurationTestType = apiConfigurationTestType,
        networkRule = networkRule,
        composeTestRule = testRules.compose,
        resultCallback = ::assertCompleted,
    ) { context ->
        enqueueElementsSession()

        context.presentPaymentSheet {
            presentWithPaymentIntent(
                paymentIntentClientSecret = "pi_123_secret_123",
                configuration = apiConfigurationTestType.applyTo(
                    PaymentSheet.Configuration.Builder(merchantDisplayName = "Example, Inc.")
                        .billingDetailsCollectionConfiguration(
                            PaymentSheet.BillingDetailsCollectionConfiguration(
                                address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
                                attachDefaultsToPaymentMethod = true,
                            ),
                        )
                        .defaultBillingDetails(
                            PaymentSheet.BillingDetails(
                                address = PaymentSheet.Address(
                                    line1 = PREFILLED_AUTOCOMPLETE_QUERY,
                                    city = "Chicago",
                                    state = "IL",
                                    country = "US",
                                    postalCode = "83985"
                                )
                            )
                        )
                        .googlePlacesApiKey("gp_123")
                        .build()
                ),
            )
        }

        clickAndFillOutCard()

        paymentSheetPage.waitForContentDescription(description = "Search")
        enqueueAutocompletePredictions(query = PREFILLED_AUTOCOMPLETE_QUERY)
        paymentSheetPage.clickViewWithContentDescription(description = "Search")

        paymentSheetPage.waitForText(SELECTING_ADDRESS_SECONDARY_TEXT)
        enqueuePlaceFetch()
        paymentSheetPage.clickViewWithText(SELECTING_ADDRESS_SECONDARY_TEXT)

        enqueueConfirm()

        paymentSheetPage.clickPrimaryButton()
    }

    private fun enqueueAutocompletePredictions(query: String) {
        networkRule.enqueue(
            method("POST"),
            path("/v1/elements/address/autocomplete"),
            bodyPart("search_text", query),
        ) { response ->
            response.setBody(
                """
                {
                  "source": "google",
                  "suggestions": [
                    {
                      "address": null,
                      "display_data": {
                        "subtitle": "$SELECTING_ADDRESS_SECONDARY_TEXT",
                        "title": "123 Main Street"
                      },
                      "place_id": "$PLACE_ID"
                    },
                    {
                      "address": null,
                      "display_data": {
                        "subtitle": "456 Main Street, Unit #123, New York, New York, US 52523",
                        "title": "123 Main Street"
                      },
                      "place_id": "2"
                    }
                  ]
                }
                """.trimIndent()
            )
        }
    }

    private fun enqueuePlaceFetch() {
        networkRule.enqueue(
            method("POST"),
            path("/v1/elements/address/details"),
            bodyPart("place_id", PLACE_ID),
        ) { response ->
            response.setBody(
                """
                {
                  "address": {
                    "line1": "123 Main Street",
                    "line2": "Unit #123",
                    "city": "South San Francisco",
                    "state": "CA",
                    "postal_code": "94111",
                    "country": "US"
                  }
                }
                """.trimIndent()
            )
        }
    }

    private fun enqueueElementsSession() {
        networkRule.elementsSession { response ->
            response.testBodyFromFile("elements-sessions-requires_payment_method.json") { json ->
                json.getJSONObject("flags")
                    .put("ocs_mobile_should_use_autocomplete_proxy_endpoints", true)
            }
        }
    }

    private fun enqueueConfirm() {
        networkRule.enqueue(
            method("POST"),
            path("/v1/payment_intents/pi_123/confirm"),
            bodyPart(
                "payment_method_data[billing_details][address][line1]",
                "123 Main Street"
            ),
            bodyPart(
                "payment_method_data[billing_details][address][line2]",
                "Unit #123"
            ),
            bodyPart(
                "payment_method_data[billing_details][address][city]",
                "South San Francisco"
            ),
            bodyPart("payment_method_data[billing_details][address][state]", "CA"),
            bodyPart("payment_method_data[billing_details][address][country]", "US"),
            bodyPart("payment_method_data[billing_details][address][postal_code]", "94111"),
        ) { response ->
            response.testBodyFromFile("payment-intent-confirm.json")
        }
    }

    private fun clickAndFillOutCard() {
        paymentSheetPage.clickOnLpm(code = "card", forVerticalMode = true)
        paymentSheetPage.fillOutCardDetails(fillOutZipCode = false)
    }

    private fun fillOutAutocompletePage() {
        enqueueAutocompletePredictions(query = AUTOCOMPLETE_QUERY)
        paymentSheetPage.clickAndFillField(label = "Address", text = AUTOCOMPLETE_QUERY)

        paymentSheetPage.waitForText(SELECTING_ADDRESS_SECONDARY_TEXT)
        enqueuePlaceFetch()
        paymentSheetPage.clickViewWithText(SELECTING_ADDRESS_SECONDARY_TEXT)
    }

    private companion object {
        const val AUTOCOMPLETE_QUERY = "Main Street"
        const val PREFILLED_AUTOCOMPLETE_QUERY = "123 Coffee Street"
        const val PLACE_ID = "1"
        const val SELECTING_ADDRESS_SECONDARY_TEXT = "123 Main Street, Unit #123, South San Francisco, CA, US 94111"
    }
}
