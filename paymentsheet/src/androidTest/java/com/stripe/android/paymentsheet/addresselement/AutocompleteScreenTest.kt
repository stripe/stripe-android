package com.stripe.android.paymentsheet.addresselement

import android.app.Application
import android.text.SpannableString
import androidx.activity.ComponentActivity
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.paymentsheet.addresselement.analytics.AddressLauncherEventReporter
import com.stripe.android.ui.core.elements.autocomplete.PlacesClientProxy
import com.stripe.android.ui.core.elements.autocomplete.model.AddressComponent
import com.stripe.android.ui.core.elements.autocomplete.model.AutocompletePrediction
import com.stripe.android.ui.core.elements.autocomplete.model.FetchPlaceResponse
import com.stripe.android.ui.core.elements.autocomplete.model.FindAutocompletePredictionsResponse
import com.stripe.android.ui.core.elements.autocomplete.model.Place
import com.stripe.android.uicore.DefaultStripeTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalAnimationApi
@RunWith(AndroidJUnit4::class)
class AutocompleteScreenTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val args = AddressElementActivityContract.Args(
        "publishableKey",
        AddressLauncher.Configuration(),
    )
    private val application = ApplicationProvider.getApplicationContext<Application>()
    private val eventReporter = FakeEventReporter()

    @Test
    fun ensure_elements_exist() {
        setContent()
        onAddressOptionsAppBar().assertExists()
        onQueryField().assertExists()
        onEnterAddressManually().assertExists()
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
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule
                .onAllNodesWithText("primaryText")
                .fetchSemanticsNodes().size == 1
        }
    }

    private fun setContent(
        mockClient: FakeGooglePlacesClient = FakeGooglePlacesClient()
    ) =
        composeTestRule.setContent {
            DefaultStripeTheme {
                AutocompleteScreenUI(
                    viewModel = AutocompleteViewModel(
                        args,
                        AddressElementNavigator(),
                        mockClient,
                        AutocompleteViewModel.Args(
                            "US"
                        ),
                        eventReporter,
                        application
                    )
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

    private class FakeEventReporter : AddressLauncherEventReporter {
        override fun onShow(country: String) {
            // no-op
        }

        override fun onCompleted(
            country: String,
            autocompleteResultSelected: Boolean,
            editDistance: Int?
        ) {
            // no-op
        }
    }
}
