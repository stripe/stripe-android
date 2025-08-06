package com.stripe.android.paymentelement.confirmation.challenge

import com.google.common.truth.Truth.assertThat
import com.stripe.android.challenge.PassiveChallengeActivityContract
import com.stripe.android.challenge.PassiveChallengeActivityResult
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.isInstanceOf
import com.stripe.android.model.PassiveCaptchaParams
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.FakeConfirmationOption
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.paymentelement.confirmation.asCallbackFor
import com.stripe.android.paymentelement.confirmation.asFail
import com.stripe.android.paymentelement.confirmation.asLaunch
import com.stripe.android.paymentelement.confirmation.asNextStep
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.android.testing.PaymentIntentFactory
import com.stripe.android.utils.DummyActivityResultCaller
import com.stripe.android.utils.FakeActivityResultLauncher
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class PassiveChallengeConfirmationDefinitionTest {

    @Test
    fun `'key' should be 'ChallengePassive'`() {
        val definition = createPassiveChallengeConfirmationDefinition()

        assertThat(definition.key).isEqualTo("ChallengePassive")
    }

    @Test
    fun `'option' return casted 'PaymentMethodConfirmationOption New'`() {
        val definition = createPassiveChallengeConfirmationDefinition()

        assertThat(definition.option(PAYMENT_METHOD_CONFIRMATION_OPTION_NEW))
            .isEqualTo(PAYMENT_METHOD_CONFIRMATION_OPTION_NEW)
    }

    @Test
    fun `'option' return null for unknown option`() {
        val definition = createPassiveChallengeConfirmationDefinition()

        assertThat(definition.option(FakeConfirmationOption())).isNull()
    }

    @Test
    fun `'option' return null for Saved PaymentMethodConfirmationOption`() {
        val definition = createPassiveChallengeConfirmationDefinition()
        val savedOption = PaymentMethodConfirmationOption.Saved(
            paymentMethod = PaymentIntentFactory.create().paymentMethod!!,
            optionsParams = null
        )

        assertThat(definition.option(savedOption)).isNull()
    }

    @Test
    fun `'canConfirm' should return true when passiveCaptchaParams is not null`() {
        val definition = createPassiveChallengeConfirmationDefinition()

        val result = definition.canConfirm(
            confirmationOption = PAYMENT_METHOD_CONFIRMATION_OPTION_NEW,
            confirmationParameters = CONFIRMATION_PARAMETERS
        )

        assertThat(result).isTrue()
    }

    @Test
    fun `'canConfirm' should return false when passiveCaptchaParams is null`() {
        val definition = createPassiveChallengeConfirmationDefinition()
        val optionWithoutCaptcha = PAYMENT_METHOD_CONFIRMATION_OPTION_NEW.copy(
            passiveCaptchaParams = null
        )

        val result = definition.canConfirm(
            confirmationOption = optionWithoutCaptcha,
            confirmationParameters = CONFIRMATION_PARAMETERS
        )

        assertThat(result).isFalse()
    }

    @Test
    fun `'createLauncher' should register launcher properly for activity result`() = runTest {
        val definition = createPassiveChallengeConfirmationDefinition()

        var onResultCalled = false
        val onResult: (PassiveChallengeActivityResult) -> Unit = { onResultCalled = true }

        DummyActivityResultCaller.test {
            definition.createLauncher(
                activityResultCaller = activityResultCaller,
                onResult = onResult,
            )

            val registerCall = awaitRegisterCall()

            assertThat(awaitNextRegisteredLauncher()).isNotNull()

            assertThat(registerCall.contract).isInstanceOf<PassiveChallengeActivityContract>()

            val callback = registerCall.callback.asCallbackFor<PassiveChallengeActivityResult>()

            callback.onActivityResult(PassiveChallengeActivityResult.Success("test_token"))

            assertThat(onResultCalled).isTrue()
        }
    }

    @Test
    fun `'action' should return expected 'Launch' action when passiveCaptchaParams is not null`() = runTest {
        val definition = createPassiveChallengeConfirmationDefinition()

        val action = definition.action(
            confirmationOption = PAYMENT_METHOD_CONFIRMATION_OPTION_NEW,
            confirmationParameters = CONFIRMATION_PARAMETERS,
        )

        assertThat(action)
            .isInstanceOf<ConfirmationDefinition.Action.Launch<PassiveChallengeConfirmationDefinition.LauncherArgs>>()

        val launchAction = action.asLaunch()

        assertThat(launchAction.launcherArguments.passiveCaptchaParams).isEqualTo(PASSIVE_CAPTCHA_PARAMS)
        assertThat(launchAction.receivesResultInProcess).isFalse()
        assertThat(launchAction.deferredIntentConfirmationType).isNull()
    }

    @Test
    fun `'action' should return 'Fail' action when passiveCaptchaParams is null`() = runTest {
        val definition = createPassiveChallengeConfirmationDefinition()
        val optionWithoutCaptcha = PAYMENT_METHOD_CONFIRMATION_OPTION_NEW.copy(
            passiveCaptchaParams = null
        )

        val action = definition.action(
            confirmationOption = optionWithoutCaptcha,
            confirmationParameters = CONFIRMATION_PARAMETERS,
        )

        assertThat(action)
            .isInstanceOf<ConfirmationDefinition.Action.Fail<PassiveChallengeConfirmationDefinition.LauncherArgs>>()

        val failAction = action.asFail()

        assertThat(failAction.cause).isInstanceOf<IllegalArgumentException>()
        assertThat(failAction.cause.message).isEqualTo("Passive challenge params are null")
        assertThat(failAction.message).isEqualTo("Passive challenge params are null".resolvableString)
        assertThat(failAction.errorType).isEqualTo(ConfirmationHandler.Result.Failed.ErrorType.Internal)
    }

    @Test
    fun `'action' should report error when passiveCaptchaParams is null`() = runTest {
        val fakeErrorReporter = FakeErrorReporter()
        val definition = createPassiveChallengeConfirmationDefinition(fakeErrorReporter)
        val optionWithoutCaptcha = PAYMENT_METHOD_CONFIRMATION_OPTION_NEW.copy(
            passiveCaptchaParams = null
        )

        definition.action(
            confirmationOption = optionWithoutCaptcha,
            confirmationParameters = CONFIRMATION_PARAMETERS,
        )

        val reportedErrors = fakeErrorReporter.getLoggedErrors()
        assertThat(reportedErrors).containsExactly(
            ErrorReporter.UnexpectedErrorEvent.INTENT_CONFIRMATION_HANDLER_PASSIVE_CHALLENGE_PARAMS_NULL.eventName
        )

        val call = fakeErrorReporter.awaitCall()
        assertThat(call.errorEvent).isEqualTo(
            ErrorReporter.UnexpectedErrorEvent.INTENT_CONFIRMATION_HANDLER_PASSIVE_CHALLENGE_PARAMS_NULL
        )
        assertThat(call.stripeException?.message).isEqualTo("Passive challenge params are null")
    }

    @Test
    fun `'launch' should launch properly with provided parameters`() = runTest {
        val definition = createPassiveChallengeConfirmationDefinition()

        val launcher = FakeActivityResultLauncher<PassiveChallengeActivityContract.Args>()
        val launcherArgs = PassiveChallengeConfirmationDefinition.LauncherArgs(PASSIVE_CAPTCHA_PARAMS)

        definition.launch(
            confirmationOption = PAYMENT_METHOD_CONFIRMATION_OPTION_NEW,
            confirmationParameters = CONFIRMATION_PARAMETERS,
            launcher = launcher,
            arguments = launcherArgs,
        )

        val launchCall = launcher.calls.awaitItem()

        assertThat(launchCall.input.passiveCaptchaParams)
            .isEqualTo(PAYMENT_METHOD_CONFIRMATION_OPTION_NEW.passiveCaptchaParams)
    }

    @Test
    fun `'launch' should launch properly with launcher arguments`() = runTest {
        val definition = createPassiveChallengeConfirmationDefinition()
        val passiveCaptchaParams = PassiveCaptchaParams(
            siteKey = "test_site_key",
            rqData = "test_rq_data"
        )
        val launcherArgs = PassiveChallengeConfirmationDefinition.LauncherArgs(passiveCaptchaParams)

        val launcher = FakeActivityResultLauncher<PassiveChallengeActivityContract.Args>()

        definition.launch(
            confirmationOption = PAYMENT_METHOD_CONFIRMATION_OPTION_NEW,
            confirmationParameters = CONFIRMATION_PARAMETERS,
            launcher = launcher,
            arguments = launcherArgs,
        )

        val launchCall = launcher.calls.awaitItem()

        assertThat(launchCall.input.passiveCaptchaParams).isEqualTo(passiveCaptchaParams)
    }

    @Test
    fun `'toResult' should return 'NextStep' with passiveCaptchaParams removed for Success result`() {
        val definition = createPassiveChallengeConfirmationDefinition()

        val result = definition.toResult(
            confirmationOption = PAYMENT_METHOD_CONFIRMATION_OPTION_NEW,
            confirmationParameters = CONFIRMATION_PARAMETERS,
            deferredIntentConfirmationType = null,
            result = PassiveChallengeActivityResult.Success("test_token"),
        )

        assertThat(result).isInstanceOf<ConfirmationDefinition.Result.NextStep>()

        val nextStepResult = result.asNextStep()

        assertThat(nextStepResult.confirmationOption).isEqualTo(
            PAYMENT_METHOD_CONFIRMATION_OPTION_NEW.copy(passiveCaptchaParams = null)
        )
        assertThat(nextStepResult.parameters).isEqualTo(CONFIRMATION_PARAMETERS)
    }

    @Test
    fun `'toResult' should return 'NextStep' with passiveCaptchaParams removed for Failed result`() {
        val definition = createPassiveChallengeConfirmationDefinition()
        val exception = RuntimeException("Captcha failed")

        val result = definition.toResult(
            confirmationOption = PAYMENT_METHOD_CONFIRMATION_OPTION_NEW,
            confirmationParameters = CONFIRMATION_PARAMETERS,
            deferredIntentConfirmationType = null,
            result = PassiveChallengeActivityResult.Failed(exception),
        )

        assertThat(result).isInstanceOf<ConfirmationDefinition.Result.NextStep>()

        val nextStepResult = result.asNextStep()

        assertThat(nextStepResult.confirmationOption).isEqualTo(
            PAYMENT_METHOD_CONFIRMATION_OPTION_NEW.copy(passiveCaptchaParams = null)
        )
        assertThat(nextStepResult.parameters).isEqualTo(CONFIRMATION_PARAMETERS)
    }

    private fun createPassiveChallengeConfirmationDefinition(
        errorReporter: ErrorReporter = FakeErrorReporter()
    ): PassiveChallengeConfirmationDefinition {
        return PassiveChallengeConfirmationDefinition(errorReporter)
    }

    private companion object {
        private val PAYMENT_INTENT = PaymentIntentFactory.create()

        private val CONFIRMATION_PARAMETERS = ConfirmationDefinition.Parameters(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = "pi_123_secret_123",
            ),
            intent = PAYMENT_INTENT,
            appearance = PaymentSheet.Appearance(),
            shippingDetails = AddressDetails(),
        )

        private val PASSIVE_CAPTCHA_PARAMS = PassiveCaptchaParams(
            siteKey = "site_key",
            rqData = null
        )

        private val PAYMENT_METHOD_CONFIRMATION_OPTION_NEW = PaymentMethodConfirmationOption.New(
            createParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
            optionsParams = null,
            extraParams = null,
            shouldSave = false,
            passiveCaptchaParams = PASSIVE_CAPTCHA_PARAMS,
        )
    }
}
