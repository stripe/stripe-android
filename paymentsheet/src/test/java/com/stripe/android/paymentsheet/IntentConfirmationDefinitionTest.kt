package com.stripe.android.paymentsheet

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.payments.paymentlauncher.InternalPaymentResult
import com.stripe.android.payments.paymentlauncher.PaymentLauncher
import com.stripe.android.payments.paymentlauncher.PaymentLauncherContract
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.addresselement.toConfirmPaymentIntentShipping
import com.stripe.android.testing.FakePaymentLauncher
import com.stripe.android.utils.FakeIntentConfirmationInterceptor
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class IntentConfirmationDefinitionTest {
    @Test
    fun `'createLauncher' should call factory when creating launcher`() {
        val launcher = FakePaymentLauncher()

        val definition = createIntentConfirmationDefinition(
            paymentLauncher = launcher,
        )

        val createdLauncher = definition.createLauncher(
            activityResultCaller = mock {
                on {
                    registerForActivityResult<PaymentLauncherContract.Args, InternalPaymentResult>(any(), any())
                } doReturn mock()
            },
            onResult = {}
        )

        assertThat(createdLauncher).isEqualTo(launcher)
    }

    @Test
    fun `On 'action' with new payment method, should call 'IntentConfirmationInterceptor' with expected params`() =
        runTest {
            val intentConfirmationInterceptor = FakeIntentConfirmationInterceptor().apply {
                enqueueCompleteStep()
            }

            val definition = createIntentConfirmationDefinition(
                intentConfirmationInterceptor = intentConfirmationInterceptor,
            )

            definition.action(
                confirmationOption = PaymentConfirmationOption.PaymentMethod.New(
                    initializationMode = PaymentSheet.InitializationMode.PaymentIntent(
                        clientSecret = "pi_123"
                    ),
                    createParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
                    optionsParams = null,
                    shippingDetails = AddressDetails(name = "John Doe"),
                    shouldSave = true,
                ),
                intent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            )

            assertThat(intentConfirmationInterceptor.calls.awaitItem()).isEqualTo(
                FakeIntentConfirmationInterceptor.InterceptCall.WithNewPaymentMethod(
                    initializationMode = PaymentSheet.InitializationMode.PaymentIntent(
                        clientSecret = "pi_123"
                    ),
                    paymentMethodCreateParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
                    paymentMethodOptionsParams = null,
                    shippingValues = AddressDetails(name = "John Doe").toConfirmPaymentIntentShipping(),
                    customerRequestedSave = true,
                )
            )
        }

    @Test
    fun `On 'action' with saved payment method, should call 'IntentConfirmationInterceptor' with expected params`() =
        runTest {
            val intentConfirmationInterceptor = FakeIntentConfirmationInterceptor().apply {
                enqueueCompleteStep()
            }

            val definition = createIntentConfirmationDefinition(
                intentConfirmationInterceptor = intentConfirmationInterceptor,
            )

            definition.action(
                confirmationOption = SAVED_PAYMENT_CONFIRMATION_OPTION,
                intent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            )

            assertThat(intentConfirmationInterceptor.calls.awaitItem()).isEqualTo(
                FakeIntentConfirmationInterceptor.InterceptCall.WithExistingPaymentMethod(
                    initializationMode = PaymentSheet.InitializationMode.PaymentIntent(
                        clientSecret = "pi_123"
                    ),
                    paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
                    paymentMethodOptionsParams = PaymentMethodOptionsParams.Card(
                        cvc = "505",
                    ),
                    shippingValues = AddressDetails(name = "John Doe").toConfirmPaymentIntentShipping(),
                )
            )
        }

    @Test
    fun `On 'IntentConfirmationInterceptor' complete, should return 'Complete' confirmation action`() = runTest {
        val intentConfirmationInterceptor = FakeIntentConfirmationInterceptor().apply {
            enqueueCompleteStep()
        }

        val definition = createIntentConfirmationDefinition(
            intentConfirmationInterceptor = intentConfirmationInterceptor,
        )

        val action = definition.action(
            confirmationOption = SAVED_PAYMENT_CONFIRMATION_OPTION,
            intent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
        )

        assertThat(action).isEqualTo(
            PaymentConfirmationDefinition.ConfirmationAction.Complete<IntentConfirmationDefinition.Args>(
                intent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
                confirmationOption = SAVED_PAYMENT_CONFIRMATION_OPTION,
                deferredIntentConfirmationType = DeferredIntentConfirmationType.Server,
            )
        )
    }

    @Test
    fun `On 'IntentConfirmationInterceptor' failure, should return 'Fail' confirmation action`() = runTest {
        val message = "Failed!"
        val cause = IllegalStateException("Failed with exception!")

        val intentConfirmationInterceptor = FakeIntentConfirmationInterceptor().apply {
            enqueueFailureStep(
                cause = cause,
                message = message
            )
        }

        val definition = createIntentConfirmationDefinition(
            intentConfirmationInterceptor = intentConfirmationInterceptor,
        )

        val action = definition.action(
            confirmationOption = SAVED_PAYMENT_CONFIRMATION_OPTION,
            intent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
        )

        assertThat(action).isEqualTo(
            PaymentConfirmationDefinition.ConfirmationAction.Fail<IntentConfirmationDefinition.Args>(
                cause = cause,
                message = message.resolvableString,
                errorType = PaymentConfirmationErrorType.Internal,
            )
        )
    }

    @Test
    fun `On 'IntentConfirmationInterceptor' next step, should return 'Launch' confirmation action`() = runTest {
        val intentConfirmationInterceptor = FakeIntentConfirmationInterceptor().apply {
            enqueueNextActionStep(clientSecret = "pi_123")
        }

        val definition = createIntentConfirmationDefinition(
            intentConfirmationInterceptor = intentConfirmationInterceptor,
        )

        val action = definition.action(
            confirmationOption = SAVED_PAYMENT_CONFIRMATION_OPTION,
            intent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
        )

        assertThat(action).isEqualTo(
            PaymentConfirmationDefinition.ConfirmationAction.Launch<IntentConfirmationDefinition.Args>(
                launcherArguments = IntentConfirmationDefinition.Args.NextAction(
                    clientSecret = "pi_123"
                ),
                deferredIntentConfirmationType = DeferredIntentConfirmationType.Server,
            )
        )
    }

    @Test
    fun `On 'IntentConfirmationInterceptor' confirm, should return 'Launch' confirmation action`() = runTest {
        val confirmParams = ConfirmSetupIntentParams.create(
            clientSecret = "pi_1234",
            paymentMethodType = PaymentMethod.Type.Card
        )

        val intentConfirmationInterceptor = FakeIntentConfirmationInterceptor().apply {
            enqueueConfirmStep(confirmParams = confirmParams)
        }

        val definition = createIntentConfirmationDefinition(
            intentConfirmationInterceptor = intentConfirmationInterceptor,
        )

        val action = definition.action(
            confirmationOption = SAVED_PAYMENT_CONFIRMATION_OPTION,
            intent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD,
        )

        assertThat(action).isEqualTo(
            PaymentConfirmationDefinition.ConfirmationAction.Launch<IntentConfirmationDefinition.Args>(
                launcherArguments = IntentConfirmationDefinition.Args.Confirm(
                    confirmNextParams = confirmParams,
                ),
                deferredIntentConfirmationType = null,
            )
        )
    }

    @Test
    fun `On launch with confirm action and 'SetupIntent' params, should launch with expected params`() = runTest {
        val confirmParams = ConfirmSetupIntentParams.create(
            clientSecret = "pi_1234",
            paymentMethodType = PaymentMethod.Type.Card
        )

        val launcher = FakePaymentLauncher()

        val definition = createIntentConfirmationDefinition(
            paymentLauncher = launcher,
        )

        definition.launch(
            launcher = launcher,
            arguments = IntentConfirmationDefinition.Args.Confirm(confirmParams),
            intent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD,
            confirmationOption = SAVED_PAYMENT_CONFIRMATION_OPTION,
        )

        assertThat(launcher.calls.awaitItem()).isEqualTo(
            FakePaymentLauncher.Call.Confirm.SetupIntent(confirmParams)
        )
    }

    @Test
    fun `On launch with next step action and 'SetupIntent', should launch with expected params`() = runTest {
        val launcher = FakePaymentLauncher()

        val definition = createIntentConfirmationDefinition(
            paymentLauncher = launcher,
        )

        definition.launch(
            launcher = launcher,
            arguments = IntentConfirmationDefinition.Args.NextAction(clientSecret = "si_123"),
            intent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD,
            confirmationOption = SAVED_PAYMENT_CONFIRMATION_OPTION,
        )

        assertThat(launcher.calls.awaitItem()).isEqualTo(
            FakePaymentLauncher.Call.HandleNextAction.SetupIntent(clientSecret = "si_123")
        )
    }

    @Test
    fun `On launch with confirm action and 'PaymentIntent' params, should launch with expected params`() = runTest {
        val confirmParams = ConfirmPaymentIntentParams.create(
            clientSecret = "pi_1234",
            paymentMethodType = PaymentMethod.Type.Card
        )

        val launcher = FakePaymentLauncher()

        val definition = createIntentConfirmationDefinition(
            paymentLauncher = launcher,
        )

        definition.launch(
            launcher = launcher,
            arguments = IntentConfirmationDefinition.Args.Confirm(confirmParams),
            intent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD,
            confirmationOption = SAVED_PAYMENT_CONFIRMATION_OPTION,
        )

        assertThat(launcher.calls.awaitItem()).isEqualTo(
            FakePaymentLauncher.Call.Confirm.PaymentIntent(confirmParams)
        )
    }

    @Test
    fun `On launch with next step action and 'PaymentIntent', should launch with expected params`() = runTest {
        val launcher = FakePaymentLauncher()

        val definition = createIntentConfirmationDefinition(
            paymentLauncher = launcher,
        )

        definition.launch(
            launcher = launcher,
            arguments = IntentConfirmationDefinition.Args.NextAction(clientSecret = "pi_123"),
            intent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            confirmationOption = SAVED_PAYMENT_CONFIRMATION_OPTION,
        )

        assertThat(launcher.calls.awaitItem()).isEqualTo(
            FakePaymentLauncher.Call.HandleNextAction.PaymentIntent(clientSecret = "pi_123")
        )
    }

    @Test
    fun `On 'Completed' payment result, should return successful confirmation result`() {
        val launcher = FakePaymentLauncher()

        val definition = createIntentConfirmationDefinition(
            paymentLauncher = launcher,
        )

        val result = definition.toPaymentConfirmationResult(
            confirmationOption = SAVED_PAYMENT_CONFIRMATION_OPTION,
            intent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            deferredIntentConfirmationType = DeferredIntentConfirmationType.Client,
            result = InternalPaymentResult.Completed(PaymentIntentFixtures.PI_SUCCEEDED),
        )

        assertThat(result).isEqualTo(
            PaymentConfirmationResult.Succeeded(
                intent = PaymentIntentFixtures.PI_SUCCEEDED,
                confirmationOption = SAVED_PAYMENT_CONFIRMATION_OPTION,
                deferredIntentConfirmationType = DeferredIntentConfirmationType.Client,
            ),
        )
    }

    @Test
    fun `On 'Failed' payment result, should return failed confirmation result`() {
        val launcher = FakePaymentLauncher()

        val definition = createIntentConfirmationDefinition(
            paymentLauncher = launcher,
        )

        val exception = IllegalStateException("Failed!")

        val result = definition.toPaymentConfirmationResult(
            confirmationOption = SAVED_PAYMENT_CONFIRMATION_OPTION,
            intent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            deferredIntentConfirmationType = null,
            result = InternalPaymentResult.Failed(exception),
        )

        assertThat(result).isEqualTo(
            PaymentConfirmationResult.Failed(
                cause = exception,
                message = R.string.stripe_something_went_wrong.resolvableString,
                type = PaymentConfirmationErrorType.Payment,
            ),
        )
    }

    @Test
    fun `On 'Canceled' payment result, should return canceled confirmation result`() {
        val launcher = FakePaymentLauncher()

        val definition = createIntentConfirmationDefinition(
            paymentLauncher = launcher,
        )

        val result = definition.toPaymentConfirmationResult(
            confirmationOption = SAVED_PAYMENT_CONFIRMATION_OPTION,
            intent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            deferredIntentConfirmationType = null,
            result = InternalPaymentResult.Canceled,
        )

        assertThat(result).isEqualTo(
            PaymentConfirmationResult.Canceled(
                action = PaymentCancellationAction.InformCancellation,
            ),
        )
    }

    private fun createIntentConfirmationDefinition(
        intentConfirmationInterceptor: IntentConfirmationInterceptor = FakeIntentConfirmationInterceptor(),
        paymentLauncher: PaymentLauncher = FakePaymentLauncher()
    ): IntentConfirmationDefinition {
        return IntentConfirmationDefinition(
            intentConfirmationInterceptor = intentConfirmationInterceptor,
            paymentLauncherFactory = { paymentLauncher }
        )
    }

    private companion object {
        private val SAVED_PAYMENT_CONFIRMATION_OPTION = PaymentConfirmationOption.PaymentMethod.Saved(
            initializationMode = PaymentSheet.InitializationMode.PaymentIntent(
                clientSecret = "pi_123"
            ),
            paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
            optionsParams = PaymentMethodOptionsParams.Card(
                cvc = "505",
            ),
            shippingDetails = AddressDetails(name = "John Doe"),
        )
    }
}
