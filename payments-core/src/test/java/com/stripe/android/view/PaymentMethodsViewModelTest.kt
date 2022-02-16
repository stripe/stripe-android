package com.stripe.android.view

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.CustomerSession
import com.stripe.android.PaymentSession
import com.stripe.android.core.exception.APIException
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import org.junit.runner.RunWith
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class PaymentMethodsViewModelTest {

    private val customerSession: CustomerSession = mock()
    private val listenerArgumentCaptor: KArgumentCaptor<CustomerSession.PaymentMethodsRetrievalListener> = argumentCaptor()
    private val viewModel = PaymentMethodsViewModel(
        application = ApplicationProvider.getApplicationContext(),
        customerSession = Result.success(customerSession),
        startedFromPaymentSession = true
    )

    @Test
    fun getPaymentMethods_whenSuccess_returnsExpectedPaymentMethods() {
        var paymentMethods: List<PaymentMethod> = emptyList()
        viewModel.getPaymentMethods().observeForever {
            paymentMethods = it.getOrThrow()
        }

        verify(customerSession).getPaymentMethods(
            eq(PaymentMethod.Type.Card),
            isNull(),
            isNull(),
            isNull(),
            eq(EXPECTED_PRODUCT_USAGE),
            listenerArgumentCaptor.capture()
        )

        listenerArgumentCaptor.firstValue.onPaymentMethodsRetrieved(
            listOf(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        )

        assertThat(paymentMethods)
            .isEqualTo(listOf(PaymentMethodFixtures.CARD_PAYMENT_METHOD))
    }

    @Test
    fun getPaymentMethods_whenError_returnsExpectedException() {
        var throwable: Throwable? = null
        viewModel.getPaymentMethods().observeForever {
            throwable = it.exceptionOrNull()
        }

        verify(customerSession).getPaymentMethods(
            eq(PaymentMethod.Type.Card),
            isNull(),
            isNull(),
            isNull(),
            eq(EXPECTED_PRODUCT_USAGE),
            listenerArgumentCaptor.capture()
        )

        listenerArgumentCaptor.firstValue.onError(
            404,
            "error!",
            null
        )

        assertThat(throwable)
            .isInstanceOf(APIException::class.java)
    }

    @Test
    fun onPaymentMethodAdded_shouldUpdateSnackbarData() {
        val values: MutableList<String?> = mutableListOf()
        viewModel.snackbarData.observeForever { values.add(it) }

        viewModel.onPaymentMethodAdded(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        assertThat(values[0])
            .isEqualTo("Added Visa ending in 4242")
        assertThat(values[1])
            .isNull()
    }

    @Test
    fun onPaymentMethodRemoved_shouldUpdateSnackbarData() {
        val values: MutableList<String?> = mutableListOf()
        viewModel.snackbarData.observeForever { values.add(it) }

        viewModel.onPaymentMethodRemoved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        assertThat(values[0])
            .isEqualTo("Removed Visa ending in 4242")
        assertThat(values[1])
            .isNull()
    }

    @Test
    fun `getPaymentMethods() with CustomerSession failure should return failure result`() {
        var result: Result<List<PaymentMethod>>? = null
        PaymentMethodsViewModel(
            application = ApplicationProvider.getApplicationContext(),
            customerSession = Result.failure(RuntimeException("failure")),
            startedFromPaymentSession = true
        ).getPaymentMethods().observeForever {
            result = it
        }

        requireNotNull(result)
        assertThat(result?.isFailure)
            .isTrue()
    }

    private companion object {
        private val EXPECTED_PRODUCT_USAGE = setOf(
            PaymentSession.PRODUCT_TOKEN,
            PaymentMethodsActivity.PRODUCT_TOKEN
        )
    }
}
