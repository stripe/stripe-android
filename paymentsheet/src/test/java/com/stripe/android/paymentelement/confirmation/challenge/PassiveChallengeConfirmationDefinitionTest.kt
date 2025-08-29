package com.stripe.android.paymentelement.confirmation.challenge

import com.google.common.truth.Truth.assertThat
import com.stripe.android.challenge.PassiveChallengeActivityContract
import com.stripe.android.challenge.PassiveChallengeActivityResult
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.isInstanceOf
import com.stripe.android.model.PassiveCaptchaParams
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.model.RadarOptions
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
    fun `'option' return casted 'PaymentMethodConfirmationOption Saved'`() {
        val definition = createPassiveChallengeConfirmationDefinition()

        assertThat(definition.option(PAYMENT_METHOD_CONFIRMATION_OPTION_SAVED))
            .isEqualTo(PAYMENT_METHOD_CONFIRMATION_OPTION_SAVED)
    }

    @Test
    fun `'option' return null for unknown option`() {
        val definition = createPassiveChallengeConfirmationDefinition()

        assertThat(definition.option(FakeConfirmationOption())).isNull()
    }

    @Test
    fun `'canConfirm' should return true when passiveCaptchaParams is not null for New option`() {
        val definition = createPassiveChallengeConfirmationDefinition()

        val result = definition.canConfirm(
            confirmationOption = PAYMENT_METHOD_CONFIRMATION_OPTION_NEW,
            confirmationParameters = CONFIRMATION_PARAMETERS
        )

        assertThat(result).isFalse()
    }

    @Test
    fun `'canConfirm' should return true when passiveCaptchaParams is not null for Saved option`() {
        val definition = createPassiveChallengeConfirmationDefinition()

        val result = definition.canConfirm(
            confirmationOption = PAYMENT_METHOD_CONFIRMATION_OPTION_SAVED,
            confirmationParameters = CONFIRMATION_PARAMETERS
        )

        assertThat(result).isFalse()
    }

    @Test
    fun `'canConfirm' should return false when passiveCaptchaParams is null for New option`() {
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
    fun `'canConfirm' should return false when passiveCaptchaParams is null for Saved option`() {
        val definition = createPassiveChallengeConfirmationDefinition()
        val optionWithoutCaptcha = PAYMENT_METHOD_CONFIRMATION_OPTION_SAVED.copy(
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
            .isInstanceOf<ConfirmationDefinition.Action.Launch<PassiveChallengeActivityContract.Args>>()

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
            .isInstanceOf<ConfirmationDefinition.Action.Fail<PassiveChallengeActivityContract.Args>>()

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

        val launcher = FakeActivityResultLauncher<PassiveChallengeActivityContract.Args>()

        definition.launch(
            confirmationOption = PAYMENT_METHOD_CONFIRMATION_OPTION_NEW,
            confirmationParameters = CONFIRMATION_PARAMETERS,
            launcher = launcher,
            arguments = launcherArgs,
        )

        val launchCall = launcher.calls.awaitItem()

        assertThat(launchCall.input.passiveCaptchaParams).isEqualTo(PASSIVE_CAPTCHA_PARAMS)
        assertThat(launchCall.input.publishableKey).isEqualTo(launcherArgs.publishableKey)
        assertThat(launchCall.input.productUsage).isEqualTo(launcherArgs.productUsage)
    }

    @Test
    fun `'toResult' should return 'NextStep' with passiveCaptchaParams removed for Success result with New option`() {
        val definition = createPassiveChallengeConfirmationDefinition()
        val testToken = "test_token"

        val result = definition.toResult(
            confirmationOption = PAYMENT_METHOD_CONFIRMATION_OPTION_NEW,
            confirmationParameters = CONFIRMATION_PARAMETERS,
            deferredIntentConfirmationType = null,
            result = PassiveChallengeActivityResult.Success(testToken),
        )

        assertThat(result).isInstanceOf<ConfirmationDefinition.Result.NextStep>()

        val nextStepResult = result.asNextStep()

        val expectedOption = PAYMENT_METHOD_CONFIRMATION_OPTION_NEW.copy(
            passiveCaptchaParams = null,
            createParams = PAYMENT_METHOD_CONFIRMATION_OPTION_NEW.createParams.copy(
                radarOptions = RadarOptions(testToken)
            )
        )

        assertThat(nextStepResult.confirmationOption).isEqualTo(expectedOption)
        assertThat(nextStepResult.parameters).isEqualTo(CONFIRMATION_PARAMETERS)
    }

    @Test
    fun `'toResult' should return 'NextStep' with passiveCaptchaParams removed for Success result with Saved option`() {
        val definition = createPassiveChallengeConfirmationDefinition()
        val testToken = "test_token"

        val result = definition.toResult(
            confirmationOption = PAYMENT_METHOD_CONFIRMATION_OPTION_SAVED,
            confirmationParameters = CONFIRMATION_PARAMETERS,
            deferredIntentConfirmationType = null,
            result = PassiveChallengeActivityResult.Success(testToken),
        )

        assertThat(result).isInstanceOf<ConfirmationDefinition.Result.NextStep>()

        val nextStepResult = result.asNextStep()

        val expectedOption = PAYMENT_METHOD_CONFIRMATION_OPTION_SAVED.copy(
            passiveCaptchaParams = null,
            hCaptchaToken = testToken
        )

        assertThat(nextStepResult.confirmationOption).isEqualTo(expectedOption)
        assertThat(nextStepResult.parameters).isEqualTo(CONFIRMATION_PARAMETERS)
    }

    @Test
    fun `'toResult' should return 'NextStep' with passiveCaptchaParams removed for Failed result with New option`() {
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

    @Test
    fun `'toResult' should return 'NextStep' with passiveCaptchaParams removed for Failed result with Saved option`() {
        val definition = createPassiveChallengeConfirmationDefinition()
        val exception = RuntimeException("Captcha failed")

        val result = definition.toResult(
            confirmationOption = PAYMENT_METHOD_CONFIRMATION_OPTION_SAVED,
            confirmationParameters = CONFIRMATION_PARAMETERS,
            deferredIntentConfirmationType = null,
            result = PassiveChallengeActivityResult.Failed(exception),
        )

        assertThat(result).isInstanceOf<ConfirmationDefinition.Result.NextStep>()

        val nextStepResult = result.asNextStep()

        assertThat(nextStepResult.confirmationOption).isEqualTo(
            PAYMENT_METHOD_CONFIRMATION_OPTION_SAVED.copy(passiveCaptchaParams = null, hCaptchaToken = null)
        )
        assertThat(nextStepResult.parameters).isEqualTo(CONFIRMATION_PARAMETERS)
    }

    @Test
    fun `'action' should work with Saved PaymentMethodConfirmationOption when passiveCaptchaParams is not null`() =
        runTest {
            val definition = createPassiveChallengeConfirmationDefinition()

            val action = definition.action(
                confirmationOption = PAYMENT_METHOD_CONFIRMATION_OPTION_SAVED,
                confirmationParameters = CONFIRMATION_PARAMETERS,
            )

            assertThat(action)
                .isInstanceOf<ConfirmationDefinition.Action.Launch<PassiveChallengeActivityContract.Args>>()

            val launchAction = action.asLaunch()

            assertThat(launchAction.launcherArguments.passiveCaptchaParams).isEqualTo(PASSIVE_CAPTCHA_PARAMS)
            assertThat(launchAction.receivesResultInProcess).isFalse()
            assertThat(launchAction.deferredIntentConfirmationType).isNull()
        }

    @Test
    fun `'launch' should work with Saved PaymentMethodConfirmationOption`() = runTest {
        val definition = createPassiveChallengeConfirmationDefinition()

        val launcher = FakeActivityResultLauncher<PassiveChallengeActivityContract.Args>()

        definition.launch(
            confirmationOption = PAYMENT_METHOD_CONFIRMATION_OPTION_SAVED,
            confirmationParameters = CONFIRMATION_PARAMETERS,
            launcher = launcher,
            arguments = launcherArgs,
        )

        val launchCall = launcher.calls.awaitItem()

        assertThat(launchCall.input.passiveCaptchaParams)
            .isEqualTo(PAYMENT_METHOD_CONFIRMATION_OPTION_SAVED.passiveCaptchaParams)
    }

    private fun createPassiveChallengeConfirmationDefinition(
        errorReporter: ErrorReporter = FakeErrorReporter(),
        publishableKey: String = launcherArgs.publishableKey,
        productUsage: Set<String> = launcherArgs.productUsage
    ): PassiveChallengeConfirmationDefinition {
        return PassiveChallengeConfirmationDefinition(errorReporter, { publishableKey }, productUsage)
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

        private val PAYMENT_METHOD_CONFIRMATION_OPTION_SAVED = PaymentMethodConfirmationOption.Saved(
            paymentMethod = PAYMENT_INTENT.paymentMethod!!,
            optionsParams = null,
            originatedFromWallet = false,
            passiveCaptchaParams = PASSIVE_CAPTCHA_PARAMS,
            hCaptchaToken = null,
        )

        private val launcherArgs = PassiveChallengeActivityContract.Args(
            passiveCaptchaParams = PASSIVE_CAPTCHA_PARAMS,
            publishableKey = "pk_123",
            productUsage = setOf("PaymentSheet")
        )
    }
}
