package com.stripe.android.paymentsheet

import android.content.Context
import androidx.test.core.app.ApplicationProvider
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
import com.stripe.android.networking.StripeRepository
import com.stripe.android.paymentsheet.PaymentSheet.InitializationMode
import com.stripe.android.testing.AbsFakeStripeRepository
import com.stripe.android.utils.IntentConfirmationInterceptorTestRule
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import java.util.Objects
import kotlin.test.assertFailsWith

@RunWith(RobolectricTestRunner::class)
class DefaultIntentConfirmationInterceptorTest {

    @get:Rule
    val intentConfirmationInterceptorTestRule = IntentConfirmationInterceptorTestRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun `Returns confirm as next step if invoked with client secret for existing payment method`() = runTest {
        val interceptor = DefaultIntentConfirmationInterceptor(
            context = context,
            stripeRepository = object : AbsFakeStripeRepository() {},
            publishableKeyProvider = { "pk" },
            stripeAccountIdProvider = { null },
            isFlowController = false,
        )

        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD

        val nextStep = interceptor.intercept(
            initializationMode = InitializationMode.PaymentIntent("pi_1234_secret_4321"),
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
            context = context,
            stripeRepository = object : AbsFakeStripeRepository() {},
            publishableKeyProvider = { "pk" },
            stripeAccountIdProvider = { null },
            isFlowController = false,
        )

        val createParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD

        val nextStep = interceptor.intercept(
            initializationMode = InitializationMode.PaymentIntent("pi_1234_secret_4321"),
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
            context = context,
            stripeRepository = object : AbsFakeStripeRepository() {},
            publishableKeyProvider = { "pk" },
            stripeAccountIdProvider = { null },
            isFlowController = false,
        )

        val error = assertFailsWith<IllegalStateException> {
            interceptor.intercept(
                initializationMode = InitializationMode.DeferredIntent(
                    intentConfiguration = PaymentSheet.IntentConfiguration(
                        mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                            amount = 1099L,
                            currency = "usd",
                        ),
                    ),
                ),
                paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
                shippingValues = null,
                setupForFutureUsage = null,
            )
        }

