package com.stripe.android.view

import androidx.test.core.app.ApplicationProvider
import com.nhaarman.mockitokotlin2.KArgumentCaptor
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.stripe.android.CustomerSession
import com.stripe.android.exception.APIException
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PaymentMethodsViewModelTest {

    private val customerSession: CustomerSession = mock()
    private val listenerArgumentCaptor: KArgumentCaptor<CustomerSession.PaymentMethodsRetrievalListener> = argumentCaptor()
    private val viewModel: PaymentMethodsViewModel by lazy {
        PaymentMethodsViewModel(
            application = ApplicationProvider.getApplicationContext(),
            customerSession = customerSession
        )
    }

    @Test
    fun getPaymentMethods_whenSuccess_returnsExpectedPaymentMethods() {
        val paymentMethods = listOf(PaymentMethodFixtures.CARD_PAYMENT_METHOD)

        val liveData = viewModel.getPaymentMethods()

        verify(customerSession).getPaymentMethods(
            eq(PaymentMethod.Type.Card),
            listenerArgumentCaptor.capture()
        )

        listenerArgumentCaptor.firstValue.onPaymentMethodsRetrieved(
            listOf(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        )

        val result =
            liveData.value as PaymentMethodsViewModel.Result.Success
        assertEquals(paymentMethods, result.paymentMethods)
    }

    @Test
    fun getPaymentMethods_whenError_returnsExpectedException() {
        val liveData = viewModel.getPaymentMethods()

        verify(customerSession).getPaymentMethods(
            eq(PaymentMethod.Type.Card),
            listenerArgumentCaptor.capture()
        )

        listenerArgumentCaptor.firstValue.onError(
            404, "error!", null
        )

        val result =
            liveData.value as PaymentMethodsViewModel.Result.Error
        assertTrue(result.exception is APIException)
    }

    @Test
    fun onPaymentMethodAdded_shouldUpdateSnackbarData() {
        viewModel.onPaymentMethodAdded(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        assertEquals("Added Visa ending in 4242", viewModel.snackbarData.value)
    }

    @Test
    fun onPaymentMethodRemoved_shouldUpdateSnackbarData() {
        viewModel.onPaymentMethodRemoved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        assertEquals("Removed Visa ending in 4242", viewModel.snackbarData.value)
    }
}
