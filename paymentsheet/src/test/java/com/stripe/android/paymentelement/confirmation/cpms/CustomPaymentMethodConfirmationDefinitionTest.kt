package com.stripe.android.paymentelement.confirmation.cpms

import androidx.activity.result.ActivityResultCallback
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.isInstanceOf
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentelement.ConfirmCustomPaymentMethodCallback
import com.stripe.android.paymentelement.ExperimentalCustomPaymentMethodsApi
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
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.android.testing.PaymentIntentFactory
import com.stripe.android.utils.DummyActivityResultCaller
import com.stripe.android.utils.FakeActivityResultLauncher
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCustomPaymentMethodsApi::class)
class CustomPaymentMethodConfirmationDefinitionTest {
    @Test
    fun `'key' should be 'CustomPaymentMethod'`() = test {
        assertThat(definition.key).isEqualTo("CustomPaymentMethod")
    }

    @Test
    fun `'option' return casted 'CustomPaymentMethodConfirmationOption'`() = test {
        val confirmationOption = createCustomPaymentMethodConfirmationOption()

        assertThat(definition.option(confirmationOption)).isEqualTo(confirmationOption)
    }

    @Test
    fun `'option' return null for unknown option`() = test {
        assertThat(definition.option(FakeConfirmationOption())).isNull()
    }

    @Test
    fun `'createLauncher' should register launcher properly for activity result`() = test {
        var onResultCalled = false
        val onResult: (InternalCustomPaymentMethodResult) -> Unit = { onResultCalled = true }

        DummyActivityResultCaller.test {
            definition.createLauncher(
                activityResultCaller = activityResultCaller,
                onResult = onResult,
            )

            val registerCall = awaitRegisterCall()

            assertThat(awaitNextRegisteredLauncher()).isNotNull()

            assertThat(registerCall.contract).isInstanceOf<CustomPaymentMethodContract>()
            assertThat(registerCall.callback).isInstanceOf<ActivityResultCallback<*>>()

            val callback = registerCall.callback.asCallbackFor<InternalCustomPaymentMethodResult>()

            callback.onActivityResult(InternalCustomPaymentMethodResult.Completed)

            assertThat(onResultCalled).isTrue()
        }
    }

    @Test
    fun `'Fail' action should be returned if CPM handler is not set and report error`() = test(
        callback = null,
    ) {
        val action = definition.action(
            confirmationOption = createCustomPaymentMethodConfirmationOption(),
            confirmationParameters = CONFIRMATION_PARAMETERS,
        )

        assertThat(action).isInstanceOf<ConfirmationDefinition.Action.Fail<CustomPaymentMethodInput>>()

        val failAction = action.asFail()

        assertThat(failAction.cause).isInstanceOf<IllegalStateException>()
        assertThat(failAction.cause.message).isEqualTo(
            "confirmCustomPaymentMethodCallback is null." +
                " Cannot process payment for payment selection: cpmt_123"
        )
        assertThat(failAction.message).isEqualTo(R.string.stripe_something_went_wrong.resolvableString)
        assertThat(failAction.errorType).isEqualTo(
            ConfirmationHandler.Result.Failed.ErrorType.Payment,
        )

        val event = errorReporter.awaitCall()

        assertThat(event.errorEvent)
            .isEqualTo(ErrorReporter.ExpectedErrorEvent.CUSTOM_PAYMENT_METHOD_CONFIRM_HANDLER_NULL)
        assertThat(event.additionalNonPiiParams)
            .containsEntry("custom_payment_method_type", "cpmt_123")
    }

    @Test
    fun `'Launch' action should be returned if CPM handler is set`() = test(
        callback = { _, _ ->
            error("Should not be called!")
        }
    ) {
        val action = definition.action(
            confirmationOption = createCustomPaymentMethodConfirmationOption(),
            confirmationParameters = CONFIRMATION_PARAMETERS,
        )

        assertThat(action).isInstanceOf<ConfirmationDefinition.Action.Launch<CustomPaymentMethodInput>>()

        val launchAction = action.asLaunch()

        assertThat(launchAction.launcherArguments).isEqualTo(Unit)
        assertThat(launchAction.deferredIntentConfirmationType).isNull()
    }

