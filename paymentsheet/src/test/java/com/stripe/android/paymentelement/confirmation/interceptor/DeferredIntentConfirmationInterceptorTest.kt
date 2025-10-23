package com.stripe.android.paymentelement.confirmation.interceptor

import com.google.common.truth.Truth.assertThat
import com.stripe.android.SharedPaymentTokenSessionPreview
import com.stripe.android.core.StripeError
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.exception.InvalidRequestException
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.isInstanceOf
import com.stripe.android.model.AndroidVerificationObject
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.model.StripeIntent
import com.stripe.android.networking.StripeRepository
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.paymentelement.confirmation.createIntentConfirmationInterceptor
import com.stripe.android.paymentelement.confirmation.intent.CreateIntentCallbackFailureException
import com.stripe.android.paymentelement.confirmation.intent.DeferredIntentConfirmationType
import com.stripe.android.paymentelement.confirmation.intent.IntentConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.intent.IntentConfirmationInterceptor
import com.stripe.android.paymentelement.confirmation.intent.InvalidDeferredIntentUsageException
import com.stripe.android.paymentsheet.CreateIntentCallback
import com.stripe.android.paymentsheet.CreateIntentResult
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.state.PaymentElementLoader.InitializationMode
import com.stripe.android.testing.AbsFakeStripeRepository
import com.stripe.android.testing.PaymentIntentFactory
import com.stripe.android.testing.RadarOptionsFactory
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

@RunWith(RobolectricTestRunner::class)
@OptIn(SharedPaymentTokenSessionPreview::class)
class DeferredIntentConfirmationInterceptorTest {
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
            initializationMode = DEFAULT_DEFERRED_INTENT,
            stripeRepository = object : AbsFakeStripeRepository() {
                override suspend fun createPaymentMethod(
                    paymentMethodCreateParams: PaymentMethodCreateParams,
                    options: ApiRequest.Options
                ): Result<PaymentMethod> {
                    return Result.failure(invalidRequestException)
                }
            },
            intentCreationCallbackProvider = Provider {
                CreateIntentCallback { _, _ ->
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
        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD

        val apiException = APIException(
            requestId = "req_123",
            statusCode = 500,
            message = "Whoopsie",
        )

        val interceptor = createIntentConfirmationInterceptor(
            initializationMode = DEFAULT_DEFERRED_INTENT,
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

        val nextStep = interceptor.interceptDefaultSavedPaymentMethod()

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
            stripeRepository = mock(),
            intentCreationCallbackProvider = {
                failingCreateIntentCallback(
                    message = "that didn't work…"
                )
            },
        )
    ) { interceptor ->

        val nextStep = interceptor.interceptDefaultSavedPaymentMethod()

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
            initializationMode = DEFAULT_DEFERRED_INTENT,
            stripeRepository = mock(),
            intentCreationCallbackProvider = {
                failingCreateIntentCallback()
            },
        )

        val nextStep = interceptor.interceptDefaultSavedPaymentMethod()

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
        initializationMode = DEFAULT_DEFERRED_INTENT,
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
        initializationMode = DEFAULT_DEFERRED_INTENT,
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
            initializationMode = DEFAULT_DEFERRED_INTENT,
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
            initializationMode = DEFAULT_DEFERRED_INTENT,
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

    @Test
    fun `Returns success as next step if merchant is forcing success`() = runTest {
        val stripeRepository = mock<StripeRepository>()

        val interceptor = createIntentConfirmationInterceptor(
            initializationMode = DEFAULT_DEFERRED_INTENT,
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
                initializationMode = DEFAULT_DEFERRED_INTENT,
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
    fun `Returns confirm params with attestationToken for Saved payment method`() = runTest {
        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        val attestationToken = "test_attestation_token"
        val hCaptchaToken = "test_hcaptcha_token"

        val interceptor = createIntentConfirmationInterceptor(
            initializationMode = DEFAULT_DEFERRED_INTENT,
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
            },
        )

        val nextStep = interceptor.intercept(
            intent = PaymentIntentFactory.create(),
            confirmationOption = PaymentMethodConfirmationOption.Saved(
                paymentMethod = paymentMethod,
                optionsParams = null,
                passiveCaptchaParams = null,
                hCaptchaToken = hCaptchaToken,
                attestationToken = attestationToken,
            ),
            shippingValues = null,
        )

        val confirmParams = nextStep.asConfirmParams<ConfirmPaymentIntentParams>()

        assertThat(confirmParams?.radarOptions)
            .isEqualTo(
                RadarOptionsFactory.create(
                    hCaptchaToken = hCaptchaToken,
                    verificationObject = AndroidVerificationObject(
                        androidVerificationToken = attestationToken
                    )
                )
            )
    }

    @Test
    fun `Returns confirm params with null attestationToken when not provided for Saved payment method`() = runTest {
        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        val hCaptchaToken = "test_hcaptcha_token"

        val interceptor = createIntentConfirmationInterceptor(
            initializationMode = DEFAULT_DEFERRED_INTENT,
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
            },
        )

        val nextStep = interceptor.intercept(
            intent = PaymentIntentFactory.create(),
            confirmationOption = PaymentMethodConfirmationOption.Saved(
                paymentMethod = paymentMethod,
                optionsParams = null,
                passiveCaptchaParams = null,
                hCaptchaToken = hCaptchaToken,
                attestationToken = null,
            ),
            shippingValues = null,
        )

        val confirmParams = nextStep.asConfirmParams<ConfirmPaymentIntentParams>()

        assertThat(confirmParams?.radarOptions)
            .isEqualTo(
                RadarOptionsFactory.create(
                    hCaptchaToken = hCaptchaToken,
                    verificationObject = AndroidVerificationObject(null)
                )
            )
    }

    @Test
    fun `Returns confirm with RadarOptions when both tokens are null for Saved payment method`() = runTest {
        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD

        val interceptor = createIntentConfirmationInterceptor(
            initializationMode = DEFAULT_DEFERRED_INTENT,
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
            },
        )

        val nextStep = interceptor.intercept(
            intent = PaymentIntentFactory.create(),
            confirmationOption = PaymentMethodConfirmationOption.Saved(
                paymentMethod = paymentMethod,
                optionsParams = null,
                passiveCaptchaParams = null,
                hCaptchaToken = null,
                attestationToken = null,
            ),
            shippingValues = null,
        )

        val confirmParams = nextStep.asConfirmParams<ConfirmPaymentIntentParams>()

        assertThat(confirmParams?.radarOptions)
            .isEqualTo(
                RadarOptionsFactory.create(
                    hCaptchaToken = null,
                    verificationObject = AndroidVerificationObject(null)
                )
            )
    }

