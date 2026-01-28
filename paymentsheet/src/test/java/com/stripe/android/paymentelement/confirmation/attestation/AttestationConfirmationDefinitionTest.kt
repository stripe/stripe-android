package com.stripe.android.paymentelement.confirmation.attestation

import com.google.common.truth.Truth.assertThat
import com.stripe.android.attestation.AttestationActivityContract
import com.stripe.android.attestation.AttestationActivityResult
import com.stripe.android.attestation.analytics.AttestationAnalyticsEventsReporter
import com.stripe.android.attestation.analytics.FakeAttestationAnalyticsEventsReporter
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.isInstanceOf
import com.stripe.android.link.FakeIntegrityRequestManager
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.AndroidVerificationObject
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
import com.stripe.android.testing.RadarOptionsFactory
import com.stripe.android.utils.FakeActivityResultLauncher
import com.stripe.attestation.IntegrityRequestManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.coroutines.CoroutineContext

internal class AttestationConfirmationDefinitionTest {

    @Test
    fun `'key' should be 'Attestation'`() {
        val definition = createAttestationConfirmationDefinition()

        assertThat(definition.key).isEqualTo("Attestation")
    }

    @Test
    fun `'option' return casted 'PaymentMethodConfirmationOption New'`() {
        val definition = createAttestationConfirmationDefinition()

        assertThat(definition.option(PAYMENT_METHOD_CONFIRMATION_OPTION_NEW))
            .isEqualTo(PAYMENT_METHOD_CONFIRMATION_OPTION_NEW)
    }

    @Test
    fun `'option' return casted 'PaymentMethodConfirmationOption Saved'`() {
        val definition = createAttestationConfirmationDefinition()

        assertThat(definition.option(PAYMENT_METHOD_CONFIRMATION_OPTION_SAVED))
            .isEqualTo(PAYMENT_METHOD_CONFIRMATION_OPTION_SAVED)
    }

    @Test
    fun `'option' return null for unknown option`() {
        val definition = createAttestationConfirmationDefinition()

        assertThat(definition.option(FakeConfirmationOption())).isNull()
    }

    @Test
    fun `'canConfirm' should return true when attestation is enabled`() {
        val definition = createAttestationConfirmationDefinition()

        val result = definition.canConfirm(
            confirmationOption = PAYMENT_METHOD_CONFIRMATION_OPTION_NEW,
            confirmationArgs = confirmationParametersWithAttestation(enabled = true)
        )

        assertThat(result).isTrue()
    }

    @Test
    fun `'canConfirm' should return false when attestation is disabled`() {
        val definition = createAttestationConfirmationDefinition()

        val result = definition.canConfirm(
            confirmationOption = PAYMENT_METHOD_CONFIRMATION_OPTION_NEW,
            confirmationArgs = confirmationParametersWithAttestation(enabled = false)
        )

        assertThat(result).isFalse()
    }

    @Test
    fun `'canConfirm' should return false when attestation is disabled in confirmationArgs`() {
        val definition = createAttestationConfirmationDefinition()

        val result = definition.canConfirm(
            confirmationOption = PAYMENT_METHOD_CONFIRMATION_OPTION_NEW,
            confirmationArgs = CONFIRMATION_PARAMETERS
        )

        assertThat(result).isFalse()
    }

    @Test
    fun `'createLauncher' should register launcher properly for activity result`() = runTest {
        val definition = createAttestationConfirmationDefinition()

        var onResultCalled = false
        val onResult: (AttestationActivityResult) -> Unit = { onResultCalled = true }

        DummyActivityResultCaller.test {
            definition.createLauncher(
                activityResultCaller = activityResultCaller,
                onResult = onResult,
            )

            val registerCall = awaitRegisterCall()

            assertThat(awaitNextRegisteredLauncher()).isNotNull()

            assertThat(registerCall.contract).isInstanceOf<AttestationActivityContract>()

            val callback = registerCall.callback.asCallbackFor<AttestationActivityResult>()

            callback.onActivityResult(AttestationActivityResult.Success("test_token"))

            assertThat(onResultCalled).isTrue()
        }
    }

