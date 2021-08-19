package com.stripe.android.paymentsheet.repositories

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
internal class CustomerRepositoryTest {
//    private val testDispatcher = TestCoroutineDispatcher()
//    private val stripeRepository = FakeStripeRepository()
//    private val repository = CustomerApiRepository(
//        stripeRepository,
//        { PaymentConfiguration(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY, "acct_123") },
//        Logger.getInstance(false),
//        workContext = testDispatcher
//    )
//
//    @AfterTest
//    fun cleanup() {
//        testDispatcher.cleanupTestCoroutines()
//    }
//
//    @Test
//    fun `getPaymentMethods() should create expected ListPaymentMethodsParams`() =
//        testDispatcher.runBlockingTest {
//            repository.getPaymentMethods(
//                PaymentSheet.CustomerConfiguration(
//                    "customer_id",
//                    "ephemeral_key"
//                ),
//                listOf(PaymentMethod.Type.Card)
//            )
//
//            Truth.assertThat(stripeRepository.paramArgs)
//                .containsExactly(
//                    ListPaymentMethodsParams(
//                        customerId = "customer_id",
//                        paymentMethodType = PaymentMethod.Type.Card
//                    )
//                )
//        }
//
//    @Test
//    fun `getPaymentMethods() with partially failing requests should emit list with successful values`() =
//        testDispatcher.runBlockingTest {
//            val repository = CustomerApiRepository(
//                FailsOnceStripeRepository(),
//                { PaymentConfiguration(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY) },
//                Logger.getInstance(false),
//                workContext = testDispatcher
//            )
//
//            // Requesting 3 payment method types, the first request will fail
//            val result = repository.getPaymentMethods(
//                PaymentSheet.CustomerConfiguration(
//                    "customer_id",
//                    "ephemeral_key"
//                ),
//                listOf(PaymentMethod.Type.Card, PaymentMethod.Type.Card, PaymentMethod.Type.Card)
//            )
//
//            Truth.assertThat(result).containsExactly(
//                PaymentMethodFixtures.CARD_PAYMENT_METHOD,
//                PaymentMethodFixtures.CARD_PAYMENT_METHOD
//            )
//        }
//
//    @Test
//    fun `detachPaymentMethod() should return null on failure`() =
//        testDispatcher.runBlockingTest {
//            val result = repository.detachPaymentMethod(
//                PaymentSheet.CustomerConfiguration(
//                    "customer_id",
//                    "ephemeral_key"
//                ),
//                "payment_method_id"
//            )
//
//            Truth.assertThat(result).isNull()
//        }
//
//    private class FailsOnceStripeRepository : AbsFakeStripeRepository() {
//        var firstCall = true
//        override suspend fun getPaymentMethods(
//            listPaymentMethodsParams: ListPaymentMethodsParams,
//            publishableKey: String,
//            productUsageTokens: Set<String>,
//            requestOptions: ApiRequest.Options
//        ): List<PaymentMethod> = if (firstCall) {
//            firstCall = false
//            error("Request failed.")
//        } else {
//            listOf(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
//        }
//    }
//
//    private class FakeStripeRepository : AbsFakeStripeRepository() {
//        var paymentMethods: List<PaymentMethod> = emptyList()
//
//        val paramArgs = mutableListOf<ListPaymentMethodsParams>()
//
//        var createPaymentMethod: PaymentMethod? = null
//        var attachPaymentMethod: PaymentMethod? = null
//
//        override suspend fun getPaymentMethods(
//            listPaymentMethodsParams: ListPaymentMethodsParams,
//            publishableKey: String,
//            productUsageTokens: Set<String>,
//            requestOptions: ApiRequest.Options
//        ): List<PaymentMethod> {
//            paramArgs.add(listPaymentMethodsParams)
//            return paymentMethods
//        }
//
//        override suspend fun createPaymentMethod(
//            paymentMethodCreateParams: PaymentMethodCreateParams,
//            options: ApiRequest.Options
//        ): PaymentMethod? {
//            return createPaymentMethod
//        }
//
//        override suspend fun attachPaymentMethod(
//            customerId: String,
//            publishableKey: String,
//            productUsageTokens: Set<String>,
//            paymentMethodId: String,
//            requestOptions: ApiRequest.Options
//        ): PaymentMethod? {
//            return attachPaymentMethod
//        }
//
//        override suspend fun detachPaymentMethod(
//            publishableKey: String,
//            productUsageTokens: Set<String>,
//            paymentMethodId: String,
//            requestOptions: ApiRequest.Options
//        ): PaymentMethod? {
//            error("Error")
//        }
//    }
}
