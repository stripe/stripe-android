package com.stripe.android.paymentsheet.repositories

import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.Logger
import com.stripe.android.model.ListPaymentMethodsParams
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.networking.StripeRepository
import com.stripe.android.paymentsheet.PaymentSheet
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.anyString
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import java.security.InvalidParameterException

@RunWith(RobolectricTestRunner::class)
internal class CustomerRepositoryTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val stripeRepository = mock<StripeRepository>()

    private val repository = CustomerApiRepository(
        stripeRepository,
        { PaymentConfiguration(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY, "acct_123") },
        Logger.getInstance(false),
        workContext = testDispatcher
    )

    @Test
    fun `getPaymentMethods() should create expected ListPaymentMethodsParams`() =
        runTest {
            givenGetPaymentMethodsReturns(
                Result.success(emptyList())
            )

            repository.getPaymentMethods(
                PaymentSheet.CustomerConfiguration(
                    "customer_id",
                    "ephemeral_key"
                ),
                listOf(PaymentMethod.Type.Card)
            )

            verify(stripeRepository).getPaymentMethods(
                eq(
                    ListPaymentMethodsParams(
                        customerId = "customer_id",
                        paymentMethodType = PaymentMethod.Type.Card
                    )
                ),
                anyString(),
                any(),
                any()
            )
        }

    @Test
    fun `getPaymentMethods() should return empty list on failure`() =
        runTest {
            givenGetPaymentMethodsReturns(
                Result.failure(InvalidParameterException("error"))
            )

            val result = repository.getPaymentMethods(
                PaymentSheet.CustomerConfiguration(
                    "customer_id",
                    "ephemeral_key"
                ),
                listOf(PaymentMethod.Type.Card)
            )

            assertThat(result).isEmpty()
        }

    @Test
    fun `getPaymentMethods() with partially failing requests should emit list with successful values`() =
        runTest {
            val repository = CustomerApiRepository(
                failsOnceStripeRepository(),
                { PaymentConfiguration(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY) },
                Logger.getInstance(false),
                workContext = testDispatcher
            )

            // Requesting 3 payment method types, the first request will fail
            val result = repository.getPaymentMethods(
                PaymentSheet.CustomerConfiguration(
                    "customer_id",
                    "ephemeral_key"
                ),
                listOf(PaymentMethod.Type.Card, PaymentMethod.Type.Card, PaymentMethod.Type.Card)
            )

            assertThat(result).containsExactly(
                PaymentMethodFixtures.CARD_PAYMENT_METHOD,
                PaymentMethodFixtures.CARD_PAYMENT_METHOD
            )
        }

    @Test
    fun `detachPaymentMethod() should return payment method on success`() =
        runTest {
            givenDetachPaymentMethodReturns(
                Result.success(
                    PaymentMethodFixtures.CARD_PAYMENT_METHOD
                )
            )

            val result = repository.detachPaymentMethod(
                PaymentSheet.CustomerConfiguration(
                    "customer_id",
                    "ephemeral_key"
                ),
                "payment_method_id"
            )

            assertThat(result).isEqualTo(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        }

    @Test
    fun `detachPaymentMethod() should return null on failure`() =
        runTest {
            givenDetachPaymentMethodReturns(
                Result.failure(InvalidParameterException("error"))
            )

            val result = repository.detachPaymentMethod(
                PaymentSheet.CustomerConfiguration(
                    "customer_id",
                    "ephemeral_key"
                ),
                "payment_method_id"
            )

            assertThat(result).isNull()
        }

    @Test
    fun `attachPaymentMethod() should return payment method on success`() =
        runTest {
            givenAttachPaymentMethodReturns(
                Result.success(
                    PaymentMethodFixtures.CARD_PAYMENT_METHOD
                )
            )

            val result = repository.attachPaymentMethod(
                PaymentSheet.CustomerConfiguration(
                    "customer_id",
                    "ephemeral_key"
                ),
                "payment_method_id"
            )

            assertThat(result).isEqualTo(
                Result.success(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
            )
        }

    @Test
    fun `attachPaymentMethod() should return failure`() =
        runTest {
            val error = Result.failure<PaymentMethod>(InvalidParameterException("error"))
            givenAttachPaymentMethodReturns(error)

            val result = repository.attachPaymentMethod(
                PaymentSheet.CustomerConfiguration(
                    "customer_id",
                    "ephemeral_key"
                ),
                "payment_method_id"
            )

            assertThat(result).isEqualTo(error)
        }

    private suspend fun failsOnceStripeRepository(): StripeRepository {
        val repository = mock<StripeRepository>()
        whenever(
            repository.getPaymentMethods(
                any(),
                anyString(),
                any(),
                any()
            )
        )
            .doReturn(Result.failure(InvalidParameterException("Request Failed")))
            .doReturn(Result.success(listOf(PaymentMethodFixtures.CARD_PAYMENT_METHOD)))
        return repository
    }

    private fun givenGetPaymentMethodsReturns(
        result: Result<List<PaymentMethod>>
    ) {
        stripeRepository.stub {
            onBlocking {
                getPaymentMethods(
                    listPaymentMethodsParams = any(),
                    publishableKey = anyString(),
                    productUsageTokens = any(),
                    requestOptions = any(),
                )
            }.doReturn(result)
        }
    }

    private fun givenDetachPaymentMethodReturns(
        result: Result<PaymentMethod>
    ) {
        stripeRepository.stub {
            onBlocking {
                detachPaymentMethod(
                    publishableKey = anyString(),
                    productUsageTokens = any(),
                    paymentMethodId = anyString(),
                    requestOptions = any(),
                )
            }.doReturn(result)
        }
    }

    private fun givenAttachPaymentMethodReturns(
        result: Result<PaymentMethod>
    ) {
        stripeRepository.stub {
            onBlocking {
                attachPaymentMethod(
                    customerId = anyString(),
                    publishableKey = anyString(),
                    productUsageTokens = any(),
                    paymentMethodId = anyString(),
                    requestOptions = any(),
                )
            }.doReturn(result)
        }
    }
}
