package com.stripe.android.paymentsheet.addresselement

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import app.cash.turbine.test
import app.cash.turbine.turbineScope
import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.core.exception.LocalStripeException
import com.stripe.android.isInstanceOf
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures.DEFAULT_API_CONFIG
import com.stripe.android.model.Address
import com.stripe.android.paymentelement.AddressElementSameAsBillingPreview
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.analytics.AddressElementEventReporter
import com.stripe.android.paymentsheet.addresselement.analytics.AddressLauncherEventReporter
import com.stripe.android.paymentsheet.addresselement.analytics.FakeAddressElementEventReporter
import com.stripe.android.paymentsheet.addresselement.analytics.FakeAddressLauncherEventReporter
import com.stripe.android.paymentsheet.addresselement.analytics.StandaloneAddressElementEventReporter
import com.stripe.android.paymentsheet.utils.ViewModelStoreTestRule
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.ui.core.elements.autocomplete.PlacesClientProxy
import com.stripe.android.ui.core.elements.autocomplete.model.FindAutocompletePredictionsResponse
import com.stripe.android.uicore.elements.AutocompleteAddressElement
import com.stripe.android.uicore.elements.AutocompleteAddressInteractor
import com.stripe.android.uicore.elements.FormFieldId
import com.stripe.android.uicore.elements.SectionElement
import com.stripe.android.uicore.forms.FormFieldEntry
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class InputAddressViewModelTest {
    private val navigator = mock<AddressElementNavigator>()
    private val resultStateHolder = AddressElementResultStateHolder()
    private val eventReporter = mock<AddressLauncherEventReporter>()

    private fun createViewModel(
        address: AddressDetails? = null,
        config: AddressLauncher.Configuration = AddressLauncher.Configuration.Builder()
            .address(address)
            .build(),
        primaryButtonAction: AddressElementPrimaryButtonAction = FakeAddressElementPrimaryButtonAction {
            AddressElementActivityContract.Result.StandaloneSucceeded(it)
        },
        eventReporter: AddressLauncherEventReporter = this.eventReporter,
        addressElementEventReporter: AddressElementEventReporter? = null,
        placesClient: PlacesClientProxy? = null,
        argsFactory:
            (AddressLauncher.Configuration) -> AddressElementActivityContract.Args = { currentConfig ->
                AddressElementActivityContract.Args.Standalone(
                    apiConfiguration = DEFAULT_API_CONFIG,
                    config = currentConfig,
                )
            },
    ): InputAddressViewModel {
        return InputAddressViewModel(
            argsFactory(config),
            navigator,
            resultStateHolder,
            addressElementEventReporter ?: StandaloneAddressElementEventReporter(eventReporter),
            placesClient = placesClient,
            primaryButtonAction = primaryButtonAction,
        ).also { viewModelStoreRule.track(it) }
    }

    @get:Rule
    val viewModelStoreRule = ViewModelStoreTestRule()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    @Test
    fun `onScreenShown fires onShow with the form country from the initial address`() = runScenario(
        address = AddressDetails(address = PaymentSheet.Address(country = "US")),
    ) {
        viewModel.onScreenShown()

        assertThat(eventReporter.showCalls.awaitItem()).isEqualTo("US")
    }

    @Test
    fun `onScreenShown fires onShow with the form default when no initial country`() = runScenario {
        assertThat(viewModel.addressFormController.getCurrentFormValues()[FormFieldId.Country]?.value)
            .isEqualTo("US")

        viewModel.onScreenShown()

        assertThat(eventReporter.showCalls.awaitItem()).isEqualTo("US")
    }

    @Test
    fun `onScreenShown fires onShow with the allowed form country`() = runScenario(
        config = AddressLauncher.Configuration.Builder()
            .allowedCountries(setOf("CA"))
            .build(),
    ) {
        assertThat(viewModel.addressFormController.getCurrentFormValues()[FormFieldId.Country]?.value)
            .isEqualTo("CA")

        viewModel.onScreenShown()

        assertThat(eventReporter.showCalls.awaitItem()).isEqualTo("CA")
    }

    @Test
    fun `no autocomplete address passed has an empty address to start`() = runTest(UnconfinedTestDispatcher()) {
        val flow = MutableStateFlow<AddressDetails?>(null)
        whenever(navigator.getResultFlow<AddressDetails?>(any())).thenReturn(flow)

        val viewModel = createViewModel()
        assertThat(viewModel.collectedAddress.value).isEqualTo(AddressDetails())
    }

    @Test
    fun `autocomplete address passed is collected to start`() = runTest(UnconfinedTestDispatcher()) {
        val expectedAddress = PaymentSheet.Address(country = "US")
        val flow = MutableStateFlow<AddressElementNavigator.AutocompleteEvent?>(
            AddressElementNavigator.AutocompleteEvent.OnBack(expectedAddress)
        )
        whenever(
            navigator.getResultFlow<AddressElementNavigator.AutocompleteEvent?>(
                AddressElementNavigator.AutocompleteEvent.KEY
            )
        ).thenReturn(flow)

        val viewModel = createViewModel()
        assertThat(viewModel.collectedAddress.value).isEqualTo(
            AddressDetails(
                address = expectedAddress
            )
        )
    }

    @Test
    fun `takes only fields in new address`() = runTest(UnconfinedTestDispatcher()) {
        val usAddress = PaymentSheet.Address(country = "US")
        val flow = MutableStateFlow<AddressElementNavigator.AutocompleteEvent?>(
            AddressElementNavigator.AutocompleteEvent.OnBack(usAddress)
        )
        whenever(
            navigator.getResultFlow<AddressElementNavigator.AutocompleteEvent?>(
                AddressElementNavigator.AutocompleteEvent.KEY
            )
        ).thenReturn(flow)

        val viewModel = createViewModel()
        assertThat(viewModel.collectedAddress.value).isEqualTo(
            AddressDetails(
                address = usAddress,
            )
        )

        val expectedAddress = PaymentSheet.Address(country = "CAN", line1 = "foobar")
        flow.tryEmit(AddressElementNavigator.AutocompleteEvent.OnBack(expectedAddress))
        assertThat(viewModel.collectedAddress.value).isEqualTo(
            AddressDetails(
                address = expectedAddress,
            )
        )
    }

    @Test
    fun `default address from merchant is parsed`() = runTest(UnconfinedTestDispatcher()) {
        val expectedAddress = AddressDetails(name = "skyler", address = PaymentSheet.Address(country = "US"))

        val viewModel = createViewModel(expectedAddress)
        assertThat(viewModel.collectedAddress.value).isEqualTo(expectedAddress)
    }

    @Test
    fun `default configuration enables stripe-hosted autocomplete with hosted countries`() =
        runTest(UnconfinedTestDispatcher()) {
            val viewModel = createViewModel(config = AddressLauncher.Configuration())

            assertThat(viewModel.autocompleteConfig.shouldUseStripeHostedAutocomplete).isTrue()
            assertThat(viewModel.autocompleteConfig.autocompleteCountries)
                .isEqualTo(AUTOCOMPLETE_STRIPE_HOSTED_DEFAULT_COUNTRIES)
        }

    @Test
    fun `builder preserves custom autocomplete countries with stripe-hosted autocomplete`() =
        runTest(UnconfinedTestDispatcher()) {
            val customCountries = setOf("US", "GB")
            val viewModel = createViewModel(
                config = AddressLauncher.Configuration.Builder()
                    .autocompleteCountries(customCountries)
                    .build()
            )

            assertThat(viewModel.autocompleteConfig.shouldUseStripeHostedAutocomplete).isTrue()
            assertThat(viewModel.autocompleteConfig.autocompleteCountries).isEqualTo(customCountries)
        }

    @Test
    fun `completion analytics does not treat merchant default as autocomplete selection`() = runScenario(
        address = AddressDetails(
            address = PaymentSheet.Address(
                line1 = "99 Broadway St",
                city = "Seattle",
                country = "US",
            )
        ),
    ) {
        viewModel.clickPrimaryButton(
            completedFormValues = mapOf(
                FormFieldId.Line1 to FormFieldEntry(value = "99 Broadway St", isComplete = true),
                FormFieldId.City to FormFieldEntry(value = "Seattle", isComplete = true),
                FormFieldId.Country to FormFieldEntry(value = "US", isComplete = true),
            ),
            checkboxChecked = true,
        )

        assertThat(primaryButtonAction.calls.awaitItem().address?.line1).isEqualTo("99 Broadway St")
        assertThat(resultStateHolder.result.value).isEqualTo(
            AddressElementActivityContract.Result.StandaloneSucceeded(
                AddressDetails(
                    address = PaymentSheet.Address(
                        line1 = "99 Broadway St",
                        city = "Seattle",
                        country = "US",
                    ),
                    isCheckboxSelected = true,
                )
            )
        )
        assertThat(viewModel.formEnabled.value).isFalse()

        assertThat(eventReporter.completedCalls.awaitItem()).isEqualTo(
            FakeAddressLauncherEventReporter.CompletedCall(
                country = "US",
                autocompleteResultSelected = false,
                editDistance = null,
            )
        )
    }

    @Test
    fun `completion analytics compares against the selected autocomplete prediction`() = runScenario(
        address = AddressDetails(
            address = PaymentSheet.Address(
                line1 = "88 Market Street",
                city = "San Francisco",
                country = "US",
                postalCode = "94103",
                state = "CA",
            )
        ),
    ) {
        selectAutocompletePrediction(SELECTED_AUTOCOMPLETE_ADDRESS)

        viewModel.clickPrimaryButton(
            completedFormValues = SELECTED_AUTOCOMPLETE_FORM_VALUES,
            checkboxChecked = false,
        )

        assertThat(primaryButtonAction.calls.awaitItem().address?.line1).isEqualTo("123 Main Street")
        assertThat(eventReporter.completedCalls.awaitItem()).isEqualTo(
            FakeAddressLauncherEventReporter.CompletedCall(
                country = "US",
                autocompleteResultSelected = true,
                editDistance = 0,
            )
        )
    }

    @Test
    fun `completion analytics measures edits against the selected autocomplete prediction`() = runScenario {
        selectAutocompletePrediction(SELECTED_AUTOCOMPLETE_ADDRESS)

        viewModel.clickPrimaryButton(
            completedFormValues = SELECTED_AUTOCOMPLETE_FORM_VALUES +
                (FormFieldId.Line1 to FormFieldEntry("123 Main St", true)),
            checkboxChecked = false,
        )

        assertThat(primaryButtonAction.calls.awaitItem().address?.line1).isEqualTo("123 Main St")
        assertThat(eventReporter.completedCalls.awaitItem()).isEqualTo(
            FakeAddressLauncherEventReporter.CompletedCall(
                country = "US",
                autocompleteResultSelected = true,
                // "123 Main Street" -> "123 Main St"
                editDistance = 4,
            )
        )
    }

    @Test
    fun `clickPrimaryButton reports save started and completed`() = runScenario(
        useStandaloneEventReporter = false,
    ) {
        viewModel.clickPrimaryButton(COMPLETED_FORM_VALUES, checkboxChecked = true)

        assertThat(primaryButtonAction.calls.awaitItem()).isEqualTo(EXPECTED_ADDRESS)
        val started = addressElementEventReporter.saveStartedCalls.awaitItem()
        assertThat(started).isEqualTo(
            FakeAddressElementEventReporter.AnalyticsCall(
                country = "US",
                autocompleteResultSelected = false,
                editDistance = null,
            )
        )
        assertThat(addressElementEventReporter.saveCompletedCalls.awaitItem()).isEqualTo(started)
        assertThat(resultStateHolder.result.value)
            .isEqualTo(AddressElementActivityContract.Result.StandaloneSucceeded(EXPECTED_ADDRESS))
    }

    @Test
    fun `clickPrimaryButton does not report a prefilled address as an autocomplete selection`() = runScenario(
        address = EXPECTED_ADDRESS,
        useStandaloneEventReporter = false,
    ) {
        viewModel.clickPrimaryButton(COMPLETED_FORM_VALUES, checkboxChecked = true)

        assertThat(primaryButtonAction.calls.awaitItem()).isEqualTo(EXPECTED_ADDRESS)
        val started = addressElementEventReporter.saveStartedCalls.awaitItem()
        assertThat(started).isEqualTo(
            FakeAddressElementEventReporter.AnalyticsCall(
                country = "US",
                autocompleteResultSelected = false,
                editDistance = null,
            )
        )
        assertThat(addressElementEventReporter.saveCompletedCalls.awaitItem()).isEqualTo(started)
    }

    @Test
    fun `clickPrimaryButton reports the selected inline autocomplete address`() = runScenario(
        useStandaloneEventReporter = false,
    ) {
        selectAutocompletePrediction(
            Address(
                city = "San Francisco",
                country = "US",
                line1 = "510 Townsend St",
                line2 = "Floor 2",
                postalCode = "94103",
                state = "CA",
            )
        )

        val editedFormValues = COMPLETED_FORM_VALUES +
            (FormFieldId.Line1 to FormFieldEntry("510 Townsend Sta", true))
        viewModel.clickPrimaryButton(editedFormValues, checkboxChecked = true)

        assertThat(primaryButtonAction.calls.awaitItem()).isEqualTo(
            EXPECTED_ADDRESS.copy(address = EXPECTED_ADDRESS.address?.copy(line1 = "510 Townsend Sta"))
        )
        val started = addressElementEventReporter.saveStartedCalls.awaitItem()
        assertThat(started).isEqualTo(
            FakeAddressElementEventReporter.AnalyticsCall(
                country = "US",
                autocompleteResultSelected = true,
                editDistance = 1,
            )
        )
        assertThat(addressElementEventReporter.saveCompletedCalls.awaitItem()).isEqualTo(started)
    }

    @Test
    fun `clickPrimaryButton reports save failure and re-enables the form`() = runScenario(
        useStandaloneEventReporter = false,
    ) {
        val error = IllegalStateException("save failed")
        primaryButtonAction.action = { Result.failure(error) }

        viewModel.clickPrimaryButton(COMPLETED_FORM_VALUES, checkboxChecked = true)

        assertThat(primaryButtonAction.calls.awaitItem()).isEqualTo(EXPECTED_ADDRESS)
        val started = addressElementEventReporter.saveStartedCalls.awaitItem()
        val failed = addressElementEventReporter.saveFailedCalls.awaitItem()
        assertThat(failed.analyticsCall).isEqualTo(started)
        assertThat(failed.error).isSameInstanceAs(error)
        assertThat(viewModel.formEnabled.value).isTrue()
        addressElementEventReporter.saveCompletedCalls.expectNoEvents()
    }

    @Test
    fun `cancellation reports canceled`() = runScenario(
        useStandaloneEventReporter = false,
    ) {
        resultStateHolder.setResult(AddressElementActivityContract.Result.Canceled)

        assertThat(addressElementEventReporter.canceledCalls.awaitItem()).isEqualTo(
            FakeAddressElementEventReporter.AnalyticsCall(
                country = "US",
                autocompleteResultSelected = false,
                editDistance = null,
            )
        )
    }

    @Test
    fun `cancellation during save does not report save completed`() = runScenario(
        useStandaloneEventReporter = false,
    ) {
        val primaryButtonResult = CompletableDeferred<Result<AddressElementActivityContract.Result>>()
        primaryButtonAction.action = { primaryButtonResult.await() }

        viewModel.clickPrimaryButton(COMPLETED_FORM_VALUES, checkboxChecked = true)

        assertThat(addressElementEventReporter.saveStartedCalls.awaitItem()).isEqualTo(
            FakeAddressElementEventReporter.AnalyticsCall(
                country = "US",
                autocompleteResultSelected = false,
                editDistance = null,
            )
        )
        assertThat(primaryButtonAction.calls.awaitItem()).isEqualTo(EXPECTED_ADDRESS)

        resultStateHolder.setResult(AddressElementActivityContract.Result.Canceled)
        assertThat(addressElementEventReporter.canceledCalls.awaitItem()).isEqualTo(
            FakeAddressElementEventReporter.AnalyticsCall(
                country = "US",
                autocompleteResultSelected = false,
                editDistance = null,
            )
        )
        primaryButtonResult.complete(
            Result.success(AddressElementActivityContract.Result.StandaloneSucceeded(EXPECTED_ADDRESS))
        )
        testScheduler.runCurrent()

        assertThat(resultStateHolder.result.value).isEqualTo(AddressElementActivityContract.Result.Canceled)
        addressElementEventReporter.saveCompletedCalls.expectNoEvents()
        addressElementEventReporter.saveFailedCalls.expectNoEvents()
    }

    @Test
    fun `clickPrimaryButton accepts a second click when first submission fails`() = runTest {
        val results = ArrayDeque<Result<AddressElementActivityContract.Result>>(
            listOf(
                Result.failure(IllegalStateException("first submission failed")),
                Result.success(
                    AddressElementActivityContract.Result.StandaloneSucceeded(EXPECTED_ADDRESS)
                ),
            )
        )
        val primaryButtonAction = RecordingPrimaryButtonAction {
            results.removeFirst()
        }
        val eventReporter = FakeAddressLauncherEventReporter()
        val viewModel = createViewModel(
            primaryButtonAction = primaryButtonAction,
            eventReporter = eventReporter,
        )

        assertThat(viewModel.saveError.value).isNull()

        viewModel.clickPrimaryButton(COMPLETED_FORM_VALUES, checkboxChecked = true)

        assertThat(primaryButtonAction.calls.awaitItem()).isEqualTo(EXPECTED_ADDRESS)
        assertThat(viewModel.formEnabled.value).isTrue()
        assertThat(viewModel.saveError.value)
            .isEqualTo(IllegalStateException("first submission failed").stripeErrorMessage())
        eventReporter.completedCalls.expectNoEvents()
        assertThat(resultStateHolder.result.value).isNull()

        viewModel.clickPrimaryButton(COMPLETED_FORM_VALUES, checkboxChecked = true)

        assertThat(primaryButtonAction.calls.awaitItem()).isEqualTo(EXPECTED_ADDRESS)
        assertThat(viewModel.formEnabled.value).isFalse()
        assertThat(viewModel.saveError.value).isNull()
        assertThat(eventReporter.completedCalls.awaitItem().country).isEqualTo("US")
        assertThat(resultStateHolder.result.value).isEqualTo(
            AddressElementActivityContract.Result.StandaloneSucceeded(EXPECTED_ADDRESS)
        )

        primaryButtonAction.calls.expectNoEvents()
        primaryButtonAction.validate()
        eventReporter.validate()
    }

    @Test
    fun `clickPrimaryButton replaces save error when retry fails`() = runTest {
        val firstError = LocalStripeException("first submission failed", null)
        val secondError = LocalStripeException("second submission failed", null)
        val results = ArrayDeque<Result<AddressElementActivityContract.Result>>(
            listOf(
                Result.failure(firstError),
                Result.failure(secondError),
            )
        )
        val primaryButtonAction = RecordingPrimaryButtonAction {
            results.removeFirst()
        }
        val eventReporter = FakeAddressLauncherEventReporter()
        val viewModel = createViewModel(
            primaryButtonAction = primaryButtonAction,
            eventReporter = eventReporter,
        )

        viewModel.clickPrimaryButton(COMPLETED_FORM_VALUES, checkboxChecked = true)

        assertThat(primaryButtonAction.calls.awaitItem()).isEqualTo(EXPECTED_ADDRESS)
        assertThat(viewModel.saveError.value).isEqualTo(firstError.stripeErrorMessage())
        assertThat(viewModel.formEnabled.value).isTrue()

        viewModel.clickPrimaryButton(COMPLETED_FORM_VALUES, checkboxChecked = true)

        assertThat(primaryButtonAction.calls.awaitItem()).isEqualTo(EXPECTED_ADDRESS)
        assertThat(viewModel.saveError.value).isEqualTo(secondError.stripeErrorMessage())
        assertThat(viewModel.formEnabled.value).isTrue()
        assertThat(resultStateHolder.result.value).isNull()
        eventReporter.completedCalls.expectNoEvents()

        primaryButtonAction.validate()
        eventReporter.validate()
    }

    @Test
    fun `editing the form clears the save error`() = runTest {
        val error = LocalStripeException("submission failed", null)
        val primaryButtonAction = RecordingPrimaryButtonAction {
            Result.failure(error)
        }
        val viewModel = createViewModel(
            address = EXPECTED_ADDRESS,
            primaryButtonAction = primaryButtonAction,
        )

        viewModel.clickPrimaryButton(COMPLETED_FORM_VALUES, checkboxChecked = true)

        assertThat(primaryButtonAction.calls.awaitItem()).isEqualTo(EXPECTED_ADDRESS)
        assertThat(viewModel.saveError.value).isEqualTo(error.stripeErrorMessage())

        viewModel.setRawValues(mapOf(FormFieldId.Line1 to ""))

        assertThat(viewModel.saveError.value).isNull()

        primaryButtonAction.validate()
    }

    @Test
    fun `clickPrimaryButton ignores a second click while first submission is in flight`() = runTest {
        val primaryButtonResult = CompletableDeferred<Result<AddressElementActivityContract.Result>>()
        val primaryButtonAction = RecordingPrimaryButtonAction {
            primaryButtonResult.await()
        }
        val eventReporter = FakeAddressLauncherEventReporter()
        val viewModel = createViewModel(
            primaryButtonAction = primaryButtonAction,
            eventReporter = eventReporter,
        )
        val controller = (
            (viewModel.addressFormController.elements.single() as SectionElement).fields.single()
                as AutocompleteAddressElement
            ).sectionFieldErrorController()

        controller.validationMessage.test {
            assertThat(awaitItem()).isNull()

            viewModel.clickPrimaryButton(COMPLETED_FORM_VALUES, checkboxChecked = true)

            assertThat(primaryButtonAction.calls.awaitItem()).isEqualTo(EXPECTED_ADDRESS)
            assertThat(viewModel.formEnabled.value).isFalse()

            viewModel.clickPrimaryButton(completedFormValues = null, checkboxChecked = true)

            primaryButtonAction.calls.expectNoEvents()
            eventReporter.completedCalls.expectNoEvents()
            expectNoEvents()
            assertThat(resultStateHolder.result.value).isNull()

            primaryButtonResult.complete(
                Result.success(
                    AddressElementActivityContract.Result.StandaloneSucceeded(EXPECTED_ADDRESS)
                )
            )
            eventReporter.completedCalls.awaitItem()
        }

        primaryButtonAction.validate()
        eventReporter.validate()
    }

    @Test
    fun `default checkbox should emit true to start if passed by merchant`() = runTest(UnconfinedTestDispatcher()) {
        val viewModel = createViewModel(
            AddressDetails(
                isCheckboxSelected = true
            )
        )
        assertThat(viewModel.checkboxChecked.value).isTrue()
    }

    @Test
    fun `default checkbox should emit false to start if passed by merchant`() = runTest(UnconfinedTestDispatcher()) {
        val viewModel = createViewModel(
            AddressDetails(
                isCheckboxSelected = false
            )
        )
        assertThat(viewModel.checkboxChecked.value).isFalse()
    }

    @Test
    fun `default checkbox should emit false to start by default`() = runTest(UnconfinedTestDispatcher()) {
        val viewModel = createViewModel()
        assertThat(viewModel.checkboxChecked.value).isFalse()
    }

    @Test
    fun `clicking the checkbox should change the internal state`() = runTest(UnconfinedTestDispatcher()) {
        val viewModel = createViewModel()

        assertThat(viewModel.checkboxChecked.value).isFalse()

        viewModel.clickCheckbox(true)
        assertThat(viewModel.checkboxChecked.value).isTrue()

        viewModel.clickCheckbox(false)
        assertThat(viewModel.checkboxChecked.value).isFalse()

        viewModel.clickCheckbox(true)
        assertThat(viewModel.checkboxChecked.value).isTrue()
    }

    @Test
    fun `If default address country not in allowed countries, state should be 'Hide'`() =
        billingSameAsShippingInitialValueTest(
            billingAddress = PaymentSheet.BillingDetails(
                name = "John Doe",
                address = PaymentSheet.Address(
                    country = "CA"
                )
            ),
            allowedCountries = setOf("US", "MX"),
            address = null,
            expectedShippingSameAsBillingState = InputAddressViewModel.ShippingSameAsBillingState.Hide,
        )

    @Test
    fun `If billing address is null, state should be 'Hide'`() =
        billingSameAsShippingInitialValueTest(
            billingAddress = null,
            allowedCountries = setOf("US"),
            address = null,
            expectedShippingSameAsBillingState = InputAddressViewModel.ShippingSameAsBillingState.Hide,
        )

    @Test
    fun `If default address supported in allowed countries & checkbox enabled, state should be 'Show' & checked`() =
        billingSameAsShippingInitialValueTest(
            billingAddress = PaymentSheet.BillingDetails(
                name = "John Doe",
                address = PaymentSheet.Address(
                    line1 = "123 Apple Street",
                    city = "San Francisco",
                    country = "US",
                    state = "CA",
                    postalCode = "99999"
                )
            ),
            allowedCountries = setOf("US"),
            address = null,
            expectedShippingSameAsBillingState = InputAddressViewModel.ShippingSameAsBillingState.Show(
                isChecked = true,
            ),
        )

    @Test
    fun `If default address has no country & checkbox enabled, state should be 'Show' & checked`() =
        billingSameAsShippingInitialValueTest(
            billingAddress = PaymentSheet.BillingDetails(
                name = "John Doe",
                address = PaymentSheet.Address(
                    line1 = "123 Apple Street",
                    city = "San Francisco",
                    postalCode = "99999"
                )
            ),
            allowedCountries = setOf("US"),
            address = null,
            expectedShippingSameAsBillingState = InputAddressViewModel.ShippingSameAsBillingState.Show(
                isChecked = true,
            ),
        )

    @Test
    fun `If empty allowed countries, state should be 'Show' & checked since default countries are used`() =
        billingSameAsShippingInitialValueTest(
            billingAddress = PaymentSheet.BillingDetails(
                name = "John Doe",
                address = PaymentSheet.Address(
                    line1 = "123 Apple Street",
                    city = "San Francisco",
                    country = "US",
                    state = "CA",
                    postalCode = "99999"
                )
            ),
            allowedCountries = emptySet(),
            address = null,
            expectedShippingSameAsBillingState = InputAddressViewModel.ShippingSameAsBillingState.Show(
                isChecked = true,
            ),
        )

    @Test
    fun `If shipping address provided with billing, state should be 'Show' but not checked`() =
        billingSameAsShippingInitialValueTest(
            billingAddress = PaymentSheet.BillingDetails(
                name = "John Doe",
                address = PaymentSheet.Address(
                    line1 = "123 Apple Street",
                    city = "San Francisco",
                    country = "US",
                    state = "CA",
                    postalCode = "99999"
                )
            ),
            allowedCountries = emptySet(),
            address = AddressDetails(
                name = "Jane Doe",
                address = PaymentSheet.Address(
                    line1 = "123 Pear Street",
                    city = "San Jose",
                    country = "US",
                    state = "CA",
                    postalCode = "88888"
                )
            ),
            expectedShippingSameAsBillingState = InputAddressViewModel.ShippingSameAsBillingState.Show(
                isChecked = false,
            ),
        )

    @OptIn(AddressElementSameAsBillingPreview::class)
    @Test
    fun `'Shipping same as billing' should work as expected when only billing provided`() = runTest {
        val viewModel = createViewModel(
            config = AddressLauncher.Configuration.Builder()
                .allowedCountries(setOf("US"))
                .billingAddress(
                    PaymentSheet.BillingDetails(
                        name = "John Doe",
                        address = PaymentSheet.Address(
                            line1 = "123 Apple Street",
                            city = "San Francisco",
                            country = "US",
                            state = "CA",
                            postalCode = "99999"
                        ),
                        phone = "+11234567890"
                    )
                )
                .additionalFields(
                    AddressLauncher.AdditionalFieldsConfiguration(
                        phone = AddressLauncher.AdditionalFieldsConfiguration.FieldConfiguration.HIDDEN,
                    )
                )
                .build()
        )

        turbineScope {
            val shippingSameAsBillingStateTurbine = viewModel.shippingSameAsBillingState.testIn(scope = this)
            val formValuesTurbine = viewModel.addressFormController.uncompletedFormValues.testIn(scope = this)

            // Should initially be empty
            assertThat(shippingSameAsBillingStateTurbine.awaitItem()).isEqualTo(createShowState(isChecked = true))
            assertThat(formValuesTurbine.awaitItem()).containsExactlyEntriesIn(
                mapOf(
                    FormFieldId.Name to FormFieldEntry(value = "John Doe", isComplete = true),
                    FormFieldId.Country to FormFieldEntry(value = "US", isComplete = true),
                    FormFieldId.State to FormFieldEntry(value = "CA", isComplete = true),
                    FormFieldId.Line1 to FormFieldEntry(value = "123 Apple Street", isComplete = true),
                    FormFieldId.Line2 to FormFieldEntry(value = "", isComplete = true),
                    FormFieldId.City to FormFieldEntry(value = "San Francisco", isComplete = true),
                    FormFieldId.PostalCode to FormFieldEntry(value = "99999", isComplete = true)
                )
            )

            viewModel.clickBillingSameAsShipping(newValue = false)

            // Should be checked and filled with default address
            assertThat(shippingSameAsBillingStateTurbine.awaitItem()).isEqualTo(createShowState(isChecked = false))
            assertThat(formValuesTurbine.awaitItem()).containsExactlyEntriesIn(
                mapOf(
                    FormFieldId.Name to FormFieldEntry(value = "", isComplete = false),
                    FormFieldId.Country to FormFieldEntry(value = "US", isComplete = true),
                    FormFieldId.Generic("address") to FormFieldEntry(value = "", isComplete = false),
                )
            )

            viewModel.onEnterManuallyFromInline()
            assertThat(formValuesTurbine.awaitItem().keys).contains(FormFieldId.Line1)

            viewModel.setRawValues(
                mapOf(
                    FormFieldId.Name to "Jane Doe",
                    FormFieldId.Line1 to "123 Pear Street",
                    FormFieldId.PostalCode to "88888",
                )
            )

            // Should be unchecked and use input
            shippingSameAsBillingStateTurbine.expectNoEvents()
            assertThat(formValuesTurbine.expectMostRecentItem()).containsExactlyEntriesIn(
                mapOf(
                    FormFieldId.Name to FormFieldEntry(value = "Jane Doe", isComplete = true),
                    FormFieldId.Country to FormFieldEntry(value = "US", isComplete = true),
                    FormFieldId.State to FormFieldEntry(value = null, isComplete = false),
                    FormFieldId.Line1 to FormFieldEntry(value = "123 Pear Street", isComplete = true),
                    FormFieldId.Line2 to FormFieldEntry(value = "", isComplete = true),
                    FormFieldId.City to FormFieldEntry(value = "", isComplete = false),
                    FormFieldId.PostalCode to FormFieldEntry(value = "88888", isComplete = true)
                )
            )

            viewModel.clickBillingSameAsShipping(newValue = true)

            // Should be checked and filled with default address
            assertThat(shippingSameAsBillingStateTurbine.awaitItem()).isEqualTo(createShowState(isChecked = true))
            assertThat(formValuesTurbine.awaitItem()).containsExactlyEntriesIn(
                mapOf(
                    FormFieldId.Name to FormFieldEntry(value = "John Doe", isComplete = true),
                    FormFieldId.Country to FormFieldEntry(value = "US", isComplete = true),
                    FormFieldId.State to FormFieldEntry(value = "CA", isComplete = true),
                    FormFieldId.Line1 to FormFieldEntry(value = "123 Apple Street", isComplete = true),
                    FormFieldId.Line2 to FormFieldEntry(value = "", isComplete = true),
                    FormFieldId.City to FormFieldEntry(value = "San Francisco", isComplete = true),
                    FormFieldId.PostalCode to FormFieldEntry(value = "99999", isComplete = true)
                )
            )

            viewModel.clickBillingSameAsShipping(newValue = false)

            // Should be unchecked and filled with previous user input
            assertThat(shippingSameAsBillingStateTurbine.awaitItem()).isEqualTo(createShowState(isChecked = false))
            assertThat(formValuesTurbine.awaitItem()).containsExactlyEntriesIn(
                mapOf(
                    FormFieldId.Name to FormFieldEntry(value = "Jane Doe", isComplete = true),
                    FormFieldId.Country to FormFieldEntry(value = "US", isComplete = true),
                    FormFieldId.State to FormFieldEntry(value = null, isComplete = false),
                    FormFieldId.Line1 to FormFieldEntry(value = "123 Pear Street", isComplete = true),
                    FormFieldId.Line2 to FormFieldEntry(value = "", isComplete = true),
                    FormFieldId.City to FormFieldEntry(value = "", isComplete = false),
                    FormFieldId.PostalCode to FormFieldEntry(value = "88888", isComplete = true)
                )
            )

            viewModel.clickBillingSameAsShipping(newValue = true)

            // Should be checked and filled with provided billing details
            assertThat(shippingSameAsBillingStateTurbine.awaitItem()).isEqualTo(createShowState(isChecked = true))
            assertThat(formValuesTurbine.awaitItem()).containsExactlyEntriesIn(
                mapOf(
                    FormFieldId.Name to FormFieldEntry(value = "John Doe", isComplete = true),
                    FormFieldId.Country to FormFieldEntry(value = "US", isComplete = true),
                    FormFieldId.State to FormFieldEntry(value = "CA", isComplete = true),
                    FormFieldId.Line1 to FormFieldEntry(value = "123 Apple Street", isComplete = true),
                    FormFieldId.Line2 to FormFieldEntry(value = "", isComplete = true),
                    FormFieldId.City to FormFieldEntry(value = "San Francisco", isComplete = true),
                    FormFieldId.PostalCode to FormFieldEntry(value = "99999", isComplete = true)
                )
            )

            viewModel.setRawValues(
                mapOf(
                    FormFieldId.Name to "Jane Doe",
                    FormFieldId.Line1 to "123 Coffee Street",
                    FormFieldId.PostalCode to "77777",
                )
            )

            // Should be unchecked and filled with new user input
            assertThat(shippingSameAsBillingStateTurbine.awaitItem()).isEqualTo(createShowState(isChecked = false))
            assertThat(formValuesTurbine.expectMostRecentItem()).containsExactlyEntriesIn(
                mapOf(
                    FormFieldId.Name to FormFieldEntry(value = "Jane Doe", isComplete = true),
                    FormFieldId.Country to FormFieldEntry(value = "US", isComplete = true),
                    FormFieldId.State to FormFieldEntry(value = "CA", isComplete = true),
                    FormFieldId.Line1 to FormFieldEntry(value = "123 Coffee Street", isComplete = true),
                    FormFieldId.Line2 to FormFieldEntry(value = "", isComplete = true),
                    FormFieldId.City to FormFieldEntry(value = "San Francisco", isComplete = true),
                    FormFieldId.PostalCode to FormFieldEntry(value = "77777", isComplete = true)
                )
            )

            shippingSameAsBillingStateTurbine.cancel()
            formValuesTurbine.cancel()
        }
    }

    @OptIn(AddressElementSameAsBillingPreview::class)
    @Test
    fun `'Shipping same as billing' should work as expected with both billing & shipping`() = runTest {
        val viewModel = createViewModel(
            config = AddressLauncher.Configuration.Builder()
                .allowedCountries(setOf("US"))
                .address(
                    AddressDetails(
                        name = "Jane Doe",
                        address = PaymentSheet.Address(
                            line1 = "123 Coffee Street",
                            city = "San Jose",
                            country = "US",
                            state = "CA",
                            postalCode = "77777"
                        ),
                    )
                )
                .billingAddress(
                    PaymentSheet.BillingDetails(
                        name = "John Doe",
                        address = PaymentSheet.Address(
                            line1 = "123 Apple Street",
                            city = "San Francisco",
                            country = "US",
                            state = "CA",
                            postalCode = "99999"
                        ),
                    )
                )
                .additionalFields(
                    AddressLauncher.AdditionalFieldsConfiguration(
                        phone = AddressLauncher.AdditionalFieldsConfiguration.FieldConfiguration.HIDDEN,
                    )
                )
                .build()
        )

        turbineScope {
            val shippingSameAsBillingStateTurbine = viewModel.shippingSameAsBillingState.testIn(scope = this)
            val formValuesTurbine = viewModel.addressFormController.uncompletedFormValues.testIn(scope = this)

            // Should be unchecked and use initial shipping address
            assertThat(shippingSameAsBillingStateTurbine.awaitItem()).isEqualTo(createShowState(isChecked = false))
            assertThat(formValuesTurbine.awaitItem()).containsExactlyEntriesIn(
                mapOf(
                    FormFieldId.Name to FormFieldEntry(value = "Jane Doe", isComplete = true),
                    FormFieldId.Country to FormFieldEntry(value = "US", isComplete = true),
                    FormFieldId.State to FormFieldEntry(value = "CA", isComplete = true),
                    FormFieldId.Line1 to FormFieldEntry(value = "123 Coffee Street", isComplete = true),
                    FormFieldId.Line2 to FormFieldEntry(value = "", isComplete = true),
                    FormFieldId.City to FormFieldEntry(value = "San Jose", isComplete = true),
                    FormFieldId.PostalCode to FormFieldEntry(value = "77777", isComplete = true)
                )
            )

            viewModel.clickBillingSameAsShipping(newValue = true)

            // Should be checked and filled with billing address
            assertThat(shippingSameAsBillingStateTurbine.awaitItem()).isEqualTo(createShowState(isChecked = true))
            assertThat(formValuesTurbine.awaitItem()).containsExactlyEntriesIn(
                mapOf(
                    FormFieldId.Name to FormFieldEntry(value = "John Doe", isComplete = true),
                    FormFieldId.Country to FormFieldEntry(value = "US", isComplete = true),
                    FormFieldId.State to FormFieldEntry(value = "CA", isComplete = true),
                    FormFieldId.Line1 to FormFieldEntry(value = "123 Apple Street", isComplete = true),
                    FormFieldId.Line2 to FormFieldEntry(value = "", isComplete = true),
                    FormFieldId.City to FormFieldEntry(value = "San Francisco", isComplete = true),
                    FormFieldId.PostalCode to FormFieldEntry(value = "99999", isComplete = true)
                )
            )

            viewModel.clickBillingSameAsShipping(newValue = false)

            // Should re-use shipping address since no previous input
            assertThat(shippingSameAsBillingStateTurbine.awaitItem()).isEqualTo(createShowState(isChecked = false))
            assertThat(formValuesTurbine.expectMostRecentItem()).containsExactlyEntriesIn(
                mapOf(
                    FormFieldId.Name to FormFieldEntry(value = "Jane Doe", isComplete = true),
                    FormFieldId.Country to FormFieldEntry(value = "US", isComplete = true),
                    FormFieldId.State to FormFieldEntry(value = "CA", isComplete = true),
                    FormFieldId.Line1 to FormFieldEntry(value = "123 Coffee Street", isComplete = true),
                    FormFieldId.Line2 to FormFieldEntry(value = "", isComplete = true),
                    FormFieldId.City to FormFieldEntry(value = "San Jose", isComplete = true),
                    FormFieldId.PostalCode to FormFieldEntry(value = "77777", isComplete = true)
                )
            )

            shippingSameAsBillingStateTurbine.cancel()
            formValuesTurbine.cancel()
        }
    }

    @OptIn(AddressElementSameAsBillingPreview::class)
    @Test
    fun `'Shipping same as billing' should work as expected with same billing & shipping`() = runTest {
        val viewModel = createViewModel(
            config = AddressLauncher.Configuration.Builder()
                .allowedCountries(setOf("US"))
                .address(
                    AddressDetails(
                        name = "John Doe",
                        address = PaymentSheet.Address(
                            line1 = "123 Apple Street",
                            city = "San Francisco",
                            country = "US",
                            state = "CA",
                            postalCode = "99999"
                        ),
                    )
                )
                .billingAddress(
                    PaymentSheet.BillingDetails(
                        name = "John Doe",
                        address = PaymentSheet.Address(
                            line1 = "123 Apple Street",
                            city = "San Francisco",
                            country = "US",
                            state = "CA",
                            postalCode = "99999"
                        ),
                    )
                )
                .additionalFields(
                    AddressLauncher.AdditionalFieldsConfiguration(
                        phone = AddressLauncher.AdditionalFieldsConfiguration.FieldConfiguration.HIDDEN,
                    )
                )
                .build()
        )

        turbineScope {
            val shippingSameAsBillingStateTurbine = viewModel.shippingSameAsBillingState.testIn(scope = this)
            val formValuesTurbine = viewModel.addressFormController.uncompletedFormValues.testIn(scope = this)

            // Should be checked
            assertThat(shippingSameAsBillingStateTurbine.awaitItem()).isEqualTo(createShowState(isChecked = true))
            assertThat(formValuesTurbine.awaitItem()).containsExactlyEntriesIn(
                mapOf(
                    FormFieldId.Name to FormFieldEntry(value = "John Doe", isComplete = true),
                    FormFieldId.Country to FormFieldEntry(value = "US", isComplete = true),
                    FormFieldId.State to FormFieldEntry(value = "CA", isComplete = true),
                    FormFieldId.Line1 to FormFieldEntry(value = "123 Apple Street", isComplete = true),
                    FormFieldId.Line2 to FormFieldEntry(value = "", isComplete = true),
                    FormFieldId.City to FormFieldEntry(value = "San Francisco", isComplete = true),
                    FormFieldId.PostalCode to FormFieldEntry(value = "99999", isComplete = true)
                )
            )

            viewModel.clickBillingSameAsShipping(newValue = false)

            // Should be unchecked and empty
            assertThat(shippingSameAsBillingStateTurbine.awaitItem()).isEqualTo(createShowState(isChecked = false))
            assertThat(formValuesTurbine.awaitItem()).containsExactlyEntriesIn(
                mapOf(
                    FormFieldId.Name to FormFieldEntry(value = "", isComplete = false),
                    FormFieldId.Country to FormFieldEntry(value = "US", isComplete = true),
                    FormFieldId.Generic("address") to FormFieldEntry(value = "", isComplete = false),
                )
            )

            shippingSameAsBillingStateTurbine.cancel()
            formValuesTurbine.cancel()
        }
    }

    @OptIn(AddressElementSameAsBillingPreview::class)
    @Test
    fun `'Shipping same as billing' should work as expected with same billing & shipping & empty values`() = runTest {
        val viewModel = createViewModel(
            config = AddressLauncher.Configuration.Builder()
                .allowedCountries(setOf("US"))
                .address(
                    AddressDetails(
                        name = "John Doe",
                        address = PaymentSheet.Address(
                            line1 = "123 Apple Street",
                            line2 = "",
                            city = "San Francisco",
                            country = "US",
                            state = "CA",
                            postalCode = "99999"
                        ),
                    )
                )
                .billingAddress(
                    PaymentSheet.BillingDetails(
                        name = "John Doe",
                        address = PaymentSheet.Address(
                            line1 = "123 Apple Street",
                            line2 = null,
                            city = "San Francisco",
                            country = "US",
                            state = "CA",
                            postalCode = "99999"
                        ),
                    )
                )
                .additionalFields(
                    AddressLauncher.AdditionalFieldsConfiguration(
                        phone = AddressLauncher.AdditionalFieldsConfiguration.FieldConfiguration.HIDDEN,
                    )
                )
                .build()
        )

        turbineScope {
            val shippingSameAsBillingStateTurbine = viewModel.shippingSameAsBillingState.testIn(scope = this)
            val formValuesTurbine = viewModel.addressFormController.uncompletedFormValues.testIn(scope = this)

            // Should be checked
            assertThat(shippingSameAsBillingStateTurbine.awaitItem()).isEqualTo(createShowState(isChecked = true))
            assertThat(formValuesTurbine.awaitItem()).containsExactlyEntriesIn(
                mapOf(
                    FormFieldId.Name to FormFieldEntry(value = "John Doe", isComplete = true),
                    FormFieldId.Country to FormFieldEntry(value = "US", isComplete = true),
                    FormFieldId.State to FormFieldEntry(value = "CA", isComplete = true),
                    FormFieldId.Line1 to FormFieldEntry(value = "123 Apple Street", isComplete = true),
                    FormFieldId.Line2 to FormFieldEntry(value = "", isComplete = true),
                    FormFieldId.City to FormFieldEntry(value = "San Francisco", isComplete = true),
                    FormFieldId.PostalCode to FormFieldEntry(value = "99999", isComplete = true)
                )
            )

            viewModel.clickBillingSameAsShipping(newValue = false)

            // Should be unchecked and empty
            assertThat(shippingSameAsBillingStateTurbine.awaitItem()).isEqualTo(createShowState(isChecked = false))
            assertThat(formValuesTurbine.awaitItem()).containsExactlyEntriesIn(
                mapOf(
                    FormFieldId.Name to FormFieldEntry(value = "", isComplete = false),
                    FormFieldId.Country to FormFieldEntry(value = "US", isComplete = true),
                    FormFieldId.Generic("address") to FormFieldEntry(value = "", isComplete = false),
                )
            )

            shippingSameAsBillingStateTurbine.cancel()
            formValuesTurbine.cancel()
        }
    }

    @Test
    fun `Does not use initial shipping address if not allowed`() = doesNotUseAddressTest(
        config = AddressLauncher.Configuration.Builder()
            .allowedCountries(setOf("CA"))
            .address(
                AddressDetails(
                    name = "John Doe",
                    address = PaymentSheet.Address(
                        line1 = "123 Apple Street",
                        line2 = "",
                        city = "San Francisco",
                        country = "US",
                        state = "CA",
                        postalCode = "99999"
                    ),
                )
            )
            .additionalFields(
                AddressLauncher.AdditionalFieldsConfiguration(
                    phone = AddressLauncher.AdditionalFieldsConfiguration.FieldConfiguration.HIDDEN,
                )
            )
            .build()
    )

    @OptIn(AddressElementSameAsBillingPreview::class)
    @Test
    fun `Does not use initial billing address if not allowed`() = doesNotUseAddressTest(
        config = AddressLauncher.Configuration.Builder()
            .allowedCountries(setOf("CA"))
            .billingAddress(
                PaymentSheet.BillingDetails(
                    name = "John Doe",
                    address = PaymentSheet.Address(
                        line1 = "123 Apple Street",
                        line2 = "",
                        city = "San Francisco",
                        country = "US",
                        state = "CA",
                        postalCode = "99999"
                    ),
                )
            )
            .additionalFields(
                AddressLauncher.AdditionalFieldsConfiguration(
                    phone = AddressLauncher.AdditionalFieldsConfiguration.FieldConfiguration.HIDDEN,
                )
            )
            .build()
    )

    @OptIn(AddressElementSameAsBillingPreview::class)
    @Test
    fun `Does not use initial shipping or billing address if not allowed`() = doesNotUseAddressTest(
        config = AddressLauncher.Configuration.Builder()
            .allowedCountries(setOf("CA"))
            .address(
                AddressDetails(
                    name = "Jane Doe",
                    address = PaymentSheet.Address(
                        line1 = "123 Coffee Street",
                        city = "San Jose",
                        country = "US",
                        state = "CA",
                        postalCode = "77777"
                    ),
                )
            )
            .billingAddress(
                PaymentSheet.BillingDetails(
                    name = "John Doe",
                    address = PaymentSheet.Address(
                        line1 = "123 Apple Street",
                        line2 = "",
                        city = "San Francisco",
                        country = "US",
                        state = "CA",
                        postalCode = "99999"
                    ),
                )
            )
            .additionalFields(
                AddressLauncher.AdditionalFieldsConfiguration(
                    phone = AddressLauncher.AdditionalFieldsConfiguration.FieldConfiguration.HIDDEN,
                )
            )
            .build()
    )

    @OptIn(AddressElementSameAsBillingPreview::class)
    @Test
    fun `Billing same as shipping box is checked even if initial inputs have slightly different formatting`() =
        runTest {
            val viewModel = createViewModel(
                config = AddressLauncher.Configuration.Builder()
                    .allowedCountries(setOf("US"))
                    .address(
                        AddressDetails(
                            name = "John Doe",
                            address = PaymentSheet.Address(
                                line1 = "123 Apple Street",
                                line2 = "",
                                city = "San Francisco",
                                country = "US",
                                state = "CA",
                                postalCode = "99999 "
                            ),
                            phoneNumber = "+12347682350"
                        )
                    )
                    .billingAddress(
                        PaymentSheet.BillingDetails(
                            name = "John Doe",
                            address = PaymentSheet.Address(
                                line1 = "123 Apple Street",
                                line2 = null,
                                city = "San Francisco",
                                country = "US",
                                state = "CA",
                                postalCode = "99999"
                            ),
                            phone = "(234) 768-2350"
                        )
                    )
                    .additionalFields(
                        AddressLauncher.AdditionalFieldsConfiguration(
                            phone = AddressLauncher.AdditionalFieldsConfiguration.FieldConfiguration.REQUIRED,
                        )
                    )
                    .build()
            )

            viewModel.shippingSameAsBillingState.test {
                // Should be checked
                assertThat(awaitItem()).isEqualTo(createShowState(isChecked = true))
            }
        }

    private fun doesNotUseAddressTest(
        config: AddressLauncher.Configuration,
    ) = runTest {
        val viewModel = createViewModel(
            config = config,
        )

        turbineScope {
            val shippingSameAsBillingStateTurbine = viewModel.shippingSameAsBillingState.testIn(scope = this)
            val formValuesTurbine = viewModel.addressFormController.uncompletedFormValues.testIn(scope = this)

            assertThat(shippingSameAsBillingStateTurbine.awaitItem())
                .isEqualTo(InputAddressViewModel.ShippingSameAsBillingState.Hide)
            assertThat(formValuesTurbine.awaitItem()).containsExactlyEntriesIn(
                mapOf(
                    FormFieldId.Name to FormFieldEntry(value = "", isComplete = false),
                    FormFieldId.Country to FormFieldEntry(value = "CA", isComplete = true),
                    FormFieldId.Generic("address") to FormFieldEntry(value = "", isComplete = false),
                )
            )

            shippingSameAsBillingStateTurbine.cancel()
            formValuesTurbine.cancel()
        }
    }

    @OptIn(AddressElementSameAsBillingPreview::class)
    private fun billingSameAsShippingInitialValueTest(
        address: AddressDetails?,
        allowedCountries: Set<String>,
        billingAddress: PaymentSheet.BillingDetails?,
        expectedShippingSameAsBillingState: InputAddressViewModel.ShippingSameAsBillingState,
    ) = runTest {
        val viewModel = createViewModel(
            config = AddressLauncher.Configuration.Builder()
                .allowedCountries(allowedCountries)
                .address(address)
                .billingAddress(billingAddress)
                .build()
        )

        viewModel.shippingSameAsBillingState.test {
            assertThat(awaitItem()).isEqualTo(expectedShippingSameAsBillingState)
        }
    }

    private fun InputAddressViewModel.setRawValues(
        values: Map<FormFieldId, String?>
    ) {
        val elements = addressFormController.elements

        assertThat(elements).hasSize(1)
        assertThat(elements[0]).isInstanceOf<SectionElement>()

        val sectionElement = elements[0] as SectionElement
        val fields = sectionElement.fields

        assertThat(fields).hasSize(1)
        assertThat(fields[0]).isInstanceOf<AutocompleteAddressElement>()

        val autocompleteElement = fields[0] as AutocompleteAddressElement

        val addressFields = autocompleteElement.sectionFieldErrorController()
            .addressElementFlow
            .value
            .addressController
            .value
            .fieldsFlowable
            .value

        addressFields.forEach {
            it.setRawValue(values)
        }
    }

    @Test
    fun `clickPrimaryButton with null triggers validation errors without a result`() = runTest {
        val viewModel = createViewModel()

        val sectionElement = viewModel.addressFormController.elements[0] as SectionElement
        val autocompleteElement = sectionElement.fields[0] as AutocompleteAddressElement
        val controller = autocompleteElement.sectionFieldErrorController()

        assertThat(controller.validationMessage.value).isNull()

        viewModel.clickPrimaryButton(
            completedFormValues = null,
            checkboxChecked = false
        )

        assertThat(controller.validationMessage.value).isNotNull()
        assertThat(viewModel.formEnabled.value).isTrue()
        assertThat(resultStateHolder.result.value).isNull()
    }

    @Test
    fun `standalone save emits standalone success`() {
        val viewModel = createViewModel()

        viewModel.clickPrimaryButton(COMPLETED_FORM_VALUES, checkboxChecked = true)

        assertThat(resultStateHolder.result.value).isEqualTo(
            AddressElementActivityContract.Result.StandaloneSucceeded(EXPECTED_ADDRESS)
        )
    }

    @Test
    fun `isInlineAutocompleteEnabled is always true`() {
        val viewModel = createViewModel()
        assertThat(viewModel.autocompleteConfig.isInlineAutocompleteEnabled).isTrue()
    }

    // --- Inline Autocomplete Tests ---
    // Core controller logic (predictions, debouncing, selection, suppression, dismissal)
    // is tested in InlineAutocompleteControllerTest.

    @Suppress("DEPRECATION")
    private fun createInlineViewModel(
        googlePlacesApiKey: String = "test_key",
        autocompleteCountries: Set<String> = emptySet(),
    ): InputAddressViewModel {
        return InputAddressViewModel(
            AddressElementActivityContract.Args.Standalone(
                apiConfiguration = DEFAULT_API_CONFIG,
                config = AddressLauncher.Configuration.Builder()
                    .googlePlacesApiKey(googlePlacesApiKey)
                    .autocompleteCountries(autocompleteCountries)
                    .build(),
            ),
            navigator,
            resultStateHolder,
            StandaloneAddressElementEventReporter(eventReporter),
            placesClient = FakePlacesClientProxy(
                findPredictionsResult = Result.success(FindAutocompletePredictionsResponse(emptyList())),
                fetchPlaceResult = Result.success(Address()),
            ),
            primaryButtonAction = FakeAddressElementPrimaryButtonAction {
                AddressElementActivityContract.Result.StandaloneSucceeded(it)
            },
        ).also { viewModelStoreRule.track(it) }
    }

    @Test
    fun `onEnterManuallyFromInline emits OnExpandForm with current country when query is empty`() = runTest {
        val viewModel = createInlineViewModel()
        var emittedEvent: AutocompleteAddressInteractor.Event? = null
        viewModel.register { emittedEvent = it }

        viewModel.onEnterManuallyFromInline()

        assertThat(emittedEvent)
            .isEqualTo(
                AutocompleteAddressInteractor.Event.OnExpandForm(
                    values = mapOf(FormFieldId.Country to "US")
                )
            )
    }

    @Test
    fun `onEnterManuallyFromInline pre-fills Line1 with typed inline query`() = runTest(UnconfinedTestDispatcher()) {
        val viewModel = createInlineViewModel()
        var emittedEvent: AutocompleteAddressInteractor.Event? = null
        viewModel.register { emittedEvent = it }

        val queryFlow = MutableStateFlow("")
        val countryFlow = MutableStateFlow<String?>("US")
        viewModel.observeQueryChanges(queryFlow, countryFlow)

        queryFlow.value = "123 Main St"
        viewModel.onEnterManuallyFromInline()

        assertThat(emittedEvent).isEqualTo(
            AutocompleteAddressInteractor.Event.OnExpandForm(
                values = mapOf(
                    FormFieldId.Line1 to "123 Main St",
                    FormFieldId.Country to "US",
                )
            )
        )
    }

    private fun createShowState(isChecked: Boolean) =
        InputAddressViewModel.ShippingSameAsBillingState.Show(isChecked)

    private fun runScenario(
        address: AddressDetails? = null,
        config: AddressLauncher.Configuration = AddressLauncher.Configuration.Builder()
            .address(address)
            .build(),
        useStandaloneEventReporter: Boolean = true,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val eventReporter = FakeAddressLauncherEventReporter()
        val addressElementEventReporter = FakeAddressElementEventReporter()
        val primaryButtonAction = RecordingPrimaryButtonAction {
            Result.success(AddressElementActivityContract.Result.StandaloneSucceeded(it))
        }
        val placesClient = FakePlacesClientProxy(
            findPredictionsResult = Result.success(FindAutocompletePredictionsResponse(emptyList())),
            fetchPlaceResult = Result.success(Address()),
        )
        val viewModel = createViewModel(
            config = config,
            eventReporter = eventReporter,
            addressElementEventReporter = if (useStandaloneEventReporter) {
                StandaloneAddressElementEventReporter(eventReporter)
            } else {
                addressElementEventReporter
            },
            placesClient = placesClient,
            primaryButtonAction = primaryButtonAction,
        )

        Scenario(
            viewModel = viewModel,
            eventReporter = eventReporter,
            addressElementEventReporter = addressElementEventReporter,
            placesClient = placesClient,
            primaryButtonAction = primaryButtonAction,
            resultStateHolder = resultStateHolder,
            testScheduler = testScheduler,
        ).apply { block() }

        eventReporter.validate()
        addressElementEventReporter.ensureAllEventsConsumed()
        placesClient.ensureAllEventsConsumed()
        primaryButtonAction.validate()
    }

    private data class Scenario(
        val viewModel: InputAddressViewModel,
        val eventReporter: FakeAddressLauncherEventReporter,
        val addressElementEventReporter: FakeAddressElementEventReporter,
        val placesClient: FakePlacesClientProxy,
        val primaryButtonAction: RecordingPrimaryButtonAction,
        val resultStateHolder: AddressElementResultStateHolder,
        val testScheduler: TestCoroutineScheduler,
    ) {
        suspend fun selectAutocompletePrediction(address: Address) {
            placesClient.fetchPlaceResult = Result.success(address)

            viewModel.onPredictionSelected("selected-place")

            assertThat(placesClient.fetchPlaceCalls.awaitItem().placeId).isEqualTo("selected-place")
            placesClient.resetSessionCalls.awaitItem()
        }
    }

    private companion object {
        val SELECTED_AUTOCOMPLETE_ADDRESS = Address(
            city = "San Francisco",
            country = "US",
            line1 = "123 Main Street",
            postalCode = "94105",
            state = "CA",
        )
        val SELECTED_AUTOCOMPLETE_FORM_VALUES = mapOf(
            FormFieldId.City to FormFieldEntry(SELECTED_AUTOCOMPLETE_ADDRESS.city, true),
            FormFieldId.Country to FormFieldEntry(SELECTED_AUTOCOMPLETE_ADDRESS.country, true),
            FormFieldId.Line1 to FormFieldEntry(SELECTED_AUTOCOMPLETE_ADDRESS.line1, true),
            FormFieldId.PostalCode to FormFieldEntry(SELECTED_AUTOCOMPLETE_ADDRESS.postalCode, true),
            FormFieldId.State to FormFieldEntry(SELECTED_AUTOCOMPLETE_ADDRESS.state, true),
        )
        val EXPECTED_ADDRESS = AddressDetails(
            name = "Jenny Rosen",
            address = PaymentSheet.Address(
                city = "San Francisco",
                country = "US",
                line1 = "510 Townsend St",
                line2 = "Floor 2",
                postalCode = "94103",
                state = "CA",
            ),
            phoneNumber = "+14155551212",
            isCheckboxSelected = true,
        )
        val COMPLETED_FORM_VALUES = mapOf(
            FormFieldId.Name to FormFieldEntry("Jenny Rosen", true),
            FormFieldId.City to FormFieldEntry("San Francisco", true),
            FormFieldId.Country to FormFieldEntry("US", true),
            FormFieldId.Line1 to FormFieldEntry("510 Townsend St", true),
            FormFieldId.Line2 to FormFieldEntry("Floor 2", true),
            FormFieldId.Phone to FormFieldEntry("+14155551212", true),
            FormFieldId.PostalCode to FormFieldEntry("94103", true),
            FormFieldId.State to FormFieldEntry("CA", true),
        )
    }
}

private class FakeAddressElementPrimaryButtonAction(
    private val action: (AddressDetails) -> AddressElementActivityContract.Result,
) : AddressElementPrimaryButtonAction {
    override suspend fun invoke(
        addressDetails: AddressDetails,
    ): Result<AddressElementActivityContract.Result> {
        return Result.success(action(addressDetails))
    }
}

private class RecordingPrimaryButtonAction(
    var action: suspend (AddressDetails) -> Result<AddressElementActivityContract.Result>,
) : AddressElementPrimaryButtonAction {
    private val _calls = Turbine<AddressDetails>()
    val calls: ReceiveTurbine<AddressDetails> = _calls

    override suspend fun invoke(
        addressDetails: AddressDetails,
    ): Result<AddressElementActivityContract.Result> {
        _calls.add(addressDetails)
        return action(addressDetails)
    }

    fun validate() {
        _calls.ensureAllEventsConsumed()
    }
}
