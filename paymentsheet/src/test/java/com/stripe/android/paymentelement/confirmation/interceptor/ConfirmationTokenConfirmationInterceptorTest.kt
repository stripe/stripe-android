package com.stripe.android.paymentelement.confirmation.interceptor

import com.google.common.truth.Truth.assertThat
import com.stripe.android.SharedPaymentTokenSessionPreview
import com.stripe.android.core.StripeError
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.exception.InvalidRequestException
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.isInstanceOf
import com.stripe.android.model.ConfirmationToken
import com.stripe.android.model.ConfirmationTokenParams
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.model.StripeIntent
import com.stripe.android.model.parsers.ConfirmationTokenJsonParser
import com.stripe.android.networking.StripeRepository
import com.stripe.android.paymentelement.CreateIntentWithConfirmationTokenCallback
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.ConfirmationTokenFixtures
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.paymentelement.confirmation.createIntentConfirmationInterceptor
import com.stripe.android.paymentelement.confirmation.intent.CreateIntentWithConfirmationTokenCallbackFailureException
import com.stripe.android.paymentelement.confirmation.intent.DeferredIntentConfirmationType
import com.stripe.android.paymentelement.confirmation.intent.IntentConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.intent.IntentConfirmationInterceptor
import com.stripe.android.paymentelement.confirmation.interceptor.DeferredIntentConfirmationInterceptorTest.Companion.DEFAULT_DEFERRED_INTENT
import com.stripe.android.paymentsheet.CreateIntentResult
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.testing.AbsFakeStripeRepository
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.android.testing.PaymentIntentFactory
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import javax.inject.Provider

@RunWith(RobolectricTestRunner::class)
@OptIn(SharedPaymentTokenSessionPreview::class)
class ConfirmationTokenConfirmationInterceptorTest {
    private val confirmationTokenParser = ConfirmationTokenJsonParser()

    private val confirmationToken by lazy {
        confirmationTokenParser.parse(ConfirmationTokenFixtures.CONFIRMATION_TOKEN_JSON)!!
    }

    @Test
    fun `Fails if creating confirmation token did not succeed`() = runTest {
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
            initializationMode = DEFAULT_DEFERRED_INTENT,
            stripeRepository = object : AbsFakeStripeRepository() {
                override suspend fun createConfirmationToken(
                    confirmationTokenParams: ConfirmationTokenParams,
                    options: ApiRequest.Options
                ): Result<ConfirmationToken> {
                    return Result.failure(invalidRequestException)
                }
            },
            intentCreationConfirmationTokenCallbackProvider = Provider {
                CreateIntentWithConfirmationTokenCallback { _ ->
                    CreateIntentResult.Success(clientSecret = "pi_123")
                }
            },
        )

        val nextStep = interceptor.interceptDefaultNewPaymentMethod()

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
        val apiException = APIException(
            requestId = "req_123",
            statusCode = 500,
            message = "Whoopsie",
        )

        val interceptor = createIntentConfirmationInterceptor(
            initializationMode = DEFAULT_DEFERRED_INTENT,
            stripeRepository = object : AbsFakeStripeRepository() {
                override suspend fun createConfirmationToken(
                    confirmationTokenParams: ConfirmationTokenParams,
                    options: ApiRequest.Options
                ): Result<ConfirmationToken> {
                    return Result.success(confirmationToken)
                }

                override suspend fun retrieveStripeIntent(
                    clientSecret: String,
                    options: ApiRequest.Options,
                    expandFields: List<String>
                ): Result<StripeIntent> {
                    return Result.failure(apiException)
                }
            },
            intentCreationConfirmationTokenCallbackProvider = Provider {
                succeedingCreateIntentWithConfirmationTokenCallback(confirmationToken)
            },
        )

