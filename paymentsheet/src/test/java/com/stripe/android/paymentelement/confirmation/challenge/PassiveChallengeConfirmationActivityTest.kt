package com.stripe.android.paymentelement.confirmation.challenge

import android.app.Activity
import android.app.Application
import android.app.Instrumentation
import android.content.Intent
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.rule.IntentsRule
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.challenge.PassiveChallengeActivityContract
import com.stripe.android.challenge.PassiveChallengeActivityResult
import com.stripe.android.model.PassiveCaptchaParamsFactory
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.model.RadarOptions
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.ConfirmationTestScenario
import com.stripe.android.paymentelement.confirmation.PaymentElementConfirmationTestActivity
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.paymentelement.confirmation.assertComplete
import com.stripe.android.paymentelement.confirmation.assertConfirming
import com.stripe.android.paymentelement.confirmation.assertIdle
import com.stripe.android.paymentelement.confirmation.assertSucceeded
import com.stripe.android.paymentelement.confirmation.paymentElementConfirmationTest
import com.stripe.android.payments.paymentlauncher.InternalPaymentResult
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.createTestActivityRule
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.testing.PaymentIntentFactory
import com.stripe.android.testing.PaymentMethodFactory
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class PassiveChallengeConfirmationActivityTest {
    private val application = ApplicationProvider.getApplicationContext<Application>()

    @get:Rule
    val intentsRule = IntentsRule()

    @get:Rule
    val testActivityRule = createTestActivityRule<PaymentElementConfirmationTestActivity>()

    @Test
    fun `On PassiveChallenge params provided, should launch challenge flow then intent flow with success`() = test {
        intendingPassiveChallengeToBeLaunched(
            PassiveChallengeActivityResult.Success("test_token")
        )
        intendingPaymentConfirmationToBeLaunched(
            InternalPaymentResult.Completed(PAYMENT_INTENT.copy(paymentMethod = PAYMENT_METHOD))
        )

        confirmationHandler.state.test {
            awaitItem().assertIdle()

            confirmationHandler.start(CONFIRMATION_ARGUMENTS)

            val confirmingWithChallengeOption = awaitItem().assertConfirming()

            assertThat(confirmingWithChallengeOption.option).isEqualTo(CONFIRMATION_OPTION)

            intendedPassiveChallengeToBeLaunched()

            val confirmingWithNewOption = awaitItem().assertConfirming()

            assertThat(confirmingWithNewOption.option)
                .isEqualTo(
                    PaymentMethodConfirmationOption.New(
                        createParams = CONFIRMATION_OPTION.createParams.copy(
                            radarOptions = RadarOptions("test_token")
                        ),
                        optionsParams = CONFIRMATION_OPTION.optionsParams,
                        extraParams = null,
                        shouldSave = false,
                        passiveCaptchaParams = null
                    )
                )

            intendedPaymentConfirmationToBeLaunched()

            val successResult = awaitItem().assertComplete().result.assertSucceeded()

            assertThat(successResult.intent).isEqualTo(PAYMENT_INTENT.copy(paymentMethod = PAYMENT_METHOD))
            assertThat(successResult.deferredIntentConfirmationType).isNull()
        }
    }

    @Test
    fun `On PassiveChallenge params provided, should launch challenge flow then intent flow with failure`() = test {
        intendingPassiveChallengeToBeLaunched(
            PassiveChallengeActivityResult.Failed(RuntimeException("Challenge failed"))
        )
        intendingPaymentConfirmationToBeLaunched(
            InternalPaymentResult.Completed(PAYMENT_INTENT.copy(paymentMethod = PAYMENT_METHOD))
        )

        confirmationHandler.state.test {
            awaitItem().assertIdle()

            confirmationHandler.start(CONFIRMATION_ARGUMENTS)

            val confirmingWithChallengeOption = awaitItem().assertConfirming()

            assertThat(confirmingWithChallengeOption.option).isEqualTo(CONFIRMATION_OPTION)

            intendedPassiveChallengeToBeLaunched()

            val confirmingWithNewOption = awaitItem().assertConfirming()

            assertThat(confirmingWithNewOption.option)
                .isEqualTo(
                    PaymentMethodConfirmationOption.New(
                        createParams = CONFIRMATION_OPTION.createParams,
                        optionsParams = CONFIRMATION_OPTION.optionsParams,
                        extraParams = null,
                        shouldSave = false,
                        passiveCaptchaParams = null
                    )
                )

            intendedPaymentConfirmationToBeLaunched()

            val successResult = awaitItem().assertComplete().result.assertSucceeded()

            assertThat(successResult.intent).isEqualTo(PAYMENT_INTENT.copy(paymentMethod = PAYMENT_METHOD))
            assertThat(successResult.deferredIntentConfirmationType).isNull()
        }
    }

    @Test
    fun `On PassiveChallenge params provided with Saved option, should launch challenge flow then intent flow`() =
        test {
            intendingPassiveChallengeToBeLaunched(
                PassiveChallengeActivityResult.Success("test_token")
            )
            intendingPaymentConfirmationToBeLaunched(
                InternalPaymentResult.Completed(PAYMENT_INTENT.copy(paymentMethod = PAYMENT_METHOD))
            )

            val savedConfirmationArgs = CONFIRMATION_ARGUMENTS.copy(
                confirmationOption = SAVED_CONFIRMATION_OPTION
            )

            confirmationHandler.state.test {
                awaitItem().assertIdle()

                confirmationHandler.start(savedConfirmationArgs)

                val confirmingWithChallengeOption = awaitItem().assertConfirming()

                assertThat(confirmingWithChallengeOption.option).isEqualTo(SAVED_CONFIRMATION_OPTION)

                intendedPassiveChallengeToBeLaunched()

                val confirmingWithNewOption = awaitItem().assertConfirming()

                assertThat(confirmingWithNewOption.option)
                    .isEqualTo(
                        PaymentMethodConfirmationOption.Saved(
                            paymentMethod = SAVED_CONFIRMATION_OPTION.paymentMethod,
                            optionsParams = SAVED_CONFIRMATION_OPTION.optionsParams,
                            originatedFromWallet = false,
                            passiveCaptchaParams = null,
                            hCaptchaToken = "test_token"
                        )
                    )

                intendedPaymentConfirmationToBeLaunched()

                val successResult = awaitItem().assertComplete().result.assertSucceeded()

                assertThat(successResult.intent).isEqualTo(PAYMENT_INTENT.copy(paymentMethod = PAYMENT_METHOD))
                assertThat(successResult.deferredIntentConfirmationType).isNull()
            }
        }

    private fun test(
        test: suspend ConfirmationTestScenario.() -> Unit
    ) {
        paymentElementConfirmationTest(application, test)
    }

    private fun intendingPassiveChallengeToBeLaunched(result: PassiveChallengeActivityResult) {
        intending(hasComponent(PASSIVE_CHALLENGE_ACTIVITY_NAME)).respondWith(
            Instrumentation.ActivityResult(
                Activity.RESULT_OK,
                Intent().putExtra(PassiveChallengeActivityContract.EXTRA_RESULT, result)
            )
        )
    }

    private fun intendingPaymentConfirmationToBeLaunched(result: InternalPaymentResult) {
        intending(hasComponent(PAYMENT_CONFIRMATION_LAUNCHER_ACTIVITY_NAME)).respondWith(
            Instrumentation.ActivityResult(
                Activity.RESULT_OK,
                Intent().putExtras(bundleOf("extra_args" to result))
            )
        )
    }

    private fun intendedPassiveChallengeToBeLaunched() {
        intended(hasComponent(PASSIVE_CHALLENGE_ACTIVITY_NAME))
    }

    private fun intendedPaymentConfirmationToBeLaunched() {
        intended(hasComponent(PAYMENT_CONFIRMATION_LAUNCHER_ACTIVITY_NAME))
    }

    private companion object {
        val PAYMENT_INTENT = PaymentIntentFactory.create()

        val PAYMENT_METHOD = PaymentMethodFactory.card()

        val PASSIVE_CAPTCHA_PARAMS = PassiveCaptchaParamsFactory.passiveCaptchaParams()

        val CONFIRMATION_OPTION = PaymentMethodConfirmationOption.New(
            createParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
            optionsParams = null,
            extraParams = null,
            shouldSave = false,
            passiveCaptchaParams = PASSIVE_CAPTCHA_PARAMS
        )

        val SAVED_CONFIRMATION_OPTION = PaymentMethodConfirmationOption.Saved(
            paymentMethod = PAYMENT_METHOD,
            optionsParams = null,
            originatedFromWallet = false,
            passiveCaptchaParams = PASSIVE_CAPTCHA_PARAMS,
            hCaptchaToken = null
        )

        val CONFIRMATION_ARGUMENTS = ConfirmationHandler.Args(
            confirmationOption = CONFIRMATION_OPTION,
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = "pi_123_secret_123"
            ),
            shippingDetails = AddressDetails(),
            intent = PAYMENT_INTENT,
            appearance = PaymentSheet.Appearance(),
        )

        const val PASSIVE_CHALLENGE_ACTIVITY_NAME =
            "com.stripe.android.challenge.PassiveChallengeActivity"

        const val PAYMENT_CONFIRMATION_LAUNCHER_ACTIVITY_NAME =
            "com.stripe.android.payments.paymentlauncher.PaymentLauncherConfirmationActivity"
    }
}
