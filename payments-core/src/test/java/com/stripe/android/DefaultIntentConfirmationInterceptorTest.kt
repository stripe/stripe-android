package com.stripe.android

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.StripeIntent
import com.stripe.android.networking.AbsFakeStripeRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.mock

class DefaultIntentConfirmationInterceptorTest {

    @Test
    fun `Returns confirm as next step if invoked with client secret for existing payment method`() = runTest {
        val interceptor = DefaultIntentConfirmationInterceptor(
            stripeRepository = object : AbsFakeStripeRepository() {},
            publishableKeyProvider = { "pk" },
            stripeAccountIdProvider = { null },
            confirmCallback = null,
        )

        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD

        val nextStep = interceptor.intercept(
            clientSecret = "pi_1234_secret_4321",
            paymentMethod = paymentMethod,
            shippingValues = null,
            setupForFutureUsage = null,
        )

        val confirmNextStep = nextStep as? IntentConfirmationInterceptor.NextStep.Confirm
        val confirmParams = confirmNextStep?.confirmParams as? ConfirmPaymentIntentParams

        assertThat(confirmParams?.paymentMethodId).isEqualTo(paymentMethod.id)
        assertThat(confirmParams?.paymentMethodCreateParams).isNull()
    }

    @Test
    fun `Returns confirm as next step if invoked with client secret for new payment method`() = runTest {
        val interceptor = DefaultIntentConfirmationInterceptor(
            stripeRepository = object : AbsFakeStripeRepository() {},
            publishableKeyProvider = { "pk" },
            stripeAccountIdProvider = { null },
            confirmCallback = null,
        )

        val createParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD

        val nextStep = interceptor.intercept(
            clientSecret = "pi_1234_secret_4321",
            paymentMethodCreateParams = createParams,
            shippingValues = null,
            setupForFutureUsage = null,
        )

        val confirmNextStep = nextStep as? IntentConfirmationInterceptor.NextStep.Confirm
        val confirmParams = confirmNextStep?.confirmParams as? ConfirmPaymentIntentParams

        assertThat(confirmParams?.paymentMethodId).isNull()
        assertThat(confirmParams?.paymentMethodCreateParams).isEqualTo(createParams)
    }

    @Test
    fun `Fails if invoked without a confirm callback for existing payment method`() = runTest {
        val interceptor = DefaultIntentConfirmationInterceptor(
            stripeRepository = object : AbsFakeStripeRepository() {},
            publishableKeyProvider = { "pk" },
            stripeAccountIdProvider = { null },
            confirmCallback = null,
        )

        val nextStep = interceptor.intercept(
            clientSecret = null,
            paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
            shippingValues = null,
            setupForFutureUsage = null,
        )

        assertThat(nextStep).isInstanceOf(IntentConfirmationInterceptor.NextStep.Fail::class.java)
    }

    @Test
    fun `Fails if invoked without a confirm callback for new payment method`() = runTest {
        val interceptor = DefaultIntentConfirmationInterceptor(
            stripeRepository = object : AbsFakeStripeRepository() {},
            publishableKeyProvider = { "pk" },
            stripeAccountIdProvider = { null },
            confirmCallback = null,
        )

        val nextStep = interceptor.intercept(
            clientSecret = null,
            paymentMethodCreateParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
            shippingValues = null,
            setupForFutureUsage = null,
        )

        assertThat(nextStep).isInstanceOf(IntentConfirmationInterceptor.NextStep.Fail::class.java)
    }

    @Test
    fun `Fails if creating payment method did not succeed`() = runTest {
        val interceptor = DefaultIntentConfirmationInterceptor(
            stripeRepository = object : AbsFakeStripeRepository() {
                override suspend fun createPaymentMethod(
                    paymentMethodCreateParams: PaymentMethodCreateParams,
                    options: ApiRequest.Options
                ): PaymentMethod? {
                    throw APIException(
                        requestId = "req_123",
                        statusCode = 500,
                        message = "Whoopsie",
                    )
                }
            },
            publishableKeyProvider = { "pk" },
            stripeAccountIdProvider = { null },
            confirmCallback = null,
        )

        val nextStep = interceptor.intercept(
            clientSecret = null,
            paymentMethodCreateParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
            shippingValues = null,
            setupForFutureUsage = null,
        )

        assertThat(nextStep).isInstanceOf(IntentConfirmationInterceptor.NextStep.Fail::class.java)
    }