        assertThat(error.message).isEqualTo(
            "CreateIntentCallback must be implemented when using IntentConfiguration with PaymentSheet"
        )
    }

    @Test
    fun `Fails if invoked without a confirm callback for new payment method`() = runTest {
        val interceptor = DefaultIntentConfirmationInterceptor(
            context = context,
            stripeRepository = object : AbsFakeStripeRepository() {
                override suspend fun createPaymentMethod(
                    paymentMethodCreateParams: PaymentMethodCreateParams,
                    options: ApiRequest.Options
                ): Result<PaymentMethod> {
                    return Result.success(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
                }
            },
            publishableKeyProvider = { "pk" },
            stripeAccountIdProvider = { null },
            isFlowController = false,
        )

        val error = assertFailsWith<IllegalStateException> {
            interceptor.intercept(
                initializationMode = InitializationMode.DeferredIntent(mock()),
                paymentMethodCreateParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
                shippingValues = null,
                setupForFutureUsage = null,
            )
        }

        assertThat(error.message).isEqualTo(
            "CreateIntentCallback must be implemented when using IntentConfiguration with PaymentSheet"
        )
    }

    @Test
    fun `Fails if creating payment method did not succeed`() = runTest {
        val apiException = APIException(
            requestId = "req_123",
            statusCode = 500,
            message = "Whoopsie",
        )

        val interceptor = DefaultIntentConfirmationInterceptor(
            context = context,
            stripeRepository = object : AbsFakeStripeRepository() {
                override suspend fun createPaymentMethod(
                    paymentMethodCreateParams: PaymentMethodCreateParams,
                    options: ApiRequest.Options
                ): Result<PaymentMethod> {
                    return Result.failure(apiException)
                }
            },
            publishableKeyProvider = { "pk" },
            stripeAccountIdProvider = { null },
            isFlowController = false,
        )

        val nextStep = interceptor.intercept(
            initializationMode = InitializationMode.DeferredIntent(mock()),
            paymentMethodCreateParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
            shippingValues = null,
            setupForFutureUsage = null,
        )

        assertThat(nextStep).isEqualTo(
            IntentConfirmationInterceptor.NextStep.Fail(
                cause = apiException,
                message = "Unable to complete operation",
            )
        )
    }

    @Test
    fun `Fails if retrieving intent did not succeed`() = runTest {
        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD

        val apiException = APIException(
            requestId = "req_123",
            statusCode = 500,
            message = "Whoopsie",
        )

        val interceptor = DefaultIntentConfirmationInterceptor(
            context = context,
            stripeRepository = object : AbsFakeStripeRepository() {
                override suspend fun retrieveStripeIntent(
                    clientSecret: String,
                    options: ApiRequest.Options,
                    expandFields: List<String>
                ): Result<StripeIntent> {
                    return Result.failure(apiException)
                }
            },
            publishableKeyProvider = { "pk" },
            stripeAccountIdProvider = { null },
            isFlowController = false,
        )

        IntentConfirmationInterceptor.createIntentCallback = succeedingCreateIntentCallback(paymentMethod)

        val nextStep = interceptor.intercept(
            initializationMode = InitializationMode.DeferredIntent(mock()),
            paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
            shippingValues = null,
            setupForFutureUsage = null,
        )

        assertThat(nextStep).isEqualTo(
            IntentConfirmationInterceptor.NextStep.Fail(
                cause = apiException,
                message = "Unable to complete operation",
            )
        )
    }

    @Test
    fun `Fails if callback returns failure with custom error message`() = runTest {
        val interceptor = DefaultIntentConfirmationInterceptor(
            context = context,
            stripeRepository = mock(),
            publishableKeyProvider = { "pk" },
            stripeAccountIdProvider = { null },
            isFlowController = false,
        )

        IntentConfirmationInterceptor.createIntentCallback = failingCreateIntentCallback(
            message = "that didn't work…"
        )

        val nextStep = interceptor.intercept(
            initializationMode = InitializationMode.DeferredIntent(mock()),
            paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
            shippingValues = null,
            setupForFutureUsage = null,
        )

        assertThat(nextStep).isEqualTo(
            IntentConfirmationInterceptor.NextStep.Fail(
                cause = TestException("that didn't work…"),
                message = "that didn't work…",
            )
        )
    }

    @Test
    fun `Fails if callback returns failure without custom error message`() = runTest {
        val interceptor = DefaultIntentConfirmationInterceptor(
            context = context,
            stripeRepository = mock(),
            publishableKeyProvider = { "pk" },
            stripeAccountIdProvider = { null },
            isFlowController = false,
        )

        IntentConfirmationInterceptor.createIntentCallback = failingCreateIntentCallback()

        val nextStep = interceptor.intercept(
            initializationMode = InitializationMode.DeferredIntent(mock()),
            paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
            shippingValues = null,
            setupForFutureUsage = null,
        )

        assertThat(nextStep).isEqualTo(
            IntentConfirmationInterceptor.NextStep.Fail(
                cause = TestException(),
                message = "Unable to complete operation",
            )
        )
    }

    @Test
    fun `Returns confirm as next step after creating an unconfirmed intent`() = runTest {
        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD

        val interceptor = DefaultIntentConfirmationInterceptor(
            context = context,
            stripeRepository = object : AbsFakeStripeRepository() {
                override suspend fun retrieveStripeIntent(
                    clientSecret: String,
                    options: ApiRequest.Options,
                    expandFields: List<String>,
                ): Result<StripeIntent> {
                    return Result.success(
                        PaymentIntentFixtures.PI_SUCCEEDED.copy(
                            status = StripeIntent.Status.RequiresConfirmation,
                        )
                    )
                }
            },
            publishableKeyProvider = { "pk" },
            stripeAccountIdProvider = { null },
            isFlowController = false,
        )

        IntentConfirmationInterceptor.createIntentCallback = succeedingCreateIntentCallback(paymentMethod)

        val nextStep = interceptor.intercept(
            initializationMode = InitializationMode.DeferredIntent(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 1099L,
                        currency = "usd",
                    ),
                ),
            ),
            paymentMethod = paymentMethod,
            shippingValues = null,
            setupForFutureUsage = null,
        )

        assertThat(nextStep).isInstanceOf(IntentConfirmationInterceptor.NextStep.Confirm::class.java)
    }

    @Test
    fun `Returns complete as next step after creating and confirming a succeeded intent`() = runTest {
        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD

        val interceptor = DefaultIntentConfirmationInterceptor(
            context = context,
            stripeRepository = object : AbsFakeStripeRepository() {
                override suspend fun retrieveStripeIntent(
                    clientSecret: String,
                    options: ApiRequest.Options,
                    expandFields: List<String>
                ): Result<StripeIntent> {
                    return Result.success(PaymentIntentFixtures.PI_SUCCEEDED)
                }
            },
            publishableKeyProvider = { "pk" },
            stripeAccountIdProvider = { null },
            isFlowController = false,
        )

        IntentConfirmationInterceptor.createIntentCallback = succeedingCreateIntentCallback(paymentMethod)

        val nextStep = interceptor.intercept(
            initializationMode = InitializationMode.DeferredIntent(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 1099L,
                        currency = "usd",
                    ),
                ),
            ),
            paymentMethod = paymentMethod,
            shippingValues = null,
            setupForFutureUsage = null,
        )

        assertThat(nextStep).isEqualTo(
            IntentConfirmationInterceptor.NextStep.Complete(isForceSuccess = false)
        )
    }

    @Test
    fun `Returns handleNextAction as next step after creating and confirming a non-succeeded intent`() = runTest {
        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD

        val interceptor = DefaultIntentConfirmationInterceptor(
            context = context,
            stripeRepository = object : AbsFakeStripeRepository() {
                override suspend fun retrieveStripeIntent(
                    clientSecret: String,
                    options: ApiRequest.Options,
                    expandFields: List<String>
                ): Result<StripeIntent> {
                    return Result.success(
                        PaymentIntentFixtures.PI_SUCCEEDED.copy(
                            status = StripeIntent.Status.RequiresAction,
                        )
                    )
                }
            },
            publishableKeyProvider = { "pk" },
            stripeAccountIdProvider = { null },
            isFlowController = false,
        )

        IntentConfirmationInterceptor.createIntentCallback = succeedingCreateIntentCallback(paymentMethod)

        val nextStep = interceptor.intercept(
            initializationMode = InitializationMode.DeferredIntent(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 1099L,
                        currency = "usd",
                    ),
                ),
            ),
            paymentMethod = paymentMethod,
            shippingValues = null,
            setupForFutureUsage = null,
        )

        assertThat(nextStep).isEqualTo(
            IntentConfirmationInterceptor.NextStep.HandleNextAction("pi_123_secret_456")
        )
    }

    @Test
    fun `Passes correct shouldSavePaymentMethod to CreateIntentCallback`() = runTest {
        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        val observedValues = mutableListOf<Boolean>()

        val interceptor = DefaultIntentConfirmationInterceptor(
            context = context,
            stripeRepository = object : AbsFakeStripeRepository() {
                override suspend fun retrieveStripeIntent(
                    clientSecret: String,
                    options: ApiRequest.Options,
                    expandFields: List<String>
                ): Result<StripeIntent> {
                    return Result.success(PaymentIntentFixtures.PI_SUCCEEDED)
                }
            },
            publishableKeyProvider = { "pk" },
            stripeAccountIdProvider = { null },
            isFlowController = false,
        )

        val inputs = listOf(
            null,
            ConfirmPaymentIntentParams.SetupFutureUsage.Blank,
            ConfirmPaymentIntentParams.SetupFutureUsage.OffSession,
            ConfirmPaymentIntentParams.SetupFutureUsage.OnSession,
        )

        for (input in inputs) {
            IntentConfirmationInterceptor.createIntentCallback =
                CreateIntentCallback { _, shouldSavePaymentMethod ->
                    observedValues += shouldSavePaymentMethod
                    CreateIntentResult.Success("pi_123_secret_456")
                }

            interceptor.intercept(
                initializationMode = InitializationMode.DeferredIntent(
                    intentConfiguration = PaymentSheet.IntentConfiguration(
                        mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                            amount = 1099L,
                            currency = "usd",
                        ),
                    ),
                ),
                paymentMethod = paymentMethod,
                shippingValues = null,
                setupForFutureUsage = input,
            )
        }

        assertThat(observedValues).containsExactly(false, false, true, false).inOrder()
    }

    @Test
    fun `Returns success as next step if merchant is forcing success`() = runTest {
        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        val stripeRepository = mock<StripeRepository>()

        val interceptor = DefaultIntentConfirmationInterceptor(
            context = context,
            stripeRepository = stripeRepository,
            publishableKeyProvider = { "pk" },
            stripeAccountIdProvider = { null },
            isFlowController = false,
        )

        IntentConfirmationInterceptor.createIntentCallback = CreateIntentCallback { _, _ ->
            CreateIntentResult.Success(IntentConfirmationInterceptor.COMPLETE_WITHOUT_CONFIRMING_INTENT)
        }

        val nextStep = interceptor.intercept(
            initializationMode = InitializationMode.DeferredIntent(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 1099L,
                        currency = "usd",
                    ),
                ),
            ),
            paymentMethod = paymentMethod,
            shippingValues = null,
            setupForFutureUsage = null,
        )

        verify(stripeRepository, never()).retrieveStripeIntent(any(), any(), any())

        assertThat(nextStep).isEqualTo(
            IntentConfirmationInterceptor.NextStep.Complete(isForceSuccess = true)
        )
    }

    private fun succeedingCreateIntentCallback(
        expectedPaymentMethod: PaymentMethod,
    ): CreateIntentCallback {
        return CreateIntentCallback { paymentMethod, _ ->
            assertThat(paymentMethod).isEqualTo(expectedPaymentMethod)
            CreateIntentResult.Success(clientSecret = "pi_123_secret_456")
        }
    }

    private fun failingCreateIntentCallback(
        message: String? = null
    ): CreateIntentCallback {
        return CreateIntentCallback { _, _ ->
            CreateIntentResult.Failure(
                cause = TestException(message),
                displayMessage = message
            )
        }
    }

    private class TestException(message: String? = null) : Exception(message) {

        override fun hashCode(): Int {
            return Objects.hash(message)
        }

        override fun equals(other: Any?): Boolean {
            return other is TestException && other.message == message
        }
    }
}
