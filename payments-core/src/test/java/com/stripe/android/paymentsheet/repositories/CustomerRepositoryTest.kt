package com.stripe.android.paymentsheet.repositories

import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.Logger
import com.stripe.android.PaymentConfiguration
import com.stripe.android.model.ListPaymentMethodsParams
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.networking.AbsFakeStripeRepository
import com.stripe.android.networking.ApiRequest
import com.stripe.android.paymentsheet.PaymentSheet
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.AfterTest
import kotlin.test.Test

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
internal class CustomerRepositoryTest {
    private val testDispatcher = TestCoroutineDispatcher()
    private val stripeRepository = FakeStripeRepository()
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

            assertThat(stripeRepository.paramArgs)
                .containsExactly(
                    ListPaymentMethodsParams(
                        customerId = "customer_id",
                        paymentMethodType = PaymentMethod.Type.Card
                    )
                )
        }

    @Test
    fun `getPaymentMethods() with partially failing requests should emit list with successful values`() =
        testDispatcher.runBlockingTest {
            val repository = CustomerApiRepository(
                FailsOnceStripeRepository(),
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
    fun `detachPaymentMethod() should return null on failure`() =
        testDispatcher.runBlockingTest {
            val result = repository.detachPaymentMethod(
                PaymentSheet.CustomerConfiguration(
                    "customer_id",
                    "ephemeral_key"
                ),
                "payment_method_id"
            )

            assertThat(result).isNull()
        }

    private class FailsOnceStripeRepository : AbsFakeStripeRepository() {
        var firstCall = true
        override suspend fun getPaymentMethods(
            listPaymentMethodsParams: ListPaymentMethodsParams,
            publishableKey: String,
            productUsageTokens: Set<String>,
            requestOptions: ApiRequest.Options
        ): List<PaymentMethod> = if (firstCall) {
            firstCall = false
            error("Request failed.")
        } else {
            listOf(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        }
    }

    private class FakeStripeRepository : AbsFakeStripeRepository() {
        var paymentMethods: List<PaymentMethod> = emptyList()

        val paramArgs = mutableListOf<ListPaymentMethodsParams>()

        var createPaymentMethod: PaymentMethod? = null
        var attachPaymentMethod: PaymentMethod? = null

        override suspend fun getPaymentMethods(
            listPaymentMethodsParams: ListPaymentMethodsParams,
            publishableKey: String,
            productUsageTokens: Set<String>,
            requestOptions: ApiRequest.Options
        ): List<PaymentMethod> {
            paramArgs.add(listPaymentMethodsParams)
            return paymentMethods
        }

        override suspend fun createPaymentMethod(
            paymentMethodCreateParams: PaymentMethodCreateParams,
            options: ApiRequest.Options
        ): PaymentMethod? {
            return createPaymentMethod
        }

        override suspend fun attachPaymentMethod(
            customerId: String,
            publishableKey: String,
            productUsageTokens: Set<String>,
            paymentMethodId: String,
            requestOptions: ApiRequest.Options
        ): PaymentMethod? {
            return attachPaymentMethod
        }

        override suspend fun detachPaymentMethod(
            publishableKey: String,
            productUsageTokens: Set<String>,
            paymentMethodId: String,
            requestOptions: ApiRequest.Options
        ): PaymentMethod? {
            error("Error")
        }
    }
}
