package com.stripe.android.paymentsheet.repositories

import com.google.common.truth.Truth
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.core.Logger
import com.stripe.android.PaymentConfiguration
import com.stripe.android.model.ListPaymentMethodsParams
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.networking.StripeRepository
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.utils.any
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.anyString
import org.mockito.Mockito.verify
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import java.security.InvalidParameterException
import kotlin.test.AfterTest

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
internal class CustomerRepositoryTest {
    private val testDispatcher = TestCoroutineDispatcher()
    private val stripeRepository = mock<StripeRepository>() {
        onBlocking { getPaymentMethods(any(), anyString(), any(), any()) }.doReturn(emptyList())
        onBlocking { detachPaymentMethod(anyString(), any(), anyString(), any()) }.doThrow(InvalidParameterException("error"))
    }
    private val repository = CustomerApiRepository(
        stripeRepository,
        { PaymentConfiguration(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY, "acct_123") },
        Logger.getInstance(false),
        workContext = testDispatcher
    )

    @AfterTest
    fun cleanup() {
        testDispatcher.cleanupTestCoroutines()
    }

    @Test
    fun `getPaymentMethods() should create expected ListPaymentMethodsParams`() =
        testDispatcher.runBlockingTest {
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
    fun `getPaymentMethods() with partially failing requests should emit list with successful values`() =
        testDispatcher.runBlockingTest {
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

            Truth.assertThat(result).containsExactly(
                PaymentMethodFixtures.CARD_PAYMENT_METHOD,
                PaymentMethodFixtures.CARD_PAYMENT_METHOD
            )
        }

    @Test
    fun `detachPaymentMethod() should return null on failure`() =
        testDispatcher.runBlockingTest {
            val result = repository.detachPaymentMethod(
                PaymentSheet.CustomerConfiguration(
                    "customer_id",
                    "ephemeral_key"
                ),
                "payment_method_id"
            )

            Truth.assertThat(result).isNull()
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
            .doThrow(InvalidParameterException("Request Failed"))
            .doReturn(listOf(PaymentMethodFixtures.CARD_PAYMENT_METHOD))
        return repository
    }
}
