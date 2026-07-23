package com.stripe.android.paymentsheet

import android.text.SpannableString
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.stripe.android.networktesting.RequestMatchers.bodyPart
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.networktesting.elementsSession
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentelement.AddressAutocompletePreview
import com.stripe.android.paymentsheet.utils.PlacesClientProxyTestRule
import com.stripe.android.paymentsheet.utils.TestRules
import com.stripe.android.paymentsheet.utils.assertCompleted
import com.stripe.android.paymentsheet.utils.runPaymentSheetTest
import com.stripe.android.model.Address
import com.stripe.android.ui.core.elements.autocomplete.model.AutocompletePrediction
import com.stripe.android.ui.core.elements.autocomplete.model.FindAutocompletePredictionsResponse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(AddressAutocompletePreview::class)
@RunWith(TestParameterInjector::class)
class PaymentSheetAddressAutocompleteTest {
    private val placesClientProxyTestRule = PlacesClientProxyTestRule()

    @get:Rule
    val testRules: TestRules = TestRules.create {
        around(placesClientProxyTestRule)
    }

    private val composeTestRule = testRules.compose
    private val networkRule = testRules.networkRule

    private val paymentSheetPage = PaymentSheetPage(composeTestRule)

    @Test
    fun testUnfilled() = runPaymentSheetTest(
        networkRule = networkRule,
        resultCallback = ::assertCompleted,
    ) { context ->
        enqueueAutocompletePredictions()
        enqueuePlaceFetch()
        enqueueElementsSession()

        context.presentPaymentSheet {
            presentWithPaymentIntent(
                paymentIntentClientSecret = "pi_123_secret_123",
                configuration = PaymentSheet.Configuration.Builder(merchantDisplayName = "Example, Inc.")
                    .billingDetailsCollectionConfiguration(
                        PaymentSheet.BillingDetailsCollectionConfiguration(
                            address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
                            attachDefaultsToPaymentMethod = true,
                        ),
                    )
                    .googlePlacesApiKey("gp_123")
                    .build(),
            )
        }

        clickAndFillOutCard()

        paymentSheetPage.waitForText(text = "Address")
        paymentSheetPage.clickViewWithText(text = "Address")

        fillOutAutocompletePage()

        enqueueConfirm()

        paymentSheetPage.clickPrimaryButton()
    }

    @Test
    fun testPrefilled() = runPaymentSheetTest(
        networkRule = networkRule,
        resultCallback = ::assertCompleted,
    ) { context ->
        enqueueAutocompletePredictions()
        enqueuePlaceFetch()
        enqueueElementsSession()

        context.presentPaymentSheet {
            presentWithPaymentIntent(
                paymentIntentClientSecret = "pi_123_secret_123",
                configuration = PaymentSheet.Configuration.Builder(merchantDisplayName = "Example, Inc.")
                    .billingDetailsCollectionConfiguration(
                        PaymentSheet.BillingDetailsCollectionConfiguration(
                            address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
                            attachDefaultsToPaymentMethod = true,
                        ),
                    )
                    .defaultBillingDetails(
                        PaymentSheet.BillingDetails(
                            address = PaymentSheet.Address(
                                line1 = "123 Coffee Street",
                                city = "Chicago",
                                state = "IL",
                                country = "US",
                                postalCode = "83985"
                            )
                        )
                    )
                    .googlePlacesApiKey("gp_123")
                    .build(),
            )
        }

        clickAndFillOutCard()

        paymentSheetPage.waitForContentDescription(description = "Search")
        paymentSheetPage.clickViewWithContentDescription(description = "Search")

        fillOutAutocompletePage()

        enqueueConfirm()

        paymentSheetPage.clickPrimaryButton()
    }

    private fun enqueueAutocompletePredictions() {
        placesClientProxyTestRule.enqueueFindAutocompletePredictionsResponse(
            Result.success(
                FindAutocompletePredictionsResponse(
                    autocompletePredictions = listOf(
                        AutocompletePrediction(
                            primaryText = SpannableString("123 Main Street"),
                            secondaryText = SpannableString(SELECTING_ADDRESS_SECONDARY_TEXT),
                            placeId = "1"
                        ),
                        AutocompletePrediction(
                            primaryText = SpannableString("123 Main Street"),
                            secondaryText = SpannableString(
                                "456 Main Street, Unit #123, New York, New York, US 52523"
                            ),
                            placeId = "2"
                        )
                    )
                )
            )
        )
    }

    private fun enqueuePlaceFetch() {
        placesClientProxyTestRule.enqueueFetchPlaceResponse(
            Result.success(
                Address(
                    line1 = "123 Main Street",
                    line2 = "Unit #123",
                    city = "South San Francisco",
                    state = "CA",
                    country = "US",
                    postalCode = "94111",
                )
            )
        )
    }

    private fun enqueueElementsSession() {
        networkRule.elementsSession { response ->
            response.testBodyFromFile("elements-sessions-requires_payment_method.json")
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
        paymentSheetPage.waitForText(text = "Enter address manually")
        paymentSheetPage.fillOutFieldWithLabel(label = "Address", "Main Street")

        paymentSheetPage.waitForText(SELECTING_ADDRESS_SECONDARY_TEXT)
        paymentSheetPage.clickViewWithText(SELECTING_ADDRESS_SECONDARY_TEXT)
    }

    private companion object {
        const val SELECTING_ADDRESS_SECONDARY_TEXT = "123 Main Street, Unit #123, South San Francisco, CA, US 94111"
    }
}
