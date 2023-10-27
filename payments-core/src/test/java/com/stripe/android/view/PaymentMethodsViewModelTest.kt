package com.stripe.android.view

import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.CustomerSession
import com.stripe.android.PaymentSession
import com.stripe.android.core.exception.APIException
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
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
    fun paymentMethodsData_whenSuccess_returnsExpectedPaymentMethods() = runTest {
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

        viewModel.paymentMethodsData.test {
            assertThat(awaitItem())
                .isEqualTo(Result.success(listOf(PaymentMethodFixtures.CARD_PAYMENT_METHOD)))
        }
    }

    @Test
    fun paymentMethodsData_whenError_returnsExpectedException() = runTest {
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

        viewModel.paymentMethodsData.test {
            assertThat(awaitItem()?.exceptionOrNull())
                .isInstanceOf(APIException::class.java)
        }
    }

    @Test
    fun onPaymentMethodAdded_shouldUpdateSnackbarData() = runTest {
        viewModel.snackbarData.test {
            assertThat(awaitItem())
                .isNull()
            viewModel.onPaymentMethodAdded(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
            assertThat(awaitItem())
                .isEqualTo("Added Visa ending in 4242")
            assertThat(awaitItem())
                .isNull()
            // First time is init, second time is from the onPaymentMethodAdded call.
            verify(customerSession, times(2)).getPaymentMethods(
                eq(PaymentMethod.Type.Card),
                isNull(),
                isNull(),
                isNull(),
                eq(EXPECTED_PRODUCT_USAGE),
                listenerArgumentCaptor.capture()
            )
        }
    }

    @Test
    fun onPaymentMethodRemoved_shouldUpdateSnackbarData() = runTest {
        viewModel.snackbarData.test {
            assertThat(awaitItem())
                .isNull()
            viewModel.onPaymentMethodRemoved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
            assertThat(awaitItem())
                .isEqualTo("Removed Visa ending in 4242")
            assertThat(awaitItem())
                .isNull()
        }
    }

    @Test
    fun `paymentMethodsData with CustomerSession failure should return failure result`() = runTest {
        PaymentMethodsViewModel(
            application = ApplicationProvider.getApplicationContext(),
            customerSession = Result.failure(RuntimeException("failure")),
            startedFromPaymentSession = true
        ).paymentMethodsData.test {
            assertThat(awaitItem()?.isFailure)
                .isTrue()
        }
    }

    private companion object {
        private val EXPECTED_PRODUCT_USAGE = setOf(
            PaymentSession.PRODUCT_TOKEN,
            PaymentMethodsActivity.PRODUCT_TOKEN
        )
    }
}
