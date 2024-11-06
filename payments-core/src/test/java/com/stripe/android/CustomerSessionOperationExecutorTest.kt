package com.stripe.android

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.Customer
import com.stripe.android.model.CustomerFixtures
import com.stripe.android.model.ListPaymentMethodsParams
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.Source
import com.stripe.android.networking.StripeRepository
import com.stripe.android.testing.AbsFakeStripeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.mockito.ArgumentMatchers.eq
import org.mockito.kotlin.argWhere
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

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
                    productUsageTokens: Set<String>,
                    paymentMethodId: String,
                    requestOptions: ApiRequest.Options
                ): Result<PaymentMethod> {
                    return Result.success(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
                }
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
    fun `execute with AttachPaymentMethod operation should call listener with error on failure`() = runTest {
        val listener = mock<CustomerSession.PaymentMethodRetrievalListener>()
        listeners[OPERATION_ID] = listener

        val errorMessage = "an error or something"

        val executor = createExecutor(
            object : AbsFakeStripeRepository() {
                override suspend fun attachPaymentMethod(
                    customerId: String,
                    productUsageTokens: Set<String>,
                    paymentMethodId: String,
                    requestOptions: ApiRequest.Options
                ): Result<PaymentMethod> {
                    return Result.failure(RuntimeException(errorMessage))
                }
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
            errorMessage,
            null
        )
        assertThat(customerCallbacks)
            .isEmpty()
    }

    @Test
    fun `execute with GetPaymentMethods operation should call listener on failure`() = runTest {
        val listener = mock<CustomerSession.PaymentMethodsRetrievalListener>()
        listeners[OPERATION_ID] = listener

        val errorMessage = "an error or something"

        val executor = createExecutor(
            object : AbsFakeStripeRepository() {
                override suspend fun getPaymentMethods(
                    listPaymentMethodsParams: ListPaymentMethodsParams,
                    productUsageTokens: Set<String>,
                    requestOptions: ApiRequest.Options
                ): Result<List<PaymentMethod>> {
                    return Result.failure(RuntimeException(errorMessage))
                }
            }
        )
        executor.execute(
            EphemeralKeyFixtures.FIRST,
            EphemeralOperation.Customer.GetPaymentMethods(
                type = PaymentMethod.Type.Card,
                id = OPERATION_ID,
                productUsage = emptySet()
            )
        )

        verify(listener).onError(
            0,
            errorMessage,
            null
        )
        assertThat(customerCallbacks)
            .isEmpty()
    }

    @Test
    fun `execute with GetPaymentMethods operation should call exception listener on failure`() = runTest {
        val listener = mock<CustomerSession.PaymentMethodsRetrievalWithExceptionListener>()
        listeners[OPERATION_ID] = listener

        val errorMessage = "an error or something"

        val executor = createExecutor(
            object : AbsFakeStripeRepository() {
                override suspend fun getPaymentMethods(
                    listPaymentMethodsParams: ListPaymentMethodsParams,
                    productUsageTokens: Set<String>,
                    requestOptions: ApiRequest.Options
                ): Result<List<PaymentMethod>> {
                    return Result.failure(RuntimeException(errorMessage))
                }
            }
        )
        executor.execute(
            EphemeralKeyFixtures.FIRST,
            EphemeralOperation.Customer.GetPaymentMethods(
                type = PaymentMethod.Type.Card,
                id = OPERATION_ID,
                productUsage = emptySet()
            )
        )

        verify(listener).onError(
            eq(0),
            eq(errorMessage) ?: errorMessage,
            isNull(),
            argWhere { error ->
                error is RuntimeException && error.message == errorMessage
            }
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
                ): Result<Customer> = Result.success(CustomerFixtures.CUSTOMER)
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
