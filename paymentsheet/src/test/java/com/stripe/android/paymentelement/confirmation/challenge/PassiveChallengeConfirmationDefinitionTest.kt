package com.stripe.android.paymentelement.confirmation.challenge

import com.google.common.truth.Truth.assertThat
import com.stripe.android.challenge.passive.PassiveChallengeActivityContract
import com.stripe.android.challenge.passive.PassiveChallengeActivityResult
import com.stripe.android.challenge.passive.warmer.PassiveChallengeWarmer
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.isInstanceOf
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.AndroidVerificationObject
import com.stripe.android.model.PassiveCaptchaParams
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.model.RadarOptions
import com.stripe.android.paymentelement.confirmation.CONFIRMATION_PARAMETERS
import com.stripe.android.paymentelement.confirmation.ConfirmationChallengeState
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.FakeConfirmationOption
import com.stripe.android.paymentelement.confirmation.FakeIsEligibleForConfirmationChallenge
import com.stripe.android.paymentelement.confirmation.IsEligibleForConfirmationChallenge
import com.stripe.android.paymentelement.confirmation.PAYMENT_INTENT
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.paymentelement.confirmation.asCallbackFor
import com.stripe.android.paymentelement.confirmation.asFail
import com.stripe.android.paymentelement.confirmation.asLaunch
import com.stripe.android.paymentelement.confirmation.asNextStep
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.testing.DummyActivityResultCaller
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.android.utils.FakeActivityResultLauncher
import com.stripe.android.utils.FakePassiveChallengeWarmer
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
            confirmationArgs = CONFIRMATION_PARAMETERS
        )

        assertThat(result).isTrue()
    }

    @Test
    fun `'canConfirm' should return true when passiveCaptchaParams is not null for Saved option`() {
        val definition = createPassiveChallengeConfirmationDefinition()

        val result = definition.canConfirm(
            confirmationOption = PAYMENT_METHOD_CONFIRMATION_OPTION_SAVED,
            confirmationArgs = CONFIRMATION_PARAMETERS
        )

        assertThat(result).isTrue()
    }

    @Test
    fun `'canConfirm' should return false when passiveCaptchaParams is null for New option`() {
        val definition = createPassiveChallengeConfirmationDefinition()

        val result = definition.canConfirm(
            confirmationOption = PAYMENT_METHOD_CONFIRMATION_OPTION_NEW,
            confirmationArgs = CONFIRMATION_PARAMETERS.copy(
                paymentMethodMetadata = PaymentMethodMetadataFactory.create(passiveCaptchaParams = null)
            )
        )

        assertThat(result).isFalse()
    }

    @Test
    fun `'canConfirm' should return false when passiveCaptchaParams is null for Saved option`() {
        val definition = createPassiveChallengeConfirmationDefinition()

        val result = definition.canConfirm(
            confirmationOption = PAYMENT_METHOD_CONFIRMATION_OPTION_SAVED,
            confirmationArgs = CONFIRMATION_PARAMETERS.copy(
                paymentMethodMetadata = PaymentMethodMetadataFactory.create(passiveCaptchaParams = null)
            )
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
    fun `'createLauncher' should call passiveChallengeWarmer register`() = runTest {
        val fakePassiveChallengeWarmer = FakePassiveChallengeWarmer()
        val definition = createPassiveChallengeConfirmationDefinition(
            passiveChallengeWarmer = fakePassiveChallengeWarmer
        )

        DummyActivityResultCaller.test {
            definition.createLauncher(
                activityResultCaller = activityResultCaller,
                onResult = {},
            )

            awaitRegisterCall()
            awaitNextRegisteredLauncher()

            val registerCall = fakePassiveChallengeWarmer.awaitRegisterCall()
            assertThat(registerCall.activityResultCaller).isEqualTo(activityResultCaller)
        }
    }

    @Test
    fun `'action' should return expected 'Launch' action when passiveCaptchaParams is not null`() = runTest {
        val definition = createPassiveChallengeConfirmationDefinition()

        val action = definition.action(
            confirmationOption = PAYMENT_METHOD_CONFIRMATION_OPTION_NEW,
            confirmationArgs = CONFIRMATION_PARAMETERS,
        )

        assertThat(action)
            .isInstanceOf<ConfirmationDefinition.Action.Launch<PassiveChallengeActivityContract.Args>>()

        val launchAction = action.asLaunch()

        assertThat(launchAction.launcherArguments.passiveCaptchaParams)
            .isEqualTo(CONFIRMATION_PARAMETERS.paymentMethodMetadata.passiveCaptchaParams)
        assertThat(launchAction.receivesResultInProcess).isFalse()
    }

    @Test
    fun `'action' should return 'Fail' action when passiveCaptchaParams is null`() = runTest {
        val definition = createPassiveChallengeConfirmationDefinition()

        val action = definition.action(
            confirmationOption = PAYMENT_METHOD_CONFIRMATION_OPTION_NEW,
            confirmationArgs = CONFIRMATION_PARAMETERS.copy(
                paymentMethodMetadata = PaymentMethodMetadataFactory.create(passiveCaptchaParams = null)
            ),
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

        definition.action(
            confirmationOption = PAYMENT_METHOD_CONFIRMATION_OPTION_NEW,
            confirmationArgs = CONFIRMATION_PARAMETERS.copy(
                paymentMethodMetadata = PaymentMethodMetadataFactory.create(passiveCaptchaParams = null)
            )
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
            confirmationArgs = CONFIRMATION_PARAMETERS.copy(
                paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                    passiveCaptchaParams = PASSIVE_CAPTCHA_PARAMS,
                )
            ),
            launcher = launcher,
            arguments = launcherArgs,
        )

        val launchCall = launcher.calls.awaitItem()

        assertThat(launchCall.input.passiveCaptchaParams).isEqualTo(PASSIVE_CAPTCHA_PARAMS)
    }

    @Test
    fun `'launch' should launch properly with launcher arguments`() = runTest {
        val definition = createPassiveChallengeConfirmationDefinition()

        val launcher = FakeActivityResultLauncher<PassiveChallengeActivityContract.Args>()

        definition.launch(
            confirmationOption = PAYMENT_METHOD_CONFIRMATION_OPTION_NEW,
            confirmationArgs = CONFIRMATION_PARAMETERS,
            launcher = launcher,
            arguments = launcherArgs,
        )

        val launchCall = launcher.calls.awaitItem()

        assertThat(launchCall.input.passiveCaptchaParams).isEqualTo(PASSIVE_CAPTCHA_PARAMS)
        assertThat(launchCall.input.publishableKey).isEqualTo(launcherArgs.publishableKey)
        assertThat(launchCall.input.productUsage).isEqualTo(launcherArgs.productUsage)
    }

    @Test
    fun `'action' should work with Saved PaymentMethodConfirmationOption when passiveCaptchaParams is not null`() =
        runTest {
            val definition = createPassiveChallengeConfirmationDefinition()

            val action = definition.action(
                confirmationOption = PAYMENT_METHOD_CONFIRMATION_OPTION_SAVED,
                confirmationArgs = CONFIRMATION_PARAMETERS,
            )

            assertThat(action)
                .isInstanceOf<ConfirmationDefinition.Action.Launch<PassiveChallengeActivityContract.Args>>()

            val launchAction = action.asLaunch()

            assertThat(launchAction.launcherArguments.passiveCaptchaParams)
                .isEqualTo(CONFIRMATION_PARAMETERS.paymentMethodMetadata.passiveCaptchaParams)
            assertThat(launchAction.receivesResultInProcess).isFalse()
            assertThat(launchAction.deferredIntentConfirmationType).isNull()
        }

    @Test
    fun `'launch' should work with Saved PaymentMethodConfirmationOption`() = runTest {
        val definition = createPassiveChallengeConfirmationDefinition()

        val launcher = FakeActivityResultLauncher<PassiveChallengeActivityContract.Args>()

        definition.launch(
            confirmationOption = PAYMENT_METHOD_CONFIRMATION_OPTION_SAVED,
            confirmationArgs = CONFIRMATION_PARAMETERS.copy(
                paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                    passiveCaptchaParams = PASSIVE_CAPTCHA_PARAMS,
                )
            ),
            launcher = launcher,
            arguments = launcherArgs,
        )

        val launchCall = launcher.calls.awaitItem()

        assertThat(launchCall.input.passiveCaptchaParams)
            .isEqualTo(PASSIVE_CAPTCHA_PARAMS)
    }

    @Test
    fun `'toResult' should return 'NextStep' with passiveChallengeComplete=true for Success result with New option`() {
        val definition = createPassiveChallengeConfirmationDefinition()
        val testToken = "test_token"

        val result = definition.toResult(
            confirmationOption = PAYMENT_METHOD_CONFIRMATION_OPTION_NEW,
            confirmationArgs = CONFIRMATION_PARAMETERS,
            launcherArgs = launcherArgs,
            result = PassiveChallengeActivityResult.Success(testToken),
        )

        assertThat(result).isInstanceOf<ConfirmationDefinition.Result.NextStep>()

        val nextStepResult = result.asNextStep()

        val expectedOption = PAYMENT_METHOD_CONFIRMATION_OPTION_NEW.copy(
            confirmationChallengeState = ConfirmationChallengeState(passiveChallengeComplete = true),
            createParams = PAYMENT_METHOD_CONFIRMATION_OPTION_NEW.createParams.copy(
                radarOptions = RadarOptions(
                    hCaptchaToken = testToken,
                    androidVerificationObject = null
                )
            )
        )

        assertThat(nextStepResult.confirmationOption).isEqualTo(expectedOption)
        assertThat(nextStepResult.arguments).isEqualTo(CONFIRMATION_PARAMETERS)
    }

    @Test
    fun `'toResult' should return 'NextStep' with passiveChallengeComplete=true for Failed result with New option`() {
        val definition = createPassiveChallengeConfirmationDefinition()
        val exception = RuntimeException("Captcha failed")

        val result = definition.toResult(
            confirmationOption = PAYMENT_METHOD_CONFIRMATION_OPTION_NEW,
            confirmationArgs = CONFIRMATION_PARAMETERS,
            launcherArgs = launcherArgs,
            result = PassiveChallengeActivityResult.Failed(exception),
        )

        assertThat(result).isInstanceOf<ConfirmationDefinition.Result.NextStep>()

        val nextStepResult = result.asNextStep()

        assertThat(nextStepResult.confirmationOption).isEqualTo(
            PAYMENT_METHOD_CONFIRMATION_OPTION_NEW.copy(
                confirmationChallengeState = ConfirmationChallengeState(passiveChallengeComplete = true)
            )
        )
        assertThat(nextStepResult.arguments).isEqualTo(CONFIRMATION_PARAMETERS)
    }

    @Test
    fun `'toResult' should return 'NextStep' for Success result with Saved option`() {
        val definition = createPassiveChallengeConfirmationDefinition()
        val testToken = "test_token"

        val result = definition.toResult(
            confirmationOption = PAYMENT_METHOD_CONFIRMATION_OPTION_SAVED,
            confirmationArgs = CONFIRMATION_PARAMETERS,
            deferredIntentConfirmationType = null,
            result = PassiveChallengeActivityResult.Success(testToken),
        )

        assertThat(result).isInstanceOf<ConfirmationDefinition.Result.NextStep>()

        val nextStepResult = result.asNextStep()

        val expectedOption = PAYMENT_METHOD_CONFIRMATION_OPTION_SAVED.copy(
            confirmationChallengeState = ConfirmationChallengeState(
                passiveChallengeComplete = true,
                hCaptchaToken = testToken
            )
        )

        assertThat(nextStepResult.confirmationOption).isEqualTo(expectedOption)
        assertThat(nextStepResult.arguments).isEqualTo(CONFIRMATION_PARAMETERS)
    }

    @Test
    fun `'toResult' should return 'NextStep' with passiveChallengeComplete=true for Failed result with Saved option`() {
        val definition = createPassiveChallengeConfirmationDefinition()
        val exception = RuntimeException("Captcha failed")

        val result = definition.toResult(
            confirmationOption = PAYMENT_METHOD_CONFIRMATION_OPTION_SAVED,
            confirmationArgs = CONFIRMATION_PARAMETERS,
            deferredIntentConfirmationType = null,
            result = PassiveChallengeActivityResult.Failed(exception),
        )

        assertThat(result).isInstanceOf<ConfirmationDefinition.Result.NextStep>()

        val nextStepResult = result.asNextStep()

        assertThat(nextStepResult.confirmationOption).isEqualTo(
            PAYMENT_METHOD_CONFIRMATION_OPTION_SAVED.copy(
                confirmationChallengeState = ConfirmationChallengeState(
                    passiveChallengeComplete = true,
                )
            )
        )
        assertThat(nextStepResult.arguments).isEqualTo(CONFIRMATION_PARAMETERS)
    }

    @Test
    fun `'bootstrap' should start passive challenge warmer with passiveCaptchaParams`() = runTest {
        val fakePassiveChallengeWarmer = FakePassiveChallengeWarmer()
        val definition = createPassiveChallengeConfirmationDefinition(
            passiveChallengeWarmer = fakePassiveChallengeWarmer
        )

        val paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            passiveCaptchaParams = PASSIVE_CAPTCHA_PARAMS
        )

        definition.bootstrap(paymentMethodMetadata)

        val startCall = fakePassiveChallengeWarmer.awaitStartCall()

        assertThat(startCall.passiveCaptchaParams).isEqualTo(PASSIVE_CAPTCHA_PARAMS)
        assertThat(startCall.publishableKey).isEqualTo(launcherArgs.publishableKey)
        assertThat(startCall.productUsage).isEqualTo(launcherArgs.productUsage)
    }

    @Test
    fun `'bootstrap' should not start warmer if passiveCaptchaParams is null`() {
        val fakePassiveChallengeWarmer = FakePassiveChallengeWarmer()
        val definition = createPassiveChallengeConfirmationDefinition(
            passiveChallengeWarmer = fakePassiveChallengeWarmer
        )

        val paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            passiveCaptchaParams = null
        )

        definition.bootstrap(paymentMethodMetadata)

        fakePassiveChallengeWarmer.ensureAllEventsConsumed()
    }

    @Test
    fun `'unregister' should call passiveChallengeWarmer unregister`() = runTest {
        val fakePassiveChallengeWarmer = FakePassiveChallengeWarmer()
        val definition = createPassiveChallengeConfirmationDefinition(
            passiveChallengeWarmer = fakePassiveChallengeWarmer
        )

        val launcher = FakeActivityResultLauncher<PassiveChallengeActivityContract.Args>()

        definition.unregister(launcher)

        fakePassiveChallengeWarmer.awaitUnregisterCall()
        fakePassiveChallengeWarmer.ensureAllEventsConsumed()
    }

    @Test
    fun `'toResult' should set androidVerificationObject to null for new RadarOptions object`() {
        val definition = createPassiveChallengeConfirmationDefinition()
        val testToken = "test_token"

        val result = definition.toResult(
            confirmationOption = PAYMENT_METHOD_CONFIRMATION_OPTION_NEW,
            confirmationArgs = CONFIRMATION_PARAMETERS,
            launcherArgs = launcherArgs,
            result = PassiveChallengeActivityResult.Success(testToken),
        )

        val nextStepResult = result.asNextStep()
        val option = nextStepResult.confirmationOption as PaymentMethodConfirmationOption.New

        val expectedCreateParams = PAYMENT_METHOD_CONFIRMATION_OPTION_NEW.createParams.copy(
            radarOptions = RadarOptions(
                hCaptchaToken = testToken,
                androidVerificationObject = null
            )
        )
        assertThat(option.createParams).isEqualTo(expectedCreateParams)
    }

    @Test
    fun `'toResult' should set RadarOptions to null for New option when token is null (Failed result)`() {
        val definition = createPassiveChallengeConfirmationDefinition()

        val result = definition.toResult(
            confirmationOption = PAYMENT_METHOD_CONFIRMATION_OPTION_NEW,
            confirmationArgs = CONFIRMATION_PARAMETERS,
            launcherArgs = launcherArgs,
            result = PassiveChallengeActivityResult.Failed(RuntimeException("Failed")),
        )

        val nextStepResult = result.asNextStep()
        val option = nextStepResult.confirmationOption as PaymentMethodConfirmationOption.New

        // Verify that radarOptions is not set by checking equality with expected option
        val expectedOption = PAYMENT_METHOD_CONFIRMATION_OPTION_NEW.copy(
            confirmationChallengeState = ConfirmationChallengeState(passiveChallengeComplete = true),
            createParams = PAYMENT_METHOD_CONFIRMATION_OPTION_NEW.createParams.copy(
                radarOptions = null
            )
        )
        assertThat(option).isEqualTo(expectedOption)
    }

    @Test
    fun `'toResult' should leave androidVerificationObject as is in RadarOptions for New option`() {
        val definition = createPassiveChallengeConfirmationDefinition()
        val testToken = "test_token"
        val attestationToken = "attestation_token"

        val result = definition.toResult(
            confirmationOption = PAYMENT_METHOD_CONFIRMATION_OPTION_NEW.copy(
                createParams = PAYMENT_METHOD_CONFIRMATION_OPTION_NEW.createParams.copy(
                    radarOptions = RadarOptions(
                        androidVerificationObject = AndroidVerificationObject(attestationToken),
                        hCaptchaToken = null
                    )
                )
            ),
            confirmationArgs = CONFIRMATION_PARAMETERS,
            launcherArgs = launcherArgs,
            result = PassiveChallengeActivityResult.Success(testToken),
        )

        val nextStepResult = result.asNextStep()
        val option = nextStepResult.confirmationOption as PaymentMethodConfirmationOption.New

        val expectedCreateParams = PAYMENT_METHOD_CONFIRMATION_OPTION_NEW.createParams.copy(
            radarOptions = RadarOptions(
                hCaptchaToken = testToken,
                androidVerificationObject = AndroidVerificationObject(attestationToken)
            )
        )
        assertThat(option.createParams).isEqualTo(expectedCreateParams)
    }

    @Test
    fun `'canConfirm' should return false when not eligible for confirmation challenge`() {
        val definition = createPassiveChallengeConfirmationDefinition(
            isEligibleForConfirmationChallenge = FakeIsEligibleForConfirmationChallenge(isEligible = false)
        )

        val result = definition.canConfirm(
            confirmationOption = PAYMENT_METHOD_CONFIRMATION_OPTION_NEW,
            confirmationArgs = CONFIRMATION_PARAMETERS
        )

        assertThat(result).isFalse()
    }

    private fun createPassiveChallengeConfirmationDefinition(
        errorReporter: ErrorReporter = FakeErrorReporter(),
        passiveChallengeWarmer: PassiveChallengeWarmer = FakePassiveChallengeWarmer(),
        publishableKey: String = launcherArgs.publishableKey,
        productUsage: Set<String> = launcherArgs.productUsage,
        isEligibleForConfirmationChallenge: IsEligibleForConfirmationChallenge =
            FakeIsEligibleForConfirmationChallenge()
    ): PassiveChallengeConfirmationDefinition {
        return PassiveChallengeConfirmationDefinition(
            errorReporter = errorReporter,
            publishableKeyProvider = { publishableKey },
            productUsage = productUsage,
            passiveChallengeWarmer = passiveChallengeWarmer,
            isEligibleForConfirmationChallenge = isEligibleForConfirmationChallenge
        )
    }

    private companion object {
        private val PASSIVE_CAPTCHA_PARAMS = PassiveCaptchaParams(
            siteKey = "site_key",
            rqData = null
        )

        private val PAYMENT_METHOD_CONFIRMATION_OPTION_NEW = PaymentMethodConfirmationOption.New(
            createParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
            optionsParams = null,
            extraParams = null,
            shouldSave = false,
        )

        private val PAYMENT_METHOD_CONFIRMATION_OPTION_SAVED = PaymentMethodConfirmationOption.Saved(
            paymentMethod = PAYMENT_INTENT.paymentMethod!!,
            optionsParams = null,
            originatedFromWallet = false,
        )

        private val launcherArgs = PassiveChallengeActivityContract.Args(
            passiveCaptchaParams = PASSIVE_CAPTCHA_PARAMS,
            publishableKey = "pk_123",
            productUsage = setOf("PaymentSheet")
        )
    }
}
