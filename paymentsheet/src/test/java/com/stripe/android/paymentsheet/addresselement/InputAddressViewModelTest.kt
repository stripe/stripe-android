package com.stripe.android.paymentsheet.addresselement

import com.google.common.truth.Truth.assertThat
import com.stripe.android.ui.core.injection.FormControllerSubcomponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import javax.inject.Provider
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

@RunWith(RobolectricTestRunner::class)
@ExperimentalCoroutinesApi
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
            whenever(viewOnlyFields(anyOrNull())).thenReturn(this)
            whenever(viewModelScope(anyOrNull())).thenReturn(this)
            whenever(merchantName(anyOrNull())).thenReturn(this)
            whenever(stripeIntent(anyOrNull())).thenReturn(this)
            whenever(build()).thenReturn(formControllerSubcomponent)
        }
    }

    private fun createViewModel(defaultAddress: AddressDetails? = null): InputAddressViewModel {
        defaultAddress?.let {
            whenever(config.defaultValues).thenReturn(defaultAddress)
        }
        whenever(args.config).thenReturn(config)
        return InputAddressViewModel(
            args,
            navigator,
            formControllerProvider
        )
    }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
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
        val expectedAddress = AddressDetails(name = "skyler", company = "stripe")
        val flow = MutableStateFlow<AddressDetails?>(expectedAddress)
        whenever(navigator.getResultFlow<AddressDetails?>(any())).thenReturn(flow)

        val viewModel = createViewModel()
        assertThat(viewModel.collectedAddress.value).isEqualTo(expectedAddress)
    }

    @Test
    fun `default address from merchant is parsed`() = runTest(UnconfinedTestDispatcher()) {
        val expectedAddress = AddressDetails(name = "skyler", company = "stripe")

        val viewModel = createViewModel(expectedAddress)
        assertThat(viewModel.collectedAddress.value).isEqualTo(expectedAddress)
    }
}
