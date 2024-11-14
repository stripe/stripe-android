package com.stripe.android.paymentelement.confirmation

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
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.addresselement.toConfirmPaymentIntentShipping
import com.stripe.android.paymentsheet.state.PaymentElementLoader
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

            val initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(clientSecret = "pi_123")
            val createParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD
            val shippingDetails = AddressDetails(name = "John Doe")

            definition.action(
                confirmationOption = ConfirmationHandler.Option.PaymentMethod.New(
                    initializationMode = initializationMode,
                    createParams = createParams,
                    optionsParams = null,
                    shippingDetails = shippingDetails,
                    shouldSave = true,
                ),
                intent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            )

            val result = intentConfirmationInterceptor
                .await<FakeIntentConfirmationInterceptor.InterceptCall.WithNewPaymentMethod>()

            assertThat(result.initializationMode).isEqualTo(initializationMode)
            assertThat(result.paymentMethodCreateParams).isEqualTo(createParams)
            assertThat(result.paymentMethodOptionsParams).isNull()
            assertThat(result.shippingValues).isEqualTo(shippingDetails.toConfirmPaymentIntentShipping())
            assertThat(result.customerRequestedSave).isTrue()
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

            val confirmationOption = SAVED_PAYMENT_CONFIRMATION_OPTION

            definition.action(
                confirmationOption = confirmationOption,
                intent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            )

            val result = intentConfirmationInterceptor
                .await<FakeIntentConfirmationInterceptor.InterceptCall.WithExistingPaymentMethod>()

            assertThat(result.initializationMode).isEqualTo(confirmationOption.initializationMode)
            assertThat(result.paymentMethod).isEqualTo(confirmationOption.paymentMethod)
            assertThat(result.paymentMethodOptionsParams).isEqualTo(confirmationOption.optionsParams)
            assertThat(result.shippingValues).isEqualTo(
                confirmationOption.shippingDetails?.toConfirmPaymentIntentShipping()
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

        val completeAction = action.asComplete()

        assertThat(completeAction.intent).isEqualTo(PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD)
        assertThat(completeAction.confirmationOption).isEqualTo(SAVED_PAYMENT_CONFIRMATION_OPTION)
        assertThat(completeAction.deferredIntentConfirmationType).isEqualTo(DeferredIntentConfirmationType.Server)
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

        val failAction = action.asFail()

        assertThat(failAction.cause).isEqualTo(cause)
        assertThat(failAction.message).isEqualTo(message.resolvableString)
        assertThat(failAction.errorType).isEqualTo(ConfirmationHandler.Result.Failed.ErrorType.Payment)
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

        val launchAction = action.asLaunch()

        assertThat(launchAction.launcherArguments).isEqualTo(
            IntentConfirmationDefinition.Args.NextAction(
                clientSecret = "pi_123"
            )
        )
        assertThat(launchAction.deferredIntentConfirmationType).isEqualTo(DeferredIntentConfirmationType.Server)
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

        val launchAction = action.asLaunch()

        assertThat(launchAction.launcherArguments).isEqualTo(
            IntentConfirmationDefinition.Args.Confirm(
                confirmNextParams = confirmParams,
            )
        )
        assertThat(launchAction.deferredIntentConfirmationType).isNull()
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

        val succeededResult = result.asSucceeded()

        assertThat(succeededResult.intent).isEqualTo(PaymentIntentFixtures.PI_SUCCEEDED)
        assertThat(succeededResult.deferredIntentConfirmationType).isEqualTo(DeferredIntentConfirmationType.Client)
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

        val failedResult = result.asFailed()

        assertThat(failedResult.cause).isEqualTo(exception)
        assertThat(failedResult.message).isEqualTo(R.string.stripe_something_went_wrong.resolvableString)
        assertThat(failedResult.type).isEqualTo(ConfirmationHandler.Result.Failed.ErrorType.Payment)
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

        val canceledResult = result.asCanceled()

        assertThat(canceledResult.action).isEqualTo(ConfirmationHandler.Result.Canceled.Action.InformCancellation)
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

    private suspend inline fun <reified T : FakeIntentConfirmationInterceptor.InterceptCall>
        FakeIntentConfirmationInterceptor.await(): T {
        return calls.awaitItem() as T
    }

    private inline fun <reified T> ConfirmationDefinition.ConfirmationAction<T>.asComplete():
        ConfirmationDefinition.ConfirmationAction.Complete<T> {
        return this as ConfirmationDefinition.ConfirmationAction.Complete<T>
    }

    private inline fun <reified T> ConfirmationDefinition.ConfirmationAction<T>.asFail():
        ConfirmationDefinition.ConfirmationAction.Fail<T> {
        return this as ConfirmationDefinition.ConfirmationAction.Fail<T>
    }

    private inline fun <reified T> ConfirmationDefinition.ConfirmationAction<T>.asLaunch():
        ConfirmationDefinition.ConfirmationAction.Launch<T> {
        return this as ConfirmationDefinition.ConfirmationAction.Launch<T>
    }

    private fun ConfirmationHandler.Result.asSucceeded(): ConfirmationHandler.Result.Succeeded {
        return this as ConfirmationHandler.Result.Succeeded
    }

    private fun ConfirmationHandler.Result.asFailed(): ConfirmationHandler.Result.Failed {
        return this as ConfirmationHandler.Result.Failed
    }

    private fun ConfirmationHandler.Result.asCanceled(): ConfirmationHandler.Result.Canceled {
        return this as ConfirmationHandler.Result.Canceled
    }

    private companion object {
        private val SAVED_PAYMENT_CONFIRMATION_OPTION = ConfirmationHandler.Option.PaymentMethod.Saved(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
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
