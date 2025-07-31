package com.stripe.android.paymentsheet

import android.text.SpannableString
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.stripe.android.core.utils.urlEncode
import com.stripe.android.elements.Address
import com.stripe.android.elements.BillingDetails
import com.stripe.android.elements.BillingDetailsCollectionConfiguration
import com.stripe.android.networktesting.RequestMatchers.bodyPart
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentelement.AddressAutocompletePreview
import com.stripe.android.paymentsheet.utils.PlacesClientProxyTestRule
import com.stripe.android.paymentsheet.utils.TestRules
import com.stripe.android.paymentsheet.utils.assertCompleted
import com.stripe.android.paymentsheet.utils.runPaymentSheetTest
import com.stripe.android.ui.core.elements.autocomplete.model.AddressComponent
import com.stripe.android.ui.core.elements.autocomplete.model.AutocompletePrediction
import com.stripe.android.ui.core.elements.autocomplete.model.FetchPlaceResponse
import com.stripe.android.ui.core.elements.autocomplete.model.FindAutocompletePredictionsResponse
import com.stripe.android.ui.core.elements.autocomplete.model.Place
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
                        BillingDetailsCollectionConfiguration(
                            address = BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
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
                        BillingDetailsCollectionConfiguration(
                            address = BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
                            attachDefaultsToPaymentMethod = true,
                        ),
                    )
                    .defaultBillingDetails(
                        BillingDetails(
                            address = Address(
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
                FetchPlaceResponse(
                    place = Place(
                        listOf(
                            AddressComponent(
                                shortName = "123",
                                longName = "123",
                                types = listOf(Place.Type.STREET_NUMBER.value)
                            ),
                            AddressComponent(
                                shortName = "Main Street",
                                longName = "Main Street",
                                types = listOf(Place.Type.ROUTE.value)
                            ),
                            AddressComponent(
                                shortName = "Unit #123",
                                longName = "Unit #123",
                                types = listOf(Place.Type.PREMISE.value)
                            ),
                            AddressComponent(
                                shortName = "South SF",
                                longName = "South San Francisco",
                                types = listOf(Place.Type.LOCALITY.value)
                            ),
                            AddressComponent(
                                shortName = "CA",
                                longName = "California",
                                types = listOf(Place.Type.ADMINISTRATIVE_AREA_LEVEL_1.value)
                            ),
                            AddressComponent(
                                shortName = "US",
                                longName = "United States",
                                types = listOf(Place.Type.COUNTRY.value)
                            ),
                            AddressComponent(
                                shortName = "94111",
                                longName = "94111",
                                types = listOf(Place.Type.POSTAL_CODE.value)
                            )
                        )
                    )
                )
            )
        )
    }

    private fun enqueueElementsSession() {
        networkRule.enqueue(
            method("GET"),
            path("/v1/elements/sessions"),
        ) { response ->
            response.testBodyFromFile("elements-sessions-requires_payment_method.json")
        }
    }

    private fun enqueueConfirm() {
        networkRule.enqueue(
            method("POST"),
            path("/v1/payment_intents/pi_123/confirm"),
            bodyPart(
                urlEncode("payment_method_data[billing_details][address][line1]"),
                urlEncode("123 Main Street")
            ),
            bodyPart(
                urlEncode("payment_method_data[billing_details][address][line2]"),
                urlEncode("Unit #123")
            ),
            bodyPart(
                urlEncode("payment_method_data[billing_details][address][city]"),
                urlEncode("South San Francisco")
            ),
            bodyPart(urlEncode("payment_method_data[billing_details][address][state]"), "CA"),
            bodyPart(urlEncode("payment_method_data[billing_details][address][country]"), "US"),
            bodyPart(urlEncode("payment_method_data[billing_details][address][postal_code]"), "94111"),
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
