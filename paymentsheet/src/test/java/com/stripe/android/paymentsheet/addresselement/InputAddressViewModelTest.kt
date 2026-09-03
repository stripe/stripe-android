@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.paymentsheet.addresselement

import app.cash.turbine.test
import app.cash.turbine.turbineScope
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkout.CheckoutController
import com.stripe.android.checkout.CheckoutSessionTaxRegionUpdater
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.isInstanceOf
import com.stripe.android.model.Address
import com.stripe.android.paymentelement.AddressElementSameAsBillingPreview
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.addresselement.analytics.AddressLauncherEventReporter
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.paymentsheet.utils.ViewModelStoreTestRule
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.ui.core.elements.autocomplete.model.FindAutocompletePredictionsResponse
import com.stripe.android.uicore.elements.AutocompleteAddressElement
import com.stripe.android.uicore.elements.AutocompleteAddressInteractor
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.SectionElement
import com.stripe.android.uicore.forms.FormFieldEntry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class InputAddressViewModelTest {
    private val navigator = mock<AddressElementNavigator>()
    private val eventReporter = mock<AddressLauncherEventReporter>()

    private fun createViewModel(
        address: AddressDetails? = null,
        config: AddressLauncher.Configuration = AddressLauncher.Configuration.Builder()
            .address(address)
            .build(),
        launchMode: AddressElementActivityContract.LaunchMode =
            AddressElementActivityContract.LaunchMode.Standalone,
        processingState: AddressElementActivityProcessingState = AddressElementActivityProcessingState(),
        checkoutSessionTaxRegionUpdater: CheckoutSessionTaxRegionUpdater = mock(),
    ): InputAddressViewModel {
        return InputAddressViewModel(
            AddressElementActivityContract.Args(
                publishableKey = "pk_123",
                config = config,
                launchMode = launchMode,
            ),
            navigator,
            processingState,
            eventReporter,
            checkoutSessionTaxRegionUpdater,
            placesClient = null,
        ).also { viewModelStoreRule.track(it) }
    }

    @get:Rule
    val viewModelStoreRule = ViewModelStoreTestRule()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    @Test
    fun `onScreenShown fires onShow with initial country`() {
        val viewModel = createViewModel(
            address = AddressDetails(address = PaymentSheet.Address(country = "US"))
        )
        viewModel.onScreenShown()
        verify(eventReporter).onShow(eq("US"))
    }

    @Test
    fun `onScreenShown fires onShow with empty string when no initial country`() {
        val viewModel = createViewModel()
        viewModel.onScreenShown()
        verify(eventReporter).onShow(eq(""))
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
    fun `viewModel emits onComplete event`() = runTest(UnconfinedTestDispatcher()) {
        val viewModel = createViewModel(
            AddressDetails(
                address = PaymentSheet.Address(
                    line1 = "99 Broadway St",
                    city = "Seattle",
                    country = "US"
                )
            )
        )
        viewModel.dismissWithAddress(
            AddressDetails(
                address = PaymentSheet.Address(
                    line1 = "99 Broadway St",
                    city = "Seattle",
                    country = "US"
                )
            )
        )
        verify(eventReporter).onCompleted(
            country = eq("US"),
            autocompleteResultSelected = eq(true),
            editDistance = eq(0)
        )
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
                    IdentifierSpec.Name to FormFieldEntry(value = "John Doe", isComplete = true),
                    IdentifierSpec.Country to FormFieldEntry(value = "US", isComplete = true),
                    IdentifierSpec.State to FormFieldEntry(value = "CA", isComplete = true),
                    IdentifierSpec.Line1 to FormFieldEntry(value = "123 Apple Street", isComplete = true),
                    IdentifierSpec.Line2 to FormFieldEntry(value = "", isComplete = true),
                    IdentifierSpec.City to FormFieldEntry(value = "San Francisco", isComplete = true),
                    IdentifierSpec.PostalCode to FormFieldEntry(value = "99999", isComplete = true)
                )
            )

            viewModel.clickBillingSameAsShipping(newValue = false)

            // Should be checked and filled with default address
            assertThat(shippingSameAsBillingStateTurbine.awaitItem()).isEqualTo(createShowState(isChecked = false))
            assertThat(formValuesTurbine.awaitItem()).containsExactlyEntriesIn(
                mapOf(
                    IdentifierSpec.Name to FormFieldEntry(value = "", isComplete = false),
                    IdentifierSpec.Country to FormFieldEntry(value = "US", isComplete = true),
                    IdentifierSpec.Generic("address") to FormFieldEntry(value = "", isComplete = false),
                )
            )

            viewModel.onEnterManuallyFromInline()
            assertThat(formValuesTurbine.awaitItem().keys).contains(IdentifierSpec.Line1)

            viewModel.setRawValues(
                mapOf(
                    IdentifierSpec.Name to "Jane Doe",
                    IdentifierSpec.Line1 to "123 Pear Street",
                    IdentifierSpec.PostalCode to "88888",
                )
            )

            // Should be unchecked and use input
            shippingSameAsBillingStateTurbine.expectNoEvents()
            assertThat(formValuesTurbine.expectMostRecentItem()).containsExactlyEntriesIn(
                mapOf(
                    IdentifierSpec.Name to FormFieldEntry(value = "Jane Doe", isComplete = true),
                    IdentifierSpec.Country to FormFieldEntry(value = "US", isComplete = true),
                    IdentifierSpec.State to FormFieldEntry(value = null, isComplete = false),
                    IdentifierSpec.Line1 to FormFieldEntry(value = "123 Pear Street", isComplete = true),
                    IdentifierSpec.Line2 to FormFieldEntry(value = "", isComplete = true),
                    IdentifierSpec.City to FormFieldEntry(value = "", isComplete = false),
                    IdentifierSpec.PostalCode to FormFieldEntry(value = "88888", isComplete = true)
                )
            )

            viewModel.clickBillingSameAsShipping(newValue = true)

            // Should be checked and filled with default address
            assertThat(shippingSameAsBillingStateTurbine.awaitItem()).isEqualTo(createShowState(isChecked = true))
            assertThat(formValuesTurbine.awaitItem()).containsExactlyEntriesIn(
                mapOf(
                    IdentifierSpec.Name to FormFieldEntry(value = "John Doe", isComplete = true),
                    IdentifierSpec.Country to FormFieldEntry(value = "US", isComplete = true),
                    IdentifierSpec.State to FormFieldEntry(value = "CA", isComplete = true),
                    IdentifierSpec.Line1 to FormFieldEntry(value = "123 Apple Street", isComplete = true),
                    IdentifierSpec.Line2 to FormFieldEntry(value = "", isComplete = true),
                    IdentifierSpec.City to FormFieldEntry(value = "San Francisco", isComplete = true),
                    IdentifierSpec.PostalCode to FormFieldEntry(value = "99999", isComplete = true)
                )
            )

            viewModel.clickBillingSameAsShipping(newValue = false)

            // Should be unchecked and filled with previous user input
            assertThat(shippingSameAsBillingStateTurbine.awaitItem()).isEqualTo(createShowState(isChecked = false))
            assertThat(formValuesTurbine.awaitItem()).containsExactlyEntriesIn(
                mapOf(
                    IdentifierSpec.Name to FormFieldEntry(value = "Jane Doe", isComplete = true),
                    IdentifierSpec.Country to FormFieldEntry(value = "US", isComplete = true),
                    IdentifierSpec.State to FormFieldEntry(value = null, isComplete = false),
                    IdentifierSpec.Line1 to FormFieldEntry(value = "123 Pear Street", isComplete = true),
                    IdentifierSpec.Line2 to FormFieldEntry(value = "", isComplete = true),
                    IdentifierSpec.City to FormFieldEntry(value = "", isComplete = false),
                    IdentifierSpec.PostalCode to FormFieldEntry(value = "88888", isComplete = true)
                )
            )

            viewModel.clickBillingSameAsShipping(newValue = true)

            // Should be checked and filled with provided billing details
            assertThat(shippingSameAsBillingStateTurbine.awaitItem()).isEqualTo(createShowState(isChecked = true))
            assertThat(formValuesTurbine.awaitItem()).containsExactlyEntriesIn(
                mapOf(
                    IdentifierSpec.Name to FormFieldEntry(value = "John Doe", isComplete = true),
                    IdentifierSpec.Country to FormFieldEntry(value = "US", isComplete = true),
                    IdentifierSpec.State to FormFieldEntry(value = "CA", isComplete = true),
                    IdentifierSpec.Line1 to FormFieldEntry(value = "123 Apple Street", isComplete = true),
                    IdentifierSpec.Line2 to FormFieldEntry(value = "", isComplete = true),
                    IdentifierSpec.City to FormFieldEntry(value = "San Francisco", isComplete = true),
                    IdentifierSpec.PostalCode to FormFieldEntry(value = "99999", isComplete = true)
                )
            )

            viewModel.setRawValues(
                mapOf(
                    IdentifierSpec.Name to "Jane Doe",
                    IdentifierSpec.Line1 to "123 Coffee Street",
                    IdentifierSpec.PostalCode to "77777",
                )
            )

            // Should be unchecked and filled with new user input
            assertThat(shippingSameAsBillingStateTurbine.awaitItem()).isEqualTo(createShowState(isChecked = false))
            assertThat(formValuesTurbine.expectMostRecentItem()).containsExactlyEntriesIn(
                mapOf(
                    IdentifierSpec.Name to FormFieldEntry(value = "Jane Doe", isComplete = true),
                    IdentifierSpec.Country to FormFieldEntry(value = "US", isComplete = true),
                    IdentifierSpec.State to FormFieldEntry(value = "CA", isComplete = true),
                    IdentifierSpec.Line1 to FormFieldEntry(value = "123 Coffee Street", isComplete = true),
                    IdentifierSpec.Line2 to FormFieldEntry(value = "", isComplete = true),
                    IdentifierSpec.City to FormFieldEntry(value = "San Francisco", isComplete = true),
                    IdentifierSpec.PostalCode to FormFieldEntry(value = "77777", isComplete = true)
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
                    IdentifierSpec.Name to FormFieldEntry(value = "Jane Doe", isComplete = true),
                    IdentifierSpec.Country to FormFieldEntry(value = "US", isComplete = true),
                    IdentifierSpec.State to FormFieldEntry(value = "CA", isComplete = true),
                    IdentifierSpec.Line1 to FormFieldEntry(value = "123 Coffee Street", isComplete = true),
                    IdentifierSpec.Line2 to FormFieldEntry(value = "", isComplete = true),
                    IdentifierSpec.City to FormFieldEntry(value = "San Jose", isComplete = true),
                    IdentifierSpec.PostalCode to FormFieldEntry(value = "77777", isComplete = true)
                )
            )

            viewModel.clickBillingSameAsShipping(newValue = true)

            // Should be checked and filled with billing address
            assertThat(shippingSameAsBillingStateTurbine.awaitItem()).isEqualTo(createShowState(isChecked = true))
            assertThat(formValuesTurbine.awaitItem()).containsExactlyEntriesIn(
                mapOf(
                    IdentifierSpec.Name to FormFieldEntry(value = "John Doe", isComplete = true),
                    IdentifierSpec.Country to FormFieldEntry(value = "US", isComplete = true),
                    IdentifierSpec.State to FormFieldEntry(value = "CA", isComplete = true),
                    IdentifierSpec.Line1 to FormFieldEntry(value = "123 Apple Street", isComplete = true),
                    IdentifierSpec.Line2 to FormFieldEntry(value = "", isComplete = true),
                    IdentifierSpec.City to FormFieldEntry(value = "San Francisco", isComplete = true),
                    IdentifierSpec.PostalCode to FormFieldEntry(value = "99999", isComplete = true)
                )
            )

            viewModel.clickBillingSameAsShipping(newValue = false)

            // Should re-use shipping address since no previous input
            assertThat(shippingSameAsBillingStateTurbine.awaitItem()).isEqualTo(createShowState(isChecked = false))
            assertThat(formValuesTurbine.expectMostRecentItem()).containsExactlyEntriesIn(
                mapOf(
                    IdentifierSpec.Name to FormFieldEntry(value = "Jane Doe", isComplete = true),
                    IdentifierSpec.Country to FormFieldEntry(value = "US", isComplete = true),
                    IdentifierSpec.State to FormFieldEntry(value = "CA", isComplete = true),
                    IdentifierSpec.Line1 to FormFieldEntry(value = "123 Coffee Street", isComplete = true),
                    IdentifierSpec.Line2 to FormFieldEntry(value = "", isComplete = true),
                    IdentifierSpec.City to FormFieldEntry(value = "San Jose", isComplete = true),
                    IdentifierSpec.PostalCode to FormFieldEntry(value = "77777", isComplete = true)
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
                    IdentifierSpec.Name to FormFieldEntry(value = "John Doe", isComplete = true),
                    IdentifierSpec.Country to FormFieldEntry(value = "US", isComplete = true),
                    IdentifierSpec.State to FormFieldEntry(value = "CA", isComplete = true),
                    IdentifierSpec.Line1 to FormFieldEntry(value = "123 Apple Street", isComplete = true),
                    IdentifierSpec.Line2 to FormFieldEntry(value = "", isComplete = true),
                    IdentifierSpec.City to FormFieldEntry(value = "San Francisco", isComplete = true),
                    IdentifierSpec.PostalCode to FormFieldEntry(value = "99999", isComplete = true)
                )
            )

            viewModel.clickBillingSameAsShipping(newValue = false)

            // Should be unchecked and empty
            assertThat(shippingSameAsBillingStateTurbine.awaitItem()).isEqualTo(createShowState(isChecked = false))
            assertThat(formValuesTurbine.awaitItem()).containsExactlyEntriesIn(
                mapOf(
                    IdentifierSpec.Name to FormFieldEntry(value = "", isComplete = false),
                    IdentifierSpec.Country to FormFieldEntry(value = "US", isComplete = true),
                    IdentifierSpec.Generic("address") to FormFieldEntry(value = "", isComplete = false),
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
                    IdentifierSpec.Name to FormFieldEntry(value = "John Doe", isComplete = true),
                    IdentifierSpec.Country to FormFieldEntry(value = "US", isComplete = true),
                    IdentifierSpec.State to FormFieldEntry(value = "CA", isComplete = true),
                    IdentifierSpec.Line1 to FormFieldEntry(value = "123 Apple Street", isComplete = true),
                    IdentifierSpec.Line2 to FormFieldEntry(value = "", isComplete = true),
                    IdentifierSpec.City to FormFieldEntry(value = "San Francisco", isComplete = true),
                    IdentifierSpec.PostalCode to FormFieldEntry(value = "99999", isComplete = true)
                )
            )

            viewModel.clickBillingSameAsShipping(newValue = false)

            // Should be unchecked and empty
            assertThat(shippingSameAsBillingStateTurbine.awaitItem()).isEqualTo(createShowState(isChecked = false))
            assertThat(formValuesTurbine.awaitItem()).containsExactlyEntriesIn(
                mapOf(
                    IdentifierSpec.Name to FormFieldEntry(value = "", isComplete = false),
                    IdentifierSpec.Country to FormFieldEntry(value = "US", isComplete = true),
                    IdentifierSpec.Generic("address") to FormFieldEntry(value = "", isComplete = false),
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
                    IdentifierSpec.Name to FormFieldEntry(value = "", isComplete = false),
                    IdentifierSpec.Country to FormFieldEntry(value = "CA", isComplete = true),
                    IdentifierSpec.Generic("address") to FormFieldEntry(value = "", isComplete = false),
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
        values: Map<IdentifierSpec, String?>
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
    fun `clickPrimaryButton with null triggers validation errors without dismissing`() = runTest {
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
        verify(navigator, never()).dismiss(any(), anyOrNull())
    }

    @Test
    fun `checkout save disables form and blocks duplicate submission while update is suspended`() = runTest {
        val updateResult = CompletableDeferred<Result<CheckoutSessionResponse>>()
        val taxRegionUpdater = mock<CheckoutSessionTaxRegionUpdater>()
        whenever(
            taxRegionUpdater.updateServerStateIfNeeded(any(), any(), any())
        ).doSuspendableAnswer {
            updateResult.await()
        }
        val processingState = AddressElementActivityProcessingState()
        val viewModel = createViewModel(
            launchMode = AddressElementActivityContract.LaunchMode.CheckoutShipping(CHECKOUT_SESSION_RESPONSE),
            processingState = processingState,
            checkoutSessionTaxRegionUpdater = taxRegionUpdater,
        )

        viewModel.clickPrimaryButton(COMPLETED_ADDRESS, checkboxChecked = false)
        viewModel.clickPrimaryButton(COMPLETED_ADDRESS, checkboxChecked = false)

        verify(taxRegionUpdater).updateServerStateIfNeeded(
            eq(CHECKOUT_SESSION_RESPONSE),
            eq(CheckoutSessionResponse.TaxAddressSource.SHIPPING),
            any(),
        )
        assertThat(viewModel.formEnabled.value).isFalse()
        assertThat(viewModel.isProcessing.value).isTrue()
        assertThat(processingState.isProcessing.value).isTrue()
        verify(navigator, never()).dismiss(any(), anyOrNull())

        updateResult.complete(Result.success(UPDATED_CHECKOUT_SESSION_RESPONSE))
        runCurrent()

        assertThat(viewModel.isProcessing.value).isFalse()
        assertThat(processingState.isProcessing.value).isFalse()
        verify(navigator).dismiss(
            AddressLauncherResult.Succeeded(EXPECTED_ADDRESS),
            UPDATED_CHECKOUT_SESSION_RESPONSE,
        )
    }

    @Test
    fun `failed checkout save retains values shows error and permits successful retry`() = runTest {
        val taxRegionUpdater = mock<CheckoutSessionTaxRegionUpdater>()
        whenever(taxRegionUpdater.updateServerStateIfNeeded(any(), any(), any()))
            .thenReturn(
                Result.failure(IllegalStateException("Failed")),
                Result.success(UPDATED_CHECKOUT_SESSION_RESPONSE),
            )
        val processingState = AddressElementActivityProcessingState()
        val viewModel = createViewModel(
            address = EXPECTED_ADDRESS,
            launchMode = AddressElementActivityContract.LaunchMode.CheckoutShipping(CHECKOUT_SESSION_RESPONSE),
            processingState = processingState,
            checkoutSessionTaxRegionUpdater = taxRegionUpdater,
        )
        val originalFormValues = viewModel.addressFormController.getCurrentFormValues()

        viewModel.clickPrimaryButton(COMPLETED_ADDRESS, checkboxChecked = false)
        runCurrent()

        assertThat(viewModel.formEnabled.value).isTrue()
        assertThat(viewModel.isProcessing.value).isFalse()
        assertThat(processingState.isProcessing.value).isFalse()
        assertThat(viewModel.saveError.value)
            .isEqualTo(R.string.stripe_something_went_wrong.resolvableString)
        assertThat(viewModel.addressFormController.getCurrentFormValues())
            .isEqualTo(originalFormValues)
        verify(navigator, never()).dismiss(any(), anyOrNull())

        viewModel.clickPrimaryButton(COMPLETED_ADDRESS, checkboxChecked = false)
        runCurrent()

        assertThat(viewModel.saveError.value).isNull()
        verify(navigator).dismiss(
            AddressLauncherResult.Succeeded(EXPECTED_ADDRESS),
            UPDATED_CHECKOUT_SESSION_RESPONSE,
        )
    }

    @Test
    fun `thrown tax region update shows retryable error`() = runTest {
        val taxRegionUpdater = mock<CheckoutSessionTaxRegionUpdater>()
        whenever(taxRegionUpdater.updateServerStateIfNeeded(any(), any(), any()))
            .thenThrow(IllegalStateException("Failed"))
        val viewModel = createViewModel(
            launchMode = AddressElementActivityContract.LaunchMode.CheckoutShipping(CHECKOUT_SESSION_RESPONSE),
            checkoutSessionTaxRegionUpdater = taxRegionUpdater,
        )

        viewModel.clickPrimaryButton(COMPLETED_ADDRESS, checkboxChecked = false)
        runCurrent()

        assertThat(viewModel.formEnabled.value).isTrue()
        assertThat(viewModel.isProcessing.value).isFalse()
        assertThat(viewModel.saveError.value)
            .isEqualTo(R.string.stripe_something_went_wrong.resolvableString)
        verify(navigator, never()).dismiss(any(), anyOrNull())
    }

    @Test
    fun `missing shipping country shows retryable error`() = runTest {
        val taxRegionUpdater = mock<CheckoutSessionTaxRegionUpdater>()
        val viewModel = createViewModel(
            launchMode = AddressElementActivityContract.LaunchMode.CheckoutShipping(CHECKOUT_SESSION_RESPONSE),
            checkoutSessionTaxRegionUpdater = taxRegionUpdater,
        )

        viewModel.clickPrimaryButton(
            completedFormValues = COMPLETED_ADDRESS - IdentifierSpec.Country,
            checkboxChecked = false,
        )
        runCurrent()

        assertThat(viewModel.formEnabled.value).isTrue()
        assertThat(viewModel.isProcessing.value).isFalse()
        assertThat(viewModel.saveError.value)
            .isEqualTo(R.string.stripe_something_went_wrong.resolvableString)
        verify(taxRegionUpdater, never()).updateServerStateIfNeeded(any(), any(), any())
        verify(navigator, never()).dismiss(any(), anyOrNull())
    }

    @Test
    fun `checkout save sends complete name and address to tax region updater`() = runTest {
        val taxRegionUpdater = mock<CheckoutSessionTaxRegionUpdater>()
        whenever(taxRegionUpdater.updateServerStateIfNeeded(any(), any(), any()))
            .thenReturn(Result.success(UPDATED_CHECKOUT_SESSION_RESPONSE))
        val viewModel = createViewModel(
            launchMode = AddressElementActivityContract.LaunchMode.CheckoutShipping(CHECKOUT_SESSION_RESPONSE),
            checkoutSessionTaxRegionUpdater = taxRegionUpdater,
        )

        viewModel.clickPrimaryButton(COMPLETED_ADDRESS, checkboxChecked = false)
        runCurrent()

        val address = argumentCaptor<CheckoutController.Address.State>()
        verify(taxRegionUpdater).updateServerStateIfNeeded(
            checkoutSessionResponse = eq(CHECKOUT_SESSION_RESPONSE),
            addressSource = eq(CheckoutSessionResponse.TaxAddressSource.SHIPPING),
            address = address.capture(),
        )
        assertThat(address.firstValue).isEqualTo(
            CheckoutController.Address.State(
                city = "San Francisco",
                country = "US",
                line1 = "510 Townsend St",
                line2 = "Floor 2",
                postalCode = "94103",
                state = "CA",
            )
        )
    }

    @Test
    fun `checkout save cancellation finishes processing without showing retryable error`() = runTest {
        val taxRegionUpdater = mock<CheckoutSessionTaxRegionUpdater>()
        whenever(taxRegionUpdater.updateServerStateIfNeeded(any(), any(), any()))
            .thenThrow(CancellationException())
        val processingState = AddressElementActivityProcessingState()
        val viewModel = createViewModel(
            launchMode = AddressElementActivityContract.LaunchMode.CheckoutShipping(CHECKOUT_SESSION_RESPONSE),
            processingState = processingState,
            checkoutSessionTaxRegionUpdater = taxRegionUpdater,
        )

        viewModel.clickPrimaryButton(COMPLETED_ADDRESS, checkboxChecked = false)
        runCurrent()

        assertThat(processingState.isProcessing.value).isFalse()
        assertThat(viewModel.saveError.value).isNull()
        verify(navigator, never()).dismiss(any(), anyOrNull())
    }

    @Test
    fun `standalone save dismisses without calling checkout controller`() = runTest {
        val taxRegionUpdater = mock<CheckoutSessionTaxRegionUpdater>()
        val viewModel = createViewModel(
            launchMode = AddressElementActivityContract.LaunchMode.Standalone,
            checkoutSessionTaxRegionUpdater = taxRegionUpdater,
        )

        viewModel.clickPrimaryButton(COMPLETED_ADDRESS, checkboxChecked = false)

        verifyNoInteractions(taxRegionUpdater)
        assertThat(viewModel.isProcessing.value).isFalse()
        assertThat(viewModel.saveError.value).isNull()
        verify(navigator).dismiss(AddressLauncherResult.Succeeded(EXPECTED_ADDRESS))
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
            AddressElementActivityContract.Args(
                publishableKey = "pk_123",
                config = AddressLauncher.Configuration.Builder()
                    .googlePlacesApiKey(googlePlacesApiKey)
                    .autocompleteCountries(autocompleteCountries)
                    .build(),
                launchMode = AddressElementActivityContract.LaunchMode.Standalone,
            ),
            navigator,
            AddressElementActivityProcessingState(),
            eventReporter,
            checkoutSessionTaxRegionUpdater = mock(),
            placesClient = FakePlacesClientProxy(
                findPredictionsResult = Result.success(FindAutocompletePredictionsResponse(emptyList())),
                fetchPlaceResult = Result.success(Address()),
            ),
        ).also { viewModelStoreRule.track(it) }
    }

    private companion object {
        val CHECKOUT_SESSION_RESPONSE = CheckoutSessionResponseFactory.create(
            automaticTaxEnabled = true,
            taxAddressSource = CheckoutSessionResponse.TaxAddressSource.SHIPPING,
        )
        val UPDATED_CHECKOUT_SESSION_RESPONSE = CHECKOUT_SESSION_RESPONSE.copy(
            totalSummary = CheckoutSessionResponse.TotalSummaryResponse(
                subtotal = 1000L,
                totalDueToday = 1100L,
                totalAmountDue = 1100L,
                discountAmounts = emptyList(),
                taxAmounts = emptyList(),
                shippingRate = null,
                appliedBalance = null,
            ),
        )

        val COMPLETED_ADDRESS = mapOf(
            IdentifierSpec.Name to FormFieldEntry("Jenny Rosen", isComplete = true),
            IdentifierSpec.City to FormFieldEntry("San Francisco", isComplete = true),
            IdentifierSpec.Country to FormFieldEntry("US", isComplete = true),
            IdentifierSpec.Line1 to FormFieldEntry("510 Townsend St", isComplete = true),
            IdentifierSpec.Line2 to FormFieldEntry("Floor 2", isComplete = true),
            IdentifierSpec.PostalCode to FormFieldEntry("94103", isComplete = true),
            IdentifierSpec.State to FormFieldEntry("CA", isComplete = true),
            IdentifierSpec.Phone to FormFieldEntry("555-0100", isComplete = true),
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
            phoneNumber = "555-0100",
            isCheckboxSelected = false,
        )
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
                    values = mapOf(IdentifierSpec.Country to "US")
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
                    IdentifierSpec.Line1 to "123 Main St",
                    IdentifierSpec.Country to "US",
                )
            )
        )
    }

    private fun createShowState(isChecked: Boolean) =
        InputAddressViewModel.ShippingSameAsBillingState.Show(isChecked)
}