        val nextStep = interceptor.interceptDefaultNewPaymentMethod()

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
        initializationMode = DEFAULT_DEFERRED_INTENT,
        scenario = InterceptorTestScenario(
            stripeRepository = createFakeStripeRepositoryForConfirmationToken(),
            errorReporter = FakeErrorReporter(),
            intentCreationConfirmationTokenCallbackProvider = Provider {
                failingCreateIntentWithConfirmationTokenCallback(
                    message = "that didn't work…"
                )
            },
        )
    ) { interceptor ->

        val nextStep = interceptor.interceptDefaultNewPaymentMethod()

        assertThat(nextStep).isEqualTo(
            ConfirmationDefinition.Action.Fail<IntentConfirmationDefinition.Args>(
                cause = CreateIntentWithConfirmationTokenCallbackFailureException(TestException("that didn't work…")),
                message = resolvableString("that didn't work…"),
                errorType = ConfirmationHandler.Result.Failed.ErrorType.Payment,
            )
        )
    }

    @Test
    fun `Fails if callback returns failure without custom error message`() = runTest {
        val interceptor = createIntentConfirmationInterceptor(
            initializationMode = DEFAULT_DEFERRED_INTENT,
            stripeRepository = createFakeStripeRepositoryForConfirmationToken(),
            errorReporter = FakeErrorReporter(),
            intentCreationConfirmationTokenCallbackProvider = Provider {
                failingCreateIntentWithConfirmationTokenCallback()
            },
        )

        val nextStep = interceptor.interceptDefaultNewPaymentMethod()

        assertThat(nextStep).isEqualTo(
            ConfirmationDefinition.Action.Fail<IntentConfirmationDefinition.Args>(
                cause = CreateIntentWithConfirmationTokenCallbackFailureException(TestException()),
                message = resolvableString(R.string.stripe_something_went_wrong),
                errorType = ConfirmationHandler.Result.Failed.ErrorType.Payment,
            )
        )
    }

    @Test
    fun `Returns confirm as next step after creating an unconfirmed intent`() = runInterceptorScenario(
        initializationMode = DEFAULT_DEFERRED_INTENT,
        scenario = InterceptorTestScenario(
            stripeRepository = object : AbsFakeStripeRepository() {
                override suspend fun createConfirmationToken(
                    confirmationTokenParams: ConfirmationTokenParams,
                    options: ApiRequest.Options
                ): Result<ConfirmationToken> {
                    return Result.success(confirmationToken)
                }

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
            intentCreationConfirmationTokenCallbackProvider = Provider {
                CreateIntentWithConfirmationTokenCallback { _ ->
                    CreateIntentResult.Success(clientSecret = "pi_123_secret_456")
                }
            },
        )
    ) { interceptor ->

        val nextStep = interceptor.interceptDefaultNewPaymentMethod()
        assertThat(nextStep).isInstanceOf<ConfirmationDefinition.Action.Launch<IntentConfirmationDefinition.Args>>()
    }

    @Test
    fun `Returns complete as next step after creating and confirming a succeeded intent`() = runInterceptorScenario(
        initializationMode = DEFAULT_DEFERRED_INTENT,
        scenario = InterceptorTestScenario(
            stripeRepository = object : AbsFakeStripeRepository() {
                override suspend fun createConfirmationToken(
                    confirmationTokenParams: ConfirmationTokenParams,
                    options: ApiRequest.Options
                ): Result<ConfirmationToken> {
                    return Result.success(confirmationToken)
                }

                override suspend fun retrieveStripeIntent(
                    clientSecret: String,
                    options: ApiRequest.Options,
                    expandFields: List<String>
                ): Result<StripeIntent> {
                    return Result.success(PaymentIntentFixtures.PI_SUCCEEDED)
                }
            },
            intentCreationConfirmationTokenCallbackProvider = Provider {
                CreateIntentWithConfirmationTokenCallback { _ ->
                    CreateIntentResult.Success(clientSecret = "pi_123_secret_456")
                }
            },
        )
    ) { interceptor ->

        val nextStep = interceptor.interceptDefaultNewPaymentMethod()

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
            initializationMode = DEFAULT_DEFERRED_INTENT,
            scenario = InterceptorTestScenario(
                stripeRepository = object : AbsFakeStripeRepository() {
                    override suspend fun createConfirmationToken(
                        confirmationTokenParams: ConfirmationTokenParams,
                        options: ApiRequest.Options
                    ): Result<ConfirmationToken> {
                        return Result.success(confirmationToken)
                    }

                    override suspend fun retrieveStripeIntent(
                        clientSecret: String,
                        options: ApiRequest.Options,
                        expandFields: List<String>
                    ): Result<StripeIntent> {
                        return Result.success(
                            PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2
                        )
                    }
                },
                intentCreationConfirmationTokenCallbackProvider = Provider {
                    CreateIntentWithConfirmationTokenCallback { _ ->
                        CreateIntentResult.Success("pi_123_secret_456")
                    }
                },
            )
        ) { interceptor ->

            val nextStep = interceptor.interceptDefaultNewPaymentMethod()
            assertThat(nextStep).isEqualTo(
                ConfirmationDefinition.Action.Launch<IntentConfirmationDefinition.Args>(
                    launcherArguments = IntentConfirmationDefinition.Args.NextAction("pi_123_secret_456"),
                    deferredIntentConfirmationType = DeferredIntentConfirmationType.Server,
                    receivesResultInProcess = false,
                )
            )
        }

    @Test
    fun `Returns success as next step if merchant is forcing success`() = runTest {
        val stripeRepository = mock<StripeRepository>()
        whenever(stripeRepository.createConfirmationToken(any(), any()))
            .thenReturn(Result.success(confirmationToken))

        val interceptor = createIntentConfirmationInterceptor(
            initializationMode = DEFAULT_DEFERRED_INTENT,
            stripeRepository = stripeRepository,
            errorReporter = FakeErrorReporter(),
            intentCreationConfirmationTokenCallbackProvider = Provider {
                CreateIntentWithConfirmationTokenCallback { _ ->
                    CreateIntentResult.Success(IntentConfirmationInterceptor.COMPLETE_WITHOUT_CONFIRMING_INTENT)
                }
            },
        )

        val intent = PaymentIntentFactory.create()
        val nextStep = interceptor.interceptDefaultNewPaymentMethod()

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
    fun `Passes correct confirmation token to CreateIntentWithConfirmationTokenCallback`() = runTest {
        val observedTokens = mutableListOf<ConfirmationToken>()

        val interceptor = createIntentConfirmationInterceptor(
            initializationMode = DEFAULT_DEFERRED_INTENT,
            stripeRepository = object : AbsFakeStripeRepository() {
                override suspend fun createConfirmationToken(
                    confirmationTokenParams: ConfirmationTokenParams,
                    options: ApiRequest.Options
                ): Result<ConfirmationToken> {
                    return Result.success(confirmationToken)
                }

                override suspend fun retrieveStripeIntent(
                    clientSecret: String,
                    options: ApiRequest.Options,
                    expandFields: List<String>
                ): Result<StripeIntent> {
                    return Result.success(PaymentIntentFixtures.PI_SUCCEEDED)
                }
            },
            intentCreationConfirmationTokenCallbackProvider = Provider {
                CreateIntentWithConfirmationTokenCallback { token ->
                    observedTokens += token
                    CreateIntentResult.Success("pi_123_secret_456")
                }
            },
        )

        interceptor.interceptDefaultNewPaymentMethod()

        assertThat(observedTokens).hasSize(1)
        assertThat(observedTokens[0]).isEqualTo(confirmationToken)
    }

    @Test
    fun `Includes clientContext in test mode`() = runTest {
        val observedParams = mutableListOf<ConfirmationTokenParams>()

        val interceptor = createIntentConfirmationInterceptor(
            initializationMode = DEFAULT_DEFERRED_INTENT,
            publishableKeyProvider = { "pk_test_123" },
            stripeRepository = object : AbsFakeStripeRepository() {
                override suspend fun createConfirmationToken(
                    confirmationTokenParams: ConfirmationTokenParams,
                    options: ApiRequest.Options
                ): Result<ConfirmationToken> {
                    observedParams += confirmationTokenParams
                    return Result.success(confirmationToken)
                }

                override suspend fun retrieveStripeIntent(
                    clientSecret: String,
                    options: ApiRequest.Options,
                    expandFields: List<String>
                ): Result<StripeIntent> {
                    return Result.success(PaymentIntentFixtures.PI_SUCCEEDED)
                }
            },
            intentCreationConfirmationTokenCallbackProvider = Provider {
                succeedingCreateIntentWithConfirmationTokenCallback(confirmationToken)
            },
        )

        interceptor.interceptDefaultNewPaymentMethod()

        assertThat(observedParams).hasSize(1)
        assertThat(observedParams[0].clientContext).isNotNull()
    }

    @Test
    fun `Does not include clientContext in live mode`() = runTest {
        val observedParams = mutableListOf<ConfirmationTokenParams>()

        val interceptor = createIntentConfirmationInterceptor(
            initializationMode = DEFAULT_DEFERRED_INTENT,
            publishableKeyProvider = { "pk_live_123" },
            stripeRepository = object : AbsFakeStripeRepository() {
                override suspend fun createConfirmationToken(
                    confirmationTokenParams: ConfirmationTokenParams,
                    options: ApiRequest.Options
                ): Result<ConfirmationToken> {
                    observedParams += confirmationTokenParams
                    return Result.success(confirmationToken)
                }

                override suspend fun retrieveStripeIntent(
                    clientSecret: String,
                    options: ApiRequest.Options,
                    expandFields: List<String>
                ): Result<StripeIntent> {
                    return Result.success(PaymentIntentFixtures.PI_SUCCEEDED)
                }
            },
            intentCreationConfirmationTokenCallbackProvider = Provider {
                succeedingCreateIntentWithConfirmationTokenCallback(confirmationToken)
            },
        )

        interceptor.interceptDefaultNewPaymentMethod()

        assertThat(observedParams).hasSize(1)
        assertThat(observedParams[0].clientContext).isNull()
    }

    @Test
    fun `Saved PM - succeed without ephemeralKeySecret if the payment method is not attached`() =
        runInterceptorScenario(
            initializationMode = DEFAULT_DEFERRED_INTENT,
            scenario = InterceptorTestScenario(
                stripeRepository = object : AbsFakeStripeRepository() {
                    override suspend fun createConfirmationToken(
                        confirmationTokenParams: ConfirmationTokenParams,
                        options: ApiRequest.Options
                    ): Result<ConfirmationToken> {
                        return Result.success(confirmationToken)
                    }

                    override suspend fun retrieveStripeIntent(
                        clientSecret: String,
                        options: ApiRequest.Options,
                        expandFields: List<String>
                    ): Result<StripeIntent> {
                        return Result.success(PaymentIntentFixtures.PI_SUCCEEDED)
                    }
                },
                intentCreationConfirmationTokenCallbackProvider = Provider {
                    CreateIntentWithConfirmationTokenCallback { _ ->
                        CreateIntentResult.Success(clientSecret = "pi_123_secret_456")
                    }
                },
            )
        ) { interceptor ->

            val nextStep = interceptor.intercept(
                intent = PaymentIntentFactory.create(),
                confirmationOption = PaymentMethodConfirmationOption.Saved(
                    paymentMethod = PaymentMethodFixtures.AU_BECS_DEBIT,
                    optionsParams = null,
                    passiveCaptchaParams = null,
                    hCaptchaToken = null,
                ),
                shippingValues = null,
            )

            assertThat(nextStep).isEqualTo(
                ConfirmationDefinition.Action.Complete<IntentConfirmationDefinition.Args>(
                    intent = PaymentIntentFixtures.PI_SUCCEEDED,
                    deferredIntentConfirmationType = DeferredIntentConfirmationType.Server,
                    completedFullPaymentFlow = true,
                )
            )
        }

    @Test
    fun `Saved PM - succeed if a ephemeralKeySecret associated with the customer is provided`() =
        runInterceptorScenario(
            initializationMode = DEFAULT_DEFERRED_INTENT,
            scenario = InterceptorTestScenario(
                ephemeralKeySecret = "ek_test_123",
                stripeRepository = object : AbsFakeStripeRepository() {
                    override suspend fun createConfirmationToken(
                        confirmationTokenParams: ConfirmationTokenParams,
                        options: ApiRequest.Options
                    ): Result<ConfirmationToken> {
                        return Result.success(confirmationToken)
                    }

                    override suspend fun retrieveStripeIntent(
                        clientSecret: String,
                        options: ApiRequest.Options,
                        expandFields: List<String>
                    ): Result<StripeIntent> {
                        return Result.success(PaymentIntentFixtures.PI_SUCCEEDED)
                    }
                },
                intentCreationConfirmationTokenCallbackProvider = Provider {
                    CreateIntentWithConfirmationTokenCallback { _ ->
                        CreateIntentResult.Success(clientSecret = "pi_123_secret_456")
                    }
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
    fun `Saved PM - Fails if creating confirmation token did not succeed`() = runTest {
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
            ephemeralKeySecret = "ek_test_123",
            initializationMode = DEFAULT_DEFERRED_INTENT,
            stripeRepository = object : AbsFakeStripeRepository() {
                override suspend fun createConfirmationToken(
                    confirmationTokenParams: ConfirmationTokenParams,
                    options: ApiRequest.Options
                ): Result<ConfirmationToken> {
                    return Result.failure(invalidRequestException)
                }
            },
            intentCreationConfirmationTokenCallbackProvider = Provider {
                CreateIntentWithConfirmationTokenCallback { _ ->
                    CreateIntentResult.Success(clientSecret = "pi_123")
                }
            },
        )

        val nextStep = interceptor.interceptDefaultSavedPaymentMethod()

        assertThat(nextStep).isEqualTo(
            ConfirmationDefinition.Action.Fail<IntentConfirmationDefinition.Args>(
                cause = invalidRequestException,
                message = "Your card is not supported.".resolvableString,
                errorType = ConfirmationHandler.Result.Failed.ErrorType.Payment,
            )
        )
    }

    private fun createFakeStripeRepositoryForConfirmationToken(): StripeRepository {
        return object : AbsFakeStripeRepository() {
            override suspend fun createConfirmationToken(
                confirmationTokenParams: ConfirmationTokenParams,
                options: ApiRequest.Options
            ): Result<ConfirmationToken> {
                return Result.success(confirmationToken)
            }

            override suspend fun retrieveStripeIntent(
                clientSecret: String,
                options: ApiRequest.Options,
                expandFields: List<String>
            ): Result<StripeIntent> {
                return Result.success(PaymentIntentFixtures.PI_SUCCEEDED)
            }
        }
    }

    private fun succeedingCreateIntentWithConfirmationTokenCallback(
        expectedConfirmationToken: ConfirmationToken,
    ): CreateIntentWithConfirmationTokenCallback {
        return CreateIntentWithConfirmationTokenCallback { confirmationToken ->
            assertThat(confirmationToken).isEqualTo(expectedConfirmationToken)
            CreateIntentResult.Success(clientSecret = "pi_123_secret_456")
        }
    }

    private fun failingCreateIntentWithConfirmationTokenCallback(
        message: String? = null
    ): CreateIntentWithConfirmationTokenCallback {
        return CreateIntentWithConfirmationTokenCallback { _ ->
            CreateIntentResult.Failure(
                cause = TestException(message),
                displayMessage = message
            )
        }
    }

    @Test
    fun `New PM - includes mandate when createParams requiresMandate is true`() = runTest {
        val observedParams = mutableListOf<ConfirmationTokenParams>()

        val interceptor = createIntentConfirmationInterceptor(
            initializationMode = DEFAULT_DEFERRED_INTENT,
            stripeRepository = object : AbsFakeStripeRepository() {
                override suspend fun createConfirmationToken(
                    confirmationTokenParams: ConfirmationTokenParams,
                    options: ApiRequest.Options
                ): Result<ConfirmationToken> {
                    observedParams += confirmationTokenParams
                    return Result.success(confirmationToken)
                }

                override suspend fun retrieveStripeIntent(
                    clientSecret: String,
                    options: ApiRequest.Options,
                    expandFields: List<String>
                ): Result<StripeIntent> {
                    return Result.success(PaymentIntentFixtures.PI_SUCCEEDED)
                }
            },
            intentCreationConfirmationTokenCallbackProvider = Provider {
                CreateIntentWithConfirmationTokenCallback { _ ->
                    CreateIntentResult.Success("pi_123_secret_456")
                }
            },
        )

        val createParams = PaymentMethodCreateParams(
            code = "sepa_debit",
            requiresMandate = true,
            sepaDebit = PaymentMethodCreateParams.SepaDebit(iban = "DE89370400440532013000"),
            billingDetails = PaymentMethodCreateParamsFixtures.BILLING_DETAILS,
        )

        val confirmationOption = PaymentMethodConfirmationOption.New(
            createParams = createParams,
            optionsParams = null,
            extraParams = null,
            shouldSave = false,
            passiveCaptchaParams = null,
        )

        interceptor.intercept(
            intent = PaymentIntentFactory.create(),
            confirmationOption = confirmationOption,
            shippingValues = null,
        )

        assertThat(observedParams).hasSize(1)
        assertThat(observedParams[0].mandateDataParams).isNotNull()
    }

    @Test
    fun `New PM - excludes mandate when createParams requiresMandate is false`() = runTest {
        val observedParams = mutableListOf<ConfirmationTokenParams>()

        val interceptor = createIntentConfirmationInterceptor(
            initializationMode = DEFAULT_DEFERRED_INTENT,
            stripeRepository = object : AbsFakeStripeRepository() {
                override suspend fun createConfirmationToken(
                    confirmationTokenParams: ConfirmationTokenParams,
                    options: ApiRequest.Options
                ): Result<ConfirmationToken> {
                    observedParams += confirmationTokenParams
                    return Result.success(confirmationToken)
                }

                override suspend fun retrieveStripeIntent(
                    clientSecret: String,
                    options: ApiRequest.Options,
                    expandFields: List<String>
                ): Result<StripeIntent> {
                    return Result.success(PaymentIntentFixtures.PI_SUCCEEDED)
                }
            },
            intentCreationConfirmationTokenCallbackProvider = Provider {
                CreateIntentWithConfirmationTokenCallback { _ ->
                    CreateIntentResult.Success("pi_123_secret_456")
                }
            },
        )

        val confirmationOption = PaymentMethodConfirmationOption.New(
            createParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
            optionsParams = null,
            extraParams = null,
            shouldSave = false,
            passiveCaptchaParams = null,
        )

        interceptor.intercept(
            intent = PaymentIntentFactory.create(),
            confirmationOption = confirmationOption,
            shippingValues = null,
        )

        assertThat(observedParams).hasSize(1)
        assertThat(observedParams[0].mandateDataParams).isNull()
    }

    @Test
    fun `Saved PM - includes mandate when payment method type requiresMandate is true`() = runTest {
        val observedParams = mutableListOf<ConfirmationTokenParams>()

        val interceptor = createIntentConfirmationInterceptor(
            ephemeralKeySecret = "ek_test_123",
            initializationMode = DEFAULT_DEFERRED_INTENT,
            stripeRepository = object : AbsFakeStripeRepository() {
                override suspend fun createConfirmationToken(
                    confirmationTokenParams: ConfirmationTokenParams,
                    options: ApiRequest.Options
                ): Result<ConfirmationToken> {
                    observedParams += confirmationTokenParams
                    return Result.success(confirmationToken)
                }

                override suspend fun retrieveStripeIntent(
                    clientSecret: String,
                    options: ApiRequest.Options,
                    expandFields: List<String>
                ): Result<StripeIntent> {
                    return Result.success(PaymentIntentFixtures.PI_SUCCEEDED)
                }
            },
            intentCreationConfirmationTokenCallbackProvider = Provider {
                CreateIntentWithConfirmationTokenCallback { _ ->
                    CreateIntentResult.Success("pi_123_secret_456")
                }
            },
        )

        val confirmationOption = PaymentMethodConfirmationOption.Saved(
            paymentMethod = PaymentMethodFixtures.SEPA_DEBIT_PAYMENT_METHOD,
            optionsParams = null,
            passiveCaptchaParams = null,
            hCaptchaToken = null,
        )

        interceptor.intercept(
            intent = PaymentIntentFactory.create(),
            confirmationOption = confirmationOption,
            shippingValues = null,
        )

        assertThat(observedParams).hasSize(1)
        assertThat(observedParams[0].mandateDataParams).isNotNull()
    }

    @Test
    fun `Saved PM - excludes mandate when payment method type requiresMandate is false`() = runTest {
        val observedParams = mutableListOf<ConfirmationTokenParams>()

        val interceptor = createIntentConfirmationInterceptor(
            ephemeralKeySecret = "ek_test_123",
            initializationMode = DEFAULT_DEFERRED_INTENT,
            stripeRepository = object : AbsFakeStripeRepository() {
                override suspend fun createConfirmationToken(
                    confirmationTokenParams: ConfirmationTokenParams,
                    options: ApiRequest.Options
                ): Result<ConfirmationToken> {
                    observedParams += confirmationTokenParams
                    return Result.success(confirmationToken)
                }

                override suspend fun retrieveStripeIntent(
                    clientSecret: String,
                    options: ApiRequest.Options,
                    expandFields: List<String>
                ): Result<StripeIntent> {
                    return Result.success(PaymentIntentFixtures.PI_SUCCEEDED)
                }
            },
            intentCreationConfirmationTokenCallbackProvider = Provider {
                CreateIntentWithConfirmationTokenCallback { _ ->
                    CreateIntentResult.Success(clientSecret = "pi_123_secret_456")
                }
            },
        )

        val confirmationOption = PaymentMethodConfirmationOption.Saved(
            paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
            optionsParams = null,
            passiveCaptchaParams = null,
            hCaptchaToken = null,
        )

        interceptor.intercept(
            intent = PaymentIntentFactory.create(),
            confirmationOption = confirmationOption,
            shippingValues = null,
        )

        assertThat(observedParams).hasSize(1)
        assertThat(observedParams[0].mandateDataParams).isNull()
    }

    @Test
    fun `Includes cvc when requireCvcRecollection is true and cvc is provided`() = runTest {
        val observedParams = mutableListOf<ConfirmationTokenParams>()

        val interceptor = createIntentConfirmationInterceptor(
            ephemeralKeySecret = "ek_test_123",
            initializationMode = PaymentElementLoader.InitializationMode.DeferredIntent(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 1099L,
                        currency = "usd",
                    ),
                    requireCvcRecollection = true
                )
            ),
            stripeRepository = object : AbsFakeStripeRepository() {
                override suspend fun createConfirmationToken(
                    confirmationTokenParams: ConfirmationTokenParams,
                    options: ApiRequest.Options
                ): Result<ConfirmationToken> {
                    observedParams += confirmationTokenParams
                    return Result.success(confirmationToken)
                }

                override suspend fun retrieveStripeIntent(
                    clientSecret: String,
                    options: ApiRequest.Options,
                    expandFields: List<String>
                ): Result<StripeIntent> {
                    return Result.success(PaymentIntentFixtures.PI_SUCCEEDED)
                }
            },
            intentCreationConfirmationTokenCallbackProvider = Provider {
                CreateIntentWithConfirmationTokenCallback { _ ->
                    CreateIntentResult.Success("pi_123_secret_456")
                }
            },
        )

        val confirmationOption = PaymentMethodConfirmationOption.Saved(
            paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
            optionsParams = PaymentMethodOptionsParams.Card(cvc = "123"),
            passiveCaptchaParams = null,
            hCaptchaToken = null,
        )

        interceptor.intercept(
            intent = PaymentIntentFactory.create(),
            confirmationOption = confirmationOption,
            shippingValues = null,
        )

        assertThat(observedParams).hasSize(1)
        assertThat(observedParams[0].cvc).isEqualTo("123")
    }

    @Test
    fun `Excludes cvc when requireCvcRecollection is false`() = runTest {
        val observedParams = mutableListOf<ConfirmationTokenParams>()

        val interceptor = createIntentConfirmationInterceptor(
            ephemeralKeySecret = "ek_test_123",
            initializationMode = PaymentElementLoader.InitializationMode.DeferredIntent(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 1099L,
                        currency = "usd",
                    ),
                    requireCvcRecollection = false
                )
            ),
            stripeRepository = object : AbsFakeStripeRepository() {
                override suspend fun createConfirmationToken(
                    confirmationTokenParams: ConfirmationTokenParams,
                    options: ApiRequest.Options
                ): Result<ConfirmationToken> {
                    observedParams += confirmationTokenParams
                    return Result.success(confirmationToken)
                }

                override suspend fun retrieveStripeIntent(
                    clientSecret: String,
                    options: ApiRequest.Options,
                    expandFields: List<String>
                ): Result<StripeIntent> {
                    return Result.success(PaymentIntentFixtures.PI_SUCCEEDED)
                }
            },
            intentCreationConfirmationTokenCallbackProvider = Provider {
                CreateIntentWithConfirmationTokenCallback { _ ->
                    CreateIntentResult.Success("pi_123_secret_456")
                }
            },
        )

        val confirmationOption = PaymentMethodConfirmationOption.Saved(
            paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
            optionsParams = PaymentMethodOptionsParams.Card(cvc = "123"),
            passiveCaptchaParams = null,
            hCaptchaToken = null,
        )

        interceptor.intercept(
            intent = PaymentIntentFactory.create(),
            confirmationOption = confirmationOption,
            shippingValues = null,
        )

        assertThat(observedParams).hasSize(1)
        assertThat(observedParams[0].cvc).isNull()
    }

    @Test
    fun `Includes requireCvcRecollection in clientContext when true`() = runTest {
        val observedParams = mutableListOf<ConfirmationTokenParams>()

        val interceptor = createIntentConfirmationInterceptor(
            ephemeralKeySecret = "ek_test_123",
            publishableKeyProvider = { "pk_test_123" },
            initializationMode = PaymentElementLoader.InitializationMode.DeferredIntent(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 1099L,
                        currency = "usd",
                    ),
                    requireCvcRecollection = true
                )
            ),
            stripeRepository = object : AbsFakeStripeRepository() {
                override suspend fun createConfirmationToken(
                    confirmationTokenParams: ConfirmationTokenParams,
                    options: ApiRequest.Options
                ): Result<ConfirmationToken> {
                    observedParams += confirmationTokenParams
                    return Result.success(confirmationToken)
                }

                override suspend fun retrieveStripeIntent(
                    clientSecret: String,
                    options: ApiRequest.Options,
                    expandFields: List<String>
                ): Result<StripeIntent> {
                    return Result.success(PaymentIntentFixtures.PI_SUCCEEDED)
                }
            },
            intentCreationConfirmationTokenCallbackProvider = Provider {
                CreateIntentWithConfirmationTokenCallback { _ ->
                    CreateIntentResult.Success("pi_123_secret_456")
                }
            },
        )

        val confirmationOption = PaymentMethodConfirmationOption.Saved(
            paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
            optionsParams = PaymentMethodOptionsParams.Card(cvc = "123"),
            passiveCaptchaParams = null,
            hCaptchaToken = null,
        )

        interceptor.intercept(
            intent = PaymentIntentFactory.create(),
            confirmationOption = confirmationOption,
            shippingValues = null,
        )

        assertThat(observedParams).hasSize(1)
        assertThat(observedParams[0].clientContext?.requireCvcRecollection).isEqualTo(true)
    }
}
