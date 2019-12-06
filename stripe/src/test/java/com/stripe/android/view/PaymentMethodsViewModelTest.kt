package com.stripe.android.view

import com.nhaarman.mockitokotlin2.KArgumentCaptor
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.verify
import com.stripe.android.CustomerSession
import com.stripe.android.exception.APIException
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PaymentMethodsViewModelTest {

    @Mock
    private lateinit var customerSession: CustomerSession

    private val listenerArgumentCaptor: KArgumentCaptor<CustomerSession.PaymentMethodsRetrievalListener> by lazy {
        argumentCaptor<CustomerSession.PaymentMethodsRetrievalListener>()
    }

    @BeforeTest
    fun setup() {
        MockitoAnnotations.initMocks(this)
    }

    @Test
    fun getPaymentMethods_whenSuccess_returnsExpectedPaymentMethods() {
        val paymentMethods = listOf(PaymentMethodFixtures.CARD_PAYMENT_METHOD)

        val viewModel = PaymentMethodsViewModel(customerSession = customerSession)
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
        val viewModel = PaymentMethodsViewModel(customerSession = customerSession)
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
}