    @Test
    fun `'action' should return expected 'Launch' action when attestation is enabled`() = runTest {
        val definition = createAttestationConfirmationDefinition()

        val action = definition.action(
            confirmationOption = PAYMENT_METHOD_CONFIRMATION_OPTION_NEW,
            confirmationArgs = confirmationParametersWithAttestation(enabled = true),
        )

        assertThat(action)
            .isInstanceOf<ConfirmationDefinition.Action.Launch<AttestationActivityContract.Args>>()

        val launchAction = action.asLaunch()

        assertThat(launchAction.launcherArguments.publishableKey).isEqualTo(launcherArgs.publishableKey)
        assertThat(launchAction.launcherArguments.productUsage).isEqualTo(launcherArgs.productUsage)
        assertThat(launchAction.receivesResultInProcess).isFalse()
    }

    @Test
    fun `'action' should return 'Fail' action when attestation is disabled`() = runTest {
        val definition = createAttestationConfirmationDefinition()

        val action = definition.action(
            confirmationOption = PAYMENT_METHOD_CONFIRMATION_OPTION_NEW,
            confirmationArgs = confirmationParametersWithAttestation(enabled = false),
        )

        assertThat(action)
            .isInstanceOf<ConfirmationDefinition.Action.Fail<AttestationActivityContract.Args>>()

        val failAction = action.asFail()

        assertThat(failAction.cause).isInstanceOf<IllegalArgumentException>()
        assertThat(failAction.cause.message).isEqualTo("Attestation is not enabled on intent confirmation")
        assertThat(failAction.message).isEqualTo("Attestation is not enabled on intent confirmation".resolvableString)
        assertThat(failAction.errorType).isEqualTo(ConfirmationHandler.Result.Failed.ErrorType.Internal)
    }

    @Test
    fun `'action' should report error when attestation is disabled`() = runTest {
        val fakeErrorReporter = FakeErrorReporter()
        val definition = createAttestationConfirmationDefinition(fakeErrorReporter)

        definition.action(
            confirmationOption = PAYMENT_METHOD_CONFIRMATION_OPTION_NEW,
            confirmationArgs = confirmationParametersWithAttestation(enabled = false),
        )

        val reportedErrors = fakeErrorReporter.getLoggedErrors()
        assertThat(reportedErrors).containsExactly(
            ErrorReporter.UnexpectedErrorEvent.INTENT_CONFIRMATION_HANDLER_ATTESTATION_INVOKED_WHEN_DISABLED.eventName
        )

        val call = fakeErrorReporter.awaitCall()
        assertThat(call.errorEvent).isEqualTo(
            ErrorReporter.UnexpectedErrorEvent.INTENT_CONFIRMATION_HANDLER_ATTESTATION_INVOKED_WHEN_DISABLED
        )
        assertThat(call.stripeException?.message)
            .isEqualTo("Attestation is not enabled on intent confirmation")
    }

    @Test
    fun `'launch' should launch properly with provided parameters`() = runTest {
        val definition = createAttestationConfirmationDefinition()

        val launcher = FakeActivityResultLauncher<AttestationActivityContract.Args>()

        definition.launch(
            confirmationOption = PAYMENT_METHOD_CONFIRMATION_OPTION_NEW,
            confirmationArgs = CONFIRMATION_PARAMETERS,
            launcher = launcher,
            arguments = launcherArgs,
        )

        val launchCall = launcher.calls.awaitItem()

        assertThat(launchCall.input.publishableKey).isEqualTo(launcherArgs.publishableKey)
        assertThat(launchCall.input.productUsage).isEqualTo(launcherArgs.productUsage)
    }

    @Test
    fun `'action' should work with Saved PaymentMethodConfirmationOption when attestation is enabled`() =
        runTest {
            val definition = createAttestationConfirmationDefinition()

            val action = definition.action(
                confirmationOption = PAYMENT_METHOD_CONFIRMATION_OPTION_SAVED,
                confirmationArgs = confirmationParametersWithAttestation(enabled = true),
            )

            assertThat(action)
                .isInstanceOf<ConfirmationDefinition.Action.Launch<AttestationActivityContract.Args>>()

            val launchAction = action.asLaunch()

            assertThat(launchAction.launcherArguments.publishableKey).isEqualTo(launcherArgs.publishableKey)
            assertThat(launchAction.launcherArguments.productUsage).isEqualTo(launcherArgs.productUsage)
            assertThat(launchAction.receivesResultInProcess).isFalse()
            assertThat(launchAction.deferredIntentConfirmationType).isNull()
        }

