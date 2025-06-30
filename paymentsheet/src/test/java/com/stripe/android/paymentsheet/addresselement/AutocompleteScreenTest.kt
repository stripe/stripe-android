package com.stripe.android.paymentsheet.addresselement

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.utils.requireApplication
import com.stripe.android.paymentsheet.addresselement.AddressElementNavigator.Companion.FORCE_EXPANDED_FORM_KEY
import com.stripe.android.paymentsheet.addresselement.analytics.AddressLauncherEventReporter
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.ui.core.elements.autocomplete.PlacesClientProxy
import com.stripe.android.ui.core.elements.autocomplete.model.FetchPlaceResponse
import com.stripe.android.ui.core.elements.autocomplete.model.FindAutocompletePredictionsResponse
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalAnimationApi
@RunWith(AndroidJUnit4::class)
class AutocompleteScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    @Test
    fun `On enter manually, should set result to null and force expand key to true then go back`() = runTest {
        TestAddressElementNavigator.test {
            composeTestRule.setContent {
                val viewModel = viewModel<AutocompleteViewModel> {
                    AutocompleteViewModel(
                        placesClient = TestPlacesClientProxy(),
                        application = requireApplication(),
                        eventReporter = TestAddressLauncherEventReporter,
                        autocompleteArgs = AutocompleteViewModel.Args(country = "US")
                    )
                }

                AutocompleteScreenUI(viewModel, navigator, attributionDrawable = null)
            }

            composeTestRule.waitForIdle()
            composeTestRule.onEnterAddressManually().performClick()

            val forceExpandFormSetResultCall = setResultCalls.awaitItem()

            assertThat(forceExpandFormSetResultCall.key).isEqualTo(FORCE_EXPANDED_FORM_KEY)
            assertThat(forceExpandFormSetResultCall.value).isEqualTo(true)

            val addressDetailsResultCall = setResultCalls.awaitItem()

            assertThat(addressDetailsResultCall.key).isEqualTo(AddressDetails.KEY)
            assertThat(addressDetailsResultCall.value).isNull()

            assertThat(onBackCalls.awaitItem()).isNotNull()
        }
    }

    @Test
    fun `On back, should set result to null if nothing was entered and go back`() = runTest {
        TestAddressElementNavigator.test {
            composeTestRule.setContent {
                val viewModel = viewModel<AutocompleteViewModel> {
                    AutocompleteViewModel(
                        placesClient = TestPlacesClientProxy(),
                        application = requireApplication(),
                        eventReporter = TestAddressLauncherEventReporter,
                        autocompleteArgs = AutocompleteViewModel.Args(country = "US")
                    )
                }

                AutocompleteScreenUI(viewModel, navigator, attributionDrawable = null)
            }

            composeTestRule.waitForIdle()
            composeTestRule.onAddressOptionsAppBar().performClick()

            val addressDetailsResultCall = setResultCalls.awaitItem()

            assertThat(addressDetailsResultCall.key).isEqualTo(AddressDetails.KEY)
            assertThat(addressDetailsResultCall.value).isNull()

            assertThat(onBackCalls.awaitItem()).isNotNull()
        }
    }

    @Test
    fun `On back with query, should set result to null and go back`() = runTest {
        TestAddressElementNavigator.test {
            composeTestRule.setContent {
                val viewModel = viewModel<AutocompleteViewModel> {
                    AutocompleteViewModel(
                        placesClient = TestPlacesClientProxy(),
                        application = requireApplication(),
                        eventReporter = TestAddressLauncherEventReporter,
                        autocompleteArgs = AutocompleteViewModel.Args(country = "US")
                    )
                }

                AutocompleteScreenUI(viewModel, navigator, attributionDrawable = null)
            }

            composeTestRule.waitForIdle()
            composeTestRule.onQueryField().performTextInput("King Street")
            composeTestRule.onAddressOptionsAppBar().performClick()

            val addressDetailsResultCall = setResultCalls.awaitItem()

            assertThat(addressDetailsResultCall.key).isEqualTo(AddressDetails.KEY)
            assertThat(addressDetailsResultCall.value).isNull()

            assertThat(onBackCalls.awaitItem()).isNotNull()
        }
    }

    private fun ComposeTestRule.onAddressOptionsAppBar() = onNodeWithContentDescription("Back")
    private fun ComposeTestRule.onQueryField() = onNodeWithText("Address")
    private fun ComposeTestRule.onEnterAddressManually() = onNodeWithText("Enter address manually")

    private class TestPlacesClientProxy(
        private val findAutocompletePredictionsResponse: Result<FindAutocompletePredictionsResponse> =
            Result.failure(IllegalStateException("Failed!")),
        private val fetchPlaceResponse: Result<FetchPlaceResponse> =
            Result.failure(IllegalStateException("Failed!")),
    ) : PlacesClientProxy {
        override suspend fun findAutocompletePredictions(
            query: String?,
            country: String,
            limit: Int
        ): Result<FindAutocompletePredictionsResponse> = findAutocompletePredictionsResponse

        override suspend fun fetchPlace(
            placeId: String
        ): Result<FetchPlaceResponse> = fetchPlaceResponse
    }

    private object TestAddressLauncherEventReporter : AddressLauncherEventReporter {
        override fun onShow(country: String) {
            // No-op
        }

        override fun onCompleted(country: String, autocompleteResultSelected: Boolean, editDistance: Int?) {
            // No-op
        }
    }
}
