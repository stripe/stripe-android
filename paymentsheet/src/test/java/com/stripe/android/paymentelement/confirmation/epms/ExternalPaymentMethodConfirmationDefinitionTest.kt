package com.stripe.android.paymentelement.confirmation.epms

import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContract
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.isInstanceOf
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.FakeConfirmationOption
import com.stripe.android.paymentelement.confirmation.asCallbackFor
import com.stripe.android.paymentelement.confirmation.asCanceled
import com.stripe.android.paymentelement.confirmation.asFail
import com.stripe.android.paymentelement.confirmation.asFailed
import com.stripe.android.paymentelement.confirmation.asLaunch
import com.stripe.android.paymentelement.confirmation.asSucceeded
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.android.paymentsheet.ExternalPaymentMethodConfirmHandler
import com.stripe.android.paymentsheet.ExternalPaymentMethodContract
import com.stripe.android.paymentsheet.ExternalPaymentMethodInput
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.android.testing.PaymentIntentFactory
import com.stripe.android.utils.DummyActivityResultCaller
import com.stripe.android.utils.FakeActivityResultLauncher
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ExternalPaymentMethodConfirmationDefinitionTest {
    @Test
    fun `'key' should be 'ExternalPaymentMethod`() {
        val definition = createExternalPaymentMethodConfirmationDefinition()

        assertThat(definition.key).isEqualTo("ExternalPaymentMethod")
    }

    @Test
    fun `'option' return casted 'ExternalPaymentMethod'`() {
        val definition = createExternalPaymentMethodConfirmationDefinition()

        assertThat(definition.option(EPM_CONFIRMATION_OPTION)).isEqualTo(EPM_CONFIRMATION_OPTION)
    }

    @Test
    fun `'option' return null for unknown option`() {
        val definition = createExternalPaymentMethodConfirmationDefinition()

        assertThat(definition.option(FakeConfirmationOption())).isNull()
    }

    @Test
    fun `'createLauncher' should register launcher properly for activity result`() = runTest {
        val errorReporter = FakeErrorReporter()
        val definition = createExternalPaymentMethodConfirmationDefinition(errorReporter = errorReporter)

        var onResultCalled = false
        val onResult: (PaymentResult) -> Unit = { onResultCalled = true }
        DummyActivityResultCaller.test {
            val launcher = definition.createLauncher(
                activityResultCaller = activityResultCaller,
                onResult = onResult,
            )

            val call = awaitRegisterCall()
            val registeredLauncher = awaitNextRegisteredLauncher()

            assertThat(call.contract).isInstanceOf<ExternalPaymentMethodContract>()

            val externalPaymentMethodContract = call.contract.asExternalPaymentMethodContract()

            assertThat(call.callback).isInstanceOf<ActivityResultCallback<PaymentResult>>()

            val callback = call.callback.asCallbackFor<PaymentResult>()

            assertThat(externalPaymentMethodContract.errorReporter).isEqualTo(errorReporter)

            callback.onActivityResult(PaymentResult.Completed)

            assertThat(onResultCalled).isTrue()

            assertThat(launcher).isEqualTo(registeredLauncher)
        }
    }

    @Test
    fun `'toResult' should return 'Complete' when 'PaymentResult' is 'Complete'`() = runTest {
        val definition = createExternalPaymentMethodConfirmationDefinition()

        val result = definition.toResult(
            confirmationOption = EPM_CONFIRMATION_OPTION,
            confirmationParameters = CONFIRMATION_PARAMETERS,
            deferredIntentConfirmationType = null,
            result = PaymentResult.Completed,
        )

        assertThat(result).isInstanceOf<ConfirmationDefinition.Result.Succeeded>()

        val successResult = result.asSucceeded()

        assertThat(successResult.intent).isEqualTo(PAYMENT_INTENT)
        assertThat(successResult.deferredIntentConfirmationType).isNull()
    }

    @Test
    fun `'toResult' should return 'Failed' when 'PaymentResult' is 'Failed'`() = runTest {
        val definition = createExternalPaymentMethodConfirmationDefinition()

        val exception = IllegalStateException("Failed!")
        val result = definition.toResult(
            confirmationOption = EPM_CONFIRMATION_OPTION,
            confirmationParameters = CONFIRMATION_PARAMETERS,
            deferredIntentConfirmationType = null,
            result = PaymentResult.Failed(exception),
        )

        assertThat(result).isInstanceOf<ConfirmationDefinition.Result.Failed>()

        val failedResult = result.asFailed()

        assertThat(failedResult.cause).isEqualTo(exception)
        assertThat(failedResult.message).isEqualTo(R.string.stripe_something_went_wrong.resolvableString)
        assertThat(failedResult.type).isEqualTo(ConfirmationHandler.Result.Failed.ErrorType.ExternalPaymentMethod)
    }

    @Test
    fun `'toResult' should return 'Canceled' when 'PaymentResult' is 'Canceled'`() = runTest {
        val definition = createExternalPaymentMethodConfirmationDefinition()

        val result = definition.toResult(
            confirmationOption = EPM_CONFIRMATION_OPTION,
            confirmationParameters = CONFIRMATION_PARAMETERS,
            deferredIntentConfirmationType = null,
            result = PaymentResult.Canceled,
        )

        assertThat(result).isInstanceOf<ConfirmationDefinition.Result.Canceled>()

        val canceledResult = result.asCanceled()

        assertThat(canceledResult.action).isEqualTo(ConfirmationHandler.Result.Canceled.Action.None)
    }

    @Test
    fun `'Fail' action should be returned if EPM handler is not set and report error`() = runTest {
        val errorReporter = FakeErrorReporter()
        val definition = createExternalPaymentMethodConfirmationDefinition(
            errorReporter = errorReporter,
        )

        val action = definition.action(
            confirmationOption = EPM_CONFIRMATION_OPTION,
            confirmationParameters = CONFIRMATION_PARAMETERS,
        )

        assertThat(action).isInstanceOf<ConfirmationDefinition.Action.Fail<Unit>>()

        val failAction = action.asFail()

        assertThat(failAction.cause).isInstanceOf<IllegalStateException>()
        assertThat(failAction.cause.message).isEqualTo(
            "externalPaymentMethodConfirmHandler is null." +
                " Cannot process payment for payment selection: paypal"
        )
        assertThat(failAction.message).isEqualTo(R.string.stripe_something_went_wrong.resolvableString)
        assertThat(failAction.errorType).isEqualTo(
            ConfirmationHandler.Result.Failed.ErrorType.ExternalPaymentMethod,
        )

        assertThat(errorReporter.getLoggedErrors()).containsExactly(
            ErrorReporter.ExpectedErrorEvent.EXTERNAL_PAYMENT_METHOD_CONFIRM_HANDLER_NULL.eventName
        )
    }

    @Test
    fun `'Launch' action should be returned if EPM handler is set & report launch`() = runTest {
        val errorReporter = FakeErrorReporter()
        val definition = createExternalPaymentMethodConfirmationDefinition(
            externalPaymentMethodConfirmHandler = { _, _ ->
                ExternalPaymentMethodConfirmHandler { _, _ ->
                    error("Not implemented!")
                }
            },
            errorReporter = errorReporter
        )

        val action = definition.action(
            confirmationOption = EPM_CONFIRMATION_OPTION,
            confirmationParameters = CONFIRMATION_PARAMETERS,
        )

        assertThat(action).isInstanceOf<ConfirmationDefinition.Action.Launch<Unit>>()

        val launchAction = action.asLaunch()

        assertThat(launchAction.launcherArguments).isEqualTo(Unit)
        assertThat(launchAction.deferredIntentConfirmationType).isNull()
    }

    @Test
    fun `On 'launch', should use launcher to launch and report event`() = runTest {
        val errorReporter = FakeErrorReporter()
        val definition = createExternalPaymentMethodConfirmationDefinition(
            errorReporter = errorReporter,
        )

        val launcher = FakeActivityResultLauncher<ExternalPaymentMethodInput>()

        definition.launch(
            confirmationOption = EPM_CONFIRMATION_OPTION,
            confirmationParameters = CONFIRMATION_PARAMETERS,
            arguments = Unit,
            launcher = launcher,
        )

        val input = launcher.calls.awaitItem().input

        assertThat(input.type).isEqualTo("paypal")
        assertThat(input.billingDetails).isEqualTo(
            PaymentMethod.BillingDetails(
                name = "John Doe",
                address = Address(
                    line1 = "123 Apple Street",
                    city = "South San Francisco",
                    state = "CA",
                    country = "US",
                ),
            )
        )

        assertThat(errorReporter.getLoggedErrors()).containsExactly(
            ErrorReporter.SuccessEvent.EXTERNAL_PAYMENT_METHODS_LAUNCH_SUCCESS.eventName
        )
    }

    private fun createExternalPaymentMethodConfirmationDefinition(
        externalPaymentMethodConfirmHandler: ExternalPaymentMethodConfirmHandler? = null,
        errorReporter: ErrorReporter = FakeErrorReporter()
    ): ExternalPaymentMethodConfirmationDefinition {
        return ExternalPaymentMethodConfirmationDefinition(
            instanceId = "ExternalPaymentMethod",
            externalPaymentMethodConfirmHandlerProvider = { externalPaymentMethodConfirmHandler },
            errorReporter = errorReporter,
        )
    }

    private fun ActivityResultContract<*, *>.asExternalPaymentMethodContract(): ExternalPaymentMethodContract {
        return this as ExternalPaymentMethodContract
    }

    private companion object {
        private val EPM_CONFIRMATION_OPTION = ExternalPaymentMethodConfirmationOption(
            type = "paypal",
            billingDetails = PaymentMethod.BillingDetails(
                name = "John Doe",
                address = Address(
                    line1 = "123 Apple Street",
                    city = "South San Francisco",
                    state = "CA",
                    country = "US",
                ),
            )
        )

        private val PAYMENT_INTENT = PaymentIntentFactory.create()

        private val CONFIRMATION_PARAMETERS = ConfirmationDefinition.Parameters(
            intent = PAYMENT_INTENT,
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = "pi_123_secret_123",
            ),
            appearance = PaymentSheet.Appearance(),
            shippingDetails = AddressDetails(),
        )
    }
}