    @Test
    fun `Fails if retrieving intent did not succeed`() = runTest {
        val interceptor = DefaultIntentConfirmationInterceptor(
            stripeRepository = object : AbsFakeStripeRepository() {
                override suspend fun retrieveStripeIntent(
                    clientSecret: String,
                    options: ApiRequest.Options,
                    expandFields: List<String>
                ): StripeIntent {
                    throw APIException(
                        requestId = "req_123",
                        statusCode = 500,
                        message = "Whoopsie",
                    )
                }
            },
            publishableKeyProvider = { "pk" },
            stripeAccountIdProvider = { null },
            confirmCallback = null,
        )

        val nextStep = interceptor.intercept(
            clientSecret = null,
            paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
            shippingValues = null,
            setupForFutureUsage = null,
        )

        assertThat(nextStep).isInstanceOf(IntentConfirmationInterceptor.NextStep.Fail::class.java)
    }

    @Test
    fun `Fails if client-side confirm callback returns failure`() = runTest {
        val interceptor = DefaultIntentConfirmationInterceptor(
            stripeRepository = mock(),
            publishableKeyProvider = { "pk" },
            stripeAccountIdProvider = { null },
            confirmCallback = failingClientSideCallback(),
        )

        val nextStep = interceptor.intercept(
            clientSecret = null,
            paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
            shippingValues = null,
            setupForFutureUsage = null,
        )

        assertThat(nextStep).isInstanceOf(IntentConfirmationInterceptor.NextStep.Fail::class.java)
    }

    @Test
    fun `Fails if server-side confirm callback returns failure`() = runTest {
        val interceptor = DefaultIntentConfirmationInterceptor(
            stripeRepository = mock(),
            publishableKeyProvider = { "pk" },
            stripeAccountIdProvider = { null },
            confirmCallback = failingServerSideCallback(),
        )

        val nextStep = interceptor.intercept(
            clientSecret = null,
            paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
            shippingValues = null,
            setupForFutureUsage = null,
        )

        assertThat(nextStep).isInstanceOf(IntentConfirmationInterceptor.NextStep.Fail::class.java)
    }

    @Test
    fun `Returns confirm as next step after creating an unconfirmed intent`() = runTest {
        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        val expectedPaymentMethodId = requireNotNull(paymentMethod.id)

        val interceptor = DefaultIntentConfirmationInterceptor(
            stripeRepository = mock(),
            publishableKeyProvider = { "pk" },
            stripeAccountIdProvider = { null },
            confirmCallback = succeedingClientSideCallback(expectedPaymentMethodId),
        )

        val nextStep = interceptor.intercept(
            clientSecret = null,
            paymentMethod = paymentMethod,
            shippingValues = null,
            setupForFutureUsage = null,
        )

        assertThat(nextStep).isInstanceOf(IntentConfirmationInterceptor.NextStep.Confirm::class.java)
    }

    @Test
    fun `Returns complete as next step after creating and confirming a succeeded intent`() = runTest {
        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        val expectedPaymentMethodId = requireNotNull(paymentMethod.id)

        val interceptor = DefaultIntentConfirmationInterceptor(
            stripeRepository = object : AbsFakeStripeRepository() {
                override suspend fun retrieveStripeIntent(
                    clientSecret: String,
                    options: ApiRequest.Options,
                    expandFields: List<String>
                ): StripeIntent {
                    return PaymentIntentFixtures.PI_SUCCEEDED
                }
            },
            publishableKeyProvider = { "pk" },
            stripeAccountIdProvider = { null },
            confirmCallback = succeedingServerSideCallback(expectedPaymentMethodId),
        )

        val nextStep = interceptor.intercept(
            clientSecret = null,
            paymentMethod = paymentMethod,
            shippingValues = null,
            setupForFutureUsage = null,
        )

        assertThat(nextStep).isInstanceOf(IntentConfirmationInterceptor.NextStep.Complete::class.java)
    }

