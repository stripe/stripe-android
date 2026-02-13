package com.stripe.android.paymentelement.confirmation.interceptor

import com.google.common.truth.Truth.assertThat
import com.stripe.android.SharedPaymentTokenSessionPreview
import com.stripe.android.lpmfoundations.paymentmethod.IntegrationMetadata
import com.stripe.android.model.AndroidVerificationObject
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.paymentelement.confirmation.ConfirmationChallengeState
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.paymentelement.confirmation.createIntentConfirmationInterceptor
import com.stripe.android.testing.PaymentIntentFactory
import com.stripe.android.testing.RadarOptionsFactory
import com.stripe.android.testing.SetupIntentFactory
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class IntentFirstConfirmationInterceptorTest {
    @Test
    fun `Returns confirm as next step if invoked with client secret for existing payment method`() =
        runInterceptorScenario(
            integrationMetadata = IntegrationMetadata.IntentFirst("pi_1234_secret_4321"),
        ) { interceptor ->

            val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
            val nextStep = interceptor.intercept(
                intent = PaymentIntentFactory.create(),
                confirmationOption = PaymentMethodConfirmationOption.Saved(
                    paymentMethod = paymentMethod,
                    optionsParams = null,
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
            integrationMetadata = IntegrationMetadata.IntentFirst("pi_1234_secret_4321"),
        ) { interceptor ->

            val createParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD
            val nextStep = interceptor.intercept(
                intent = PaymentIntentFactory.create(),
                confirmationOption = PaymentMethodConfirmationOption.New(
                    createParams = createParams,
                    optionsParams = null,
                    extraParams = null,
                    shouldSave = false,
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
            integrationMetadata = IntegrationMetadata.IntentFirst("pi_1234_secret_4321"),
        ) { interceptor ->
            val nextStep = interceptor.intercept(
                intent = PaymentIntentFactory.create(),
                confirmationOption = PaymentMethodConfirmationOption.Saved(
                    paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
                    optionsParams = PaymentMethodOptionsParams.Card(
                        setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.OffSession
                    ),
                ),
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
    fun `Returns confirm params with all challenge state fields for Saved payment method`() = runTest {
        val challengeState = ConfirmationChallengeState(
            hCaptchaToken = "test_hcaptcha_token",
            attestationToken = "test_attestation_token",
            appId = "com.stripe.test",
        )

        val confirmParams = interceptWithSavedPaymentMethod(challengeState)

        assertRadarOptionsEquals(
            confirmParams = confirmParams,
            expectedRadarOptions = challengeState.toExpectedRadarOptions()
        )
    }

    @Test
    fun `Returns confirm params with null attestationToken when not provided for Saved payment method`() = runTest {
        val challengeState = ConfirmationChallengeState(hCaptchaToken = "test_hcaptcha_token")

        val confirmParams = interceptWithSavedPaymentMethod(challengeState)

        assertRadarOptionsEquals(
            confirmParams = confirmParams,
            expectedRadarOptions = challengeState.toExpectedRadarOptions()
        )
    }

    @Test
    fun `Returns confirm with RadarOptions when both tokens are null for Saved payment method`() = runTest {
        val challengeState = ConfirmationChallengeState()

        val confirmParams = interceptWithSavedPaymentMethod(challengeState)

        assertRadarOptionsEquals(
            confirmParams = confirmParams,
            expectedRadarOptions = challengeState.toExpectedRadarOptions()
        )
    }

    @Test
    fun `Returns confirm params with only attestationToken for Saved payment method`() = runTest {
        val challengeState = ConfirmationChallengeState(
            attestationToken = "attestation_token_123",
            appId = "com.stripe.test.app",
        )

        val confirmParams = interceptWithSavedPaymentMethod(challengeState)

        assertRadarOptionsEquals(
            confirmParams = confirmParams,
            expectedRadarOptions = challengeState.toExpectedRadarOptions()
        )
    }

    @Test
    fun `radarOptions is null for new payment method confirmation option`() =
        runInterceptorScenario(
            integrationMetadata = IntegrationMetadata.IntentFirst("pi_1234_secret_4321"),
        ) { interceptor ->
            val nextStep = interceptor.intercept(
                confirmationOption = PaymentMethodConfirmationOption.New(
                    createParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
                    optionsParams = null,
                    extraParams = null,
                    shouldSave = false,
                ),
                intent = PaymentIntentFactory.create(),
                shippingValues = null,
            )

            val confirmParams = nextStep.asConfirmParams<ConfirmPaymentIntentParams>()
            assertThat(confirmParams?.radarOptions).isNull()
        }

    @Test
    fun `Returns confirm params with all challenge state fields for SetupIntent`() = runTest {
        val challengeState = ConfirmationChallengeState(
            hCaptchaToken = "test_hcaptcha_token",
            attestationToken = "test_attestation_token",
            appId = "com.stripe.test",
        )

        val confirmParams = interceptWithSetupIntent(challengeState)

        assertRadarOptionsEquals(
            confirmParams = confirmParams,
            expectedRadarOptions = challengeState.toExpectedRadarOptions()
        )
    }

    @Test
    fun `Returns confirm params with null attestationToken when not provided for SetupIntent`() = runTest {
        val challengeState = ConfirmationChallengeState(hCaptchaToken = "test_hcaptcha_token")

        val confirmParams = interceptWithSetupIntent(challengeState)

        assertRadarOptionsEquals(
            confirmParams = confirmParams,
            expectedRadarOptions = challengeState.toExpectedRadarOptions()
        )
    }

    @Test
    fun `Returns confirm with RadarOptions when both tokens are null for SetupIntent`() = runTest {
        val challengeState = ConfirmationChallengeState()

        val confirmParams = interceptWithSetupIntent(challengeState)

        assertRadarOptionsEquals(
            confirmParams = confirmParams,
            expectedRadarOptions = challengeState.toExpectedRadarOptions()
        )
    }

    @Test
    fun `Returns confirm params with only attestationToken for SetupIntent`() = runTest {
        val challengeState = ConfirmationChallengeState(
            attestationToken = "attestation_token_123",
            appId = "com.stripe.test.app",
        )

        val confirmParams = interceptWithSetupIntent(challengeState)

        assertRadarOptionsEquals(
            confirmParams = confirmParams,
            expectedRadarOptions = challengeState.toExpectedRadarOptions()
        )
    }

    @OptIn(SharedPaymentTokenSessionPreview::class)
    private suspend fun interceptWithSavedPaymentMethod(
        challengeState: ConfirmationChallengeState = ConfirmationChallengeState()
    ): ConfirmPaymentIntentParams? {
        val interceptor = createIntentConfirmationInterceptor(
            integrationMetadata = IntegrationMetadata.IntentFirst("pi_1234_secret_4321"),
        )

        val nextStep = interceptor.intercept(
            intent = PaymentIntentFactory.create(),
            confirmationOption = PaymentMethodConfirmationOption.Saved(
                paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
                optionsParams = null,
                confirmationChallengeState = challengeState,
            ),
            shippingValues = null,
        )

        return nextStep.asConfirmParams()
    }

    @OptIn(SharedPaymentTokenSessionPreview::class)
    private suspend fun interceptWithSetupIntent(
        challengeState: ConfirmationChallengeState = ConfirmationChallengeState()
    ): ConfirmSetupIntentParams? {
        val interceptor = createIntentConfirmationInterceptor(
            integrationMetadata = IntegrationMetadata.IntentFirst("seti_1234_secret_4321"),
        )

        val nextStep = interceptor.intercept(
            intent = SetupIntentFactory.create(),
            confirmationOption = PaymentMethodConfirmationOption.Saved(
                paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
                optionsParams = null,
                confirmationChallengeState = challengeState,
            ),
            shippingValues = null,
        )

        return nextStep.asConfirmParams()
    }

    private fun ConfirmationChallengeState.toExpectedRadarOptions() = RadarOptionsFactory.create(
        hCaptchaToken = hCaptchaToken,
        verificationObject = AndroidVerificationObject(
            androidVerificationToken = attestationToken,
            appId = appId
        )
    )
}
