package com.stripe.android.paymentelement.confirmation.challenge

import com.stripe.android.challenge.passive.PassiveChallengeActivityContract
import com.stripe.android.challenge.passive.PassiveChallengeActivityResult
import com.stripe.android.challenge.passive.warmer.PassiveChallengeWarmer
import com.stripe.android.model.PassiveCaptchaParams
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.paymentelement.confirmation.CONFIRMATION_PARAMETERS
import com.stripe.android.paymentelement.confirmation.ConfirmationChallengeState
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.FakeIsEligibleForConfirmationChallenge
import com.stripe.android.paymentelement.confirmation.IsEligibleForConfirmationChallenge
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.paymentelement.confirmation.runLaunchTest
import com.stripe.android.paymentelement.confirmation.runResultTest
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.android.testing.RadarOptionsFactory
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
    fun `on result with Success, should return NextStep result with token attached for New option`() = runResultTest(
        confirmationOption = NEW_CONFIRMATION_OPTION,
        definition = confirmationDefinition(),
        launcherResult = PassiveChallengeActivityResult.Success("test_token"),
        parameters = CONFIRMATION_PARAMETERS,
        launcherArgs = LAUNCHER_ARGS,
        definitionResult = ConfirmationDefinition.Result.NextStep(
            confirmationOption = PaymentMethodConfirmationOption.New(
                createParams = NEW_CONFIRMATION_OPTION.createParams.copy(
                    radarOptions = RadarOptionsFactory.create(
                        verificationObject = null
                    )
                ),
                optionsParams = NEW_CONFIRMATION_OPTION.optionsParams,
                shouldSave = false,
                extraParams = null,
                confirmationChallengeState = ConfirmationChallengeState(passiveChallengeComplete = true),
            ),
            arguments = CONFIRMATION_PARAMETERS,
        ),
    )

    @Test
    fun `on result with Failed, should return NextStep result with no token for New option`() = runResultTest(
        confirmationOption = NEW_CONFIRMATION_OPTION,
        definition = confirmationDefinition(),
        launcherResult = PassiveChallengeActivityResult.Failed(RuntimeException("Challenge failed")),
        parameters = CONFIRMATION_PARAMETERS,
        launcherArgs = LAUNCHER_ARGS,
        definitionResult = ConfirmationDefinition.Result.NextStep(
            confirmationOption = PaymentMethodConfirmationOption.New(
                createParams = NEW_CONFIRMATION_OPTION.createParams,
                optionsParams = NEW_CONFIRMATION_OPTION.optionsParams,
                shouldSave = false,
                extraParams = null,
                confirmationChallengeState = ConfirmationChallengeState(passiveChallengeComplete = true),
            ),
            arguments = CONFIRMATION_PARAMETERS,
        ),
    )

    private companion object {
        private val NEW_CONFIRMATION_OPTION = PaymentMethodConfirmationOption.New(
            createParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
            optionsParams = null,
            extraParams = null,
            shouldSave = false,
        )

        val LAUNCHER_ARGS = PassiveChallengeActivityContract.Args(
            passiveCaptchaParams = PassiveCaptchaParams(
                siteKey = "site_key",
                rqData = null,
                tokenTimeoutSeconds = null
            ),
            publishableKey = "pk_123",
            productUsage = setOf("PaymentSheet")
        )
    }

    private fun confirmationDefinition(
        errorReporter: FakeErrorReporter = FakeErrorReporter(),
        passiveChallengeWarmer: PassiveChallengeWarmer = FakePassiveChallengeWarmer(),
        isEligibleForConfirmationChallenge: IsEligibleForConfirmationChallenge =
            FakeIsEligibleForConfirmationChallenge()
    ) = PassiveChallengeConfirmationDefinition(
        errorReporter = errorReporter,
        passiveChallengeWarmer = passiveChallengeWarmer,
        publishableKeyProvider = { "pk_123" },
        productUsage = setOf("PaymentSheet"),
        isEligibleForConfirmationChallenge = isEligibleForConfirmationChallenge,
    )
}