    @Test
    fun `Returns handleNextAction as next step after creating and confirming a non-succeeded intent`() = runTest {
        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        val expectedPaymentMethodId = requireNotNull(paymentMethod.id)

        val interceptor = DefaultIntentConfirmationInterceptor(
            stripeRepository = object : AbsFakeStripeRepository() {
                override suspend fun retrieveStripeIntent(
                    clientSecret: String,
                    options: ApiRequest.Options,
                    expandFields: List<String>
                ): StripeIntent {
                    return PaymentIntentFixtures.PI_SUCCEEDED.copy(
                        status = StripeIntent.Status.RequiresAction,
                    )
                }
            },
            publishableKeyProvider = { "pk" },
            stripeAccountIdProvider = { null },
            confirmCallback = succeedingServerSideCallback(expectedPaymentMethodId),
        )

        val nextStep = interceptor.intercept(
            clientSecret = null,
            paymentMethod = paymentMethod,
            shippingValues = null,
            setupForFutureUsage = null,
        )

        assertThat(nextStep).isEqualTo(
            IntentConfirmationInterceptor.NextStep.HandleNextAction("pi_123_secret_456")
        )
    }

    @Test
    fun `Passes correct shouldSavePaymentMethod to server-side callback`() = runTest {
        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        val observedValues = mutableListOf<Boolean>()

        val interceptor = DefaultIntentConfirmationInterceptor(
            stripeRepository = mock(),
            publishableKeyProvider = { "pk" },
            stripeAccountIdProvider = { null },
            confirmCallback = object : ConfirmCallbackForServerSideConfirmation {
                override suspend fun onConfirmResponse(
                    paymentMethodId: String,
                    shouldSavePaymentMethod: Boolean
                ): ConfirmCallback.Result {
                    observedValues += shouldSavePaymentMethod
                    return ConfirmCallback.Result.Success("pi_123_secret_456")
                }
            },
        )

        val inputs = listOf(
            null,
            ConfirmPaymentIntentParams.SetupFutureUsage.Blank,
            ConfirmPaymentIntentParams.SetupFutureUsage.OffSession,
            ConfirmPaymentIntentParams.SetupFutureUsage.OnSession,
        )

        for (input in inputs) {
            interceptor.intercept(
                clientSecret = null,
                paymentMethod = paymentMethod,
                shippingValues = null,
                setupForFutureUsage = input,
            )
        }

        assertThat(observedValues).containsExactly(false, false, true, false).inOrder()
    }

    private fun succeedingClientSideCallback(
        expectedPaymentMethodId: String,
    ): ConfirmCallbackForClientSideConfirmation {
        return ConfirmCallbackForClientSideConfirmation { paymentMethodId ->
            assertThat(paymentMethodId).isEqualTo(expectedPaymentMethodId)
            ConfirmCallback.Result.Success(clientSecret = "pi_123_secret_456")
        }
    }

    private fun succeedingServerSideCallback(
        expectedPaymentMethodId: String,
    ): ConfirmCallbackForServerSideConfirmation {
        return ConfirmCallbackForServerSideConfirmation { paymentMethodId, _ ->
            assertThat(paymentMethodId).isEqualTo(expectedPaymentMethodId)
            ConfirmCallback.Result.Success(clientSecret = "pi_123_secret_456")
        }
    }

    private fun failingClientSideCallback(): ConfirmCallbackForClientSideConfirmation {
        return ConfirmCallbackForClientSideConfirmation { _ ->
            ConfirmCallback.Result.Failure(
                error = IllegalStateException("that didn't work…")
            )
        }
    }

    private fun failingServerSideCallback(): ConfirmCallbackForServerSideConfirmation {
        return ConfirmCallbackForServerSideConfirmation { _, _ ->
            ConfirmCallback.Result.Failure(
                error = IllegalStateException("that didn't work…")
            )
        }
    }
}
