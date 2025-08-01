package com.stripe.android.paymentelement.confirmation.challenge

import com.google.common.truth.Truth.assertThat
import com.stripe.android.challenge.PassiveChallengeActivityContract
import com.stripe.android.challenge.PassiveChallengeActivityResult
import com.stripe.android.isInstanceOf
import com.stripe.android.model.PassiveCaptchaParams
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.FakeConfirmationOption
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.paymentelement.confirmation.asCallbackFor
import com.stripe.android.paymentelement.confirmation.asLaunch
import com.stripe.android.paymentelement.confirmation.asNextStep
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.state.PaymentElementLoader
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
    fun `'action' should return expected 'Launch' action`() = runTest {
        val definition = createPassiveChallengeConfirmationDefinition()

        val action = definition.action(
            confirmationOption = PAYMENT_METHOD_CONFIRMATION_OPTION_NEW,
            confirmationParameters = CONFIRMATION_PARAMETERS,
        )

        assertThat(action).isInstanceOf<ConfirmationDefinition.Action.Launch<Unit>>()

        val launchAction = action.asLaunch()

        assertThat(launchAction.launcherArguments).isEqualTo(Unit)
        assertThat(launchAction.receivesResultInProcess).isFalse()
        assertThat(launchAction.deferredIntentConfirmationType).isNull()
    }

    @Test
    fun `'launch' should launch properly with provided parameters`() = runTest {
        val definition = createPassiveChallengeConfirmationDefinition()

        val launcher = FakeActivityResultLauncher<PassiveChallengeActivityContract.Args>()

        definition.launch(
            confirmationOption = PAYMENT_METHOD_CONFIRMATION_OPTION_NEW,
            confirmationParameters = CONFIRMATION_PARAMETERS,
            launcher = launcher,
            arguments = Unit,
        )

        val launchCall = launcher.calls.awaitItem()

        assertThat(launchCall.input.passiveCaptchaParams)
            .isEqualTo(PAYMENT_METHOD_CONFIRMATION_OPTION_NEW.passiveCaptchaParams)
    }

    @Test
    fun `'launch' should throw when passiveCaptchaParams is null`() = runTest {
        val definition = createPassiveChallengeConfirmationDefinition()
        val optionWithoutCaptcha = PAYMENT_METHOD_CONFIRMATION_OPTION_NEW.copy(
            passiveCaptchaParams = null
        )

        val launcher = FakeActivityResultLauncher<PassiveChallengeActivityContract.Args>()

        var thrownException: Throwable? = null
        try {
            definition.launch(
                confirmationOption = optionWithoutCaptcha,
                confirmationParameters = CONFIRMATION_PARAMETERS,
                launcher = launcher,
                arguments = Unit,
            )
        } catch (e: Exception) {
            thrownException = e
        }

        assertThat(thrownException).isInstanceOf<IllegalArgumentException>()
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

    private fun createPassiveChallengeConfirmationDefinition(): PassiveChallengeConfirmationDefinition {
        return PassiveChallengeConfirmationDefinition()
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

        private val PAYMENT_METHOD_CONFIRMATION_OPTION_NEW = PaymentMethodConfirmationOption.New(
            createParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
            optionsParams = null,
            extraParams = null,
            shouldSave = false,
            passiveCaptchaParams = PassiveCaptchaParams(
                siteKey = "site_key",
                rqData = null
            ),
        )
    }
}
