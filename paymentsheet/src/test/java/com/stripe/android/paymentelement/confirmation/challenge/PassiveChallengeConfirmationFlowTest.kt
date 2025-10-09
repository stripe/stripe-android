package com.stripe.android.paymentelement.confirmation.challenge

import com.stripe.android.challenge.PassiveChallengeActivityResult
import com.stripe.android.challenge.warmer.PassiveChallengeWarmer
import com.stripe.android.model.PassiveCaptchaParamsFactory
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.model.RadarOptions
import com.stripe.android.paymentelement.confirmation.CONFIRMATION_PARAMETERS
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.paymentelement.confirmation.runLaunchTest
import com.stripe.android.paymentelement.confirmation.runResultTest
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.android.testing.PaymentMethodFactory
import com.stripe.android.utils.FakePassiveChallengeWarmer
import org.junit.Test

internal class PassiveChallengeConfirmationFlowTest {
    @Test
    fun `on launch with New option, should persist parameters & launch using launcher as expected`() = runLaunchTest(
        confirmationOption = NEW_CONFIRMATION_OPTION,
        parameters = CONFIRMATION_PARAMETERS,
        definition = confirmationDefinition(),
    )

    @Test
    fun `on launch with Saved option, should persist parameters & launch using launcher as expected`() = runLaunchTest(
        confirmationOption = SAVED_CONFIRMATION_OPTION,
        parameters = CONFIRMATION_PARAMETERS,
        definition = confirmationDefinition(),
    )

    @Test
    fun `on result with Success, should return NextStep result with token attached for New option`() = runResultTest(
        confirmationOption = NEW_CONFIRMATION_OPTION,
        definition = confirmationDefinition(),
        launcherResult = PassiveChallengeActivityResult.Success("test_token"),
        parameters = CONFIRMATION_PARAMETERS,
        definitionResult = ConfirmationDefinition.Result.NextStep(
            confirmationOption = PaymentMethodConfirmationOption.New(
                createParams = NEW_CONFIRMATION_OPTION.createParams.copy(
                    radarOptions = RadarOptions("test_token")
                ),
                optionsParams = NEW_CONFIRMATION_OPTION.optionsParams,
                shouldSave = false,
                extraParams = null,
                passiveCaptchaParams = null
            ),
            parameters = CONFIRMATION_PARAMETERS,
        ),
    )

    @Test
    fun `on result with Success, should return NextStep result with token attached for Saved option`() = runResultTest(
        confirmationOption = SAVED_CONFIRMATION_OPTION,
        definition = confirmationDefinition(),
        launcherResult = PassiveChallengeActivityResult.Success("test_token"),
        parameters = CONFIRMATION_PARAMETERS,
        definitionResult = ConfirmationDefinition.Result.NextStep(
            confirmationOption = PaymentMethodConfirmationOption.Saved(
                paymentMethod = SAVED_CONFIRMATION_OPTION.paymentMethod,
                optionsParams = SAVED_CONFIRMATION_OPTION.optionsParams,
                originatedFromWallet = false,
                passiveCaptchaParams = null,
                hCaptchaToken = "test_token"
            ),
            parameters = CONFIRMATION_PARAMETERS,
        ),
    )

    @Test
    fun `on result with Failed, should return NextStep result with no token for New option`() = runResultTest(
        confirmationOption = NEW_CONFIRMATION_OPTION,
        definition = confirmationDefinition(),
        launcherResult = PassiveChallengeActivityResult.Failed(RuntimeException("Challenge failed")),
        parameters = CONFIRMATION_PARAMETERS,
        definitionResult = ConfirmationDefinition.Result.NextStep(
            confirmationOption = PaymentMethodConfirmationOption.New(
                createParams = NEW_CONFIRMATION_OPTION.createParams,
                optionsParams = NEW_CONFIRMATION_OPTION.optionsParams,
                shouldSave = false,
                extraParams = null,
                passiveCaptchaParams = null
            ),
            parameters = CONFIRMATION_PARAMETERS,
        ),
    )

    @Test
    fun `on result with Failed, should return NextStep result with no token for Saved option`() = runResultTest(
        confirmationOption = SAVED_CONFIRMATION_OPTION,
        definition = confirmationDefinition(),
        launcherResult = PassiveChallengeActivityResult.Failed(RuntimeException("Challenge failed")),
        parameters = CONFIRMATION_PARAMETERS,
        definitionResult = ConfirmationDefinition.Result.NextStep(
            confirmationOption = PaymentMethodConfirmationOption.Saved(
                paymentMethod = SAVED_CONFIRMATION_OPTION.paymentMethod,
                optionsParams = SAVED_CONFIRMATION_OPTION.optionsParams,
                originatedFromWallet = false,
                passiveCaptchaParams = null,
                hCaptchaToken = null
            ),
            parameters = CONFIRMATION_PARAMETERS,
        ),
    )

    private companion object {
        private val PAYMENT_METHOD = PaymentMethodFactory.card()

        private val PASSIVE_CAPTCHA_PARAMS = PassiveCaptchaParamsFactory.passiveCaptchaParams()

        private val NEW_CONFIRMATION_OPTION = PaymentMethodConfirmationOption.New(
            createParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
            optionsParams = null,
            extraParams = null,
            shouldSave = false,
            passiveCaptchaParams = PASSIVE_CAPTCHA_PARAMS,
        )

        private val SAVED_CONFIRMATION_OPTION = PaymentMethodConfirmationOption.Saved(
            paymentMethod = PAYMENT_METHOD,
            optionsParams = null,
            originatedFromWallet = false,
            passiveCaptchaParams = PASSIVE_CAPTCHA_PARAMS,
            hCaptchaToken = null,
        )
    }

    private fun confirmationDefinition(
        errorReporter: FakeErrorReporter = FakeErrorReporter(),
        passiveChallengeWarmer: PassiveChallengeWarmer = FakePassiveChallengeWarmer()
    ) = PassiveChallengeConfirmationDefinition(
        errorReporter = errorReporter,
        passiveChallengeWarmer = passiveChallengeWarmer,
        publishableKeyProvider = { "pk_123" },
        productUsage = setOf("PaymentSheet"),
    )
}
