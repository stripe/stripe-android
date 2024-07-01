package com.stripe.android.view

import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.CustomerSession
import com.stripe.android.PaymentSession
import com.stripe.android.analytics.PaymentSessionEventReporter
import com.stripe.android.core.exception.InvalidRequestException
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.any
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
    private val listenerArgumentCaptor:
        KArgumentCaptor<CustomerSession.PaymentMethodsRetrievalWithExceptionListener> =
        argumentCaptor()

    @Test
    fun init_loadStartedEvent_shouldTrigger() {
        val eventReporter: PaymentSessionEventReporter = mock()

        createViewModel(eventReporter = eventReporter)

        verify(eventReporter).onLoadStarted()
    }

    @Test
    fun paymentMethodsData_whenSuccess_returnsExpectedPaymentMethods() = runTest {
        val customerSession: CustomerSession = mock()
        val eventReporter: PaymentSessionEventReporter = mock()

        val viewModel = createViewModel(
            customerSession = customerSession,
            eventReporter = eventReporter,
            selectedPaymentMethodId = "fpx"
        )

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

            verify(eventReporter).onLoadSucceeded("fpx")
            verify(eventReporter).onOptionsShown()
        }
    }

    @Test
    fun paymentMethodsData_whenError_returnsExpectedException() = runTest {
        val customerSession: CustomerSession = mock()
        val eventReporter: PaymentSessionEventReporter = mock()

        val viewModel = createViewModel(
            eventReporter = eventReporter,
            customerSession = customerSession
        )

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
            null,
            InvalidRequestException()
        )

        viewModel.paymentMethodsData.test {
            assertThat(awaitItem()?.exceptionOrNull())
                .isInstanceOf(InvalidRequestException::class.java)

            verify(eventReporter).onLoadFailed(any<InvalidRequestException>())
        }
    }

    @Test
    fun onPaymentMethodAdded_shouldUpdateSnackbarData() = runTest {
        val customerSession: CustomerSession = mock()

        val viewModel = createViewModel(customerSession = customerSession)

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
        val viewModel = createViewModel()

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
            startedFromPaymentSession = true,
            savedStateHandle = SavedStateHandle(),
            eventReporter = mock(),
        ).paymentMethodsData.test {
            assertThat(awaitItem()?.isFailure)
                .isTrue()
        }
    }

    private fun createViewModel(
        customerSession: CustomerSession = mock(),
        eventReporter: PaymentSessionEventReporter = mock(),
        selectedPaymentMethodId: String? = null,
    ): PaymentMethodsViewModel {
        return PaymentMethodsViewModel(
            application = ApplicationProvider.getApplicationContext(),
            customerSession = Result.success(customerSession),
            startedFromPaymentSession = true,
            savedStateHandle = SavedStateHandle(),
            eventReporter = eventReporter,
            selectedPaymentMethodId = selectedPaymentMethodId,
        )
    }

    private companion object {
        private val EXPECTED_PRODUCT_USAGE = setOf(
            PaymentSession.PRODUCT_TOKEN,
            PaymentMethodsActivity.PRODUCT_TOKEN
        )
    }
}
