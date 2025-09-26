package com.stripe.android.paymentelement.confirmation

import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.SharedPaymentTokenSessionPreview
import com.stripe.android.core.StripeError
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.exception.InvalidRequestException
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.isInstanceOf
import com.stripe.android.model.Address
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.ConfirmStripeIntentParams
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.model.RadarOptions
import com.stripe.android.model.RadarSessionWithHCaptcha
import com.stripe.android.model.StripeIntent
import com.stripe.android.networking.StripeRepository
import com.stripe.android.paymentelement.PaymentMethodOptionsSetupFutureUsagePreview
import com.stripe.android.paymentelement.PreparePaymentMethodHandler
import com.stripe.android.paymentelement.confirmation.intent.CreateIntentCallbackFailureException
import com.stripe.android.paymentelement.confirmation.intent.DefaultIntentConfirmationInterceptorFactory
import com.stripe.android.paymentelement.confirmation.intent.DeferredIntentConfirmationType
import com.stripe.android.paymentelement.confirmation.intent.IntentConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.intent.IntentConfirmationInterceptor
import com.stripe.android.paymentelement.confirmation.intent.InvalidDeferredIntentUsageException
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.CreateIntentCallback
import com.stripe.android.paymentsheet.CreateIntentResult
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.state.PaymentElementLoader.InitializationMode
import com.stripe.android.testing.AbsFakeStripeRepository
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.android.testing.PaymentIntentFactory
import com.stripe.android.testing.PaymentMethodFactory
import com.stripe.android.testing.SetupIntentFactory
import kotlinx.coroutines.CompletableDeferred
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
import javax.inject.Provider
import com.stripe.android.R as PaymentsCoreR

@OptIn(SharedPaymentTokenSessionPreview::class)
@RunWith(RobolectricTestRunner::class)
class DefaultIntentConfirmationInterceptorTest {
    @Test
    fun `Returns confirm as next step if invoked with client secret for existing payment method`() =
        runInterceptorScenario(
            initializationMode = InitializationMode.PaymentIntent("pi_1234_secret_4321"),
        ) { interceptor ->

            val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
            val nextStep = interceptor.intercept(
                intent = PaymentIntentFactory.create(),
                confirmationOption = PaymentMethodConfirmationOption.Saved(
                    paymentMethod = paymentMethod,
                    optionsParams = null,
                    passiveCaptchaParams = null,
                    hCaptchaToken = null,
                ),
                shippingValues = null,
            )

            val confirmParams = nextStep.asConfirmParams<ConfirmPaymentIntentParams>()

            assertThat(confirmParams?.paymentMethodId).isEqualTo(paymentMethod.id)
            assertThat(confirmParams?.paymentMethodCreateParams).isNull()
        }

    @Test
    fun `Returns confirm as next step if invoked with client secret for new payment method`() =
        runInterceptorScenario(
            initializationMode = InitializationMode.PaymentIntent("pi_1234_secret_4321"),
        ) { interceptor ->

            val createParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD
            val nextStep = interceptor.intercept(
                intent = PaymentIntentFactory.create(),
                confirmationOption = PaymentMethodConfirmationOption.New(
                    createParams = createParams,
                    optionsParams = null,
                    extraParams = null,
                    shouldSave = false,
                    passiveCaptchaParams = null
                ),
                shippingValues = null,
            )
            val confirmParams = nextStep.asConfirmParams<ConfirmPaymentIntentParams>()

            assertThat(confirmParams?.paymentMethodId).isNull()
            assertThat(confirmParams?.paymentMethodCreateParams).isEqualTo(createParams)
        }

    @Test
    fun `Returns confirm params with 'setup_future_usage' set to 'off_session' when requires save on confirmation`() =
        runInterceptorScenario(
            initializationMode = InitializationMode.PaymentIntent("pi_1234_secret_4321"),
        ) { interceptor ->
            val nextStep = interceptor.intercept(
                confirmationOption = PaymentMethodConfirmationOption.Saved(
                    paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
                    optionsParams = PaymentMethodOptionsParams.Card(
                        setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.OffSession
                    ),
                    passiveCaptchaParams = null
                ),
                intent = PaymentIntentFactory.create(),
                shippingValues = null,
            )

            val confirmParams = nextStep.asConfirmParams<ConfirmPaymentIntentParams>()

            assertThat(
                confirmParams?.paymentMethodOptions
            ).isEqualTo(
                PaymentMethodOptionsParams.Card(
                    setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.OffSession
                )
            )
        }

