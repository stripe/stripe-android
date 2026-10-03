package com.stripe.android.checkout

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkouttesting.DEFAULT_CHECKOUT_SESSION_ID
import com.stripe.android.core.networking.AnalyticsRequest
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.elements.ShippingAddressElement
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatcher
import com.stripe.android.networktesting.RequestMatchers.analyticsPayloadField
import com.stripe.android.networktesting.RequestMatchers.bodyPart
import com.stripe.android.networktesting.RequestMatchers.hasQueryParam
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.not
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.utils.TestRules
import com.stripe.android.paymentsheet.utils.replaceText
import com.stripe.android.paymentsheet.validateAnalyticsRequest
import com.stripe.android.testing.waitUntilWithIdle
import com.stripe.paymentelementtestpages.AddressElementPage
import okhttp3.mockwebserver.MockResponse
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.seconds

@OptIn(CheckoutSessionPreview::class)
@RunWith(AndroidJUnit4::class)
internal class CheckoutShippingAddressElementAnalyticsTest {
    private val networkRule = NetworkRule(
        hostsToTrack = listOf(ApiRequest.API_HOST, AnalyticsRequest.HOST),
        validationTimeout = 5.seconds,
    )

    @get:Rule
    val testRules: TestRules = TestRules.create(networkRule = networkRule)

    private val page = AddressElementPage(testRules.compose, ApplicationProvider.getApplicationContext())

    @Test
    fun `manual edit of prefilled address reports no autocomplete selection`() {
        val initialLine1 = "510 Townsend St"
        val updatedCity = "Cambridge"
        val configuration = CheckoutController.Configuration()
            .shippingAddressElement(ShippingAddressElement.Configuration())
            .defaults(
                CheckoutController.Configuration.Defaults().shippingDetails(
                    CheckoutController.Configuration.Defaults.ContactDetails()
                        .name("Real Name")
                        .address(
                            CheckoutController.Address()
                                .line1(initialLine1)
                                .city("Boston")
                                .state("MA")
                                .postalCode("02115")
                                .country("US")
                        )
                )
            )

        runCheckoutShippingAddressElementAnalyticsTest(configuration) { context, controller ->
            validateShippingAddressShown()
            context.presentShippingAddressElement()
            page.waitUntilVisible()

            testRules.compose.replaceText("City", updatedCity)

            validateShippingAddressSaveAnalytics(
                autocompleteResultSelected = false,
                editDistance = null,
            )
            validateCheckoutLoadRequests()
            page.clickSave()

            waitForShippingAddress(
                controller = controller,
                line1 = initialLine1,
                city = updatedCity,
            )
            context.markTestSucceeded()
        }
    }

    @Test
    fun `hosted autocomplete selection reports selected result`() {
        val configuration = CheckoutController.Configuration()
            .shippingAddressElement(ShippingAddressElement.Configuration())
            .defaults(
                CheckoutController.Configuration.Defaults().shippingDetails(
                    CheckoutController.Configuration.Defaults.ContactDetails()
                        .name("Real Name")
                        .address(CheckoutController.Address().country("US"))
                )
            )

        runCheckoutShippingAddressElementAnalyticsTest(configuration) { context, controller ->
            validateShippingAddressShown()
            context.presentShippingAddressElement()
            page.waitUntilVisible()

            validateAddressElementAnalyticsRequest("mc_address_autocomplete_start")
            validateAddressElementAnalyticsRequest("mc_address_autocomplete_suggestions")
            networkRule.enqueue(
                method("POST"),
                path("/v1/elements/address/autocomplete"),
                bodyPart("search_text", AUTOCOMPLETE_QUERY),
            ) { response ->
                response.setBody(AUTOCOMPLETE_RESPONSE)
            }
            page.enterAutocompleteQuery(AUTOCOMPLETE_QUERY)

            validateAddressElementAnalyticsRequest("mc_address_autocomplete_selected")
            networkRule.enqueue(
                method("POST"),
                path("/v1/elements/address/details"),
                bodyPart("place_id", PLACE_ID),
            ) { response ->
                response.setBody(PLACE_DETAILS_RESPONSE)
            }
            page.selectAutocompletePrediction(PICKED_LINE1)

            validateShippingAddressSaveAnalytics(
                autocompleteResultSelected = true,
                editDistance = 0,
            )
            validateCheckoutLoadRequests()
            page.clickSave()

            waitForShippingAddress(
                controller = controller,
                line1 = PICKED_LINE1,
                city = PICKED_CITY,
            )
            context.markTestSucceeded()
        }
    }

    private fun runCheckoutShippingAddressElementAnalyticsTest(
        configuration: CheckoutController.Configuration,
        block: (CheckoutPaymentElementTestRunnerContext, CheckoutController) -> Unit,
    ) {
        var controller: CheckoutController? = null
        runCheckoutPaymentElementTest(
            networkRule = networkRule,
            checkoutInitResponse = ::createCheckoutInitResponse,
            renderPaymentElementContent = false,
            setup = { configuredController ->
                controller = configuredController
                validateCheckoutLoadRequests()
                configuredController.configure(DEFAULT_CLIENT_SECRET, configuration).getOrThrow()
            },
            block = { context ->
                block(context, requireNotNull(controller))
            },
        )
    }

