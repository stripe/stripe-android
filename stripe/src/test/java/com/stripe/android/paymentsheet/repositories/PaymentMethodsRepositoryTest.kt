package com.stripe.android.paymentsheet.repositories

import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.model.ListPaymentMethodsParams
import com.stripe.android.model.PaymentMethod
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
internal class PaymentMethodsRepositoryTest {
    private val testDispatcher = TestCoroutineDispatcher()
    private val stripeRepository = FakeStripeRepository()
    private val repository = PaymentMethodsRepository.Api(
        stripeRepository,
        ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
        "acct_123",
        testDispatcher
    )

    @AfterTest
    fun cleanup() {
        testDispatcher.cleanupTestCoroutines()
    }

    @Test
    fun `get() should create expected ListPaymentMethodsParams`() = testDispatcher.runBlockingTest {
        repository.get(
            PaymentSheet.CustomerConfiguration(
                "customer_id",
                "ephemeral_key"
            ),
            PaymentMethod.Type.Card
        )

        assertThat(stripeRepository.paramArgs)
            .containsExactly(
                ListPaymentMethodsParams(
                    customerId = "customer_id",
                    paymentMethodType = PaymentMethod.Type.Card
                )
            )
    }

    private class FakeStripeRepository : AbsFakeStripeRepository() {
        var paymentMethods: List<PaymentMethod> = emptyList()

        val paramArgs = mutableListOf<ListPaymentMethodsParams>()

        override suspend fun getPaymentMethods(
            listPaymentMethodsParams: ListPaymentMethodsParams,
            publishableKey: String,
            productUsageTokens: Set<String>,
            requestOptions: ApiRequest.Options
        ): List<PaymentMethod> {
            paramArgs.add(listPaymentMethodsParams)
            return paymentMethods
        }
    }
}
