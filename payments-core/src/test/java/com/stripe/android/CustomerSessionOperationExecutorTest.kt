package com.stripe.android

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.Customer
import com.stripe.android.model.CustomerFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.Source
import com.stripe.android.networking.AbsFakeStripeRepository
import com.stripe.android.networking.ApiRequest
import com.stripe.android.networking.StripeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@ExperimentalCoroutinesApi
internal class CustomerSessionOperationExecutorTest {
    private val testDispatcher = StandardTestDispatcher()

    private val listeners = mutableMapOf<String, CustomerSession.RetrievalListener?>()
    private val customerCallbacks = mutableListOf<Customer>()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun cleanup() {
        Dispatchers.resetMain()
    }

    @Test
    fun `execute with AttachPaymentMethod operation when valid PaymentMethod returned should call listener with PaymentMethod`() = runTest {
        val listener = mock<CustomerSession.PaymentMethodRetrievalListener>()
        listeners[OPERATION_ID] = listener

        val executor = createExecutor(
            object : AbsFakeStripeRepository() {
                override suspend fun attachPaymentMethod(
                    customerId: String,
                    publishableKey: String,
                    productUsageTokens: Set<String>,
                    paymentMethodId: String,
                    requestOptions: ApiRequest.Options
                ): PaymentMethod? = PaymentMethodFixtures.CARD_PAYMENT_METHOD
            }
        )
        executor.execute(
            EphemeralKeyFixtures.FIRST,
            EphemeralOperation.Customer.AttachPaymentMethod(
                "pm_123",
                OPERATION_ID,
                emptySet()
            )
        )

        verify(listener).onPaymentMethodRetrieved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        assertThat(customerCallbacks)
            .isEmpty()
    }

    @Test
    fun `execute with AttachPaymentMethod operation when null PaymentMethod returned should call listener with error`() = runTest {
        val listener = mock<CustomerSession.PaymentMethodRetrievalListener>()
        listeners[OPERATION_ID] = listener

        val executor = createExecutor(
            object : AbsFakeStripeRepository() {
                override suspend fun attachPaymentMethod(
                    customerId: String,
                    publishableKey: String,
                    productUsageTokens: Set<String>,
                    paymentMethodId: String,
                    requestOptions: ApiRequest.Options
                ): PaymentMethod? = null
            }
        )
        executor.execute(
            EphemeralKeyFixtures.FIRST,
            EphemeralOperation.Customer.AttachPaymentMethod(
                "pm_123",
                OPERATION_ID,
                emptySet()
            )
        )

        verify(listener).onError(
            0,
            "API request returned an invalid response.",
            null
        )
        assertThat(customerCallbacks)
            .isEmpty()
    }

    @Test
    fun `execute with UpdateDefaultSource operation when valid Customer returned should call listener with PaymentMethod and invoke customerCallback`() = runTest {
        val listener = mock<CustomerSession.CustomerRetrievalListener>()
        listeners[OPERATION_ID] = listener

        val executor = createExecutor(
            object : AbsFakeStripeRepository() {
                override suspend fun setDefaultCustomerSource(
                    customerId: String,
                    publishableKey: String,
                    productUsageTokens: Set<String>,
                    sourceId: String,
                    sourceType: String,
                    requestOptions: ApiRequest.Options
                ): Customer? = CustomerFixtures.CUSTOMER
            }
        )
        executor.execute(
            EphemeralKeyFixtures.FIRST,
            EphemeralOperation.Customer.UpdateDefaultSource(
                "src_123",
                Source.SourceType.CARD,
                OPERATION_ID,
                emptySet()
            )
        )

        verify(listener).onCustomerRetrieved(CustomerFixtures.CUSTOMER)
        assertThat(customerCallbacks)
            .containsExactly(CustomerFixtures.CUSTOMER)
    }

    private fun createExecutor(
        repository: StripeRepository
    ): CustomerSessionOperationExecutor {
        return CustomerSessionOperationExecutor(
            repository,
            ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
            null,
            listeners
        ) { customer ->
            customerCallbacks.add(customer)
        }
    }

    private companion object {
        private const val OPERATION_ID = "123"
    }
}
