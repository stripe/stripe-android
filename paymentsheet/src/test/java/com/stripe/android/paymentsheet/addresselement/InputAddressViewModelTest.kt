package com.stripe.android.paymentsheet.addresselement

import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.analytics.AddressLauncherEventReporter
import com.stripe.android.testing.CoroutineTestRule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import javax.inject.Provider

@RunWith(RobolectricTestRunner::class)
class InputAddressViewModelTest {
    private val args = mock<AddressElementActivityContract.Args>()
    private val config = mock<AddressLauncher.Configuration>()
    private val navigator = mock<AddressElementNavigator>()
    private val formControllerSubcomponent = mock<FormControllerSubcomponent>().apply {
        whenever(formController).thenReturn(mock())
    }
    private val formControllerProvider = Provider {
        mock<FormControllerSubcomponent.Builder>().apply {
            whenever(formSpec(anyOrNull())).thenReturn(this)
            whenever(initialValues(anyOrNull())).thenReturn(this)
            whenever(viewModelScope(anyOrNull())).thenReturn(this)
            whenever(merchantName(anyOrNull())).thenReturn(this)
            whenever(stripeIntent(anyOrNull())).thenReturn(this)
            whenever(shippingValues(anyOrNull())).thenReturn(this)
            whenever(build()).thenReturn(formControllerSubcomponent)
        }
    }
    private val eventReporter = mock<AddressLauncherEventReporter>()

    private fun createViewModel(address: AddressDetails? = null): InputAddressViewModel {
        address?.let {
            whenever(config.address).thenReturn(address)
        }
        whenever(args.config).thenReturn(config)
        return InputAddressViewModel(
            args,
            navigator,
            eventReporter,
            formControllerProvider
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
        val expectedAddress = AddressDetails(name = "skyler", address = PaymentSheet.Address(country = "US"))
        val flow = MutableStateFlow<AddressDetails?>(expectedAddress)
        whenever(navigator.getResultFlow<AddressDetails?>(AddressDetails.KEY)).thenReturn(flow)

        val viewModel = createViewModel()
        assertThat(viewModel.collectedAddress.value).isEqualTo(expectedAddress)
    }

    @Test
    fun `takes only fields in new address`() = runTest(UnconfinedTestDispatcher()) {
        val usAddress = AddressDetails(name = "skyler", address = PaymentSheet.Address(country = "US"))
        val flow = MutableStateFlow<AddressDetails?>(usAddress)
        whenever(navigator.getResultFlow<AddressDetails?>(AddressDetails.KEY)).thenReturn(flow)

        val viewModel = createViewModel()
        assertThat(viewModel.collectedAddress.value).isEqualTo(usAddress)

        val expectedAddress = AddressDetails(
            name = "skyler",
            address = PaymentSheet.Address(country = "CAN", line1 = "foobar")
        )
        flow.tryEmit(expectedAddress)
        assertThat(viewModel.collectedAddress.value).isEqualTo(expectedAddress)
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
}
