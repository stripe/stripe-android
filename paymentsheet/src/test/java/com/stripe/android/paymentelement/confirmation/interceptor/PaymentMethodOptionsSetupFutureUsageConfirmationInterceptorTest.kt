package com.stripe.android.paymentelement.confirmation.interceptor

import com.google.common.truth.Truth.assertThat
import com.stripe.android.SharedPaymentTokenSessionPreview
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentelement.PaymentMethodOptionsSetupFutureUsagePreview
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.paymentelement.confirmation.createIntentConfirmationInterceptor
import com.stripe.android.paymentsheet.CreateIntentCallback
import com.stripe.android.paymentsheet.CreateIntentResult
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.state.PaymentElementLoader.InitializationMode
import com.stripe.android.testing.AbsFakeStripeRepository
import com.stripe.android.testing.PaymentIntentFactory
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(
    PaymentMethodOptionsSetupFutureUsagePreview::class,
    SharedPaymentTokenSessionPreview::class
)
class PaymentMethodOptionsSetupFutureUsageConfirmationInterceptorTest {
    @Test
    fun `Sets shouldSavePaymentMethod to true for CreateIntentCallback if top level SFU is set with PMO`() = runTest {
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

    @Test
    fun `Sets shouldSavePaymentMethod to true for CreateIntentCallback if PMO SFU is set`() = runTest {
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
                    passiveCaptchaParams = null,
                    clientAttributionMetadata = null,
                    attestationRequired = false
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
                    passiveCaptchaParams = null,
                    clientAttributionMetadata = null,
                    attestationRequired = false
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
}