    @Test
    fun `attestationToken flows correctly from Saved option to confirm params through creation flow`() = runTest {
        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        val attestationToken = "attestation_token_123"

        val interceptor = createIntentConfirmationInterceptor(
            initializationMode = DEFAULT_DEFERRED_INTENT,
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
                CreateIntentCallback { pm, _ ->
                    assertThat(pm).isEqualTo(paymentMethod)
                    CreateIntentResult.Success("pi_123_secret_456")
                }
            },
        )

        val nextStep = interceptor.intercept(
            intent = PaymentIntentFactory.create(),
            confirmationOption = PaymentMethodConfirmationOption.Saved(
                paymentMethod = paymentMethod,
                optionsParams = null,
                passiveCaptchaParams = null,
                hCaptchaToken = null,
                attestationToken = attestationToken,
            ),
            shippingValues = null,
        )

        val confirmParams = nextStep.asConfirmParams<ConfirmPaymentIntentParams>()

        assertThat(confirmParams?.radarOptions)
            .isEqualTo(
                RadarOptionsFactory.create(
                    hCaptchaToken = null,
                    verificationObject = AndroidVerificationObject(
                        androidVerificationToken = attestationToken
                    )
                )
            )
    }

    companion object {
        internal val DEFAULT_DEFERRED_INTENT = InitializationMode.DeferredIntent(
            intentConfiguration = PaymentSheet.IntentConfiguration(
                mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                    amount = 1099L,
                    currency = "usd",
                ),
            ),
        )
    }
}

internal class TestException(message: String? = null) : Exception(message) {

    override fun hashCode(): Int {
        return Objects.hash(message)
    }

    override fun equals(other: Any?): Boolean {
        return other is TestException && other.message == message
    }
}

internal fun succeedingCreateIntentCallback(
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
