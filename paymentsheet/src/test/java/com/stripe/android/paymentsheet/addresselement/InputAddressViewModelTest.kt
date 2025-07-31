package com.stripe.android.paymentsheet.addresselement

import app.cash.turbine.test
import app.cash.turbine.turbineScope
import com.google.common.truth.Truth.assertThat
import com.stripe.android.elements.Address
import com.stripe.android.elements.AddressDetails
import com.stripe.android.elements.AddressLauncher
import com.stripe.android.elements.BillingDetails
import com.stripe.android.isInstanceOf
import com.stripe.android.paymentelement.AddressElementSameAsBillingPreview
import com.stripe.android.paymentsheet.addresselement.analytics.AddressLauncherEventReporter
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.uicore.elements.AutocompleteAddressElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.SectionElement
import com.stripe.android.uicore.forms.FormFieldEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
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
            .build()
    ): InputAddressViewModel {
        return InputAddressViewModel(
            AddressElementActivityContract.Args(
                publishableKey = "pk_123",
                config = config,
            ),
            navigator,
            eventReporter,
        )
    }

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    @Test
    fun `no autocomplete address passed has an empty address to start`() = runTest(UnconfinedTestDispatcher()) {
        val flow = MutableStateFlow<AddressDetails?>(null)
        whenever(navigator.getResultFlow<AddressDetails?>(any())).thenReturn(flow)

        val viewModel = createViewModel()
        assertThat(viewModel.collectedAddress.value).isEqualTo(AddressDetails())
    }

    @Test
    fun `autocomplete address passed is collected to start`() = runTest(UnconfinedTestDispatcher()) {
        val expectedAddress = AddressDetails(name = "skyler", address = Address(country = "US"))
        val flow = MutableStateFlow<AddressElementNavigator.AutocompleteEvent?>(
            AddressElementNavigator.AutocompleteEvent.OnBack(expectedAddress)
        )
        whenever(
            navigator.getResultFlow<AddressElementNavigator.AutocompleteEvent?>(
                AddressElementNavigator.AutocompleteEvent.KEY
            )
        ).thenReturn(flow)

        val viewModel = createViewModel()
        assertThat(viewModel.collectedAddress.value).isEqualTo(expectedAddress)
    }

    @Test
    fun `takes only fields in new address`() = runTest(UnconfinedTestDispatcher()) {
        val usAddress = AddressDetails(name = "skyler", address = Address(country = "US"))
        val flow = MutableStateFlow<AddressElementNavigator.AutocompleteEvent?>(
            AddressElementNavigator.AutocompleteEvent.OnBack(usAddress)
        )
        whenever(
            navigator.getResultFlow<AddressElementNavigator.AutocompleteEvent?>(
                AddressElementNavigator.AutocompleteEvent.KEY
            )
        ).thenReturn(flow)

        val viewModel = createViewModel()
        assertThat(viewModel.collectedAddress.value).isEqualTo(usAddress)

        val expectedAddress = AddressDetails(
            name = "skyler",
            address = Address(country = "CAN", line1 = "foobar")
        )
        flow.tryEmit(AddressElementNavigator.AutocompleteEvent.OnBack(expectedAddress))
        assertThat(viewModel.collectedAddress.value).isEqualTo(expectedAddress)
    }

    @Test
    fun `default address from merchant is parsed`() = runTest(UnconfinedTestDispatcher()) {
        val expectedAddress = AddressDetails(name = "skyler", address = Address(country = "US"))

        val viewModel = createViewModel(expectedAddress)
        assertThat(viewModel.collectedAddress.value).isEqualTo(expectedAddress)
    }

    @Test
    fun `viewModel emits onComplete event`() = runTest(UnconfinedTestDispatcher()) {
        val viewModel = createViewModel(
            AddressDetails(
                address = Address(
                    line1 = "99 Broadway St",
                    city = "Seattle",
                    country = "US"
                )
            )
        )
        viewModel.dismissWithAddress(
            AddressDetails(
                address = Address(
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
            billingAddress = BillingDetails(
                name = "John Doe",
                address = Address(
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
            billingAddress = BillingDetails(
                name = "John Doe",
                address = Address(
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
            billingAddress = BillingDetails(
                name = "John Doe",
                address = Address(
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
            billingAddress = BillingDetails(
                name = "John Doe",
                address = Address(
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
            billingAddress = BillingDetails(
                name = "John Doe",
                address = Address(
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
                address = Address(
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
                    BillingDetails(
                        name = "John Doe",
                        address = Address(
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
                        address = Address(
                            line1 = "123 Coffee Street",
                            city = "San Jose",
                            country = "US",
                            state = "CA",
                            postalCode = "77777"
                        ),
                    )
                )
                .billingAddress(
                    BillingDetails(
                        name = "John Doe",
                        address = Address(
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
    private fun billingSameAsShippingInitialValueTest(
        address: AddressDetails?,
        allowedCountries: Set<String>,
        billingAddress: BillingDetails?,
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
