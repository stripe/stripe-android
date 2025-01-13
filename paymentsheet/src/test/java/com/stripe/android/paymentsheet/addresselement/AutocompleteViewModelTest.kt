package com.stripe.android.paymentsheet.addresselement

import android.app.Application
import android.text.SpannableString
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.analytics.AddressLauncherEventReporter
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.ui.core.elements.autocomplete.PlacesClientProxy
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
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AutocompleteViewModelTest {
    private val args = mock<AddressElementActivityContract.Args>()
    private val navigator = mock<AddressElementNavigator>()
    private val application = ApplicationProvider.getApplicationContext<Application>()
    private val mockClient = mock<PlacesClientProxy>()
    private val mockEventReporter = mock<AddressLauncherEventReporter>()

    private fun createViewModel() =
        AutocompleteViewModel(
            args,
            navigator,
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
    fun `selectPrediction emits successful result`() = runTest(UnconfinedTestDispatcher()) {
        val viewModel = createViewModel()
        val fetchPlaceResponse = Result.success(
            FetchPlaceResponse(
                Place(
                    listOf()
                )
            )
        )
        val expectedResult = Result.success(
            AddressDetails(
                address = PaymentSheet.Address(
                    city = null,
                    country = null,
                    line1 = "",
                    line2 = null,
                    postalCode = null,
                    state = null
                )
            )
        )
        whenever(mockClient.fetchPlace(any())).thenReturn(fetchPlaceResponse)

        viewModel.selectPrediction(
            AutocompletePrediction(
                SpannableString("primaryText"),
                SpannableString("secondaryText"),
                "placeId"
            )
        )

        assertThat(viewModel.loading.value).isEqualTo(false)
        assertThat(viewModel.addressResult.value)
            .isEqualTo(expectedResult)

        verify(navigator).setResult(anyOrNull(), eq(expectedResult.getOrNull()))
        verify(navigator).onBack()
    }

    @Test
    fun `selectPrediction emits failure result`() = runTest(UnconfinedTestDispatcher()) {
        val viewModel = createViewModel()
        val exception = Exception("fake exception")
        val result = Result.failure<FetchPlaceResponse>(exception)

        mockClient.stub {
            onBlocking { fetchPlace(any()) }.thenReturn(result)
        }

        viewModel.selectPrediction(
            AutocompletePrediction(
                SpannableString("primaryText"),
                SpannableString("secondaryText"),
                "placeId"
            )
        )

        assertThat(viewModel.loading.value).isEqualTo(false)
        assertThat(viewModel.addressResult.value)
            .isEqualTo(Result.failure<FetchPlaceResponse>(exception))

        verify(navigator).setResult(anyOrNull(), eq(null))
        verify(navigator).onBack()
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
        val expectedResult = Result.success(
            AddressDetails(
                address = PaymentSheet.Address(
                    line1 = "Some query"
                )
            )
        )

        viewModel.textFieldController.onRawValueChange("Some query")
        viewModel.onEnterAddressManually()

        verify(navigator).setResult(anyOrNull(), eq(expectedResult.getOrNull()))
        verify(navigator).onBack()
    }

    @Test
    fun `onEnterAddressManually navigates back with empty address`() = runTest(UnconfinedTestDispatcher()) {
        val viewModel = createViewModel()
        val expectedResult = Result.success(
            AddressDetails(
                address = PaymentSheet.Address(
                    line1 = ""
                )
            )
        )

        viewModel.onEnterAddressManually()

        verify(navigator).setResult(anyOrNull(), eq(expectedResult.getOrNull()))
        verify(navigator).onBack()
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
    fun `when query is not empty then return line1 on back`() = runTest(UnconfinedTestDispatcher()) {
        val viewModel = createViewModel()

        viewModel.textFieldController.onRawValueChange("a")
        viewModel.onBackPressed()

        verify(viewModel.navigator).setResult(
            eq(AddressDetails.KEY),
            eq(AddressDetails(address = PaymentSheet.Address(line1 = "a")))
        )
    }

    @Test
    fun `when query is empty then do nothing on back`() = runTest(UnconfinedTestDispatcher()) {
        val viewModel = createViewModel()

        viewModel.textFieldController.onRawValueChange("")
        viewModel.onBackPressed()

        verify(viewModel.navigator, never()).setResult(any(), any())
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
