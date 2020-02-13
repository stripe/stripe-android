package com.stripe.android

import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.stripe.android.model.CustomerFixtures
import com.stripe.android.view.PaymentMethodsActivityStarter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PaymentSessionViewModelTest {
    private val customerSession: CustomerSession = mock()
    private val paymentSessionPrefs: PaymentSessionPrefs = mock()
    private val savedStateHandle: SavedStateHandle = mock()

    private val viewModel: PaymentSessionViewModel by lazy {
        PaymentSessionViewModel(
            ApplicationProvider.getApplicationContext(),
            savedStateHandle,
            PaymentSessionFixtures.PAYMENT_SESSION_DATA,
            customerSession,
            paymentSessionPrefs
        )
    }

    @Test
    fun init_shouldUpdateProductUsage() {
        viewModel.paymentSessionData

        verify(customerSession).resetUsageTokens()
        verify(customerSession).addProductUsageTokenIfValid(
            PaymentSession.TOKEN_PAYMENT_SESSION
        )
    }

    @Test
    fun init_shouldGetButNotSetPaymentSessionDataFromSavedStateHandle() {
        viewModel.paymentSessionData

        verify(savedStateHandle).get<PaymentSessionData>(
            PaymentSessionViewModel.KEY_PAYMENT_SESSION_DATA
        )
        verify(savedStateHandle, never()).set(
            eq(PaymentSessionViewModel.KEY_PAYMENT_SESSION_DATA),
            any<PaymentSessionData>()
        )
    }

    @Test
    fun init_whenSavedStateHasData_shouldUpdatePaymentSessionData() {
        whenever(savedStateHandle.get<PaymentSessionData>(
            PaymentSessionViewModel.KEY_PAYMENT_SESSION_DATA
        )).thenReturn(UPDATED_DATA)

        assertEquals(
            UPDATED_DATA, viewModel.paymentSessionData
        )
    }

    @Test
    fun updateCartTotal_shouldUpdatePaymentSessionData() {
        viewModel.updateCartTotal(5000)
        assertEquals(
            5000,
            viewModel.paymentSessionData.cartTotal
        )
    }

    @Test
    fun getSelectedPaymentMethodId_whenPrefsNotSet_returnsNull() {
        whenever(customerSession.cachedCustomer)
            .thenReturn(FIRST_CUSTOMER)
        assertNull(viewModel.getSelectedPaymentMethodId(null))
    }

    @Test
    fun getSelectedPaymentMethodId_whenHasPrefsSet_returnsExpectedId() {
        val customerId = requireNotNull(FIRST_CUSTOMER.id)
        whenever(paymentSessionPrefs.getSelectedPaymentMethodId(customerId))
            .thenReturn("pm_12345")

        whenever(customerSession.cachedCustomer).thenReturn(FIRST_CUSTOMER)
        CustomerSession.instance = customerSession

        assertEquals("pm_12345", viewModel.getSelectedPaymentMethodId())
    }

    @Test
    fun settingPaymentSessionData_withSameValue_shouldUpdateSavedStateHandleOnce() {
        repeat(3) {
            viewModel.paymentSessionData = UPDATED_DATA
        }
        verify(savedStateHandle)
            .set(PaymentSessionViewModel.KEY_PAYMENT_SESSION_DATA, UPDATED_DATA)
    }

    @Test
    fun settingPaymentSessionData_shouldUpdateLiveData() {
        var liveData: PaymentSessionData? = null
        viewModel.paymentSessionDataLiveData.observeForever { liveData = it }
        viewModel.paymentSessionData = UPDATED_DATA
        assertEquals(UPDATED_DATA, liveData)
    }

    @Test
    fun onPaymentMethodResult_withGooglePay_shouldUpdateLiveData() {
        var liveData: PaymentSessionData? = null
        viewModel.paymentSessionDataLiveData.observeForever { liveData = it }
        viewModel.onPaymentMethodResult(PaymentMethodsActivityStarter.Result(
            useGooglePay = true
        ))
        assertTrue(liveData?.useGooglePay == true)
    }

    private companion object {
        private val FIRST_CUSTOMER = CustomerFixtures.CUSTOMER

        private val UPDATED_DATA = PaymentSessionFixtures.PAYMENT_SESSION_DATA
            .copy(cartTotal = 999999)
    }
}
