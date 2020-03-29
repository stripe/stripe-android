package com.stripe.android

import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.KArgumentCaptor
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.doNothing
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.stripe.android.model.CustomerFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.view.PaymentMethodsActivityStarter
import kotlin.test.BeforeTest
import kotlin.test.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PaymentSessionViewModelTest {
    private val customerSession: CustomerSession = mock()
    private val paymentSessionPrefs: PaymentSessionPrefs = mock()
    private val savedStateHandle: SavedStateHandle = mock()

    private val paymentMethodsListenerCaptor: KArgumentCaptor<CustomerSession.PaymentMethodsRetrievalListener> = argumentCaptor()
    private val customerRetrievalListenerCaptor: KArgumentCaptor<CustomerSession.CustomerRetrievalListener> = argumentCaptor()

    private val viewModel: PaymentSessionViewModel by lazy { createViewModel() }

    private val paymentSessionDatas: MutableList<PaymentSessionData> = mutableListOf()
    private val networkStates: MutableList<PaymentSessionViewModel.NetworkState> = mutableListOf()

    @BeforeTest
    fun before() {
        viewModel.networkState.observeForever {
            networkStates.add(it)
        }

        viewModel.paymentSessionDataLiveData.observeForever {
            paymentSessionDatas.add(it)
        }
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

        val viewModel = createViewModel()
        viewModel.paymentSessionDataLiveData.observeForever {
            paymentSessionDatas.add(it)
        }

        assertThat(paymentSessionDatas)
            .containsExactly(UPDATED_DATA)
    }

    @Test
    fun updateCartTotal_shouldUpdatePaymentSessionData() {
        viewModel.updateCartTotal(5000)
        assertThat(viewModel.paymentSessionData.cartTotal)
            .isEqualTo(5000)
    }

    @Test
    fun getSelectedPaymentMethodId_whenPrefsNotSet_returnsNull() {
        whenever(customerSession.cachedCustomer)
            .thenReturn(FIRST_CUSTOMER)
        assertThat(viewModel.getSelectedPaymentMethodId(null))
            .isNull()
    }

    @Test
    fun getSelectedPaymentMethodId_whenHasPrefsSet_returnsExpectedId() {
        val customerId = requireNotNull(FIRST_CUSTOMER.id)
        whenever(paymentSessionPrefs.getPaymentMethodId(customerId))
            .thenReturn("pm_12345")

        whenever(customerSession.cachedCustomer).thenReturn(FIRST_CUSTOMER)
        CustomerSession.instance = customerSession

        assertThat(viewModel.getSelectedPaymentMethodId())
            .isEqualTo("pm_12345")
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
        viewModel.paymentSessionData = UPDATED_DATA
        assertThat(paymentSessionDatas)
            .containsExactly(UPDATED_DATA)
    }

    @Test
    fun onPaymentMethodResult_withGooglePay_shouldUpdateLiveData() {
        viewModel.onPaymentMethodResult(PaymentMethodsActivityStarter.Result(
            useGooglePay = true
        ))
        assertThat(paymentSessionDatas.last().useGooglePay)
            .isTrue()
    }

    @Test
    fun onCustomerRetrieved_whenIsInitialFetchAndPreviouslyUsedPaymentMethodExists_shouldUpdatePaymentSessionData() {
        val customerPaymentMethods = PaymentMethodFixtures.createCards(20)
        doNothing().whenever(customerSession).getPaymentMethods(
            paymentMethodType = eq(PaymentMethod.Type.Card),
            limit = eq(100),
            endingBefore = anyOrNull(),
            startingAfter = anyOrNull(),
            listener = any()
        )
        whenever(paymentSessionPrefs.getPaymentMethodId("cus_123"))
            .thenReturn(customerPaymentMethods.last().id)

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

        assertThat(paymentSessionDatas.last().paymentMethod)
            .isEqualTo(customerPaymentMethods.last())

        assertThat(onCompleteCallbackCount)
            .isEqualTo(1)
    }

    @Test
    fun onCustomerRetrieved_whenIsInitialFetchAndPreviouslyUsedPaymentMethodIsNotFoundInList_shouldNotUpdatePaymentSessionData() {
        val customerPaymentMethods = PaymentMethodFixtures.createCards(20)
        doNothing().whenever(customerSession).getPaymentMethods(
            paymentMethodType = eq(PaymentMethod.Type.Card),
            limit = eq(100),
            endingBefore = anyOrNull(),
            startingAfter = anyOrNull(),
            listener = any()
        )
        whenever(paymentSessionPrefs.getPaymentMethodId("cus_123"))
            .thenReturn("pm_not_in_list")

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

        assertThat(paymentSessionDatas)
            .isEmpty()

        assertThat(onCompleteCallbackCount)
            .isEqualTo(1)
    }

    @Test
    fun onCustomerRetrieved_whenIsInitialFetchAndPreviouslyUsedPaymentMethodDoesNotExist_shouldNotFetchPaymentMethods() {
        whenever(paymentSessionPrefs.getPaymentMethodId("cus_123"))
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

        assertThat(paymentSessionDatas)
            .isEmpty()

        assertThat(onCompleteCallbackCount)
            .isEqualTo(1)
    }

    @Test
    fun fetchCustomer_onSuccess_returnsSuccessResult() {
        doNothing().whenever(customerSession).retrieveCurrentCustomer(
            listener = any()
        )

        val results: MutableList<PaymentSessionViewModel.FetchCustomerResult> = mutableListOf()
        viewModel.fetchCustomer().observeForever {
            results.add(it)
        }

        verify(customerSession).retrieveCurrentCustomer(
            eq(setOf(PaymentSession.PRODUCT_TOKEN)),
            customerRetrievalListenerCaptor.capture()
        )
        customerRetrievalListenerCaptor.firstValue
            .onCustomerRetrieved(CustomerFixtures.CUSTOMER)

        assertThat(results)
            .containsExactly(PaymentSessionViewModel.FetchCustomerResult.Success)

        assertThat(networkStates)
            .isEqualTo(EXPECTED_NETWORK_STATES)
    }

    @Test
    fun fetchCustomer_onError_returnsErrorResult() {
        doNothing().whenever(customerSession).retrieveCurrentCustomer(
            listener = any()
        )

        val results: MutableList<PaymentSessionViewModel.FetchCustomerResult> = mutableListOf()
        viewModel.fetchCustomer().observeForever {
            results.add(it)
        }

        verify(customerSession).retrieveCurrentCustomer(
            eq(setOf(PaymentSession.PRODUCT_TOKEN)),
            customerRetrievalListenerCaptor.capture()
        )
        customerRetrievalListenerCaptor.firstValue
            .onError(500, "error", StripeErrorFixtures.INVALID_REQUEST_ERROR)

        assertThat(results)
            .containsExactly(
                PaymentSessionViewModel.FetchCustomerResult.Error(
                    500,
                    "error",
                    StripeErrorFixtures.INVALID_REQUEST_ERROR
                )
            )

        assertThat(networkStates)
            .isEqualTo(EXPECTED_NETWORK_STATES)
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

        private val EXPECTED_NETWORK_STATES = listOf(
            PaymentSessionViewModel.NetworkState.Active,
            PaymentSessionViewModel.NetworkState.Inactive
        )
    }
}
