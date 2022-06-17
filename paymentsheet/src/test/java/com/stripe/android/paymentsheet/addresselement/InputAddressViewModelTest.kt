package com.stripe.android.paymentsheet.addresselement

import com.google.common.truth.Truth.assertThat
import com.stripe.android.ui.core.injection.FormControllerSubcomponent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import javax.inject.Provider

@RunWith(RobolectricTestRunner::class)
@ExperimentalCoroutinesApi
class InputAddressViewModelTest {
    private val args = mock<AddressElementActivityContract.Args>()
    private val navigator = mock<AddressElementNavigator>()
    private val formcontrollerProvider = mock<Provider<FormControllerSubcomponent.Builder>>()


    private fun createViewModel() =
        InputAddressViewModel(
            args,
            navigator,
            formcontrollerProvider
        )

    @Test
    fun `no autocomplete address passed has an empty address to start`() = runTest {
        val flow = MutableStateFlow<ShippingAddress?>(null)
        whenever(navigator.getResultFlow<ShippingAddress?>(any())).thenReturn(flow)

        val viewModel = createViewModel()
        assertThat(viewModel.collectedAddress.value).isEqualTo(null)
    }

    @Test
    fun `autocomplete address passed is collected to start`() = runTest {
        val expectedAddress = ShippingAddress(name = "skyler", company = "stripe")
        val flow = MutableStateFlow<ShippingAddress?>(expectedAddress)
        whenever(navigator.getResultFlow<ShippingAddress?>(any())).thenReturn(flow)

        val viewModel = createViewModel()
        assertThat(viewModel.collectedAddress.value).isEqualTo(expectedAddress)
    }
}