    @Test
    fun `'launch' should work with Saved PaymentMethodConfirmationOption`() = runTest {
        val definition = createAttestationConfirmationDefinition()

        val launcher = FakeActivityResultLauncher<AttestationActivityContract.Args>()

        definition.launch(
            confirmationOption = PAYMENT_METHOD_CONFIRMATION_OPTION_SAVED,
            confirmationArgs = CONFIRMATION_PARAMETERS,
            launcher = launcher,
            arguments = launcherArgs,
        )

        val launchCall = launcher.calls.awaitItem()

        assertThat(launchCall.input.publishableKey).isEqualTo(launcherArgs.publishableKey)
        assertThat(launchCall.input.productUsage).isEqualTo(launcherArgs.productUsage)
    }

    @Test
    fun `'toResult' should return 'NextStep' with androidVerificationObject for Success result with New option`() {
        val definition = createAttestationConfirmationDefinition()
        val testToken = "test_token"

        val result = definition.toResult(
            confirmationOption = PAYMENT_METHOD_CONFIRMATION_OPTION_NEW,
            confirmationArgs = CONFIRMATION_PARAMETERS,
            launcherArgs = launcherArgs,
            result = AttestationActivityResult.Success(testToken),
        )

        assertThat(result).isInstanceOf<ConfirmationDefinition.Result.NextStep>()

        val nextStepResult = result.asNextStep()

        val expectedOption = PAYMENT_METHOD_CONFIRMATION_OPTION_NEW.copy(
            createParams = PAYMENT_METHOD_CONFIRMATION_OPTION_NEW.createParams.copy(
                radarOptions = RadarOptions(
                    hCaptchaToken = null,
                    androidVerificationObject = AndroidVerificationObject(
                        androidVerificationToken = testToken
                    )
                )
            ),
            confirmationChallengeState = ConfirmationChallengeState(attestationComplete = true)
        )

        assertThat(nextStepResult.confirmationOption).isEqualTo(expectedOption)
        assertThat(nextStepResult.arguments).isEqualTo(CONFIRMATION_PARAMETERS)
    }

    @Test
    fun `'toResult' should return 'NextStep' unchanged for Failed result with New option`() {
        val definition = createAttestationConfirmationDefinition()
        val exception = RuntimeException("Attestation failed")

        val result = definition.toResult(
            confirmationOption = PAYMENT_METHOD_CONFIRMATION_OPTION_NEW,
            confirmationArgs = CONFIRMATION_PARAMETERS,
            launcherArgs = launcherArgs,
            result = AttestationActivityResult.Failed(exception),
        )

        assertThat(result).isInstanceOf<ConfirmationDefinition.Result.NextStep>()

        val nextStepResult = result.asNextStep()

        // When attestation fails, continue without the token but mark attestation as complete
        val expectedOption = PAYMENT_METHOD_CONFIRMATION_OPTION_NEW.copy(
            confirmationChallengeState = ConfirmationChallengeState(attestationComplete = true)
        )
        assertThat(nextStepResult.confirmationOption).isEqualTo(expectedOption)
        assertThat(nextStepResult.arguments).isEqualTo(CONFIRMATION_PARAMETERS)
    }

