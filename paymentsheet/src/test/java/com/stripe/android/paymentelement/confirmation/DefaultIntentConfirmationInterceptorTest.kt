package com.stripe.android.paymentelement.confirmation

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.isInstanceOf
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.model.StripeIntent
import com.stripe.android.networking.StripeRepository
import com.stripe.android.paymentelement.confirmation.intent.CreateIntentCallbackFailureException
import com.stripe.android.paymentelement.confirmation.intent.DefaultIntentConfirmationInterceptor
import com.stripe.android.paymentelement.confirmation.intent.IntentConfirmationInterceptor
import com.stripe.android.paymentelement.confirmation.intent.InvalidDeferredIntentUsageException
import com.stripe.android.paymentelement.confirmation.intent.intercept
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.CreateIntentCallback
import com.stripe.android.paymentsheet.CreateIntentResult
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.state.PaymentElementLoader.InitializationMode
import com.stripe.android.testing.AbsFakeStripeRepository
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.android.testing.PaymentIntentFactory
import com.stripe.android.testing.PaymentMethodFactory
import kotlinx.coroutines.async
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import java.util.Objects
import com.stripe.android.R as PaymentsCoreR

@RunWith(RobolectricTestRunner::class)
class DefaultIntentConfirmationInterceptorTest {
    @Test
    fun `Returns confirm as next step if invoked with client secret for existing payment method`() = runTest {
        val interceptor = DefaultIntentConfirmationInterceptor(
            stripeRepository = object : AbsFakeStripeRepository() {},
            publishableKeyProvider = { "pk" },
            stripeAccountIdProvider = { null },
            errorReporter = FakeErrorReporter(),
            allowsManualConfirmation = false,
            intentCreationCallbackProvider = {
                null
            },
        )

        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD

        val nextStep = interceptor.intercept(
            initializationMode = InitializationMode.PaymentIntent("pi_1234_secret_4321"),
            intent = PaymentIntentFactory.create(),
            paymentMethod = paymentMethod,
            paymentMethodOptionsParams = null,
            shippingValues = null,
            paymentMethodExtraParams = null,
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
            errorReporter = FakeErrorReporter(),
            allowsManualConfirmation = false,
            intentCreationCallbackProvider = {
                null
            },
        )

        val createParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD

        val nextStep = interceptor.intercept(
            initializationMode = InitializationMode.PaymentIntent("pi_1234_secret_4321"),
            intent = PaymentIntentFactory.create(),
            paymentMethodCreateParams = createParams,
            shippingValues = null,
            customerRequestedSave = false,
        )

        val confirmNextStep = nextStep as? IntentConfirmationInterceptor.NextStep.Confirm
        val confirmParams = confirmNextStep?.confirmParams as? ConfirmPaymentIntentParams

        assertThat(confirmParams?.paymentMethodId).isNull()
        assertThat(confirmParams?.paymentMethodCreateParams).isEqualTo(createParams)
    }

    @Test
    fun `Returns confirm params with 'setup_future_usage' set to 'off_session' when requires save on confirmation`() =
        runTest {
            val interceptor = createIntentConfirmationInterceptor()

            val nextStep = interceptor.intercept(
                confirmationOption = PaymentMethodConfirmationOption.Saved(
                    paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
                    optionsParams = PaymentMethodOptionsParams.Card(
                        setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.OffSession
                    )
                ),
                intent = PaymentIntentFactory.create(),
                initializationMode = InitializationMode.PaymentIntent("pi_1234_secret_4321"),
                shippingDetails = null,
            )

            val confirmNextStep = nextStep as? IntentConfirmationInterceptor.NextStep.Confirm
            val confirmParams = confirmNextStep?.confirmParams as? ConfirmPaymentIntentParams

            assertThat(
                confirmParams?.paymentMethodOptions
            ).isEqualTo(
                PaymentMethodOptionsParams.Card(
                    setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.OffSession
                )
            )
        }

