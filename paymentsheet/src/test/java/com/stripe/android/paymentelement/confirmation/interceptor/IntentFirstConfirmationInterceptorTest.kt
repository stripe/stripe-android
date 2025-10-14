package com.stripe.android.paymentelement.confirmation.interceptor

import com.google.common.truth.Truth.assertThat
import com.stripe.android.SharedPaymentTokenSessionPreview
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.paymentelement.confirmation.createIntentConfirmationInterceptor
import com.stripe.android.paymentsheet.state.PaymentElementLoader.InitializationMode
import com.stripe.android.testing.PaymentIntentFactory
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
                    attestationRequired = false
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
                    passiveCaptchaParams = null,
                    clientAttributionMetadata = null,
                    attestationRequired = false
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
                    passiveCaptchaParams = null,
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
                    passiveCaptchaParams = null,
                    clientAttributionMetadata = null,
                    attestationRequired = false
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

    @OptIn(SharedPaymentTokenSessionPreview::class)
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
                passiveCaptchaParams = null,
                attestationRequired = false
            ),
            intent = PaymentIntentFactory.create(),
            shippingValues = null,
        )

        return nextStep.asConfirmParams()
    }

    @OptIn(SharedPaymentTokenSessionPreview::class)
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
                passiveCaptchaParams = null,
                attestationRequired = false
            ),
            intent = SetupIntentFactory.create(),
            shippingValues = null,
        )

        return nextStep.asConfirmParams()
    }
}
