package com.stripe.android.paymentelement.confirmation.attestation

import android.app.Activity
import android.app.Application
import android.app.Instrumentation
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.rule.IntentsRule
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.attestation.AttestationActivityContract
import com.stripe.android.attestation.AttestationActivityResult
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.AndroidVerificationObject
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.ConfirmationTestScenario
import com.stripe.android.paymentelement.confirmation.PAYMENT_INTENT
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
import com.stripe.android.testing.RadarOptionsFactory
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class AttestationConfirmationActivityTest {
    private val application = ApplicationProvider.getApplicationContext<Application>()

    @get:Rule
    val intentsRule = IntentsRule()

    @get:Rule
    val testActivityRule = createTestActivityRule<PaymentElementConfirmationTestActivity>()

    @Test
    fun `on attestation success with New option, should attach token and proceed to payment confirmation`() = test {
        val testToken = "test_attestation_token"
        val paymentMethod = PaymentMethodFactory.card()

        intendingAttestationToBeLaunched(AttestationActivityResult.Success(testToken))
        intendingPaymentConfirmationToBeLaunched(
            InternalPaymentResult.Completed(PAYMENT_INTENT.copy(paymentMethod = paymentMethod))
        )

        // Bootstrap the confirmation handler with attestation enabled
        confirmationHandler.bootstrap(
            PaymentMethodMetadataFactory.create(attestOnIntentConfirmation = true)
        )

        confirmationHandler.state.test {
            awaitItem().assertIdle()

            confirmationHandler.start(CONFIRMATION_ARGUMENTS_NEW)

            val confirmingWithNewOption = awaitItem().assertConfirming()

            assertThat(confirmingWithNewOption.option).isEqualTo(NEW_CONFIRMATION_OPTION)

            intendedAttestationToBeLaunched()

            val confirmingWithAttestationToken = awaitItem().assertConfirming()

            // Verify the token was attached to the payment method
            val optionWithToken = confirmingWithAttestationToken.option as PaymentMethodConfirmationOption.New
            assertThat(optionWithToken.createParams.radarOptions)
                .isEqualTo(
                    RadarOptionsFactory.create(
                        hCaptchaToken = null,
                        verificationObject = AndroidVerificationObject(
                            androidVerificationToken = testToken
                        )
                    )
                )

            intendedPaymentConfirmationToBeLaunched()

            val successResult = awaitItem().assertComplete().result.assertSucceeded()

            assertThat(successResult.intent).isEqualTo(PAYMENT_INTENT.copy(paymentMethod = paymentMethod))
            assertThat(successResult.deferredIntentConfirmationType).isNull()
        }
    }

    @Test
    fun `on attestation success with Saved option, should attach token and proceed to payment confirmation`() = test {
        val testToken = "test_attestation_token"
        val paymentMethod = PaymentMethodFactory.card()

        intendingAttestationToBeLaunched(AttestationActivityResult.Success(testToken))
        intendingPaymentConfirmationToBeLaunched(
            InternalPaymentResult.Completed(PAYMENT_INTENT.copy(paymentMethod = paymentMethod))
        )

        // Bootstrap the confirmation handler with attestation enabled
        confirmationHandler.bootstrap(
            PaymentMethodMetadataFactory.create(attestOnIntentConfirmation = true)
        )

        confirmationHandler.state.test {
            awaitItem().assertIdle()

            confirmationHandler.start(CONFIRMATION_ARGUMENTS_SAVED)

            val confirmingWithSavedOption = awaitItem().assertConfirming()

            assertThat(confirmingWithSavedOption.option).isEqualTo(SAVED_CONFIRMATION_OPTION)

            intendedAttestationToBeLaunched()

            val confirmingWithAttestationToken = awaitItem().assertConfirming()

            // Verify the token was attached to the saved payment method option
            val optionWithToken = confirmingWithAttestationToken.option as PaymentMethodConfirmationOption.Saved
            assertThat(optionWithToken.attestationToken).isEqualTo(testToken)

            intendedPaymentConfirmationToBeLaunched()

            val successResult = awaitItem().assertComplete().result.assertSucceeded()

            assertThat(successResult.intent).isEqualTo(PAYMENT_INTENT.copy(paymentMethod = paymentMethod))
            assertThat(successResult.deferredIntentConfirmationType).isNull()
        }
    }

    @Test
    fun `on attestation failure with New option, should proceed without token`() = test {
        val testError = Exception("Attestation failed")
        val paymentMethod = PaymentMethodFactory.card()

        intendingAttestationToBeLaunched(AttestationActivityResult.Failed(testError))
        intendingPaymentConfirmationToBeLaunched(
            InternalPaymentResult.Completed(PAYMENT_INTENT.copy(paymentMethod = paymentMethod))
        )

        // Bootstrap the confirmation handler with attestation enabled
        confirmationHandler.bootstrap(
            PaymentMethodMetadataFactory.create(attestOnIntentConfirmation = true)
        )

        confirmationHandler.state.test {
            awaitItem().assertIdle()

            confirmationHandler.start(CONFIRMATION_ARGUMENTS_NEW)

            val confirmingWithNewOption = awaitItem().assertConfirming()

            assertThat(confirmingWithNewOption.option).isEqualTo(NEW_CONFIRMATION_OPTION)

            intendedAttestationToBeLaunched()

            val successResult = awaitItem().assertComplete().result.assertSucceeded()

            intendedPaymentConfirmationToBeLaunched()
            assertThat(successResult.intent).isEqualTo(PAYMENT_INTENT.copy(paymentMethod = paymentMethod))
            assertThat(successResult.deferredIntentConfirmationType).isNull()
        }
    }

    @Test
    fun `on attestation failure with Saved option, should proceed without token`() = test {
        val testError = Exception("Attestation failed")
        val paymentMethod = PaymentMethodFactory.card()

        intendingAttestationToBeLaunched(AttestationActivityResult.Failed(testError))
        intendingPaymentConfirmationToBeLaunched(
            InternalPaymentResult.Completed(PAYMENT_INTENT.copy(paymentMethod = paymentMethod))
        )

        // Bootstrap the confirmation handler with attestation enabled
        confirmationHandler.bootstrap(
            PaymentMethodMetadataFactory.create(attestOnIntentConfirmation = true)
        )

        confirmationHandler.state.test {
            awaitItem().assertIdle()

            confirmationHandler.start(CONFIRMATION_ARGUMENTS_SAVED)

            val confirmingWithSavedOption = awaitItem().assertConfirming()

            assertThat(confirmingWithSavedOption.option).isEqualTo(SAVED_CONFIRMATION_OPTION)

            intendedAttestationToBeLaunched()

            val successResult = awaitItem().assertComplete().result.assertSucceeded()

            intendedPaymentConfirmationToBeLaunched()
            assertThat(successResult.intent).isEqualTo(PAYMENT_INTENT.copy(paymentMethod = paymentMethod))
            assertThat(successResult.deferredIntentConfirmationType).isNull()
        }
    }

    private fun test(
        test: suspend ConfirmationTestScenario.() -> Unit
    ) = paymentElementConfirmationTest(application, test)

    private fun intendingAttestationToBeLaunched(result: AttestationActivityResult) {
        intending(hasComponent(ATTESTATION_ACTIVITY_NAME)).respondWith(
            Instrumentation.ActivityResult(
                Activity.RESULT_OK,
                Intent().putExtra(AttestationActivityContract.EXTRA_RESULT, result)
            )
        )
    }

    private fun intendingPaymentConfirmationToBeLaunched(result: InternalPaymentResult) {
        intending(hasComponent(PAYMENT_CONFIRMATION_LAUNCHER_ACTIVITY_NAME)).respondWith(
            Instrumentation.ActivityResult(
                Activity.RESULT_OK,
                Intent().putExtra("extra_args", result)
            )
        )
    }

    private fun intendedAttestationToBeLaunched() {
        intended(hasComponent(ATTESTATION_ACTIVITY_NAME))
    }

    private fun intendedPaymentConfirmationToBeLaunched() {
        intended(hasComponent(PAYMENT_CONFIRMATION_LAUNCHER_ACTIVITY_NAME))
    }

    private companion object {
        private val PAYMENT_INTENT = PaymentIntentFactory.create().copy(
            id = "pm_1",
            amount = 5000,
            currency = "USD",
        )

        private val NEW_CONFIRMATION_OPTION = PaymentMethodConfirmationOption.New(
            createParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
            optionsParams = null,
            extraParams = null,
            shouldSave = false,
            passiveCaptchaParams = null,
        )

        private val SAVED_CONFIRMATION_OPTION = PaymentMethodConfirmationOption.Saved(
            paymentMethod = PaymentMethodFactory.card(),
            optionsParams = null,
            originatedFromWallet = false,
            passiveCaptchaParams = null,
            hCaptchaToken = null,
        )

        private val CONFIRMATION_ARGUMENTS_NEW = ConfirmationHandler.Args(
            confirmationOption = NEW_CONFIRMATION_OPTION,
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = "pi_123_secret_123"
            ),
            shippingDetails = AddressDetails(),
            intent = PAYMENT_INTENT,
            appearance = PaymentSheet.Appearance(),
        )

        private val CONFIRMATION_ARGUMENTS_SAVED = ConfirmationHandler.Args(
            confirmationOption = SAVED_CONFIRMATION_OPTION,
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = "pi_123_secret_123"
            ),
            shippingDetails = AddressDetails(),
            intent = PAYMENT_INTENT,
            appearance = PaymentSheet.Appearance(),
        )

        const val ATTESTATION_ACTIVITY_NAME =
            "com.stripe.android.attestation.AttestationActivity"

        const val PAYMENT_CONFIRMATION_LAUNCHER_ACTIVITY_NAME =
            "com.stripe.android.payments.paymentlauncher.PaymentLauncherConfirmationActivity"
    }
}