    @Test
    fun `Fails if invoked without a confirm callback for existing payment method`() = testNoConfirmCallback(
        userMessage = CREATE_INTENT_CALLBACK_MESSAGE.resolvableString,
    ) { errorReporter ->
        val interceptor = DefaultIntentConfirmationInterceptor(
            stripeRepository = object : AbsFakeStripeRepository() {},
            publishableKeyProvider = { "pk_test_123" },
            stripeAccountIdProvider = { null },
            errorReporter = errorReporter,
            allowsManualConfirmation = false,
            intentCreationCallbackProvider = {
                null
            },
        )

        interceptor.intercept(
            initializationMode = InitializationMode.DeferredIntent(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 1099L,
                        currency = "usd",
                    ),
                ),
            ),
            intent = PaymentIntentFactory.create(),
            paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
            paymentMethodOptionsParams = null,
            shippingValues = null,
            paymentMethodExtraParams = null,
        )
    }

    @Test
    fun `Fails if invoked without a confirm callback for new payment method`() = testNoConfirmCallback(
        userMessage = CREATE_INTENT_CALLBACK_MESSAGE.resolvableString,
    ) { errorReporter ->
        val interceptor = DefaultIntentConfirmationInterceptor(
            stripeRepository = object : AbsFakeStripeRepository() {
                override suspend fun createPaymentMethod(
                    paymentMethodCreateParams: PaymentMethodCreateParams,
                    options: ApiRequest.Options
                ): Result<PaymentMethod> {
                    return Result.success(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
                }
            },
            publishableKeyProvider = { "pk_test_123" },
            stripeAccountIdProvider = { null },
            errorReporter = errorReporter,
            allowsManualConfirmation = false,
            intentCreationCallbackProvider = {
                null
            },
        )

        interceptor.intercept(
            initializationMode = InitializationMode.DeferredIntent(mock()),
            intent = PaymentIntentFactory.create(),
            paymentMethodCreateParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
            shippingValues = null,
            customerRequestedSave = false,
        )
    }

    @Test
    fun `Message for live key when error without confirm callback is user friendly`() = testNoConfirmCallback(
        userMessage = PaymentsCoreR.string.stripe_internal_error.resolvableString
    ) { errorReporter ->
        val interceptor = DefaultIntentConfirmationInterceptor(
            stripeRepository = object : AbsFakeStripeRepository() {},
            publishableKeyProvider = { "pk_live_123" },
            stripeAccountIdProvider = { null },
            errorReporter = errorReporter,
            allowsManualConfirmation = false,
            intentCreationCallbackProvider = {
                null
            },
        )

        interceptor.intercept(
            initializationMode = InitializationMode.DeferredIntent(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 1099L,
                        currency = "usd",
                    ),
                ),
            ),
            intent = PaymentIntentFactory.create(),
            paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
            paymentMethodOptionsParams = null,
            paymentMethodExtraParams = null,
            shippingValues = null,
        )
    }

    @Test
    fun `Succeeds if callback is found before timeout time`() {
        val dispatcher = StandardTestDispatcher()
        var callback: CreateIntentCallback? = null

        runTest(dispatcher) {
            val errorReporter = FakeErrorReporter()
            val paymentMethod = PaymentMethodFactory.card()
            val interceptor = DefaultIntentConfirmationInterceptor(
                stripeRepository = object : AbsFakeStripeRepository() {
                    override suspend fun createPaymentMethod(
                        paymentMethodCreateParams: PaymentMethodCreateParams,
                        options: ApiRequest.Options
                    ): Result<PaymentMethod> {
                        return Result.success(paymentMethod)
                    }

                    override suspend fun retrieveStripeIntent(
                        clientSecret: String,
                        options: ApiRequest.Options,
                        expandFields: List<String>
                    ): Result<StripeIntent> {
                        return Result.success(PaymentIntentFixtures.PI_SUCCEEDED)
                    }
                },
                publishableKeyProvider = { "pk_live_123" },
                stripeAccountIdProvider = { null },
                errorReporter = errorReporter,
                allowsManualConfirmation = false,
                intentCreationCallbackProvider = {
                    callback
                },
            )

            val interceptJob = async {
                interceptor.intercept(
                    initializationMode = InitializationMode.DeferredIntent(
                        intentConfiguration = PaymentSheet.IntentConfiguration(
                            mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                                amount = 1099L,
                                currency = "usd",
                            ),
                        ),
                    ),
                    intent = PaymentIntentFactory.create(),
                    paymentMethod = paymentMethod,
                    paymentMethodOptionsParams = null,
                    paymentMethodExtraParams = null,
                    shippingValues = null,
                )
            }

            dispatcher.scheduler.advanceTimeBy(1000)
            assertThat(interceptJob.isActive).isTrue()

            callback = succeedingCreateIntentCallback(paymentMethod)

            dispatcher.scheduler.advanceTimeBy(1001)

            assertThat(interceptJob.isActive).isFalse()
            assertThat(interceptJob.isCompleted).isTrue()

            val nextStep = interceptJob.await()

            assertThat(nextStep).isInstanceOf<IntentConfirmationInterceptor.NextStep.Complete>()

            assertThat(errorReporter.getLoggedErrors()).containsExactly(
                ErrorReporter.SuccessEvent.FOUND_CREATE_INTENT_CALLBACK_WHILE_POLLING.eventName,
            )
        }
    }

    @Test
    fun `Fails if creating payment method did not succeed`() = runTest {
        val apiException = APIException(
            requestId = "req_123",
            statusCode = 500,
            message = "Whoopsie",
        )

        val interceptor = DefaultIntentConfirmationInterceptor(
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
            errorReporter = FakeErrorReporter(),
            allowsManualConfirmation = false,
            intentCreationCallbackProvider = {
                null
            },
        )

        val nextStep = interceptor.intercept(
            initializationMode = InitializationMode.DeferredIntent(mock()),
            intent = PaymentIntentFactory.create(),
            paymentMethodCreateParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
            shippingValues = null,
            customerRequestedSave = false,
        )

        assertThat(nextStep).isEqualTo(
            IntentConfirmationInterceptor.NextStep.Fail(
                cause = apiException,
                message = resolvableString(R.string.stripe_something_went_wrong),
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
            errorReporter = FakeErrorReporter(),
            allowsManualConfirmation = false,
            intentCreationCallbackProvider = {
                succeedingCreateIntentCallback(paymentMethod)
            },
        )

        val nextStep = interceptor.intercept(
            initializationMode = InitializationMode.DeferredIntent(mock()),
            intent = PaymentIntentFactory.create(),
            paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
            paymentMethodOptionsParams = null,
            paymentMethodExtraParams = null,
            shippingValues = null,
        )

        assertThat(nextStep).isEqualTo(
            IntentConfirmationInterceptor.NextStep.Fail(
                cause = apiException,
                message = resolvableString(R.string.stripe_something_went_wrong),
            )
        )
    }

    @Test
    fun `Fails if callback returns failure with custom error message`() = runTest {
        val interceptor = DefaultIntentConfirmationInterceptor(
            stripeRepository = mock(),
            publishableKeyProvider = { "pk" },
            stripeAccountIdProvider = { null },
            errorReporter = FakeErrorReporter(),
            allowsManualConfirmation = false,
            intentCreationCallbackProvider = {
                failingCreateIntentCallback(
                    message = "that didn't work…"
                )
            },
        )

        val nextStep = interceptor.intercept(
            initializationMode = InitializationMode.DeferredIntent(mock()),
            intent = PaymentIntentFactory.create(),
            paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
            paymentMethodOptionsParams = null,
            paymentMethodExtraParams = null,
            shippingValues = null,
        )

        assertThat(nextStep).isEqualTo(
            IntentConfirmationInterceptor.NextStep.Fail(
                cause = CreateIntentCallbackFailureException(TestException("that didn't work…")),
                message = resolvableString("that didn't work…"),
            )
        )
    }

    @Test
    fun `Fails if callback returns failure without custom error message`() = runTest {
        val interceptor = DefaultIntentConfirmationInterceptor(
            stripeRepository = mock(),
            publishableKeyProvider = { "pk" },
            stripeAccountIdProvider = { null },
            errorReporter = FakeErrorReporter(),
            allowsManualConfirmation = false,
            intentCreationCallbackProvider = {
                failingCreateIntentCallback()
            },
        )

        val nextStep = interceptor.intercept(
            initializationMode = InitializationMode.DeferredIntent(mock()),
            intent = PaymentIntentFactory.create(),
            paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
            paymentMethodOptionsParams = null,
            paymentMethodExtraParams = null,
            shippingValues = null,
        )

        assertThat(nextStep).isEqualTo(
            IntentConfirmationInterceptor.NextStep.Fail(
                cause = CreateIntentCallbackFailureException(TestException()),
                message = resolvableString(R.string.stripe_something_went_wrong),
            )
        )
    }

    @Test
    fun `Returns confirm as next step after creating an unconfirmed intent`() = runTest {
        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD

        val interceptor = DefaultIntentConfirmationInterceptor(
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
            errorReporter = FakeErrorReporter(),
            allowsManualConfirmation = false,
            intentCreationCallbackProvider = {
                succeedingCreateIntentCallback(paymentMethod)
            }
        )

        val nextStep = interceptor.intercept(
            initializationMode = InitializationMode.DeferredIntent(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 1099L,
                        currency = "usd",
                    ),
                ),
            ),
            intent = PaymentIntentFactory.create(),
            paymentMethod = paymentMethod,
            paymentMethodOptionsParams = null,
            paymentMethodExtraParams = null,
            shippingValues = null,
        )

        assertThat(nextStep).isInstanceOf<IntentConfirmationInterceptor.NextStep.Confirm>()
    }

    @Test
    fun `Returns complete as next step after creating and confirming a succeeded intent`() = runTest {
        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD

        val interceptor = DefaultIntentConfirmationInterceptor(
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
            errorReporter = FakeErrorReporter(),
            allowsManualConfirmation = false,
            intentCreationCallbackProvider = {
                succeedingCreateIntentCallback(paymentMethod)
            }
        )

        val nextStep = interceptor.intercept(
            initializationMode = InitializationMode.DeferredIntent(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 1099L,
                        currency = "usd",
                    ),
                ),
            ),
            intent = PaymentIntentFactory.create(),
            paymentMethod = paymentMethod,
            paymentMethodOptionsParams = null,
            paymentMethodExtraParams = null,
            shippingValues = null,
        )

        assertThat(nextStep).isEqualTo(
            IntentConfirmationInterceptor.NextStep.Complete(isForceSuccess = false)
        )
    }

    @Test
    fun `Returns handleNextAction as next step after creating and confirming a non-succeeded intent`() = runTest {
        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD

        val interceptor = DefaultIntentConfirmationInterceptor(
            stripeRepository = object : AbsFakeStripeRepository() {
                override suspend fun retrieveStripeIntent(
                    clientSecret: String,
                    options: ApiRequest.Options,
                    expandFields: List<String>
                ): Result<StripeIntent> {
                    return Result.success(
                        PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2.copy(
                            paymentMethodId = paymentMethod.id,
                            paymentMethod = paymentMethod,
                        )
                    )
                }
            },
            publishableKeyProvider = { "pk" },
            stripeAccountIdProvider = { null },
            errorReporter = FakeErrorReporter(),
            allowsManualConfirmation = false,
            intentCreationCallbackProvider = {
                succeedingCreateIntentCallback(paymentMethod)
            }
        )

        val nextStep = interceptor.intercept(
            initializationMode = InitializationMode.DeferredIntent(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 1099L,
                        currency = "usd",
                    ),
                ),
            ),
            intent = PaymentIntentFactory.create(),
            paymentMethod = paymentMethod,
            paymentMethodOptionsParams = null,
            paymentMethodExtraParams = null,
            shippingValues = null,
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
            errorReporter = FakeErrorReporter(),
            allowsManualConfirmation = false,
            intentCreationCallbackProvider = {
                CreateIntentCallback { _, shouldSavePaymentMethod ->
                    observedValues += shouldSavePaymentMethod
                    CreateIntentResult.Success("pi_123_secret_456")
                }
            },
        )

        val inputs = listOf(true, false)

        for (input in inputs) {
            interceptor.intercept(
                initializationMode = InitializationMode.DeferredIntent(
                    intentConfiguration = PaymentSheet.IntentConfiguration(
                        mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                            amount = 1099L,
                            currency = "usd",
                        ),
                    ),
                ),
                intent = PaymentIntentFactory.create(),
                paymentMethod = paymentMethod,
                paymentMethodOptionsParams = PaymentMethodOptionsParams.Card(
                    setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.OffSession
                ).takeIf { input },
                paymentMethodExtraParams = null,
                shippingValues = null,
            )
        }

        assertThat(observedValues).containsExactly(true, false).inOrder()
    }

    @Test
    fun `Returns success as next step if merchant is forcing success`() = runTest {
        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        val stripeRepository = mock<StripeRepository>()

        val interceptor = DefaultIntentConfirmationInterceptor(
            stripeRepository = stripeRepository,
            publishableKeyProvider = { "pk" },
            stripeAccountIdProvider = { null },
            errorReporter = FakeErrorReporter(),
            allowsManualConfirmation = false,
            intentCreationCallbackProvider = {
                CreateIntentCallback { _, _ ->
                    CreateIntentResult.Success(IntentConfirmationInterceptor.COMPLETE_WITHOUT_CONFIRMING_INTENT)
                }
            },
        )

        val nextStep = interceptor.intercept(
            initializationMode = InitializationMode.DeferredIntent(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 1099L,
                        currency = "usd",
                    ),
                ),
            ),
            intent = PaymentIntentFactory.create(),
            paymentMethod = paymentMethod,
            paymentMethodOptionsParams = null,
            paymentMethodExtraParams = null,
            shippingValues = null,
        )

        verify(stripeRepository, never()).retrieveStripeIntent(any(), any(), any())

        assertThat(nextStep).isEqualTo(
            IntentConfirmationInterceptor.NextStep.Complete(isForceSuccess = true)
        )
    }

    @Test
    fun `If requires next action with an attached payment method different then the created one, throw error`() =
        runTest {
            val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD

            val interceptor = DefaultIntentConfirmationInterceptor(
                stripeRepository = stripeRepositoryReturning(
                    onCreatePaymentMethodId = "pm_1234",
                    onRetrievePaymentMethodId = "pm_5678"
                ),
                publishableKeyProvider = { "pk" },
                stripeAccountIdProvider = { null },
                errorReporter = FakeErrorReporter(),
                allowsManualConfirmation = false,
                intentCreationCallbackProvider = {
                    CreateIntentCallback { _, _ ->
                        CreateIntentResult.Success(clientSecret = "pi_123")
                    }
                },
            )

            val nextStep = interceptor.intercept(
                initializationMode = InitializationMode.DeferredIntent(
                    intentConfiguration = PaymentSheet.IntentConfiguration(
                        mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                            amount = 1099L,
                            currency = "usd",
                        ),
                    ),
                ),
                intent = PaymentIntentFactory.create(),
                paymentMethod = paymentMethod,
                paymentMethodOptionsParams = null,
                paymentMethodExtraParams = null,
                shippingValues = null,
            )

            val failedStep = nextStep.asFail()

            assertThat(failedStep.cause).isInstanceOf(InvalidDeferredIntentUsageException::class.java)
            assertThat(failedStep.message).isEqualTo(
                R.string.stripe_paymentsheet_invalid_deferred_intent_usage.resolvableString
            )
        }

    private fun testNoConfirmCallback(
        userMessage: ResolvableString,
        interceptCall: suspend (errorReporter: ErrorReporter) -> IntentConfirmationInterceptor.NextStep
    ) {
        val errorReporter = FakeErrorReporter()
        val dispatcher = StandardTestDispatcher()

        runTest(dispatcher) {
            val interceptJob = async {
                interceptCall(errorReporter)
            }

            assertThat(interceptJob.isActive).isTrue()

            dispatcher.scheduler.advanceTimeBy(1000)

            assertThat(interceptJob.isActive).isTrue()

            dispatcher.scheduler.advanceTimeBy(1000)

            assertThat(interceptJob.isActive).isTrue()

            dispatcher.scheduler.advanceTimeBy(1)

            assertThat(interceptJob.isActive).isFalse()

            val nextStep = interceptJob.await()

            assertThat(nextStep).isInstanceOf<IntentConfirmationInterceptor.NextStep.Fail>()

            val failedStep = nextStep.asFail()

            assertThat(failedStep.cause).isInstanceOf<IllegalStateException>()
            assertThat(failedStep.cause.message).isEqualTo(CREATE_INTENT_CALLBACK_MESSAGE)
            assertThat(failedStep.message).isEqualTo(userMessage)

            assertThat(errorReporter.getLoggedErrors()).containsExactly(
                ErrorReporter.ExpectedErrorEvent.CREATE_INTENT_CALLBACK_NULL.eventName,
            )
        }
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

    private fun createIntentConfirmationInterceptor(): DefaultIntentConfirmationInterceptor {
        return DefaultIntentConfirmationInterceptor(
            stripeRepository = object : AbsFakeStripeRepository() {},
            publishableKeyProvider = { "pk" },
            stripeAccountIdProvider = { null },
            errorReporter = FakeErrorReporter(),
            allowsManualConfirmation = false,
            intentCreationCallbackProvider = {
                null
            },
        )
    }

    private fun stripeRepositoryReturning(
        onCreatePaymentMethodId: String,
        onRetrievePaymentMethodId: String,
    ): StripeRepository {
        return object : AbsFakeStripeRepository() {
            override suspend fun createPaymentMethod(
                paymentMethodCreateParams: PaymentMethodCreateParams,
                options: ApiRequest.Options
            ): Result<PaymentMethod> {
                return Result.success(
                    PaymentMethodFactory.card(random = true).copy(
                        id = onCreatePaymentMethodId
                    )
                )
            }

            override suspend fun retrieveStripeIntent(
                clientSecret: String,
                options: ApiRequest.Options,
                expandFields: List<String>
            ): Result<StripeIntent> {
                return Result.success(
                    PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2.copy(
                        paymentMethodId = onRetrievePaymentMethodId
                    )
                )
            }
        }
    }

    private fun IntentConfirmationInterceptor.NextStep.asFail(): IntentConfirmationInterceptor.NextStep.Fail {
        return this as IntentConfirmationInterceptor.NextStep.Fail
    }

    private class TestException(message: String? = null) : Exception(message) {

        override fun hashCode(): Int {
            return Objects.hash(message)
        }

        override fun equals(other: Any?): Boolean {
            return other is TestException && other.message == message
        }
    }

    private companion object {
        const val CREATE_INTENT_CALLBACK_MESSAGE =
            "CreateIntentCallback must be implemented when using IntentConfiguration with PaymentSheet"
    }
}
