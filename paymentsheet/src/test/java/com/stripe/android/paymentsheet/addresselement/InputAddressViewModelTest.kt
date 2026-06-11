package com.stripe.android.paymentsheet.addresselement

import android.text.SpannableString
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import app.cash.turbine.turbineScope
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.isInstanceOf
import com.stripe.android.paymentelement.AddressElementSameAsBillingPreview
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.analytics.AddressLauncherEventReporter
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.ui.core.elements.autocomplete.PlacesClientProxy
import com.stripe.android.ui.core.elements.autocomplete.model.AddressComponent
import com.stripe.android.ui.core.elements.autocomplete.model.AutocompletePrediction
import com.stripe.android.ui.core.elements.autocomplete.model.FetchPlaceResponse
import com.stripe.android.ui.core.elements.autocomplete.model.FindAutocompletePredictionsResponse
import com.stripe.android.ui.core.elements.autocomplete.model.Place
import com.stripe.android.uicore.elements.AutocompleteAddressElement
import com.stripe.android.uicore.elements.AutocompleteAddressInteractor
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.SectionElement
import com.stripe.android.uicore.forms.FormFieldEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class InputAddressViewModelTest {
    private val navigator = mock<AddressElementNavigator>()
    private val eventReporter = mock<AddressLauncherEventReporter>()
    private val mockPlacesClient = mock<PlacesClientProxy>()

    private fun createViewModel(
        address: AddressDetails? = null,
        config: AddressLauncher.Configuration = AddressLauncher.Configuration.Builder()
            .address(address)
            .build(),
        placesClient: PlacesClientProxy? = null,
    ): InputAddressViewModel {
        return InputAddressViewModel(
            AddressElementActivityContract.Args(
                publishableKey = "pk_123",
                config = config,
            ),
            navigator,
            eventReporter,
            placesClient = placesClient,
            context = ApplicationProvider.getApplicationContext(),
        )
    }

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    @Test
    fun `autocomplete config disables inline autocomplete by default`() {
        FeatureFlags.inlineAddressAutocomplete.reset()

        val viewModel = createViewModel()

        assertThat(viewModel.autocompleteConfig.isInlineAutocomplete).isFalse()
    }

    @Test
    fun `autocomplete config enables inline autocomplete when feature flag is enabled`() {
        FeatureFlags.inlineAddressAutocomplete.setEnabled(true)

        try {
            val viewModel = createViewModel()

            assertThat(viewModel.autocompleteConfig.isInlineAutocomplete).isTrue()
        } finally {
            FeatureFlags.inlineAddressAutocomplete.reset()
        }
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
                    IdentifierSpec.State to FormFieldEntry(value = null, isComplete = false),
                    IdentifierSpec.Line1 to FormFieldEntry(value = "", isComplete = false),
                    IdentifierSpec.Line2 to FormFieldEntry(value = "", isComplete = true),
                    IdentifierSpec.City to FormFieldEntry(value = "", isComplete = false),
                    IdentifierSpec.PostalCode to FormFieldEntry(value = "", isComplete = false)
                )
            )

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
                    IdentifierSpec.State to FormFieldEntry(value = null, isComplete = false),
                    IdentifierSpec.Line1 to FormFieldEntry(value = "", isComplete = false),
                    IdentifierSpec.Line2 to FormFieldEntry(value = "", isComplete = true),
                    IdentifierSpec.City to FormFieldEntry(value = "", isComplete = false),
                    IdentifierSpec.PostalCode to FormFieldEntry(value = "", isComplete = false)
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
                    IdentifierSpec.State to FormFieldEntry(value = null, isComplete = false),
                    IdentifierSpec.Line1 to FormFieldEntry(value = "", isComplete = false),
                    IdentifierSpec.Line2 to FormFieldEntry(value = "", isComplete = true),
                    IdentifierSpec.City to FormFieldEntry(value = "", isComplete = false),
                    IdentifierSpec.PostalCode to FormFieldEntry(value = "", isComplete = false)
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

    @Test
    fun `onQueryChanged never calls places client when query is too short`() = runTest(UnconfinedTestDispatcher()) {
        val viewModel = createViewModel(placesClient = mockPlacesClient)

        viewModel.onQueryChanged("a", "US")

        advanceTimeBy(AutocompleteViewModel.SEARCH_DEBOUNCE_MS + 1)
        verify(mockPlacesClient, never()).findAutocompletePredictions(any(), any(), any())
    }

    @Test
    fun `onQueryChanged never calls places client when country is not supported`() = runTest(UnconfinedTestDispatcher()) {
        val viewModel = createViewModel(
            config = AddressLauncher.Configuration.Builder()
                .autocompleteCountries(setOf("US", "CA"))
                .build(),
            placesClient = mockPlacesClient,
        )

        viewModel.onQueryChanged("123 Main Street", "GB")

        advanceTimeBy(AutocompleteViewModel.SEARCH_DEBOUNCE_MS + 1)
        verify(mockPlacesClient, never()).findAutocompletePredictions(any(), any(), any())
    }

    @Test
    fun `onQueryChanged allows any country when autocompleteCountries is empty`() = runTest(UnconfinedTestDispatcher()) {
        whenever(mockPlacesClient.findAutocompletePredictions(any(), any(), any())).thenReturn(
            Result.success(FindAutocompletePredictionsResponse(emptyList()))
        )
        val viewModel = createViewModel(
            config = AddressLauncher.Configuration.Builder()
                .autocompleteCountries(emptySet())
                .build(),
            placesClient = mockPlacesClient,
        )

        viewModel.inlinePredictionsState.test {
            assertThat(awaitItem()).isEqualTo(AutocompleteAddressInteractor.InlinePredictionsState.Idle)
            viewModel.onQueryChanged("123 Main Street", "JP")
            advanceTimeBy(AutocompleteViewModel.SEARCH_DEBOUNCE_MS + 1)
            assertThat(awaitItem()).isEqualTo(
                AutocompleteAddressInteractor.InlinePredictionsState.Results(
                    query = "123 Main Street",
                    predictions = emptyList(),
                )
            )
        }
    }

    @Test
    fun `onQueryChanged emits Results on successful prediction fetch`() = runTest(UnconfinedTestDispatcher()) {
        val prediction = AutocompletePrediction(
            primaryText = SpannableString("123 King Street"),
            secondaryText = SpannableString("San Francisco, CA, USA"),
            placeId = "place123",
        )
        whenever(mockPlacesClient.findAutocompletePredictions(any(), any(), any())).thenReturn(
            Result.success(FindAutocompletePredictionsResponse(listOf(prediction)))
        )
        val viewModel = createViewModel(placesClient = mockPlacesClient)

        viewModel.inlinePredictionsState.test {
            assertThat(awaitItem()).isEqualTo(AutocompleteAddressInteractor.InlinePredictionsState.Idle)
            viewModel.onQueryChanged("123 King", "US")
            advanceTimeBy(AutocompleteViewModel.SEARCH_DEBOUNCE_MS + 1)
            assertThat(awaitItem()).isEqualTo(
                AutocompleteAddressInteractor.InlinePredictionsState.Results(
                    query = "123 King",
                    predictions = listOf(
                        AutocompleteAddressInteractor.InlineAddressPrediction(
                            id = "place123",
                            primaryText = "123 King Street",
                            secondaryText = "San Francisco, CA, USA",
                        )
                    ),
                )
            )
        }
    }

    @Test
    fun `onPredictionSelected populates address fields via event and clears predictions state`() = runTest(UnconfinedTestDispatcher()) {
        whenever(mockPlacesClient.fetchPlace(eq("place123"))).thenReturn(
            Result.success(
                FetchPlaceResponse(
                    Place(
                        listOf(
                            AddressComponent(shortName = "123", longName = "123", types = listOf(Place.Type.STREET_NUMBER.value)),
                            AddressComponent(shortName = "King Street", longName = "King Street", types = listOf(Place.Type.ROUTE.value)),
                            AddressComponent(shortName = "South SF", longName = "South San Francisco", types = listOf(Place.Type.LOCALITY.value)),
                            AddressComponent(shortName = "CA", longName = "California", types = listOf(Place.Type.ADMINISTRATIVE_AREA_LEVEL_1.value)),
                            AddressComponent(shortName = "US", longName = "United States", types = listOf(Place.Type.COUNTRY.value)),
                            AddressComponent(shortName = "99999", longName = "99999", types = listOf(Place.Type.POSTAL_CODE.value)),
                        )
                    )
                )
            )
        )
        val viewModel = createViewModel(placesClient = mockPlacesClient)
        val capturedEvents = mutableListOf<AutocompleteAddressInteractor.Event>()
        viewModel.register { capturedEvents.add(it) }

        viewModel.onPredictionSelected("place123")

        assertThat(viewModel.inlinePredictionsState.value)
            .isEqualTo(AutocompleteAddressInteractor.InlinePredictionsState.Idle)
        assertThat(capturedEvents).hasSize(1)
        val event = capturedEvents[0] as AutocompleteAddressInteractor.Event.OnValues
        assertThat(event.values?.get(IdentifierSpec.Line1)).isEqualTo("123 King Street")
        assertThat(event.values?.get(IdentifierSpec.City)).isEqualTo("South San Francisco")
        assertThat(event.values?.get(IdentifierSpec.State)).isEqualTo("CA")
        assertThat(event.values?.get(IdentifierSpec.Country)).isEqualTo("US")
        assertThat(event.values?.get(IdentifierSpec.PostalCode)).isEqualTo("99999")
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
                    IdentifierSpec.State to FormFieldEntry(value = null, isComplete = false),
                    IdentifierSpec.Line1 to FormFieldEntry(value = "", isComplete = false),
                    IdentifierSpec.Line2 to FormFieldEntry(value = "", isComplete = true),
                    IdentifierSpec.City to FormFieldEntry(value = "", isComplete = false),
                    IdentifierSpec.PostalCode to FormFieldEntry(value = "", isComplete = false)
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

    private fun createShowState(isChecked: Boolean) =
        InputAddressViewModel.ShippingSameAsBillingState.Show(isChecked)
}