    @Test
    fun `Fails if invoked without a confirm callback for existing payment method`() = testNoProvider(
        event = ErrorReporter.ExpectedErrorEvent.CREATE_INTENT_CALLBACK_NULL,
        failureMessage = CREATE_INTENT_CALLBACK_MESSAGE,
        userMessage = CREATE_INTENT_CALLBACK_MESSAGE.resolvableString,
    ) { errorReporter ->
        val interceptor = createIntentConfirmationInterceptor(
            initializationMode = InitializationMode.DeferredIntent(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 1099L,
                        currency = "usd",
                    ),
                ),
            ),
            publishableKeyProvider = { "pk_test_123" },
            errorReporter = errorReporter,
        )

        interceptor.interceptDefaultSavedPaymentMethod()
    }

    @Test
    fun `Fails if invoked without a confirm callback for new payment method`() = testNoProvider(
        event = ErrorReporter.ExpectedErrorEvent.CREATE_INTENT_CALLBACK_NULL,
        failureMessage = CREATE_INTENT_CALLBACK_MESSAGE,
        userMessage = CREATE_INTENT_CALLBACK_MESSAGE.resolvableString,
    ) { errorReporter ->
        val interceptor = createIntentConfirmationInterceptor(
            initializationMode = InitializationMode.DeferredIntent(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 1099L,
                        currency = "usd",
                    ),
                ),
            ),
            stripeRepository = object : AbsFakeStripeRepository() {
                override suspend fun createPaymentMethod(
                    paymentMethodCreateParams: PaymentMethodCreateParams,
                    options: ApiRequest.Options
                ): Result<PaymentMethod> {
                    return Result.success(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
                }
            },
            publishableKeyProvider = { "pk_test_123" },
            errorReporter = errorReporter,
        )

        interceptor.interceptDefaultNewPaymentMethod()
    }

    @Test
    fun `Message for live key when error without confirm callback is user friendly`() = testNoProvider(
        event = ErrorReporter.ExpectedErrorEvent.CREATE_INTENT_CALLBACK_NULL,
        failureMessage = CREATE_INTENT_CALLBACK_MESSAGE,
        userMessage = PaymentsCoreR.string.stripe_internal_error.resolvableString,
    ) { errorReporter ->
        val interceptor = createIntentConfirmationInterceptor(
            initializationMode = InitializationMode.DeferredIntent(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 1099L,
                        currency = "usd",
                    ),
                ),
            ),
            publishableKeyProvider = { "pk_live_123" },
            errorReporter = errorReporter,
        )

        interceptor.interceptDefaultSavedPaymentMethod()
    }

    @Test
    fun `Succeeds if callback is found before timeout time`() {
        val dispatcher = StandardTestDispatcher()
        var callback: CreateIntentCallback? = null

        runTest(dispatcher) {
            val errorReporter = FakeErrorReporter()
            val paymentMethod = PaymentMethodFactory.card()
            val interceptor = createIntentConfirmationInterceptor(
                initializationMode = InitializationMode.DeferredIntent(
                    intentConfiguration = PaymentSheet.IntentConfiguration(
                        mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                            amount = 1099L,
                            currency = "usd",
                        ),
                    ),
                ),
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
                errorReporter = errorReporter,
                intentCreationCallbackProvider = {
                    callback
                },
            )

            val interceptJob = async {
                interceptor.intercept(
                    intent = PaymentIntentFactory.create(),
                    confirmationOption = PaymentMethodConfirmationOption.Saved(
                        paymentMethod = paymentMethod,
                        optionsParams = null,
                        passiveCaptchaParams = null,
                        hCaptchaToken = null,
                    ),
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

            assertThat(nextStep).isInstanceOf<
                ConfirmationDefinition.Action.Complete<IntentConfirmationDefinition.Args>
                >()

            assertThat(errorReporter.getLoggedErrors()).containsExactly(
                ErrorReporter.SuccessEvent.FOUND_CREATE_INTENT_CALLBACK_WHILE_POLLING.eventName,
            )
        }
    }

    @Test
    fun `Fails if creating payment method did not succeed`() = runTest {
        val invalidRequestException = InvalidRequestException(
            stripeError = StripeError(
                type = "card_error",
                message = "Your card is not supported.",
                code = "card_declined",
            ),
            requestId = "req_123",
            statusCode = 400,
        )

        val interceptor = createIntentConfirmationInterceptor(
            initializationMode = InitializationMode.DeferredIntent(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 1099L,
                        currency = "usd",
                    ),
                ),
            ),
            stripeRepository = object : AbsFakeStripeRepository() {
                override suspend fun createPaymentMethod(
                    paymentMethodCreateParams: PaymentMethodCreateParams,
                    options: ApiRequest.Options
                ): Result<PaymentMethod> {
                    return Result.failure(invalidRequestException)
                }
            },
        )

        val nextStep = interceptor.intercept(
            intent = PaymentIntentFactory.create(),
            confirmationOption = PaymentMethodConfirmationOption.New(
                createParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
                optionsParams = null,
                extraParams = null,
                shouldSave = false,
                passiveCaptchaParams = null
            ),
            shippingValues = null,
        )

        assertThat(nextStep).isEqualTo(
            ConfirmationDefinition.Action.Fail<IntentConfirmationDefinition.Args>(
                cause = invalidRequestException,
                message = "Your card is not supported.".resolvableString,
                errorType = ConfirmationHandler.Result.Failed.ErrorType.Payment,
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

        val interceptor = createIntentConfirmationInterceptor(
            initializationMode = InitializationMode.DeferredIntent(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 1099L,
                        currency = "usd",
                    ),
                ),
            ),
            stripeRepository = object : AbsFakeStripeRepository() {
                override suspend fun retrieveStripeIntent(
                    clientSecret: String,
                    options: ApiRequest.Options,
                    expandFields: List<String>
                ): Result<StripeIntent> {
                    return Result.failure(apiException)
                }
            },
            intentCreationCallbackProvider = {
                succeedingCreateIntentCallback(paymentMethod)
            },
        )

        val nextStep = interceptor.intercept(
            intent = PaymentIntentFactory.create(),
            confirmationOption = PaymentMethodConfirmationOption.Saved(
                paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
                optionsParams = null,
                passiveCaptchaParams = null,
                hCaptchaToken = null,
            ),
            shippingValues = null,
        )

        assertThat(nextStep).isEqualTo(
            ConfirmationDefinition.Action.Fail<IntentConfirmationDefinition.Args>(
                cause = apiException,
                message = resolvableString(R.string.stripe_something_went_wrong),
                errorType = ConfirmationHandler.Result.Failed.ErrorType.Payment,
            )
        )
    }

    @Test
    fun `Fails if callback returns failure with custom error message`() = runInterceptorScenario(
        initializationMode = InitializationMode.DeferredIntent(
            intentConfiguration = PaymentSheet.IntentConfiguration(
                mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                    amount = 1099L,
                    currency = "usd",
                ),
            ),
        ),
        scenario = InterceptorTestScenario(
            stripeRepository = mock(),
            intentCreationCallbackProvider = {
                failingCreateIntentCallback(
                    message = "that didn't work…"
                )
            },
        )
    ) { interceptor ->

        val nextStep = interceptor.intercept(
            intent = PaymentIntentFactory.create(),
            confirmationOption = PaymentMethodConfirmationOption.Saved(
                paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
                optionsParams = null,
                passiveCaptchaParams = null,
                hCaptchaToken = null,
            ),
            shippingValues = null,
        )

        assertThat(nextStep).isEqualTo(
            ConfirmationDefinition.Action.Fail<IntentConfirmationDefinition.Args>(
                cause = CreateIntentCallbackFailureException(TestException("that didn't work…")),
                message = resolvableString("that didn't work…"),
                errorType = ConfirmationHandler.Result.Failed.ErrorType.Payment,
            )
        )
    }

    @Test
    fun `Fails if callback returns failure without custom error message`() = runTest {
        val interceptor = createIntentConfirmationInterceptor(
            initializationMode = InitializationMode.DeferredIntent(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 1099L,
                        currency = "usd",
                    ),
                ),
            ),
            stripeRepository = mock(),
            intentCreationCallbackProvider = {
                failingCreateIntentCallback()
            },
        )

        val nextStep = interceptor.intercept(
            intent = PaymentIntentFactory.create(),
            confirmationOption = PaymentMethodConfirmationOption.Saved(
                paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
                optionsParams = null,
                passiveCaptchaParams = null,
                hCaptchaToken = null,
            ),
            shippingValues = null,
        )

        assertThat(nextStep).isEqualTo(
            ConfirmationDefinition.Action.Fail<IntentConfirmationDefinition.Args>(
                cause = CreateIntentCallbackFailureException(TestException()),
                message = resolvableString(R.string.stripe_something_went_wrong),
                errorType = ConfirmationHandler.Result.Failed.ErrorType.Payment,
            )
        )
    }

    @Test
    fun `Returns confirm as next step after creating an unconfirmed intent`() = runInterceptorScenario(
        initializationMode = InitializationMode.DeferredIntent(
            intentConfiguration = PaymentSheet.IntentConfiguration(
                mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                    amount = 1099L,
                    currency = "usd",
                ),
            ),
        ),
        scenario = InterceptorTestScenario(
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
            intentCreationCallbackProvider = {
                val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
                succeedingCreateIntentCallback(paymentMethod)
            },
        )
    ) { interceptor ->

        val nextStep = interceptor.interceptDefaultSavedPaymentMethod()
        assertThat(nextStep).isInstanceOf<ConfirmationDefinition.Action.Launch<IntentConfirmationDefinition.Args>>()
    }

    @Test
    fun `Returns complete as next step after creating and confirming a succeeded intent`() = runInterceptorScenario(
        initializationMode = InitializationMode.DeferredIntent(
            intentConfiguration = PaymentSheet.IntentConfiguration(
                mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                    amount = 1099L,
                    currency = "usd",
                ),
            ),
        ),
        scenario = InterceptorTestScenario(
            stripeRepository = object : AbsFakeStripeRepository() {
                override suspend fun retrieveStripeIntent(
                    clientSecret: String,
                    options: ApiRequest.Options,
                    expandFields: List<String>
                ): Result<StripeIntent> {
                    return Result.success(PaymentIntentFixtures.PI_SUCCEEDED)
                }
            },
            intentCreationCallbackProvider = {
                val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
                succeedingCreateIntentCallback(paymentMethod)
            },
        )
    ) { interceptor ->

        val nextStep = interceptor.interceptDefaultSavedPaymentMethod()

        assertThat(nextStep).isEqualTo(
            ConfirmationDefinition.Action.Complete<IntentConfirmationDefinition.Args>(
                intent = PaymentIntentFixtures.PI_SUCCEEDED,
                deferredIntentConfirmationType = DeferredIntentConfirmationType.Server,
                completedFullPaymentFlow = true,
            )
        )
    }

    @Test
    fun `Returns handleNextAction as next step after creating and confirming a non-succeeded intent`() =
        runInterceptorScenario(
            initializationMode = InitializationMode.DeferredIntent(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 1099L,
                        currency = "usd",
                    ),
                ),
            ),
            scenario = InterceptorTestScenario(
                stripeRepository = object : AbsFakeStripeRepository() {
                    override suspend fun retrieveStripeIntent(
                        clientSecret: String,
                        options: ApiRequest.Options,
                        expandFields: List<String>
                    ): Result<StripeIntent> {
                        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
                        return Result.success(
                            PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2.copy(
                                paymentMethodId = paymentMethod.id,
                                paymentMethod = paymentMethod,
                            )
                        )
                    }
                },
                intentCreationCallbackProvider = {
                    val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
                    succeedingCreateIntentCallback(paymentMethod)
                },
            )
        ) { interceptor ->

            val nextStep = interceptor.interceptDefaultSavedPaymentMethod()
            assertThat(nextStep).isEqualTo(
                ConfirmationDefinition.Action.Launch<IntentConfirmationDefinition.Args>(
                    launcherArguments = IntentConfirmationDefinition.Args.NextAction("pi_123_secret_456"),
                    deferredIntentConfirmationType = DeferredIntentConfirmationType.Server,
                    receivesResultInProcess = false,
                )
            )
        }

    @Test
    fun `Passes correct shouldSavePaymentMethod to CreateIntentCallback`() = runTest {
        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        val observedValues = mutableListOf<Boolean>()

        val interceptor = createIntentConfirmationInterceptor(
            initializationMode = InitializationMode.DeferredIntent(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 1099L,
                        currency = "usd",
                    ),
                ),
            ),
            stripeRepository = object : AbsFakeStripeRepository() {
                override suspend fun retrieveStripeIntent(
                    clientSecret: String,
                    options: ApiRequest.Options,
                    expandFields: List<String>
                ): Result<StripeIntent> {
                    return Result.success(PaymentIntentFixtures.PI_SUCCEEDED)
                }
            },
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
                intent = PaymentIntentFactory.create(),
                confirmationOption = PaymentMethodConfirmationOption.Saved(
                    paymentMethod = paymentMethod,
                    optionsParams = PaymentMethodOptionsParams.Card(
                        setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.OffSession
                    ).takeIf { input },
                    passiveCaptchaParams = null,
                    hCaptchaToken = null,
                ),
                shippingValues = null,
            )
        }

        assertThat(observedValues).containsExactly(true, false).inOrder()
    }

    @OptIn(PaymentMethodOptionsSetupFutureUsagePreview::class)
    @Test
    fun `Sets shouldSavePaymentMethod to true for CreateIntentCallback if top level SFU is set with PMO`() = runTest {
        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        var observedValue = false

        val interceptor = createIntentConfirmationInterceptor(
            initializationMode = InitializationMode.DeferredIntent(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 1099L,
                        currency = "usd",
                        setupFutureUse = PaymentSheet.IntentConfiguration.SetupFutureUse.OffSession,
                        paymentMethodOptions = PaymentSheet.IntentConfiguration.Mode.Payment.PaymentMethodOptions(
                            setupFutureUsageValues = mapOf(
                                PaymentMethod.Type.Affirm to PaymentSheet.IntentConfiguration.SetupFutureUse.None
                            )
                        )
                    ),
                ),
            ),
            stripeRepository = object : AbsFakeStripeRepository() {
                override suspend fun retrieveStripeIntent(
                    clientSecret: String,
                    options: ApiRequest.Options,
                    expandFields: List<String>
                ): Result<StripeIntent> {
                    return Result.success(PaymentIntentFactory.create())
                }
            },
            intentCreationCallbackProvider = {
                CreateIntentCallback { _, shouldSavePaymentMethod ->
                    observedValue = shouldSavePaymentMethod
                    CreateIntentResult.Success("pi_123_secret_456")
                }
            },
        )

        interceptor.interceptDefaultSavedPaymentMethod()

        assertThat(observedValue).isTrue()
    }

    @OptIn(PaymentMethodOptionsSetupFutureUsagePreview::class)
    @Test
    fun `Sets shouldSavePaymentMethod to true for CreateIntentCallback if PMO SFU is set`() = runTest {
        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        var observedValue = false

        val interceptor = createIntentConfirmationInterceptor(
            initializationMode = InitializationMode.DeferredIntent(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 1099L,
                        currency = "usd",
                        paymentMethodOptions = PaymentSheet.IntentConfiguration.Mode.Payment.PaymentMethodOptions(
                            setupFutureUsageValues = mapOf(
                                PaymentMethod.Type.Card to PaymentSheet.IntentConfiguration.SetupFutureUse.OffSession
                            )
                        )
                    ),
                ),
            ),
            stripeRepository = object : AbsFakeStripeRepository() {
                override suspend fun retrieveStripeIntent(
                    clientSecret: String,
                    options: ApiRequest.Options,
                    expandFields: List<String>
                ): Result<StripeIntent> {
                    return Result.success(PaymentIntentFactory.create())
                }
            },
            intentCreationCallbackProvider = {
                CreateIntentCallback { _, shouldSavePaymentMethod ->
                    observedValue = shouldSavePaymentMethod
                    CreateIntentResult.Success("pi_123_secret_456")
                }
            },
        )

        interceptor.interceptDefaultSavedPaymentMethod()

        assertThat(observedValue).isTrue()
    }

    @Test
    fun `Returns success as next step if merchant is forcing success`() = runTest {
        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        val stripeRepository = mock<StripeRepository>()

        val interceptor = createIntentConfirmationInterceptor(
            initializationMode = InitializationMode.DeferredIntent(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 1099L,
                        currency = "usd",
                    ),
                ),
            ),
            stripeRepository = stripeRepository,
            intentCreationCallbackProvider = {
                CreateIntentCallback { _, _ ->
                    CreateIntentResult.Success(IntentConfirmationInterceptor.COMPLETE_WITHOUT_CONFIRMING_INTENT)
                }
            },
        )

        val intent = PaymentIntentFactory.create()
        val nextStep = interceptor.interceptDefaultSavedPaymentMethod()

        verify(stripeRepository, never()).retrieveStripeIntent(any(), any(), any())

        assertThat(nextStep).isEqualTo(
            ConfirmationDefinition.Action.Complete<IntentConfirmationDefinition.Args>(
                intent = intent,
                deferredIntentConfirmationType = DeferredIntentConfirmationType.None,
                completedFullPaymentFlow = true,
            )
        )
    }

    @Test
    fun `If requires next action with an attached payment method different then the created one, throw error`() =
        runTest {
            val interceptor = createIntentConfirmationInterceptor(
                initializationMode = InitializationMode.DeferredIntent(
                    intentConfiguration = PaymentSheet.IntentConfiguration(
                        mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                            amount = 1099L,
                            currency = "usd",
                        ),
                    ),
                ),
                stripeRepository = stripeRepositoryReturning(
                    onCreatePaymentMethodId = "pm_1234",
                    onRetrievePaymentMethodId = "pm_5678"
                ),
                intentCreationCallbackProvider = {
                    CreateIntentCallback { _, _ ->
                        CreateIntentResult.Success(clientSecret = "pi_123")
                    }
                },
            )

            val nextStep = interceptor.interceptDefaultSavedPaymentMethod()

            val failedStep = nextStep as ConfirmationDefinition.Action.Fail

            assertThat(failedStep.cause).isInstanceOf(InvalidDeferredIntentUsageException::class.java)
            assertThat(failedStep.message).isEqualTo(
                R.string.stripe_paymentsheet_invalid_deferred_intent_usage.resolvableString
            )
        }

    @Test
    fun `If initialized with shared payment token, should fail if 'preparePaymentMethodHandler' in null`() =
        testNoProvider(
            event = ErrorReporter.ExpectedErrorEvent.PREPARE_PAYMENT_METHOD_HANDLER_NULL,
            failureMessage = PREPARE_PAYMENT_METHOD_HANDLER_MESSAGE,
            userMessage = PaymentsCoreR.string.stripe_internal_error.resolvableString,
        ) { errorReporter ->

            val interceptor = createIntentConfirmationInterceptor(
                initializationMode = InitializationMode.DeferredIntent(
                    intentConfiguration = PaymentSheet.IntentConfiguration(
                        sharedPaymentTokenSessionWithMode = PaymentSheet.IntentConfiguration.Mode.Payment(
                            amount = 1099L,
                            currency = "usd",
                        ),
                        sellerDetails = PaymentSheet.IntentConfiguration.SellerDetails(
                            businessName = "My business, Inc.",
                            networkId = "network_id",
                            externalId = "external_id"
                        )
                    ),
                ),
                stripeRepository = stripeRepositoryReturning(
                    onCreatePaymentMethodId = "pm_1234",
                    onRetrievePaymentMethodId = "pm_5678"
                ),
                errorReporter = errorReporter,
            )

            interceptor.interceptDefaultSavedPaymentMethod()
        }

    @Test
    @Suppress("LongMethod")
    fun `If initialized with shared payment token, should call 'onPreparePaymentMethod' with saved PM`() =
        runTest {
            val completablePaymentMethod = CompletableDeferred<PaymentMethod>()
            val completableShippingAddress = CompletableDeferred<AddressDetails?>()
            val createSavedPaymentMethodRadarSessionCalls = Turbine<CreateSavedPaymentMethodRadarSessionCall>()

            val providedPaymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
            val providedShippingAddress = SHIPPING_ADDRESS

            val interceptor = createIntentConfirmationInterceptor(
                initializationMode = InitializationMode.DeferredIntent(
                    intentConfiguration = PaymentSheet.IntentConfiguration(
                        sharedPaymentTokenSessionWithMode = PaymentSheet.IntentConfiguration.Mode.Payment(
                            amount = 1099L,
                            currency = "usd"
                        ),
                        sellerDetails = PaymentSheet.IntentConfiguration.SellerDetails(
                            businessName = "My business, Inc.",
                            networkId = "network_id",
                            externalId = "external_id"
                        )
                    ),
                ),
                stripeRepository = stripeRepositoryReturning(
                    onCreatePaymentMethodId = "pm_1234",
                    onRetrievePaymentMethodId = "pm_5678",
                    createSavedPaymentMethodRadarSessionCalls = createSavedPaymentMethodRadarSessionCalls,
                ),
                preparePaymentMethodHandlerProvider = {
                    PreparePaymentMethodHandler { paymentMethod, shippingAddress ->
                        completablePaymentMethod.complete(paymentMethod)
                        completableShippingAddress.complete(shippingAddress)
                    }
                },
            )

            val intent = PaymentIntentFactory.create()
            val nextStep = interceptor.intercept(
                intent = intent,
                confirmationOption = PaymentMethodConfirmationOption.Saved(
                    paymentMethod = providedPaymentMethod,
                    optionsParams = null,
                    passiveCaptchaParams = null,
                    hCaptchaToken = null,
                ),
                shippingValues = providedShippingAddress,
            )

            assertThat(nextStep).isEqualTo(
                ConfirmationDefinition.Action.Complete<IntentConfirmationDefinition.Args>(
                    intent = intent,
                    deferredIntentConfirmationType = DeferredIntentConfirmationType.None,
                    completedFullPaymentFlow = false,
                )
            )

            val paymentMethod = completablePaymentMethod.await()

            assertThat(paymentMethod).isEqualTo(providedPaymentMethod)

            val shippingAddress = completableShippingAddress.await()

            verifyShipping(providedShippingAddress, shippingAddress)

            val createRadarSessionCall = createSavedPaymentMethodRadarSessionCalls.awaitItem()

            assertThat(createRadarSessionCall.paymentMethodId).isEqualTo("pm_123456789")
            assertThat(createRadarSessionCall.requestOptions.apiKey).isEqualTo("pk")
            assertThat(createRadarSessionCall.requestOptions.stripeAccount).isNull()

            createSavedPaymentMethodRadarSessionCalls.ensureAllEventsConsumed()
        }

    @Test
    fun `If initialized with shared payment token, should call 'onPreparePaymentMethod' with new PM`() =
        runTest {
            val completablePaymentMethod = CompletableDeferred<PaymentMethod>()
            val completableShippingAddress = CompletableDeferred<AddressDetails?>()
            val createSavedPaymentMethodRadarSessionCalls = Turbine<CreateSavedPaymentMethodRadarSessionCall>()

            val interceptor = createIntentConfirmationInterceptor(
                initializationMode = InitializationMode.DeferredIntent(
                    intentConfiguration = PaymentSheet.IntentConfiguration(
                        sharedPaymentTokenSessionWithMode = PaymentSheet.IntentConfiguration.Mode.Payment(
                            amount = 1099L,
                            currency = "usd",
                        ),
                        sellerDetails = PaymentSheet.IntentConfiguration.SellerDetails(
                            businessName = "My business, Inc.",
                            networkId = "network_id",
                            externalId = "external_id"
                        )
                    ),
                ),
                stripeRepository = stripeRepositoryReturning(
                    onCreatePaymentMethodId = "pm_1234",
                    onRetrievePaymentMethodId = "pm_5678",
                    createSavedPaymentMethodRadarSessionCalls = createSavedPaymentMethodRadarSessionCalls,
                ),
                preparePaymentMethodHandlerProvider = {
                    PreparePaymentMethodHandler { paymentMethod, shippingAddress ->
                        completablePaymentMethod.complete(paymentMethod)
                        completableShippingAddress.complete(shippingAddress)
                    }
                },
            )

            val intent = PaymentIntentFactory.create()
            val nextStep = interceptor.interceptDefaultNewPaymentMethod()

            assertThat(nextStep).isEqualTo(
                ConfirmationDefinition.Action.Complete<IntentConfirmationDefinition.Args>(
                    intent = intent,
                    deferredIntentConfirmationType = DeferredIntentConfirmationType.None,
                    completedFullPaymentFlow = false,
                )
            )

            val paymentMethod = completablePaymentMethod.await()

            assertThat(paymentMethod.id).isEqualTo("pm_1234")

            val shippingAddress = completableShippingAddress.await()

            assertThat(shippingAddress).isNull()

            createSavedPaymentMethodRadarSessionCalls.verify()
        }

    @Test
    fun `If failed to make radar session, should still continue with preparing payment method`() =
        runTest {
            val completablePaymentMethod = CompletableDeferred<PaymentMethod>()
            val completableShippingAddress = CompletableDeferred<AddressDetails?>()
            val createSavedPaymentMethodRadarSessionCalls = Turbine<CreateSavedPaymentMethodRadarSessionCall>()

            val error = IllegalStateException("Failed to make radar session!")
            val eventReporter = FakeErrorReporter()

            val interceptor = createIntentConfirmationInterceptor(
                initializationMode = InitializationMode.DeferredIntent(
                    intentConfiguration = PaymentSheet.IntentConfiguration(
                        sharedPaymentTokenSessionWithMode = PaymentSheet.IntentConfiguration.Mode.Payment(
                            amount = 1099L,
                            currency = "usd",
                        ),
                        sellerDetails = PaymentSheet.IntentConfiguration.SellerDetails(
                            businessName = "My business, Inc.",
                            networkId = "network_id",
                            externalId = "external_id"
                        )
                    ),
                ),
                stripeRepository = stripeRepositoryReturning(
                    onCreatePaymentMethodId = "pm_1234",
                    onRetrievePaymentMethodId = "pm_5678",
                    createSavedPaymentMethodRadarSessionResult = Result.failure(error),
                    createSavedPaymentMethodRadarSessionCalls = createSavedPaymentMethodRadarSessionCalls,
                ),
                errorReporter = eventReporter,
                preparePaymentMethodHandlerProvider = {
                    PreparePaymentMethodHandler { paymentMethod, shippingAddress ->
                        completablePaymentMethod.complete(paymentMethod)
                        completableShippingAddress.complete(shippingAddress)
                    }
                },
            )

            val intent = PaymentIntentFactory.create()
            val nextStep = interceptor.interceptDefaultNewPaymentMethod()

            assertThat(nextStep).isEqualTo(
                ConfirmationDefinition.Action.Complete<IntentConfirmationDefinition.Args>(
                    intent = intent,
                    deferredIntentConfirmationType = DeferredIntentConfirmationType.None,
                    completedFullPaymentFlow = false,
                )
            )

            val paymentMethod = completablePaymentMethod.await()

            assertThat(paymentMethod.id).isEqualTo("pm_1234")

            val shippingAddress = completableShippingAddress.await()

            assertThat(shippingAddress).isNull()

            createSavedPaymentMethodRadarSessionCalls.verify()

            eventReporter.verifyCreateSavedPaymentMethodRadarSessionCall(error)
        }

    @OptIn(PaymentMethodOptionsSetupFutureUsagePreview::class)
    @Test
    fun `Returns confirm params with top level 'setup_future_usage' set to 'off_session' when set on configuration`() =
        runInterceptorScenario(
            initializationMode = InitializationMode.DeferredIntent(
                PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        currency = "usd",
                        amount = 5000,
                        setupFutureUse = PaymentSheet.IntentConfiguration.SetupFutureUse.OffSession,
                        paymentMethodOptions = PaymentSheet.IntentConfiguration.Mode.Payment.PaymentMethodOptions(
                            setupFutureUsageValues = mapOf(
                                PaymentMethod.Type.Affirm to PaymentSheet.IntentConfiguration.SetupFutureUse.None
                            )
                        )
                    ),
                )
            ),
            scenario = InterceptorTestScenario(
                stripeRepository = object : AbsFakeStripeRepository() {
                    override suspend fun retrieveStripeIntent(
                        clientSecret: String,
                        options: ApiRequest.Options,
                        expandFields: List<String>
                    ): Result<StripeIntent> {
                        return Result.success(PaymentIntentFactory.create())
                    }

                    override suspend fun createPaymentMethod(
                        paymentMethodCreateParams: PaymentMethodCreateParams,
                        options: ApiRequest.Options
                    ): Result<PaymentMethod> {
                        return Result.success(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
                    }
                },
                intentCreationCallbackProvider = {
                    CreateIntentCallback { _, _ ->
                        CreateIntentResult.Success("pi_123_secret_456")
                    }
                },
            )
        ) { interceptor ->

            val nextStep = interceptor.intercept(
                confirmationOption = PaymentMethodConfirmationOption.New(
                    createParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
                    optionsParams = PaymentMethodOptionsParams.Card(
                        setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.OffSession
                    ),
                    extraParams = null,
                    shouldSave = false,
                    passiveCaptchaParams = null
                ),
                intent = PaymentIntentFactory.create(),
                shippingValues = null,
            )

            val confirmParams = nextStep.asConfirmParams<ConfirmPaymentIntentParams>()

            assertThat(
                confirmParams?.setupFutureUsage
            ).isEqualTo(
                ConfirmPaymentIntentParams.SetupFutureUsage.OffSession
            )
        }

    @OptIn(PaymentMethodOptionsSetupFutureUsagePreview::class)
    @Test
    fun `Returns confirm params with pmo 'setup_future_usage' set to 'off_session' when set on configuration`() =
        runInterceptorScenario(
            initializationMode = InitializationMode.DeferredIntent(
                PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        currency = "usd",
                        amount = 5000,
                        paymentMethodOptions = PaymentSheet.IntentConfiguration.Mode.Payment.PaymentMethodOptions(
                            setupFutureUsageValues = mapOf(
                                PaymentMethod.Type.Card to
                                    PaymentSheet.IntentConfiguration.SetupFutureUse.OffSession
                            )
                        )
                    ),
                )
            ),
            scenario = InterceptorTestScenario(
                stripeRepository = object : AbsFakeStripeRepository() {
                    override suspend fun retrieveStripeIntent(
                        clientSecret: String,
                        options: ApiRequest.Options,
                        expandFields: List<String>
                    ): Result<StripeIntent> {
                        return Result.success(PaymentIntentFactory.create())
                    }

                    override suspend fun createPaymentMethod(
                        paymentMethodCreateParams: PaymentMethodCreateParams,
                        options: ApiRequest.Options
                    ): Result<PaymentMethod> {
                        return Result.success(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
                    }
                },
                intentCreationCallbackProvider = {
                    CreateIntentCallback { _, _ ->
                        CreateIntentResult.Success("pi_123_secret_456")
                    }
                },
            )
        ) { interceptor ->

            val nextStep = interceptor.intercept(
                confirmationOption = PaymentMethodConfirmationOption.New(
                    createParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
                    optionsParams = PaymentMethodOptionsParams.Card(),
                    extraParams = null,
                    shouldSave = false,
                    passiveCaptchaParams = null
                ),
                intent = PaymentIntentFactory.create(),
                shippingValues = null,
            )

            val confirmParams = nextStep.asConfirmParams<ConfirmPaymentIntentParams>()

            assertThat(
                confirmParams?.paymentMethodOptions
            ).isEqualTo(
                PaymentMethodOptionsParams.Card(
                    setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.OffSession
                )
            )
        }

    @Test
    fun `Returns confirm params with hCaptchaToken set as RadarOptions when hCaptchaToken provided`() = runTest {
        val hCaptchaToken = "test-hcaptcha-token"

        val confirmParams = interceptWithSavedPaymentMethod(hCaptchaToken = hCaptchaToken)

        assertRadarOptionsEquals(confirmParams, hCaptchaToken)
    }

    @Test
    fun `Returns confirm params with null RadarOptions when hCaptchaToken is null`() = runTest {
        val confirmParams = interceptWithSavedPaymentMethod(hCaptchaToken = null)

        assertRadarOptionsIsNull(confirmParams)
    }

    @Test
    fun `Returns confirm params with hCaptchaToken for deferred intent confirmation`() = runTest {
        val hCaptchaToken = "deferred-hcaptcha-token"

        val confirmParams = interceptWithDeferredIntent(hCaptchaToken = hCaptchaToken)

        assertRadarOptionsEquals(confirmParams, hCaptchaToken)
    }

    @Test
    fun `Returns confirm params with null RadarOptions for deferred intent when hCaptchaToken is null`() = runTest {
        val confirmParams = interceptWithDeferredIntent(hCaptchaToken = null)

        assertRadarOptionsIsNull(confirmParams)
    }

    @Test
    fun `hCaptchaToken is properly passed through extension function for saved payment method`() = runTest {
        val hCaptchaToken = "extension-hcaptcha-token"

        val confirmParams = interceptWithSavedPaymentMethod(hCaptchaToken = hCaptchaToken)

        assertRadarOptionsEquals(confirmParams, hCaptchaToken)
    }

    @Test
    fun `hCaptchaToken is not set for new payment method confirmation option`() =
        runInterceptorScenario(
            initializationMode = InitializationMode.PaymentIntent("pi_1234_secret_4321"),
        ) { interceptor ->
            val nextStep = interceptor.intercept(
                confirmationOption = PaymentMethodConfirmationOption.New(
                    createParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
                    optionsParams = null,
                    extraParams = null,
                    shouldSave = false,
                    passiveCaptchaParams = null
                ),
                intent = PaymentIntentFactory.create(),
                shippingValues = null,
            )

            val confirmParams = nextStep.asConfirmParams<ConfirmPaymentIntentParams>()
            assertRadarOptionsIsNull(confirmParams)
        }

    @Test
    fun `Returns confirm params with hCaptchaToken set as RadarOptions for setup intent with client secret`() =
        runTest {
            val hCaptchaToken = "setup-hcaptcha-token"

            val confirmParams = interceptWithSetupIntentClientSecret(hCaptchaToken = hCaptchaToken)

            assertRadarOptionsEquals(confirmParams, hCaptchaToken)
        }

    @Test
    fun `Returns confirm params with null RadarOptions for setup intent when hCaptchaToken is null`() = runTest {
        val confirmParams = interceptWithSetupIntentClientSecret(hCaptchaToken = null)

        assertRadarOptionsIsNull(confirmParams)
    }

    @Test
    fun `Returns confirm params with hCaptchaToken for deferred setup intent confirmation`() = runTest {
        val hCaptchaToken = "deferred-setup-hcaptcha-token"

        val confirmParams = interceptWithDeferredSetupIntent(hCaptchaToken = hCaptchaToken)

        assertRadarOptionsEquals(confirmParams, hCaptchaToken)
    }

    @Test
    fun `Returns confirm params with null RadarOptions for deferred setup intent when hCaptchaToken is null`() =
        runTest {
            val confirmParams = interceptWithDeferredSetupIntent(hCaptchaToken = null)

            assertRadarOptionsIsNull(confirmParams)
        }

    private suspend fun interceptWithSetupIntentClientSecret(
        hCaptchaToken: String?
    ): ConfirmSetupIntentParams? {
        val interceptor = createIntentConfirmationInterceptor(
            initializationMode = InitializationMode.SetupIntent("seti_1234_secret_4321"),
        )

        val nextStep = interceptor.intercept(
            confirmationOption = PaymentMethodConfirmationOption.Saved(
                paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
                optionsParams = null,
                hCaptchaToken = hCaptchaToken,
                passiveCaptchaParams = null
            ),
            intent = SetupIntentFactory.create(),
            shippingValues = null,
        )

        return nextStep.asConfirmParams()
    }

    private suspend fun interceptWithDeferredSetupIntent(
        hCaptchaToken: String?
    ): ConfirmSetupIntentParams? {
        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        val interceptor = createIntentConfirmationInterceptor(
            initializationMode = InitializationMode.DeferredIntent(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Setup(
                        currency = "usd",
                    ),
                ),
            ),
            stripeRepository = object : AbsFakeStripeRepository() {
                override suspend fun retrieveStripeIntent(
                    clientSecret: String,
                    options: ApiRequest.Options,
                    expandFields: List<String>
                ): Result<StripeIntent> {
                    return Result.success(
                        SetupIntentFactory.create(
                            status = StripeIntent.Status.RequiresConfirmation,
                            usage = StripeIntent.Usage.OffSession,
                        )
                    )
                }
            },
            intentCreationCallbackProvider = {
                succeedingCreateSetupIntentCallback(paymentMethod)
            }
        )

        val nextStep = interceptor.intercept(
            intent = SetupIntentFactory.create(),
            confirmationOption = PaymentMethodConfirmationOption.Saved(
                paymentMethod = paymentMethod,
                optionsParams = null,
                passiveCaptchaParams = null,
                hCaptchaToken = hCaptchaToken,
            ),
            shippingValues = null,
        )

        return nextStep.asConfirmParams()
    }

    private fun testNoProvider(
        event: ErrorReporter.ErrorEvent,
        failureMessage: String,
        userMessage: ResolvableString,
        interceptCall: suspend (errorReporter: ErrorReporter) ->
        ConfirmationDefinition.Action<IntentConfirmationDefinition.Args>
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

            assertThat(nextStep).isInstanceOf<ConfirmationDefinition.Action.Fail<IntentConfirmationDefinition.Args>>()

            val failedStep = nextStep as ConfirmationDefinition.Action.Fail<IntentConfirmationDefinition.Args>

            assertThat(failedStep.cause).isInstanceOf<IllegalStateException>()
            assertThat(failedStep.cause.message).isEqualTo(failureMessage)
            assertThat(failedStep.message).isEqualTo(userMessage)

            assertThat(errorReporter.awaitCall().errorEvent).isEqualTo(event)
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

    private data class InterceptorTestScenario(
        val stripeRepository: StripeRepository = object : AbsFakeStripeRepository() {},
        val publishableKeyProvider: () -> String = { "pk" },
        val errorReporter: ErrorReporter = FakeErrorReporter(),
        val intentCreationCallbackProvider: Provider<CreateIntentCallback?> = Provider { null },
        val preparePaymentMethodHandlerProvider: Provider<PreparePaymentMethodHandler?> = Provider { null }
    )

    private fun runInterceptorScenario(
        initializationMode: InitializationMode,
        scenario: InterceptorTestScenario = InterceptorTestScenario(),
        test: suspend (interceptor: IntentConfirmationInterceptor) -> Unit
    ) = runTest {
        val interceptor = DefaultIntentConfirmationInterceptorFactory(
            stripeRepository = scenario.stripeRepository,
            publishableKeyProvider = scenario.publishableKeyProvider,
            stripeAccountIdProvider = { null },
            errorReporter = scenario.errorReporter,
            allowsManualConfirmation = false,
            intentCreationCallbackProvider = scenario.intentCreationCallbackProvider,
            preparePaymentMethodHandlerProvider = scenario.preparePaymentMethodHandlerProvider,
        ).create(initializationMode)
        test(interceptor)
    }

    private fun createIntentConfirmationInterceptor(
        initializationMode: InitializationMode,
        stripeRepository: StripeRepository = object : AbsFakeStripeRepository() {},
        publishableKeyProvider: () -> String = { "pk" },
        errorReporter: ErrorReporter = FakeErrorReporter(),
        intentCreationCallbackProvider: Provider<CreateIntentCallback?> = Provider { null },
        preparePaymentMethodHandlerProvider: Provider<PreparePaymentMethodHandler?> = Provider { null }
    ): IntentConfirmationInterceptor {
        return DefaultIntentConfirmationInterceptorFactory(
            stripeRepository = stripeRepository,
            publishableKeyProvider = publishableKeyProvider,
            stripeAccountIdProvider = { null },
            errorReporter = errorReporter,
            allowsManualConfirmation = false,
            intentCreationCallbackProvider = intentCreationCallbackProvider,
            preparePaymentMethodHandlerProvider = preparePaymentMethodHandlerProvider,
        ).create(initializationMode)
    }

    private fun stripeRepositoryReturning(
        onCreatePaymentMethodId: String,
        onRetrievePaymentMethodId: String,
        createSavedPaymentMethodRadarSessionCalls: Turbine<CreateSavedPaymentMethodRadarSessionCall> = Turbine(),
        createSavedPaymentMethodRadarSessionResult: Result<RadarSessionWithHCaptcha> = Result.success(
            RadarSessionWithHCaptcha(
                id = "rse_123",
                passiveCaptchaSiteKey = "1234",
                passiveCaptchaRqdata = "123456789",
            )
        ),
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

            override suspend fun createSavedPaymentMethodRadarSession(
                paymentMethodId: String,
                requestOptions: ApiRequest.Options
            ): Result<RadarSessionWithHCaptcha> {
                createSavedPaymentMethodRadarSessionCalls.add(
                    CreateSavedPaymentMethodRadarSessionCall(paymentMethodId, requestOptions)
                )

                return createSavedPaymentMethodRadarSessionResult
            }
        }
    }

    private fun verifyShipping(
        expectedShippingAddress: ConfirmPaymentIntentParams.Shipping,
        actualShippingAddress: AddressDetails?
    ) {
        assertThat(actualShippingAddress?.name).isEqualTo(expectedShippingAddress.getName())
        assertThat(actualShippingAddress?.phoneNumber).isEqualTo(expectedShippingAddress.getPhone())
        assertThat(actualShippingAddress?.address?.line1).isEqualTo(expectedShippingAddress.getAddress().line1)
        assertThat(actualShippingAddress?.address?.line2).isEqualTo(expectedShippingAddress.getAddress().line2)
        assertThat(actualShippingAddress?.address?.city).isEqualTo(expectedShippingAddress.getAddress().city)
        assertThat(actualShippingAddress?.address?.state).isEqualTo(expectedShippingAddress.getAddress().state)
        assertThat(actualShippingAddress?.address?.country)
            .isEqualTo(expectedShippingAddress.getAddress().country)
    }

    private suspend fun Turbine<CreateSavedPaymentMethodRadarSessionCall>.verify() {
        val createRadarSessionCall = awaitItem()

        assertThat(createRadarSessionCall.paymentMethodId).isEqualTo("pm_1234")
        assertThat(createRadarSessionCall.requestOptions.apiKey).isEqualTo("pk")
        assertThat(createRadarSessionCall.requestOptions.stripeAccount).isNull()

        ensureAllEventsConsumed()
    }

    private suspend fun FakeErrorReporter.verifyCreateSavedPaymentMethodRadarSessionCall(
        error: Exception,
    ) {
        val failedRadarEvent = awaitCall()

        assertThat(failedRadarEvent.errorEvent)
            .isEqualTo(ErrorReporter.ExpectedErrorEvent.SAVED_PAYMENT_METHOD_RADAR_SESSION_FAILURE)
        assertThat(failedRadarEvent.stripeException?.cause).isEqualTo(error)

        ensureAllEventsConsumed()
    }

    private class TestException(message: String? = null) : Exception(message) {

        override fun hashCode(): Int {
            return Objects.hash(message)
        }

        override fun equals(other: Any?): Boolean {
            return other is TestException && other.message == message
        }
    }

    private data class CreateSavedPaymentMethodRadarSessionCall(
        val paymentMethodId: String,
        val requestOptions: ApiRequest.Options
    )

    private suspend fun interceptWithSavedPaymentMethod(
        hCaptchaToken: String?
    ): ConfirmPaymentIntentParams? {
        val interceptor = createIntentConfirmationInterceptor(
            initializationMode = InitializationMode.PaymentIntent("pi_1234_secret_4321"),
        )

        val nextStep = interceptor.intercept(
            confirmationOption = PaymentMethodConfirmationOption.Saved(
                paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
                optionsParams = null,
                hCaptchaToken = hCaptchaToken,
                passiveCaptchaParams = null
            ),
            intent = PaymentIntentFactory.create(),
            shippingValues = null,
        )

        return nextStep.asConfirmParams()
    }

    private suspend fun interceptWithDeferredIntent(
        hCaptchaToken: String?
    ): ConfirmPaymentIntentParams? {
        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        val interceptor = createIntentConfirmationInterceptor(
            initializationMode = InitializationMode.DeferredIntent(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 1099L,
                        currency = "usd",
                    ),
                ),
            ),
            stripeRepository = object : AbsFakeStripeRepository() {
                override suspend fun retrieveStripeIntent(
                    clientSecret: String,
                    options: ApiRequest.Options,
                    expandFields: List<String>
                ): Result<StripeIntent> {
                    return Result.success(
                        PaymentIntentFixtures.PI_SUCCEEDED.copy(
                            status = StripeIntent.Status.RequiresConfirmation,
                        )
                    )
                }
            },
            intentCreationCallbackProvider = {
                succeedingCreateIntentCallback(paymentMethod)
            }
        )

        val nextStep = interceptor.intercept(
            intent = PaymentIntentFactory.create(),
            confirmationOption = PaymentMethodConfirmationOption.Saved(
                paymentMethod = paymentMethod,
                optionsParams = null,
                passiveCaptchaParams = null,
                hCaptchaToken = hCaptchaToken,
            ),
            shippingValues = null,
        )

        return nextStep.asConfirmParams()
    }

    private fun assertRadarOptionsEquals(confirmParams: ConfirmStripeIntentParams?, expectedToken: String) {
        assertThat(confirmParams?.radarOptions()?.hCaptchaToken).isEqualTo(expectedToken)
    }

    private fun assertRadarOptionsIsNull(confirmParams: ConfirmStripeIntentParams?) {
        assertThat(confirmParams?.radarOptions()?.hCaptchaToken).isNull()
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : ConfirmStripeIntentParams>
        ConfirmationDefinition.Action<IntentConfirmationDefinition.Args>.asConfirmParams(): T? {
        return (
            (this as? ConfirmationDefinition.Action.Launch)
                ?.launcherArguments as? IntentConfirmationDefinition.Args.Confirm
            ) ?.confirmNextParams as? T
    }

    private fun ConfirmStripeIntentParams.radarOptions(): RadarOptions? {
        return when (this) {
            is ConfirmPaymentIntentParams -> radarOptions
            is ConfirmSetupIntentParams -> radarOptions
        }
    }

    private fun succeedingCreateSetupIntentCallback(
        expectedPaymentMethod: PaymentMethod,
    ): CreateIntentCallback {
        return CreateIntentCallback { paymentMethod, _ ->
            assertThat(paymentMethod).isEqualTo(expectedPaymentMethod)
            CreateIntentResult.Success(clientSecret = "seti_123_secret_456")
        }
    }

    private suspend fun IntentConfirmationInterceptor.interceptDefaultNewPaymentMethod():
        ConfirmationDefinition.Action<IntentConfirmationDefinition.Args> = intercept(
        intent = PaymentIntentFactory.create(),
        confirmationOption = PaymentMethodConfirmationOption.New(
            createParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
            optionsParams = null,
            extraParams = null,
            shouldSave = false,
            passiveCaptchaParams = null
        ),
        shippingValues = null,
    )

    private suspend fun IntentConfirmationInterceptor.interceptDefaultSavedPaymentMethod():
        ConfirmationDefinition.Action<IntentConfirmationDefinition.Args> = intercept(
        intent = PaymentIntentFactory.create(),
        confirmationOption = PaymentMethodConfirmationOption.Saved(
            paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
            optionsParams = null,
            passiveCaptchaParams = null,
            hCaptchaToken = null,
        ),
        shippingValues = null,
    )

    private companion object {
        const val CREATE_INTENT_CALLBACK_MESSAGE =
            "CreateIntentCallback must be implemented when using IntentConfiguration with PaymentSheet"

        const val PREPARE_PAYMENT_METHOD_HANDLER_MESSAGE =
            "PreparePaymentMethodHandler must be implemented when using IntentConfiguration with " +
                "shared payment tokens!"

        val SHIPPING_ADDRESS = ConfirmPaymentIntentParams.Shipping(
            address = Address(
                city = "South San Francisc",
                line1 = "123 Apple Street",
                line2 = "Unit #2",
                state = "CA",
                postalCode = "99999",
                country = "US"
            ),
            phone = "11234567890",
            name = "John Doe"
        )
    }
}