    @Test
    fun `'launch' should launch using launcher & report launch`() = test {
        val option = createCustomPaymentMethodConfirmationOption()

        val launcher = FakeActivityResultLauncher<CustomPaymentMethodInput>()

        definition.launch(
            launcher = launcher,
            arguments = Unit,
            confirmationOption = option,
            confirmationParameters = CONFIRMATION_PARAMETERS,
        )

        val input = launcher.calls.awaitItem().input

        assertThat(input.paymentElementCallbackIdentifier)
            .isEqualTo("cpm_test_payment_element_identifier")
        assertThat(input.type).isEqualTo(CUSTOM_PAYMENT_METHOD_TYPE)
        assertThat(input.billingDetails).isEqualTo(BILLING_DETAILS)

        val event = errorReporter.awaitCall()

        assertThat(event.errorEvent)
            .isEqualTo(ErrorReporter.SuccessEvent.CUSTOM_PAYMENT_METHODS_LAUNCH_SUCCESS)
    }

    @Test
    fun `'toResult' should return 'Completed' when CPM result is completed`() = test {
        val option = createCustomPaymentMethodConfirmationOption()

        val result = definition.toResult(
            confirmationOption = option,
            confirmationParameters = CONFIRMATION_PARAMETERS,
            result = InternalCustomPaymentMethodResult.Completed,
            deferredIntentConfirmationType = null,
        )

        assertThat(result).isInstanceOf<ConfirmationDefinition.Result.Succeeded>()

        val succeededResult = result.asSucceeded()
        assertThat(succeededResult.intent).isEqualTo(CONFIRMATION_PARAMETERS.intent)
        assertThat(succeededResult.deferredIntentConfirmationType).isNull()
    }

    @Test
    fun `'toResult' should return 'Failed' when CPM result is failed`() = test {
        val exception = IllegalStateException("Failed result!")
        val option = createCustomPaymentMethodConfirmationOption()

        val result = definition.toResult(
            confirmationOption = option,
            confirmationParameters = CONFIRMATION_PARAMETERS,
            result = InternalCustomPaymentMethodResult.Failed(exception),
            deferredIntentConfirmationType = null,
        )

        assertThat(result).isInstanceOf<ConfirmationDefinition.Result.Failed>()

        val failedResult = result.asFailed()

        assertThat(failedResult.cause).isEqualTo(exception)
        assertThat(failedResult.message).isEqualTo(R.string.stripe_something_went_wrong.resolvableString)
        assertThat(failedResult.type).isEqualTo(ConfirmationHandler.Result.Failed.ErrorType.Payment)
    }

    @Test
    fun `'toResult' should return 'Canceled' when CPM recollection is canceled`() = test {
        val option = createCustomPaymentMethodConfirmationOption()

        val result = definition.toResult(
            confirmationOption = option,
            confirmationParameters = CONFIRMATION_PARAMETERS,
            result = InternalCustomPaymentMethodResult.Canceled,
            deferredIntentConfirmationType = null,
        )

        assertThat(result).isInstanceOf<ConfirmationDefinition.Result.Canceled>()

        val canceledResult = result.asCanceled()

        assertThat(canceledResult.action).isEqualTo(ConfirmationHandler.Result.Canceled.Action.None)
    }

    private fun test(
        callback: ConfirmCustomPaymentMethodCallback? = null,
        test: suspend Scenario.() -> Unit,
    ) = runTest {
        val errorReporter = FakeErrorReporter()

        test(
            Scenario(
                definition = CustomPaymentMethodConfirmationDefinition(
                    paymentElementCallbackIdentifier = "cpm_test_payment_element_identifier",
                    confirmCustomPaymentMethodCallbackProvider = { callback },
                    errorReporter = errorReporter,
                ),
                errorReporter = errorReporter,
            )
        )

        errorReporter.ensureAllEventsConsumed()
    }

    private fun createCustomPaymentMethodConfirmationOption(): CustomPaymentMethodConfirmationOption {
        return CustomPaymentMethodConfirmationOption(
            customPaymentMethodType = CUSTOM_PAYMENT_METHOD_TYPE,
            billingDetails = BILLING_DETAILS,
        )
    }

    private data class Scenario(
        val definition: CustomPaymentMethodConfirmationDefinition,
        val errorReporter: FakeErrorReporter,
    )

    companion object {
        private val CONFIRMATION_PARAMETERS = ConfirmationDefinition.Parameters(
            intent = PaymentIntentFactory.create(),
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = "pi_123_secret_123",
            ),
            appearance = PaymentSheet.Appearance().copy(
                colorsDark = PaymentSheet.Colors.defaultLight,
            ),
            shippingDetails = null,
        )

        private val CUSTOM_PAYMENT_METHOD_TYPE = PaymentSheet.CustomPaymentMethod(
            id = "cpmt_123",
            subtitle = "Pay now".resolvableString,
            disableBillingDetailCollection = false,
        )

        private val BILLING_DETAILS = PaymentMethodFixtures.BILLING_DETAILS
    }
}
