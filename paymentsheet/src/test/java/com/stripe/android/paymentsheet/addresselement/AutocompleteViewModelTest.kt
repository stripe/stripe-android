package com.stripe.android.paymentsheet.addresselement

import android.app.Application
import android.text.SpannableString
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.analytics.AddressLauncherEventReporter
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.ui.core.elements.autocomplete.PlacesClientProxy
import com.stripe.android.ui.core.elements.autocomplete.model.AddressComponent
import com.stripe.android.ui.core.elements.autocomplete.model.AutocompletePrediction
import com.stripe.android.ui.core.elements.autocomplete.model.FetchPlaceResponse
import com.stripe.android.ui.core.elements.autocomplete.model.FindAutocompletePredictionsResponse
import com.stripe.android.ui.core.elements.autocomplete.model.Place
import com.stripe.android.uicore.elements.TextFieldIcon
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AutocompleteViewModelTest {
    private val application = ApplicationProvider.getApplicationContext<Application>()
    private val mockClient = mock<PlacesClientProxy>()
    private val mockEventReporter = mock<AddressLauncherEventReporter>()

    private fun createViewModel() =
        AutocompleteViewModel(
            mockClient,
            AutocompleteViewModel.Args(
                "US"
            ),
            mockEventReporter,
            application
        )

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    @Test
    fun `selectPrediction emits go back event with selected prediction`() = runTest(UnconfinedTestDispatcher()) {
        val viewModel = createViewModel()
        val fetchPlaceResponse = Result.success(
            FetchPlaceResponse(
                Place(
                    listOf(
                        AddressComponent(
                            shortName = "123",
                            longName = "123",
                            types = listOf(Place.Type.STREET_NUMBER.value)
                        ),
                        AddressComponent(
                            shortName = "King Street",
                            longName = "King Street",
                            types = listOf(Place.Type.ROUTE.value)
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
                            shortName = "99999",
                            longName = "99999",
                            types = listOf(Place.Type.POSTAL_CODE.value)
                        )
                    )
                )
            )
        )
        whenever(mockClient.fetchPlace(any())).thenReturn(fetchPlaceResponse)

        viewModel.event.test {
            viewModel.selectPrediction(
                AutocompletePrediction(
                    SpannableString("primaryText"),
                    SpannableString("secondaryText"),
                    "placeId"
                )
            )

            assertThat(viewModel.loading.value).isEqualTo(false)

            assertThat(awaitItem()).isEqualTo(
                AutocompleteViewModel.Event.GoBack(
                    addressDetails = AddressDetails(
                        address = PaymentSheet.Address(
                            city = "South San Francisco",
                            country = "US",
                            line1 = "123 King Street",
                            line2 = null,
                            postalCode = "99999",
                            state = "CA"
                        )
                    )
                )
            )
        }
    }

    @Test
    fun `selectPrediction failure emits go back event with no address`() = runTest(UnconfinedTestDispatcher()) {
        val viewModel = createViewModel()
        val exception = Exception("fake exception")
        val result = Result.failure<FetchPlaceResponse>(exception)

        mockClient.stub {
            onBlocking { fetchPlace(any()) }.thenReturn(result)
        }

        viewModel.event.test {
            viewModel.selectPrediction(
                AutocompletePrediction(
                    SpannableString("primaryText"),
                    SpannableString("secondaryText"),
                    "placeId"
                )
            )

            assertThat(viewModel.loading.value).isEqualTo(false)

            assertThat(awaitItem()).isEqualTo(AutocompleteViewModel.Event.GoBack(addressDetails = null))
        }
    }

    @Test
    fun `onEnterAddressManually sets the current address and navigates back`() = runTest(UnconfinedTestDispatcher()) {
        whenever(mockClient.findAutocompletePredictions(any(), any(), any())).thenReturn(
            Result.success(
                FindAutocompletePredictionsResponse(
                    listOf(
                        AutocompletePrediction(
                            primaryText = SpannableString("primaryText"),
                            secondaryText = SpannableString("secondaryText"),
                            placeId = "placeId",
                        )
                    )
                )
            )
        )

        val viewModel = createViewModel()

        viewModel.event.test {
            viewModel.textFieldController.onRawValueChange("Some query")
            viewModel.onEnterAddressManually()

            assertThat(awaitItem()).isEqualTo(
                AutocompleteViewModel.Event.EnterManually(
                    addressDetails = AddressDetails(
                        address = PaymentSheet.Address(
                            line1 = "Some query"
                        )
                    )
                )
            )
        }
    }

    @Test
    fun `onEnterAddressManually navigates back with enter manually event`() = runTest(UnconfinedTestDispatcher()) {
        val viewModel = createViewModel()

        viewModel.event.test {
            viewModel.onEnterAddressManually()

            assertThat(awaitItem()).isEqualTo(
                AutocompleteViewModel.Event.EnterManually(
                    addressDetails = null,
                )
            )
        }
    }

    @Test
    fun `when user presses clear text field is cleared`() = runTest {
        val viewModel = createViewModel()
        val trailingIcon = viewModel.textFieldController.trailingIcon

        (trailingIcon.value as? TextFieldIcon.Trailing)?.onClick?.invoke()

        assertThat(viewModel.textFieldController.rawFieldValue.value).isEqualTo("")
    }

    @Test
    fun `when query is valid then search is triggered with delay`() = runTest(UnconfinedTestDispatcher()) {
        val viewModel = createViewModel()

        viewModel.textFieldController.onRawValueChange("Some valid query")

        whenever(mockClient.findAutocompletePredictions(any(), any(), any())).thenReturn(
            Result.success(
                FindAutocompletePredictionsResponse(
                    listOf(
                        AutocompletePrediction(
                            SpannableString("primaryText"),
                            SpannableString("secondaryText"),
                            "placeId"
                        )
                    )
                )
            )
        )

        // Advance past search debounce delay
        advanceTimeBy(AutocompleteViewModel.SEARCH_DEBOUNCE_MS + 1)

        assertThat(viewModel.predictions.value?.size).isEqualTo(1)
    }

    @Test
    fun `when query is invalid then search is not triggered`() = runTest(UnconfinedTestDispatcher()) {
        val viewModel = createViewModel()

        viewModel.textFieldController.onRawValueChange("a")

        // Advance past search debounce delay
        advanceTimeBy(AutocompleteViewModel.SEARCH_DEBOUNCE_MS + 1)

        assertThat(viewModel.predictions.value?.size).isEqualTo(null)
    }

    @Test
    fun `when address is empty trailing icon is null`() = runTest(UnconfinedTestDispatcher()) {
        val viewModel = createViewModel()

        viewModel.textFieldController.onRawValueChange("")

        assertThat(viewModel.textFieldController.trailingIcon.value).isNull()

        viewModel.textFieldController.onRawValueChange("a")

        assertThat(viewModel.textFieldController.trailingIcon.value).isNotNull()
    }

    @Test
    fun `on back when query is not empty, should not return anything`() = runTest {
        val viewModel = createViewModel()

        viewModel.event.test {
            viewModel.textFieldController.onRawValueChange("a")
            viewModel.onBackPressed()

            assertThat(awaitItem()).isEqualTo(
                AutocompleteViewModel.Event.GoBack(addressDetails = null)
            )
        }
    }

    @Test
    fun `when query is empty then return null`() = runTest {
        val viewModel = createViewModel()

        viewModel.event.test {
            viewModel.textFieldController.onRawValueChange("")
            viewModel.onBackPressed()

            assertThat(awaitItem()).isEqualTo(
                AutocompleteViewModel.Event.GoBack(addressDetails = null)
            )
        }
    }

    @Test
    fun `initializing ViewModel emits onShow event`() {
        createViewModel()
        verify(mockEventReporter).onShow(eq("US"))
    }

    @Test
    fun `clearQuery clears textfield and predictions`() = runTest(UnconfinedTestDispatcher()) {
        val viewModel = createViewModel()

        viewModel.textFieldController.onRawValueChange("Some valid query")

        whenever(mockClient.findAutocompletePredictions(any(), any(), any())).thenReturn(
            Result.success(
                FindAutocompletePredictionsResponse(
                    listOf(
                        AutocompletePrediction(
                            SpannableString("primaryText"),
                            SpannableString("secondaryText"),
                            "placeId"
                        )
                    )
                )
            )
        )

        // Advance past search debounce delay
        advanceTimeBy(AutocompleteViewModel.SEARCH_DEBOUNCE_MS + 1)

        assertThat(viewModel.predictions.value?.size).isEqualTo(1)

        viewModel.clearQuery()

        assertThat(viewModel.predictions.value).isEqualTo(null)
        assertThat(viewModel.textFieldController.rawFieldValue.value).isEqualTo("")
    }
}