    private fun validateShippingAddressShown() {
        validateAddressElementAnalyticsRequest(
            eventName = "elements.shipping_address.shown",
            analyticsPayloadField("checkout_session_id", DEFAULT_CHECKOUT_SESSION_ID),
            analyticsPayloadField(ADDRESS_COUNTRY_FIELD, "US"),
        )
    }

    private fun validateShippingAddressSaveAnalytics(
        autocompleteResultSelected: Boolean,
        editDistance: Int?,
    ) {
        val selectedMatcher = analyticsPayloadField(
            AUTOCOMPLETE_SELECTED_FIELD,
            autocompleteResultSelected.toString(),
        )
        val distanceMatcher = editDistance?.let {
            analyticsPayloadField(EDIT_DISTANCE_FIELD, it.toString())
        } ?: not(hasQueryParam(EDIT_DISTANCE_FIELD))

        listOf(
            "elements.shipping_address.save_started",
            "elements.shipping_address.save_completed",
        ).forEach { eventName ->
            validateAddressElementAnalyticsRequest(
                eventName,
                analyticsPayloadField("checkout_session_id", DEFAULT_CHECKOUT_SESSION_ID),
                analyticsPayloadField(ADDRESS_COUNTRY_FIELD, "US"),
                selectedMatcher,
                distanceMatcher,
            )
        }
    }

    private fun validateCheckoutLoadRequests() {
        validateCheckoutAnalyticsRequest("mc_load_started")
        validateCheckoutAnalyticsRequest("mc_load_succeeded")
    }

    private fun validateAddressElementAnalyticsRequest(
        eventName: String,
        vararg requestMatchers: RequestMatcher,
    ) {
        networkRule.validateAnalyticsRequest(
            eventName = eventName,
            productUsage = ADDRESS_ELEMENT_PRODUCT_USAGE,
            *requestMatchers,
        )
    }

    private fun validateCheckoutAnalyticsRequest(eventName: String) {
        networkRule.validateAnalyticsRequest(
            eventName = eventName,
            productUsage = CHECKOUT_PRODUCT_USAGE,
        )
    }

    private fun createCheckoutInitResponse(response: MockResponse) {
        response.testBodyFromFile("checkout-session-init.json") { json ->
            json.getJSONObject("elements_session").remove("link_settings")
            json.put(
                "shipping_address_collection",
                JSONObject().put("allowed_countries", JSONArray().put("US")),
            )
        }
    }

    private fun waitForShippingAddress(
        controller: CheckoutController,
        line1: String,
        city: String,
    ) {
        testRules.compose.waitUntilWithIdle(
            conditionDescription = "Checkout session shipping address to be committed",
        ) {
            val address = controller.session.value?.shippingAddress?.address
            address?.line1 == line1 && address?.city == city
        }

        assertThat(controller.session.value?.shippingAddress?.address?.line1).isEqualTo(line1)
        assertThat(controller.session.value?.shippingAddress?.address?.city).isEqualTo(city)
    }

    private companion object {
        const val DEFAULT_CLIENT_SECRET = "${DEFAULT_CHECKOUT_SESSION_ID}_secret_example"
        const val ADDRESS_COUNTRY_FIELD = "address_data_blob[address_country_code]"
        const val AUTOCOMPLETE_SELECTED_FIELD = "address_data_blob[auto_complete_result_selected]"
        const val EDIT_DISTANCE_FIELD = "address_data_blob[edit_distance]"
        const val AUTOCOMPLETE_QUERY = "123 Market"
        const val PLACE_ID = "place_123"
        const val PICKED_LINE1 = "123 Market Street"
        const val PICKED_CITY = "San Francisco"
        const val PICKED_STATE = "CA"
        const val PICKED_POSTAL_CODE = "94103"
        val ADDRESS_ELEMENT_PRODUCT_USAGE = setOf("PaymentSheet.AddressController")
        val CHECKOUT_PRODUCT_USAGE = setOf("Checkout")
        val AUTOCOMPLETE_RESPONSE = """
            {
              "source": "google",
              "suggestions": [
                {
                  "address": null,
                  "display_data": {
                    "subtitle": "San Francisco, CA",
                    "title": "$PICKED_LINE1"
                  },
                  "place_id": "$PLACE_ID"
                }
              ]
            }
        """.trimIndent()
        val PLACE_DETAILS_RESPONSE = """
            {
              "address": {
                "line1": "$PICKED_LINE1",
                "city": "$PICKED_CITY",
                "state": "$PICKED_STATE",
                "postal_code": "$PICKED_POSTAL_CODE",
                "country": "US"
              }
            }
        """.trimIndent()
    }
}