    @Test
    fun `'toResult' should return 'NextStep' with attestationToken for Success result with Saved option`() {
        val definition = createAttestationConfirmationDefinition()
        val testToken = "test_token"

        val result = definition.toResult(
            confirmationOption = PAYMENT_METHOD_CONFIRMATION_OPTION_SAVED,
            confirmationArgs = CONFIRMATION_PARAMETERS,
            deferredIntentConfirmationType = null,
            result = AttestationActivityResult.Success(testToken),
        )

        assertThat(result).isInstanceOf<ConfirmationDefinition.Result.NextStep>()

        val nextStepResult = result.asNextStep()

        val expectedOption = PAYMENT_METHOD_CONFIRMATION_OPTION_SAVED.copy(
            confirmationChallengeState = ConfirmationChallengeState(
                attestationToken = testToken,
                attestationComplete = true
            )
        )

        assertThat(nextStepResult.confirmationOption).isEqualTo(expectedOption)
        assertThat(nextStepResult.arguments).isEqualTo(CONFIRMATION_PARAMETERS)
    }

    @Test
    fun `'toResult' should return 'NextStep' unchanged for Failed result with Saved option`() {
        val definition = createAttestationConfirmationDefinition()
        val exception = RuntimeException("Attestation failed")

        val result = definition.toResult(
            confirmationOption = PAYMENT_METHOD_CONFIRMATION_OPTION_SAVED,
            confirmationArgs = CONFIRMATION_PARAMETERS,
            deferredIntentConfirmationType = null,
            result = AttestationActivityResult.Failed(exception),
        )

        assertThat(result).isInstanceOf<ConfirmationDefinition.Result.NextStep>()

        val nextStepResult = result.asNextStep()

        // When attestation fails, continue without the token but mark attestation as complete
        val expectedOption = PAYMENT_METHOD_CONFIRMATION_OPTION_SAVED.copy(
            confirmationChallengeState = ConfirmationChallengeState(attestationComplete = true)
        )
        assertThat(nextStepResult.confirmationOption).isEqualTo(expectedOption)
        assertThat(nextStepResult.arguments).isEqualTo(CONFIRMATION_PARAMETERS)
    }

    @Test
    fun `'toResult' should leave hCaptchaToken as is in RadarOptions for New option`() {
        val definition = createAttestationConfirmationDefinition()
        val testToken = "attestation_token"
        val hCaptchaToken = "hcaptcha_token"

        val result = definition.toResult(
            confirmationOption = PAYMENT_METHOD_CONFIRMATION_OPTION_NEW.copy(
                createParams = PAYMENT_METHOD_CONFIRMATION_OPTION_NEW.createParams.copy(
                    radarOptions = RadarOptions(
                        hCaptchaToken = hCaptchaToken,
                        androidVerificationObject = null
                    )
                )
            ),
            confirmationArgs = CONFIRMATION_PARAMETERS,
            launcherArgs = launcherArgs,
            result = AttestationActivityResult.Success(testToken),
        )

        val nextStepResult = result.asNextStep()
        val option = nextStepResult.confirmationOption as PaymentMethodConfirmationOption.New

        val expectedCreateParams = PAYMENT_METHOD_CONFIRMATION_OPTION_NEW.createParams.copy(
            radarOptions = RadarOptions(
                hCaptchaToken = hCaptchaToken,
                androidVerificationObject = AndroidVerificationObject(testToken)
            )
        )
        assertThat(option.createParams).isEqualTo(expectedCreateParams)
    }

    @Test
    fun `'canConfirm' should return false when New option already has a token`() {
        val definition = createAttestationConfirmationDefinition()

        val optionWithToken = PAYMENT_METHOD_CONFIRMATION_OPTION_NEW.copy(
            createParams = PAYMENT_METHOD_CONFIRMATION_OPTION_NEW.createParams.copy(
                radarOptions = RadarOptionsFactory.create(
                    hCaptchaToken = null
                )
            )
        )

        val result = definition.canConfirm(
            confirmationOption = optionWithToken,
            confirmationArgs = confirmationParametersWithAttestation(enabled = true)
        )

        assertThat(result).isFalse()
    }

    @Test
    fun `'canConfirm' should return false when Saved option already has a token`() {
        val definition = createAttestationConfirmationDefinition()

        val optionWithToken = PAYMENT_METHOD_CONFIRMATION_OPTION_SAVED.copy(
            confirmationChallengeState = ConfirmationChallengeState(attestationToken = "existing_token")
        )

        val result = definition.canConfirm(
            confirmationOption = optionWithToken,
            confirmationArgs = confirmationParametersWithAttestation(enabled = true)
        )

        assertThat(result).isFalse()
    }

