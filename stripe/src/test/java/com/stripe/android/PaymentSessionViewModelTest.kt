package com.stripe.android

import androidx.test.core.app.ApplicationProvider
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.stripe.android.model.Customer
import com.stripe.android.model.CustomerFixtures
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PaymentSessionViewModelTest {
    private val customerSession: CustomerSession = mock()
    private val paymentSessionPrefs: PaymentSessionPrefs = mock()

    private val viewModel: PaymentSessionViewModel by lazy {
        PaymentSessionViewModel(
            ApplicationProvider.getApplicationContext(),
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
    fun updateCartTotal_shouldUpdatePaymentSessionData() {
        viewModel.updateCartTotal(5000)
        assertEquals(
            5000,
            viewModel.paymentSessionData.cartTotal
        )
    }

    @Test
    fun getSelectedPaymentMethodId_whenPrefsNotSet_returnsNull() {
        Mockito.`when`<Customer>(customerSession.cachedCustomer)
            .thenReturn(FIRST_CUSTOMER)
        assertNull(viewModel.getSelectedPaymentMethodId(null))
    }

    @Test
    fun getSelectedPaymentMethodId_whenHasPrefsSet_returnsExpectedId() {
        val customerId = requireNotNull(FIRST_CUSTOMER.id)
        Mockito.`when`<String>(paymentSessionPrefs.getSelectedPaymentMethodId(customerId))
            .thenReturn("pm_12345")

        Mockito.`when`<Customer>(customerSession.cachedCustomer).thenReturn(FIRST_CUSTOMER)
        CustomerSession.instance = customerSession

        assertEquals("pm_12345", viewModel.getSelectedPaymentMethodId())
    }

    private companion object {
        private val FIRST_CUSTOMER = CustomerFixtures.CUSTOMER
    }
}
