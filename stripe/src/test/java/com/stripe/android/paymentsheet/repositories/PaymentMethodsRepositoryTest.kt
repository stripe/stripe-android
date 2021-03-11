package com.stripe.android.paymentsheet.repositories

import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.exception.APIConnectionException
import com.stripe.android.exception.APIException
import com.stripe.android.exception.AuthenticationException
import com.stripe.android.exception.CardException
import com.stripe.android.exception.InvalidRequestException
import com.stripe.android.model.ListPaymentMethodsParams
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.model.PaymentMethodFixtures.CARD_PAYMENT_METHOD
import com.stripe.android.networking.AbsFakeStripeRepository
import com.stripe.android.networking.ApiRequest
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
internal class PaymentMethodsRepositoryTest {
    private val testDispatcher = TestCoroutineDispatcher()
    private val stripeRepository = FakeStripeRepository()
    private val repository = PaymentMethodsRepository.Api(
        stripeRepository,
        ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
        "acct_123",
        workContext = testDispatcher
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

    @Test
    fun `createMethod should throw an exception on null`() = testDispatcher.runBlockingTest {
        stripeRepository.createPaymentMethod = null

        val exception = assertFailsWith<Exception>(
            "Payment method could not be created"
        ) {
            repository.save(
                PaymentSheetFixtures.CONFIG_CUSTOMER.customer!!,
                PaymentMethodCreateParamsFixtures.DEFAULT_CARD
            )
        }
        assertThat(exception.message)
    }

    @Test
    fun `attachMethod should throw an exception on null`() = testDispatcher.runBlockingTest {
        stripeRepository.createPaymentMethod = CARD_PAYMENT_METHOD
        stripeRepository.attachPaymentMethod = null

        val exception = assertFailsWith<Exception>(
            "Payment method could not be attached"
        ) {
            repository.save(
                PaymentSheetFixtures.CONFIG_CUSTOMER.customer!!,
                PaymentMethodCreateParamsFixtures.DEFAULT_CARD
            )
        }
        assertThat(exception.message)
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

        @Throws(
            AuthenticationException::class,
            InvalidRequestException::class,
            APIConnectionException::class,
            APIException::class
        )
        override suspend fun createPaymentMethod(
            paymentMethodCreateParams: PaymentMethodCreateParams,
            options: ApiRequest.Options
        ): PaymentMethod? {
            return createPaymentMethod
        }

        @Throws(
            AuthenticationException::class,
            InvalidRequestException::class,
            APIConnectionException::class,
            APIException::class,
            CardException::class
        )
        override suspend fun attachPaymentMethod(
            customerId: String,
            publishableKey: String,
            productUsageTokens: Set<String>,
            paymentMethodId: String,
            requestOptions: ApiRequest.Options
        ): PaymentMethod? {
            return attachPaymentMethod
        }
    }
}