    @Test
    fun `'canConfirm' should return false when attestationComplete is true`() {
        val definition = createAttestationConfirmationDefinition()

        val optionWithAttestationComplete = PAYMENT_METHOD_CONFIRMATION_OPTION_NEW.copy(
            confirmationChallengeState = ConfirmationChallengeState(attestationComplete = true)
        )

        val result = definition.canConfirm(
            confirmationOption = optionWithAttestationComplete,
            confirmationArgs = confirmationParametersWithAttestation(enabled = true)
        )

        assertThat(result).isFalse()
    }

    @Test
    fun `'bootstrap' should call prepare on IntegrityRequestManager when attestation is enabled`() = runTest {
        val fakeIntegrityRequestManager = FakeIntegrityRequestManager()
        val definition = createAttestationConfirmationDefinition(
            integrityRequestManager = fakeIntegrityRequestManager
        )

        val paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            attestOnIntentConfirmation = true
        )

        definition.bootstrap(paymentMethodMetadata)

        fakeIntegrityRequestManager.awaitPrepareCall()
        fakeIntegrityRequestManager.ensureAllEventsConsumed()
    }

    @Test
    fun `'bootstrap' should not call prepare on IntegrityRequestManager when attestation is disabled`() {
        val fakeIntegrityRequestManager = FakeIntegrityRequestManager()
        val definition = createAttestationConfirmationDefinition(
            integrityRequestManager = fakeIntegrityRequestManager
        )

        val paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            attestOnIntentConfirmation = false
        )

        definition.bootstrap(paymentMethodMetadata)

        fakeIntegrityRequestManager.ensureAllEventsConsumed()
    }

    @Test
    fun `'bootstrap' should report error when IntegrityRequestManager prepare fails`() = runTest {
        val fakeErrorReporter = FakeErrorReporter()
        val fakeIntegrityRequestManager = FakeIntegrityRequestManager().apply {
            prepareResult = Result.failure(RuntimeException("Preparation failed"))
        }
        val definition = createAttestationConfirmationDefinition(
            errorReporter = fakeErrorReporter,
            integrityRequestManager = fakeIntegrityRequestManager
        )

        val paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            attestOnIntentConfirmation = true
        )

        definition.bootstrap(paymentMethodMetadata)
        fakeIntegrityRequestManager.awaitPrepareCall()

        val call = fakeErrorReporter.awaitCall()
        assertThat(call.errorEvent).isEqualTo(
            ErrorReporter.UnexpectedErrorEvent.INTENT_CONFIRMATION_HANDLER_ATTESTATION_FAILED_TO_PREPARE
        )
        assertThat(call.stripeException?.message).isEqualTo("Preparation failed")
    }

    @Test
    fun `'bootstrap' should call prepare on eventsReporter when attestation is enabled`() = runTest {
        val fakeEventsReporter = FakeAttestationAnalyticsEventsReporter()
        val definition = createAttestationConfirmationDefinition(
            eventsReporter = fakeEventsReporter
        )

        val paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            attestOnIntentConfirmation = true
        )

        definition.bootstrap(paymentMethodMetadata)

        val call = fakeEventsReporter.awaitCall()
        assertThat(call).isEqualTo(FakeAttestationAnalyticsEventsReporter.Call.Prepare)
    }

    @Test
    fun `'bootstrap' should call prepareSucceeded on eventsReporter when IntegrityRequestManager prepare succeeds`() =
        runTest {
            val fakeEventsReporter = FakeAttestationAnalyticsEventsReporter()
            val fakeIntegrityRequestManager = FakeIntegrityRequestManager()
            val definition = createAttestationConfirmationDefinition(
                eventsReporter = fakeEventsReporter,
                integrityRequestManager = fakeIntegrityRequestManager
            )

            val paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                attestOnIntentConfirmation = true
            )

            definition.bootstrap(paymentMethodMetadata)
            fakeIntegrityRequestManager.awaitPrepareCall()

            val prepareCall = fakeEventsReporter.awaitCall()
            assertThat(prepareCall).isEqualTo(FakeAttestationAnalyticsEventsReporter.Call.Prepare)

            val prepareSucceededCall = fakeEventsReporter.awaitCall()
            assertThat(prepareSucceededCall).isEqualTo(FakeAttestationAnalyticsEventsReporter.Call.PrepareSucceeded)
        }

    @Test
    fun `'bootstrap' should call prepareFailed on eventsReporter when IntegrityRequestManager prepare fails`() =
        runTest {
            val fakeEventsReporter = FakeAttestationAnalyticsEventsReporter()
            val exception = RuntimeException("Preparation failed")
            val fakeIntegrityRequestManager = FakeIntegrityRequestManager().apply {
                prepareResult = Result.failure(exception)
            }
            val definition = createAttestationConfirmationDefinition(
                eventsReporter = fakeEventsReporter,
                integrityRequestManager = fakeIntegrityRequestManager
            )

            val paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                attestOnIntentConfirmation = true
            )

            definition.bootstrap(paymentMethodMetadata)
            fakeIntegrityRequestManager.awaitPrepareCall()

            val prepareCall = fakeEventsReporter.awaitCall()
            assertThat(prepareCall).isEqualTo(FakeAttestationAnalyticsEventsReporter.Call.Prepare)

            val prepareFailedCall = fakeEventsReporter.awaitCall()
            assertThat(prepareFailedCall)
                .isEqualTo(FakeAttestationAnalyticsEventsReporter.Call.PrepareFailed(exception))
        }

    @Test
    fun `'bootstrap' should not call eventsReporter when attestation is disabled`() {
        val fakeEventsReporter = FakeAttestationAnalyticsEventsReporter()
        val definition = createAttestationConfirmationDefinition(
            eventsReporter = fakeEventsReporter
        )

        val paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            attestOnIntentConfirmation = false
        )

        definition.bootstrap(paymentMethodMetadata)

        fakeEventsReporter.ensureAllEventsConsumed()
    }

    @Test
    fun `'canConfirm' should return false when not eligible for confirmation challenge`() {
        val definition = createAttestationConfirmationDefinition(
            isEligibleForConfirmationChallenge = FakeIsEligibleForConfirmationChallenge(isEligible = false)
        )

        val result = definition.canConfirm(
            confirmationOption = PAYMENT_METHOD_CONFIRMATION_OPTION_NEW,
            confirmationArgs = confirmationParametersWithAttestation(enabled = true)
        )

        assertThat(result).isFalse()
    }

    private fun createAttestationConfirmationDefinition(
        errorReporter: ErrorReporter = FakeErrorReporter(),
        integrityRequestManager: IntegrityRequestManager = FakeIntegrityRequestManager(),
        coroutineScope: CoroutineScope = CoroutineScope(UnconfinedTestDispatcher()),
        workContext: CoroutineContext = UnconfinedTestDispatcher(),
        publishableKey: String = launcherArgs.publishableKey,
        productUsage: Set<String> = launcherArgs.productUsage,
        eventsReporter: AttestationAnalyticsEventsReporter = FakeAttestationAnalyticsEventsReporter(),
        isEligibleForConfirmationChallenge: IsEligibleForConfirmationChallenge =
            FakeIsEligibleForConfirmationChallenge()
    ): AttestationConfirmationDefinition {
        return AttestationConfirmationDefinition(
            errorReporter = errorReporter,
            integrityRequestManager = integrityRequestManager,
            coroutineScope = coroutineScope,
            workContext = workContext,
            publishableKeyProvider = { publishableKey },
            productUsage = productUsage,
            attestationAnalyticsEventsReporter = eventsReporter,
            isEligibleForConfirmationChallenge = isEligibleForConfirmationChallenge
        )
    }

    private companion object {
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

        private val launcherArgs = AttestationActivityContract.Args(
            publishableKey = "pk_123",
            productUsage = setOf("PaymentSheet")
        )

        private fun confirmationParametersWithAttestation(enabled: Boolean) = CONFIRMATION_PARAMETERS.copy(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PAYMENT_INTENT,
                attestOnIntentConfirmation = enabled
            )
        )
    }
}
