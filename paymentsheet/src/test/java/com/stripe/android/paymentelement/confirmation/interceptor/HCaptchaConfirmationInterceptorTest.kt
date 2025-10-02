package com.stripe.android.paymentelement.confirmation.interceptor

import com.google.common.truth.Truth.assertThat
import com.stripe.android.SharedPaymentTokenSessionPreview
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.paymentelement.confirmation.createIntentConfirmationInterceptor
import com.stripe.android.paymentsheet.CreateIntentCallback
import com.stripe.android.paymentsheet.CreateIntentResult
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.state.PaymentElementLoader.InitializationMode
import com.stripe.android.testing.AbsFakeStripeRepository
import com.stripe.android.testing.PaymentIntentFactory
import com.stripe.android.testing.SetupIntentFactory
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(SharedPaymentTokenSessionPreview::class)
class HCaptchaConfirmationInterceptorTest : DeferredIntentConfirmationInterceptorTest() {
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

    private fun succeedingCreateSetupIntentCallback(
        expectedPaymentMethod: PaymentMethod,
    ): CreateIntentCallback {
        return CreateIntentCallback { paymentMethod, _ ->
            assertThat(paymentMethod).isEqualTo(expectedPaymentMethod)
            CreateIntentResult.Success(clientSecret = "seti_123_secret_456")
        }
    }
}
