package com.stripe.android.paymentelement.confirmation.attestation

import com.google.common.truth.Truth.assertThat
import com.stripe.android.attestation.AttestationActivityContract
import com.stripe.android.attestation.AttestationActivityResult
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.isInstanceOf
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.AndroidVerificationObject
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.model.RadarOptions
import com.stripe.android.paymentelement.confirmation.CONFIRMATION_PARAMETERS
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.FakeConfirmationOption
import com.stripe.android.paymentelement.confirmation.PAYMENT_INTENT
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.paymentelement.confirmation.asCallbackFor
import com.stripe.android.paymentelement.confirmation.asFail
import com.stripe.android.paymentelement.confirmation.asLaunch
import com.stripe.android.paymentelement.confirmation.asNextStep
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.android.testing.RadarOptionsFactory
import com.stripe.android.utils.DummyActivityResultCaller
import com.stripe.android.utils.FakeActivityResultLauncher
import kotlinx.coroutines.test.runTest
import org.junit.Test

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
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            attestOnIntentConfirmation = true
        )
        definition.bootstrap(paymentMethodMetadata)

        val result = definition.canConfirm(
            confirmationOption = PAYMENT_METHOD_CONFIRMATION_OPTION_NEW,
            confirmationArgs = CONFIRMATION_PARAMETERS
        )

        assertThat(result).isTrue()
    }

    @Test
    fun `'canConfirm' should return false when attestation is disabled`() {
        val definition = createAttestationConfirmationDefinition()
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            attestOnIntentConfirmation = false
        )
        definition.bootstrap(paymentMethodMetadata)

        val result = definition.canConfirm(
            confirmationOption = PAYMENT_METHOD_CONFIRMATION_OPTION_NEW,
            confirmationArgs = CONFIRMATION_PARAMETERS
        )

        assertThat(result).isFalse()
    }

    @Test
    fun `'canConfirm' should return false before bootstrap is called`() {
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
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            attestOnIntentConfirmation = true
        )
        definition.bootstrap(paymentMethodMetadata)

        val action = definition.action(
            confirmationOption = PAYMENT_METHOD_CONFIRMATION_OPTION_NEW,
            confirmationArgs = CONFIRMATION_PARAMETERS,
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
    fun `'action' should return 'Fail' action when attestation is disabled`() = runTest {
        val definition = createAttestationConfirmationDefinition()
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            attestOnIntentConfirmation = false
        )
        definition.bootstrap(paymentMethodMetadata)

        val action = definition.action(
            confirmationOption = PAYMENT_METHOD_CONFIRMATION_OPTION_NEW,
            confirmationArgs = CONFIRMATION_PARAMETERS,
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
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            attestOnIntentConfirmation = false
        )
        definition.bootstrap(paymentMethodMetadata)

        definition.action(
            confirmationOption = PAYMENT_METHOD_CONFIRMATION_OPTION_NEW,
            confirmationArgs = CONFIRMATION_PARAMETERS,
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
    fun `'toResult' should return 'NextStep' with androidVerificationObject for Success result with New option`() {
        val definition = createAttestationConfirmationDefinition()
        val testToken = "test_token"

        val result = definition.toResult(
            confirmationOption = PAYMENT_METHOD_CONFIRMATION_OPTION_NEW,
            confirmationArgs = CONFIRMATION_PARAMETERS,
            deferredIntentConfirmationType = null,
            isConfirmationToken = false,
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
            )
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
            isConfirmationToken = false,
            result = AttestationActivityResult.Success(testToken),
        )

        assertThat(result).isInstanceOf<ConfirmationDefinition.Result.NextStep>()

        val nextStepResult = result.asNextStep()

        val expectedOption = PAYMENT_METHOD_CONFIRMATION_OPTION_SAVED.copy(
            attestationToken = testToken
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
            deferredIntentConfirmationType = null,
            isConfirmationToken = false,
            result = AttestationActivityResult.Failed(exception),
        )

        assertThat(result).isInstanceOf<ConfirmationDefinition.Result.NextStep>()

        val nextStepResult = result.asNextStep()

        // When attestation fails, continue without the token (no radarOptions attached)
        assertThat(nextStepResult.confirmationOption).isEqualTo(PAYMENT_METHOD_CONFIRMATION_OPTION_NEW)
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
            isConfirmationToken = false,
            result = AttestationActivityResult.Failed(exception),
        )

        assertThat(result).isInstanceOf<ConfirmationDefinition.Result.NextStep>()

        val nextStepResult = result.asNextStep()

        assertThat(nextStepResult.confirmationOption).isEqualTo(PAYMENT_METHOD_CONFIRMATION_OPTION_SAVED)
        assertThat(nextStepResult.arguments).isEqualTo(CONFIRMATION_PARAMETERS)
    }

    @Test
    fun `'action' should work with Saved PaymentMethodConfirmationOption when attestation is enabled`() =
        runTest {
            val definition = createAttestationConfirmationDefinition()
            val paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                attestOnIntentConfirmation = true
            )
            definition.bootstrap(paymentMethodMetadata)

            val action = definition.action(
                confirmationOption = PAYMENT_METHOD_CONFIRMATION_OPTION_SAVED,
                confirmationArgs = CONFIRMATION_PARAMETERS,
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
    fun `'bootstrap' should store attestOnIntentConfirmation flag`() {
        val definition = createAttestationConfirmationDefinition()

        val paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            attestOnIntentConfirmation = true
        )

        definition.bootstrap(paymentMethodMetadata)

        val result = definition.canConfirm(
            confirmationOption = PAYMENT_METHOD_CONFIRMATION_OPTION_NEW,
            confirmationArgs = CONFIRMATION_PARAMETERS
        )

        assertThat(result).isTrue()
    }

    @Test
    fun `'bootstrap' should handle disabled attestation`() {
        val definition = createAttestationConfirmationDefinition()

        val paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            attestOnIntentConfirmation = false
        )

        definition.bootstrap(paymentMethodMetadata)

        val result = definition.canConfirm(
            confirmationOption = PAYMENT_METHOD_CONFIRMATION_OPTION_NEW,
            confirmationArgs = CONFIRMATION_PARAMETERS
        )

        assertThat(result).isFalse()
    }

    @Test
    fun `'action' should become disabled after first successful call`() = runTest {
        val definition = createAttestationConfirmationDefinition()
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            attestOnIntentConfirmation = true
        )
        definition.bootstrap(paymentMethodMetadata)

        // First call should succeed
        val firstAction = definition.action(
            confirmationOption = PAYMENT_METHOD_CONFIRMATION_OPTION_NEW,
            confirmationArgs = CONFIRMATION_PARAMETERS,
        )

        assertThat(firstAction)
            .isInstanceOf<ConfirmationDefinition.Action.Launch<AttestationActivityContract.Args>>()

        // Second call should fail
        val secondAction = definition.action(
            confirmationOption = PAYMENT_METHOD_CONFIRMATION_OPTION_NEW,
            confirmationArgs = CONFIRMATION_PARAMETERS,
        )

        assertThat(secondAction)
            .isInstanceOf<ConfirmationDefinition.Action.Fail<AttestationActivityContract.Args>>()
    }

    @Test
    fun `'canConfirm' should return false after calling action`() = runTest {
        val definition = createAttestationConfirmationDefinition()
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            attestOnIntentConfirmation = true
        )
        definition.bootstrap(paymentMethodMetadata)

        // Verify it can confirm before action is called
        assertThat(
            definition.canConfirm(
                confirmationOption = PAYMENT_METHOD_CONFIRMATION_OPTION_NEW,
                confirmationArgs = CONFIRMATION_PARAMETERS
            )
        ).isTrue()

        // Call action
        definition.action(
            confirmationOption = PAYMENT_METHOD_CONFIRMATION_OPTION_NEW,
            confirmationArgs = CONFIRMATION_PARAMETERS,
        )

        // Verify it can no longer confirm after action is called
        assertThat(
            definition.canConfirm(
                confirmationOption = PAYMENT_METHOD_CONFIRMATION_OPTION_NEW,
                confirmationArgs = CONFIRMATION_PARAMETERS
            )
        ).isFalse()
    }

    @Test
    fun `'canConfirm' should return false when New option already has a token`() {
        val definition = createAttestationConfirmationDefinition()
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            attestOnIntentConfirmation = true
        )
        definition.bootstrap(paymentMethodMetadata)

        val optionWithToken = PAYMENT_METHOD_CONFIRMATION_OPTION_NEW.copy(
            createParams = PAYMENT_METHOD_CONFIRMATION_OPTION_NEW.createParams.copy(
                radarOptions = RadarOptionsFactory.create(
                    hCaptchaToken = null
                )
            )
        )

        val result = definition.canConfirm(
            confirmationOption = optionWithToken,
            confirmationArgs = CONFIRMATION_PARAMETERS
        )

        assertThat(result).isFalse()
    }

    @Test
    fun `'canConfirm' should return false when Saved option already has a token`() {
        val definition = createAttestationConfirmationDefinition()
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            attestOnIntentConfirmation = true
        )
        definition.bootstrap(paymentMethodMetadata)

        val optionWithToken = PAYMENT_METHOD_CONFIRMATION_OPTION_SAVED.copy(
            attestationToken = "existing_token"
        )

        val result = definition.canConfirm(
            confirmationOption = optionWithToken,
            confirmationArgs = CONFIRMATION_PARAMETERS
        )

        assertThat(result).isFalse()
    }

    private fun createAttestationConfirmationDefinition(
        errorReporter: ErrorReporter = FakeErrorReporter(),
        publishableKey: String = launcherArgs.publishableKey,
        productUsage: Set<String> = launcherArgs.productUsage
    ): AttestationConfirmationDefinition {
        return AttestationConfirmationDefinition(
            errorReporter = errorReporter,
            publishableKeyProvider = { publishableKey },
            productUsage = productUsage,
        )
    }

    private companion object {
        private val PAYMENT_METHOD_CONFIRMATION_OPTION_NEW = PaymentMethodConfirmationOption.New(
            createParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
            optionsParams = null,
            extraParams = null,
            shouldSave = false,
            passiveCaptchaParams = null,
        )

        private val PAYMENT_METHOD_CONFIRMATION_OPTION_SAVED = PaymentMethodConfirmationOption.Saved(
            paymentMethod = PAYMENT_INTENT.paymentMethod!!,
            optionsParams = null,
            originatedFromWallet = false,
            passiveCaptchaParams = null,
            hCaptchaToken = null,
        )

        private val launcherArgs = AttestationActivityContract.Args(
            publishableKey = "pk_123",
            productUsage = setOf("PaymentSheet")
        )
    }
}
