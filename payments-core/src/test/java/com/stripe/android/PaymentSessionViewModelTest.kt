package com.stripe.android

import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.CustomerFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.view.PaymentMethodsActivityStarter
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class PaymentSessionViewModelTest {
    private val customerSession: CustomerSession = mock()
    private val paymentSessionPrefs: PaymentSessionPrefs = mock()
    private val savedStateHandle: SavedStateHandle = mock()

    private val paymentMethodsListenerCaptor =
        argumentCaptor<CustomerSession.PaymentMethodsRetrievalListener>()

    private val viewModel: PaymentSessionViewModel = createViewModel()

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
    fun init_whenSavedStateHasData_shouldUpdatePaymentSessionData() = runTest {
        whenever(
            savedStateHandle.get<PaymentSessionData>(
                PaymentSessionViewModel.KEY_PAYMENT_SESSION_DATA
            )
        ).thenReturn(UPDATED_DATA)

        createViewModel().paymentSessionDataStateFlow.test {
            assertThat(awaitItem())
                .isEqualTo(UPDATED_DATA)
        }
    }

    @Test
    fun updateCartTotal_shouldUpdatePaymentSessionData() {
        viewModel.updateCartTotal(5000)
        assertThat(viewModel.paymentSessionData.cartTotal)
            .isEqualTo(5000)
    }

    @Test
    fun getSelectedPaymentMethod_whenPrefsNotSet_returnsNull() {
        whenever(customerSession.cachedCustomer)
            .thenReturn(FIRST_CUSTOMER)
        assertThat(viewModel.getSelectedPaymentMethod(null))
            .isNull()
    }

    @Test
    fun getSelectedPaymentMethod_whenHasPrefsSet_returnsExpectedId() {
        val customerId = requireNotNull(FIRST_CUSTOMER.id)
        whenever(paymentSessionPrefs.getPaymentMethod(customerId))
            .thenReturn(PaymentSessionPrefs.SelectedPaymentMethod.Saved("pm_12345"))

        whenever(customerSession.cachedCustomer).thenReturn(FIRST_CUSTOMER)
        CustomerSession.instance = customerSession

        assertThat(viewModel.getSelectedPaymentMethod()?.stringValue)
            .isEqualTo("pm_12345")
    }

    @Test
    fun getSelectedPaymentMethod_whenGooglePay_returnsExpectedValue() {
        val customerId = requireNotNull(FIRST_CUSTOMER.id)
        whenever(paymentSessionPrefs.getPaymentMethod(customerId))
            .thenReturn(PaymentSessionPrefs.SelectedPaymentMethod.GooglePay)

        whenever(customerSession.cachedCustomer).thenReturn(FIRST_CUSTOMER)
        CustomerSession.instance = customerSession

        assertThat(viewModel.getSelectedPaymentMethod())
            .isEqualTo(PaymentSessionPrefs.SelectedPaymentMethod.GooglePay)
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
    fun settingPaymentSessionData_shouldUpdateStateFlow() = runTest {
        viewModel.paymentSessionDataStateFlow.test {
            viewModel.paymentSessionData = UPDATED_DATA
            assertThat(awaitItem())
                .isEqualTo(UPDATED_DATA)
        }
    }

    @Test
    fun onPaymentMethodResult_withGooglePay_shouldUpdateStateFlow() = runTest {
        viewModel.paymentSessionDataStateFlow.test {
            viewModel.onPaymentMethodResult(
                PaymentMethodsActivityStarter.Result(
                    useGooglePay = true
                )
            )
            assertThat(awaitItem().useGooglePay)
                .isTrue()
        }
    }

    @Test
    fun onCustomerRetrieved_whenIsInitialFetchAndPreviouslyUsedPaymentMethodExists_shouldUpdatePaymentSessionData() = runTest {
        val customerPaymentMethods = PaymentMethodFixtures.createCards(20)
        doNothing().whenever(customerSession).getPaymentMethods(
            paymentMethodType = eq(PaymentMethod.Type.Card),
            limit = eq(100),
            endingBefore = anyOrNull(),
            startingAfter = anyOrNull(),
            listener = any()
        )
        whenever(paymentSessionPrefs.getPaymentMethod("cus_123"))
            .thenReturn(PaymentSessionPrefs.SelectedPaymentMethod.fromString(customerPaymentMethods.last().id))

        var onCompleteCallbackCount = 0
        viewModel.onCustomerRetrieved(
            customerId = "cus_123",
            isInitialFetch = true
        ) {
            onCompleteCallbackCount++
        }

        verify(customerSession).getPaymentMethods(
            paymentMethodType = eq(PaymentMethod.Type.Card),
            limit = eq(100),
            endingBefore = anyOrNull(),
            startingAfter = anyOrNull(),
            listener = paymentMethodsListenerCaptor.capture()
        )
        paymentMethodsListenerCaptor.firstValue.onPaymentMethodsRetrieved(
            customerPaymentMethods
        )

        viewModel.paymentSessionDataStateFlow.test {
            assertThat(awaitItem()?.paymentMethod)
                .isEqualTo(customerPaymentMethods.last())
        }

        assertThat(onCompleteCallbackCount)
            .isEqualTo(1)
    }

    @Test
    fun onCustomerRetrieved_whenIsInitialFetchAndPreviouslyUsedPaymentMethodIsNotFoundInList_shouldNotUpdatePaymentSessionData() = runTest {
        viewModel.paymentSessionDataStateFlow.test {
            val customerPaymentMethods = PaymentMethodFixtures.createCards(20)
            doNothing().whenever(customerSession).getPaymentMethods(
                paymentMethodType = eq(PaymentMethod.Type.Card),
                limit = eq(100),
                endingBefore = anyOrNull(),
                startingAfter = anyOrNull(),
                listener = any()
            )
            whenever(paymentSessionPrefs.getPaymentMethod("cus_123"))
                .thenReturn(PaymentSessionPrefs.SelectedPaymentMethod.Saved("pm_not_in_list"))

            var onCompleteCallbackCount = 0
            viewModel.onCustomerRetrieved(
                customerId = "cus_123",
                isInitialFetch = true
            ) {
                onCompleteCallbackCount++
            }

            verify(customerSession).getPaymentMethods(
                paymentMethodType = eq(PaymentMethod.Type.Card),
                limit = eq(100),
                endingBefore = anyOrNull(),
                startingAfter = anyOrNull(),
                listener = paymentMethodsListenerCaptor.capture()
            )
            paymentMethodsListenerCaptor.firstValue.onPaymentMethodsRetrieved(
                customerPaymentMethods
            )

            ensureAllEventsConsumed()

            assertThat(onCompleteCallbackCount)
                .isEqualTo(1)
        }
    }

    @Test
    fun onCustomerRetrieved_whenIsInitialFetchAndPreviouslyUsedPaymentMethodDoesNotExist_shouldNotFetchPaymentMethods() = runTest {
        viewModel.paymentSessionDataStateFlow.test {
            whenever(paymentSessionPrefs.getPaymentMethod("cus_123"))
                .thenReturn(null)

            var onCompleteCallbackCount = 0
            viewModel.onCustomerRetrieved(
                customerId = "cus_123",
                isInitialFetch = true
            ) {
                onCompleteCallbackCount++
            }

            verify(customerSession, never()).getPaymentMethods(
                paymentMethodType = eq(PaymentMethod.Type.Card),
                limit = eq(100),
                endingBefore = anyOrNull(),
                startingAfter = anyOrNull(),
                listener = any()
            )

            ensureAllEventsConsumed()

            assertThat(onCompleteCallbackCount)
                .isEqualTo(1)
        }
    }

    @Test
    fun fetchCustomer_onSuccess_returnsSuccessResult() = runTest {
        viewModel.networkState.test {
            assertThat(awaitItem()).isEqualTo(PaymentSessionViewModel.NetworkState.Inactive) // Initial Value

            whenever(
                customerSession.retrieveCurrentCustomer(
                    productUsage = any(),
                    listener = any()
                )
            ).thenAnswer { invocation ->
                val listener = invocation.arguments[1] as CustomerSession.CustomerRetrievalListener

                listener.onCustomerRetrieved(CustomerFixtures.CUSTOMER)
            }

            val result = viewModel.fetchCustomer()

            assertThat(awaitItem()).isEqualTo(PaymentSessionViewModel.NetworkState.Active)

            verify(customerSession).retrieveCurrentCustomer(
                eq(setOf(PaymentSession.PRODUCT_TOKEN)),
                any()
            )

            assertThat(result).isEqualTo(PaymentSessionViewModel.FetchCustomerResult.Success)

            assertThat(awaitItem()).isEqualTo(PaymentSessionViewModel.NetworkState.Inactive)
        }
    }

    @Test
    fun fetchCustomer_onError_returnsErrorResult() = runTest {
        viewModel.networkState.test {
            assertThat(awaitItem()).isEqualTo(PaymentSessionViewModel.NetworkState.Inactive) // Initial Value

            whenever(
                customerSession.retrieveCurrentCustomer(
                    productUsage = any(),
                    listener = any()
                )
            ).thenAnswer { invocation ->
                val listener = invocation.arguments[1] as CustomerSession.CustomerRetrievalListener

                listener.onError(500, "error", StripeErrorFixtures.INVALID_REQUEST_ERROR)
            }

            val result = viewModel.fetchCustomer()

            assertThat(awaitItem()).isEqualTo(PaymentSessionViewModel.NetworkState.Active)

            verify(customerSession).retrieveCurrentCustomer(
                eq(setOf(PaymentSession.PRODUCT_TOKEN)),
                any()
            )

            assertThat(result)
                .isEqualTo(
                    PaymentSessionViewModel.FetchCustomerResult.Error(
                        500,
                        "error",
                        StripeErrorFixtures.INVALID_REQUEST_ERROR
                    )
                )

            assertThat(awaitItem()).isEqualTo(PaymentSessionViewModel.NetworkState.Inactive)
        }
    }

    private fun createViewModel() = PaymentSessionViewModel(
        ApplicationProvider.getApplicationContext(),
        savedStateHandle,
        PaymentSessionFixtures.PAYMENT_SESSION_DATA,
        customerSession,
        paymentSessionPrefs
    )

    private companion object {
        private val FIRST_CUSTOMER = CustomerFixtures.CUSTOMER

        private val UPDATED_DATA = PaymentSessionFixtures.PAYMENT_SESSION_DATA
            .copy(cartTotal = 999999)
    }
}
