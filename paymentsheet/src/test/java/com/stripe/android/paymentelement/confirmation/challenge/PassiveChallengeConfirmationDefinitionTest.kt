package com.stripe.android.paymentelement.confirmation.challenge

import com.google.common.truth.Truth.assertThat
import com.stripe.android.challenge.passive.PassiveChallengeActivityContract
import com.stripe.android.challenge.passive.PassiveChallengeActivityResult
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.isInstanceOf
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures.DEFAULT_API_CONFIG
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
import com.stripe.android.paymentelement.confirmation.fakeLifecycleOwner
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.DummyActivityResultCaller
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.android.testing.FeatureFlagTestRule
import com.stripe.android.utils.FakeActivityResultLauncher
import com.stripe.android.utils.FakePassiveChallengeWarmer
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class PassiveChallengeConfirmationDefinitionTest {
    @get:Rule
    val coroutineTestRule = CoroutineTestRule(StandardTestDispatcher())

    @get:Rule
    val disablePassiveCaptchaWarmupRule = FeatureFlagTestRule(
        featureFlag = FeatureFlags.disablePassiveCaptchaWarmup,
        isEnabled = false
    )

    @Test
    fun `'key' should be 'ChallengePassive'`() = runScenario {
        assertThat(definition.key).isEqualTo("ChallengePassive")
    }

    @Test
    fun `'option' return casted 'PaymentMethodConfirmationOption New'`() = runScenario {
        assertThat(definition.option(newConfirmationOption))
            .isEqualTo(newConfirmationOption)
    }

    @Test
    fun `'option' return casted 'PaymentMethodConfirmationOption Saved'`() = runScenario {
        assertThat(definition.option(savedConfirmationOption))
            .isEqualTo(savedConfirmationOption)
    }

    @Test
    fun `'option' return null for unknown option`() = runScenario {
        assertThat(definition.option(FakeConfirmationOption())).isNull()
    }

    @Test
    fun `'canConfirm' should return true when passiveCaptchaParams is not null for New option`() = runScenario {
        val result = definition.canConfirm(
            confirmationOption = newConfirmationOption,
            confirmationArgs = CONFIRMATION_PARAMETERS
        )

        assertThat(result).isTrue()
    }

    @Test
    fun `'canConfirm' should return true when passiveCaptchaParams is not null for Saved option`() = runScenario {
        val result = definition.canConfirm(
            confirmationOption = savedConfirmationOption,
            confirmationArgs = CONFIRMATION_PARAMETERS
        )

        assertThat(result).isTrue()
    }

    @Test
    fun `'canConfirm' should return false when passiveCaptchaParams is null for New option`() = runScenario {
        val result = definition.canConfirm(
            confirmationOption = newConfirmationOption,
            confirmationArgs = CONFIRMATION_PARAMETERS.copy(
                paymentMethodMetadata = PaymentMethodMetadataFactory.create(passiveCaptchaParams = null)
            )
        )

        assertThat(result).isFalse()
    }

    @Test
    fun `'canConfirm' should return false when passiveCaptchaParams is null for Saved option`() = runScenario {
        val result = definition.canConfirm(
            confirmationOption = savedConfirmationOption,
            confirmationArgs = CONFIRMATION_PARAMETERS.copy(
                paymentMethodMetadata = PaymentMethodMetadataFactory.create(passiveCaptchaParams = null)
            )
        )

        assertThat(result).isFalse()
    }

    @Test
    fun `'createLauncher' should register launcher properly for activity result`() = runScenario {
        var onResultCalled = false
        val onResult: (PassiveChallengeActivityResult) -> Unit = { onResultCalled = true }

        DummyActivityResultCaller.test {
            definition.createLauncher(
                activityResultCaller = activityResultCaller,
                lifecycleOwner = fakeLifecycleOwner(),
                onResult = onResult,
            )

            passiveChallengeWarmer.registerCalls.awaitItem()
            val registerCall = awaitRegisterCall()

            assertThat(awaitNextRegisteredLauncher()).isNotNull()

            assertThat(registerCall.contract).isInstanceOf<PassiveChallengeActivityContract>()

            val callback = registerCall.callback.asCallbackFor<PassiveChallengeActivityResult>()

            callback.onActivityResult(PassiveChallengeActivityResult.Success("test_token"))

            assertThat(onResultCalled).isTrue()
        }
    }

    @Test
    fun `'createLauncher' registers the warmer with the caller and lifecycle owner`() = runScenario {
        DummyActivityResultCaller.test {
            val lifecycleOwner = fakeLifecycleOwner()
            definition.createLauncher(activityResultCaller, lifecycleOwner) {}

            val call = passiveChallengeWarmer.registerCalls.awaitItem()
            assertThat(call.activityResultCaller).isSameInstanceAs(activityResultCaller)
            assertThat(call.lifecycleOwner).isSameInstanceAs(lifecycleOwner)
            awaitRegisterCall()
            awaitNextRegisteredLauncher()
        }
    }

    @Test
    fun `'action' should return expected 'Launch' action when passiveCaptchaParams is not null`() = runScenario {
        val action = definition.action(
            confirmationOption = newConfirmationOption,
            confirmationArgs = CONFIRMATION_PARAMETERS,
        )

        assertThat(action)
            .isInstanceOf<ConfirmationDefinition.Action.Launch<PassiveChallengeActivityContract.Args>>()

        val launchAction = action.asLaunch()

        assertThat(launchAction.launcherArguments.passiveCaptchaParams)
            .isEqualTo(CONFIRMATION_PARAMETERS.paymentMethodMetadata.passiveCaptchaParams)
        assertThat(launchAction.launcherArguments.apiConfiguration)
            .isEqualTo(CONFIRMATION_PARAMETERS.paymentMethodMetadata.apiConfiguration)
        assertThat(launchAction.receivesResultInProcess).isFalse()
    }

    @Test
    fun `'action' should return 'Fail' action when passiveCaptchaParams is null`() = runScenario {
        val action = definition.action(
            confirmationOption = newConfirmationOption,
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
        assertThat(errorReporter.awaitCall().errorEvent).isEqualTo(
            ErrorReporter.UnexpectedErrorEvent.INTENT_CONFIRMATION_HANDLER_PASSIVE_CHALLENGE_PARAMS_NULL
        )
    }

    @Test
    fun `'action' should report error when passiveCaptchaParams is null`() = runScenario {
        definition.action(
            confirmationOption = newConfirmationOption,
            confirmationArgs = CONFIRMATION_PARAMETERS.copy(
                paymentMethodMetadata = PaymentMethodMetadataFactory.create(passiveCaptchaParams = null)
            )
        )

        val reportedErrors = errorReporter.getLoggedErrors()
        assertThat(reportedErrors).containsExactly(
            ErrorReporter.UnexpectedErrorEvent.INTENT_CONFIRMATION_HANDLER_PASSIVE_CHALLENGE_PARAMS_NULL.eventName
        )

        val call = errorReporter.awaitCall()
        assertThat(call.errorEvent).isEqualTo(
            ErrorReporter.UnexpectedErrorEvent.INTENT_CONFIRMATION_HANDLER_PASSIVE_CHALLENGE_PARAMS_NULL
        )
        assertThat(call.stripeException?.message).isEqualTo("Passive challenge params are null")
    }

    @Test
    fun `'launch' should launch properly with provided parameters`() = runScenario {
        val launcher = FakeActivityResultLauncher<PassiveChallengeActivityContract.Args>()

        definition.launch(
            confirmationOption = newConfirmationOption,
            confirmationArgs = CONFIRMATION_PARAMETERS.copy(
                paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                    passiveCaptchaParams = passiveCaptchaParams,
                )
            ),
            launcher = launcher,
            arguments = launcherArgs,
        )

        val launchCall = launcher.calls.awaitItem()

        assertThat(launchCall.input.passiveCaptchaParams).isEqualTo(passiveCaptchaParams)
    }

    @Test
    fun `'launch' should launch properly with launcher arguments`() = runScenario {
        val launcher = FakeActivityResultLauncher<PassiveChallengeActivityContract.Args>()

        definition.launch(
            confirmationOption = newConfirmationOption,
            confirmationArgs = CONFIRMATION_PARAMETERS,
            launcher = launcher,
            arguments = launcherArgs,
        )

        val launchCall = launcher.calls.awaitItem()

        assertThat(launchCall.input.passiveCaptchaParams).isEqualTo(passiveCaptchaParams)
        assertThat(launchCall.input.apiConfiguration).isEqualTo(launcherArgs.apiConfiguration)
        assertThat(launchCall.input.productUsage).isEqualTo(launcherArgs.productUsage)
    }

    @Test
    fun `'action' should work with Saved PaymentMethodConfirmationOption when passiveCaptchaParams is not null`() =
        runScenario {
            val action = definition.action(
                confirmationOption = savedConfirmationOption,
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
    fun `'launch' should work with Saved PaymentMethodConfirmationOption`() = runScenario {
        val launcher = FakeActivityResultLauncher<PassiveChallengeActivityContract.Args>()

        definition.launch(
            confirmationOption = savedConfirmationOption,
            confirmationArgs = CONFIRMATION_PARAMETERS.copy(
                paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                    passiveCaptchaParams = passiveCaptchaParams,
                )
            ),
            launcher = launcher,
            arguments = launcherArgs,
        )

        val launchCall = launcher.calls.awaitItem()

        assertThat(launchCall.input.passiveCaptchaParams)
            .isEqualTo(passiveCaptchaParams)
    }

    @Test
    fun `'toResult' completes the passive challenge for a successful New option`() = runScenario {
        val testToken = "test_token"

        val result = definition.toResult(
            confirmationOption = newConfirmationOption,
            confirmationArgs = CONFIRMATION_PARAMETERS,
            launcherArgs = launcherArgs,
            result = PassiveChallengeActivityResult.Success(testToken),
        )

        assertThat(result).isInstanceOf<ConfirmationDefinition.Result.NextStep>()

        val nextStepResult = result.asNextStep()

        val expectedOption = newConfirmationOption.copy(
            confirmationChallengeState = ConfirmationChallengeState(passiveChallengeComplete = true),
            createParams = newConfirmationOption.createParams.copy(
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
    fun `'toResult' completes the passive challenge for a failed New option`() = runScenario {
        val exception = RuntimeException("Captcha failed")

        val result = definition.toResult(
            confirmationOption = newConfirmationOption,
            confirmationArgs = CONFIRMATION_PARAMETERS,
            launcherArgs = launcherArgs,
            result = PassiveChallengeActivityResult.Failed(exception),
        )

        assertThat(result).isInstanceOf<ConfirmationDefinition.Result.NextStep>()

        val nextStepResult = result.asNextStep()

        assertThat(nextStepResult.confirmationOption).isEqualTo(
            newConfirmationOption.copy(
                confirmationChallengeState = ConfirmationChallengeState(passiveChallengeComplete = true)
            )
        )
        assertThat(nextStepResult.arguments).isEqualTo(CONFIRMATION_PARAMETERS)
    }

    @Test
    fun `'toResult' should return 'NextStep' for Success result with Saved option`() = runScenario {
        val testToken = "test_token"

        val result = definition.toResult(
            confirmationOption = savedConfirmationOption,
            confirmationArgs = CONFIRMATION_PARAMETERS,
            result = PassiveChallengeActivityResult.Success(testToken),
            launcherArgs = launcherArgs
        )

        assertThat(result).isInstanceOf<ConfirmationDefinition.Result.NextStep>()

        val nextStepResult = result.asNextStep()

        val expectedOption = savedConfirmationOption.copy(
            confirmationChallengeState = ConfirmationChallengeState(
                passiveChallengeComplete = true,
                hCaptchaToken = testToken
            )
        )

        assertThat(nextStepResult.confirmationOption).isEqualTo(expectedOption)
        assertThat(nextStepResult.arguments).isEqualTo(CONFIRMATION_PARAMETERS)
    }

    @Test
    fun `'toResult' completes the passive challenge for a failed Saved option`() = runScenario {
        val exception = RuntimeException("Captcha failed")

        val result = definition.toResult(
            confirmationOption = savedConfirmationOption,
            confirmationArgs = CONFIRMATION_PARAMETERS,
            result = PassiveChallengeActivityResult.Failed(exception),
            launcherArgs = launcherArgs
        )

        assertThat(result).isInstanceOf<ConfirmationDefinition.Result.NextStep>()

        val nextStepResult = result.asNextStep()

        assertThat(nextStepResult.confirmationOption).isEqualTo(
            savedConfirmationOption.copy(
                confirmationChallengeState = ConfirmationChallengeState(
                    passiveChallengeComplete = true,
                )
            )
        )
        assertThat(nextStepResult.arguments).isEqualTo(CONFIRMATION_PARAMETERS)
    }

    @Test
    fun `'bootstrap' passes captcha parameters to the warmer`() = runScenario {
        definition.bootstrap(PaymentMethodMetadataFactory.create(passiveCaptchaParams = passiveCaptchaParams))

        assertThat(passiveChallengeWarmer.startCalls.awaitItem()).isEqualTo(passiveCaptchaParams)
    }

    @Test
    fun `'bootstrap' skips warm-up without passive captcha parameters`() = runScenario {
        definition.bootstrap(PaymentMethodMetadataFactory.create(passiveCaptchaParams = null))
    }

    @Test
    fun `'bootstrap' skips warm-up when disabled`() = runScenario {
        disablePassiveCaptchaWarmupRule.setEnabled(true)

        definition.bootstrap(PaymentMethodMetadataFactory.create(passiveCaptchaParams = passiveCaptchaParams))
    }

    @Test
    fun `'unregister' unregisters the warmer and confirmation launcher`() = runScenario {
        val launcher = FakeActivityResultLauncher<PassiveChallengeActivityContract.Args>()

        definition.unregister(launcher)

        passiveChallengeWarmer.unregisterCalls.awaitItem()
        launcher.unregisterCalls.awaitItem()
        launcher.unregisterCalls.ensureAllEventsConsumed()
        launcher.calls.ensureAllEventsConsumed()
    }

    @Test
    fun `'toResult' should set androidVerificationObject to null for new RadarOptions object`() = runScenario {
        val testToken = "test_token"

        val result = definition.toResult(
            confirmationOption = newConfirmationOption,
            confirmationArgs = CONFIRMATION_PARAMETERS,
            launcherArgs = launcherArgs,
            result = PassiveChallengeActivityResult.Success(testToken),
        )

        val nextStepResult = result.asNextStep()
        val option = nextStepResult.confirmationOption as PaymentMethodConfirmationOption.New

        val expectedCreateParams = newConfirmationOption.createParams.copy(
            radarOptions = RadarOptions(
                hCaptchaToken = testToken,
                androidVerificationObject = null
            )
        )
        assertThat(option.createParams).isEqualTo(expectedCreateParams)
    }

    @Test
    fun `'toResult' should set RadarOptions to null for New option when token is null (Failed result)`() = runScenario {
        val result = definition.toResult(
            confirmationOption = newConfirmationOption,
            confirmationArgs = CONFIRMATION_PARAMETERS,
            launcherArgs = launcherArgs,
            result = PassiveChallengeActivityResult.Failed(RuntimeException("Failed")),
        )

        val nextStepResult = result.asNextStep()
        val option = nextStepResult.confirmationOption as PaymentMethodConfirmationOption.New

        // Verify that radarOptions is not set by checking equality with expected option
        val expectedOption = newConfirmationOption.copy(
            confirmationChallengeState = ConfirmationChallengeState(passiveChallengeComplete = true),
            createParams = newConfirmationOption.createParams.copy(
                radarOptions = null
            )
        )
        assertThat(option).isEqualTo(expectedOption)
    }

    @Test
    fun `'toResult' should leave androidVerificationObject as is in RadarOptions for New option`() = runScenario {
        val testToken = "test_token"
        val attestationToken = "attestation_token"
        val appId = "com.stripe.test"

        val result = definition.toResult(
            confirmationOption = newConfirmationOption.copy(
                createParams = newConfirmationOption.createParams.copy(
                    radarOptions = RadarOptions(
                        androidVerificationObject = AndroidVerificationObject(
                            androidVerificationToken = attestationToken,
                            appId = appId
                        ),
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

        val expectedCreateParams = newConfirmationOption.createParams.copy(
            radarOptions = RadarOptions(
                hCaptchaToken = testToken,
                androidVerificationObject = AndroidVerificationObject(
                    androidVerificationToken = attestationToken,
                    appId = appId
                )
            )
        )
        assertThat(option.createParams).isEqualTo(expectedCreateParams)
    }

    @Test
    fun `'canConfirm' should return false when not eligible for confirmation challenge`() = runScenario {
        val definition = createPassiveChallengeConfirmationDefinition(
            isEligibleForConfirmationChallenge = FakeIsEligibleForConfirmationChallenge(isEligible = false)
        )

        val result = definition.canConfirm(
            confirmationOption = newConfirmationOption,
            confirmationArgs = CONFIRMATION_PARAMETERS
        )

        assertThat(result).isFalse()
    }

    private fun runScenario(block: suspend Scenario.() -> Unit) = runTest {
        val passiveChallengeWarmer = FakePassiveChallengeWarmer()
        val errorReporter = FakeErrorReporter()
        val scenario = Scenario(passiveChallengeWarmer, errorReporter)
        try {
            scenario.block()
        } finally {
            passiveChallengeWarmer.ensureAllEventsConsumed()
            errorReporter.ensureAllEventsConsumed()
        }
    }

    private class Scenario(
        val passiveChallengeWarmer: FakePassiveChallengeWarmer,
        val errorReporter: FakeErrorReporter,
    ) {
        val passiveCaptchaParams = PassiveCaptchaParams(
            siteKey = "site_key",
            rqData = null,
            tokenTimeoutSeconds = null,
        )

        val newConfirmationOption = PaymentMethodConfirmationOption.New(
            createParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
            optionsParams = null,
            extraParams = null,
            shouldSave = false,
        )

        val savedConfirmationOption = PaymentMethodConfirmationOption.Saved(
            shippingInformation = null,
            paymentMethod = PAYMENT_INTENT.paymentMethod!!,
            optionsParams = null,
            originatedFromWallet = false,
        )

        val launcherArgs = PassiveChallengeActivityContract.Args(
            passiveCaptchaParams = passiveCaptchaParams,
            apiConfiguration = DEFAULT_API_CONFIG,
            productUsage = setOf("PaymentSheet"),
        )

        val definition = createPassiveChallengeConfirmationDefinition()

        fun createPassiveChallengeConfirmationDefinition(
            errorReporter: ErrorReporter = this.errorReporter,
            productUsage: Set<String> = launcherArgs.productUsage,
            isEligibleForConfirmationChallenge: IsEligibleForConfirmationChallenge =
                FakeIsEligibleForConfirmationChallenge(),
        ): PassiveChallengeConfirmationDefinition {
            return PassiveChallengeConfirmationDefinition(
                errorReporter = errorReporter,
                productUsage = productUsage,
                passiveChallengeWarmer = passiveChallengeWarmer,
                isEligibleForConfirmationChallenge = isEligibleForConfirmationChallenge,
            )
        }
    }
}
