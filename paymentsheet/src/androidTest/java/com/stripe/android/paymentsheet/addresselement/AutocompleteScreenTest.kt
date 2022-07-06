package com.stripe.android.paymentsheet.addresselement

import android.app.Application
import android.text.SpannableString
import androidx.activity.ComponentActivity
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.model.parsers.PaymentIntentJsonParser
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.ui.core.DefaultPaymentsTheme
import com.stripe.android.ui.core.elements.autocomplete.PlacesClientProxy
import com.stripe.android.ui.core.elements.autocomplete.model.AddressComponent
import com.stripe.android.ui.core.elements.autocomplete.model.AutocompletePrediction
import com.stripe.android.ui.core.elements.autocomplete.model.FetchPlaceResponse
import com.stripe.android.ui.core.elements.autocomplete.model.FindAutocompletePredictionsResponse
import com.stripe.android.ui.core.elements.autocomplete.model.Place
import org.json.JSONObject
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalAnimationApi
@RunWith(AndroidJUnit4::class)
class AutocompleteScreenTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val paymentIntent = requireNotNull(
        PaymentIntentJsonParser().parse(
            JSONObject(
                """
                {
                    "id": "pi_1IRg6VCRMbs6F",
                    "object": "payment_intent",
                    "amount": 1099,
                    "canceled_at": null,
                    "cancellation_reason": null,
                    "capture_method": "automatic",
                    "client_secret": "pi_1IRg6VCRMbs6F_secret_7oH5g4v8GaCrHfsGYS6kiSnwF",
                    "confirmation_method": "automatic",
                    "created": 1614960135,
                    "currency": "usd",
                    "description": "Example PaymentIntent",
                    "last_payment_error": null,
                    "livemode": false,
                    "next_action": null,
                    "payment_method": "pm_1IJs3ZCRMbs",
                    "payment_method_types": ["card"],
                    "receipt_email": null,
                    "setup_future_usage": null,
                    "shipping": null,
                    "source": null,
                    "status": "succeeded"
                }
                """.trimIndent()
            )
        )
    )

    private val args = AddressElementActivityContract.Args(
        paymentIntent,
        PaymentSheet.Configuration(
            merchantDisplayName = "Merchant, Inc.",
            customer = PaymentSheet.CustomerConfiguration(
                "customer_id",
                "ephemeral_key"
            )
        ),
        AddressElementActivityContract.Args.InjectionParams(
            "injectorKey",
            setOf("Product Usage"),
            true
        )
    )
    private val application = ApplicationProvider.getApplicationContext<Application>()

    @Test
    fun ensure_elements_exist() {
        setContent()
        onAddressOptionsAppBar().assertExists()
        onQueryField().assertExists()
        onEnterAddressManually().assertExists()
    }

    @Test
    fun no_results_found_should_appear() {
        setContent()
        onQueryField().performTextInput("Some text")
        composeTestRule.waitUntil {
            composeTestRule
                .onAllNodesWithText("No results found")
                .fetchSemanticsNodes().size == 1
        }
        composeTestRule.waitUntil {
            composeTestRule
                .onAllNodesWithTag(TEST_TAG_ATTRIBUTION_DRAWABLE)
                .fetchSemanticsNodes().size == 1
        }
    }

    @Test
    fun results_found_should_appear() {
        setContent(
            mockClient = FakeGooglePlacesClient(
                predictions = listOf(
                    AutocompletePrediction(
                        SpannableString("primaryText"),
                        SpannableString("secondaryText"),
                        "placeId"
                    )
                )
            )
        )
        onQueryField().performTextInput("Some text")
        composeTestRule.waitUntil {
            composeTestRule
                .onAllNodesWithText("primaryText")
                .fetchSemanticsNodes().size == 1
        }
    }

    private fun setContent(
        mockClient: FakeGooglePlacesClient = FakeGooglePlacesClient()
    ) =
        composeTestRule.setContent {
            DefaultPaymentsTheme {
                AutocompleteScreenUI(
                    viewModel = AutocompleteViewModel(
                        args,
                        AddressElementNavigator(),
                        application
                    ).apply {
                        initialize {
                            mockClient
                        }
                    }
                )
            }
        }

    private fun onAddressOptionsAppBar() = composeTestRule.onNodeWithContentDescription("Back")
    private fun onQueryField() = composeTestRule.onNodeWithText("Address")
    private fun onEnterAddressManually() = composeTestRule.onNodeWithText("Enter address manually")

    private class FakeGooglePlacesClient(
        private val predictions: List<AutocompletePrediction> = listOf(),
        private val addressComponents: List<AddressComponent> = listOf()
    ) : PlacesClientProxy {
        override suspend fun findAutocompletePredictions(
            query: String?,
            country: String,
            limit: Int
        ): Result<FindAutocompletePredictionsResponse> {
            return Result.success(
                FindAutocompletePredictionsResponse(predictions)
            )
        }

        override suspend fun fetchPlace(placeId: String): Result<FetchPlaceResponse> {
            return Result.success(
                FetchPlaceResponse(
                    Place(addressComponents)
                )
            )
        }
    }
}
