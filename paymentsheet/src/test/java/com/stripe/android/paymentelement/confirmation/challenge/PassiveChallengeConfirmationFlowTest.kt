package com.stripe.android.paymentelement.confirmation.challenge

import com.stripe.android.challenge.passive.PassiveChallengeActivityContract
import com.stripe.android.challenge.passive.PassiveChallengeActivityResult
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures.DEFAULT_API_CONFIG
import com.stripe.android.model.PassiveCaptchaParams
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.paymentelement.confirmation.CONFIRMATION_PARAMETERS
import com.stripe.android.paymentelement.confirmation.ConfirmationChallengeState
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.FakeIsEligibleForConfirmationChallenge
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.paymentelement.confirmation.runLaunchTest
import com.stripe.android.paymentelement.confirmation.runResultTest
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.android.testing.RadarOptionsFactory
import com.stripe.android.utils.FakePassiveChallengeWarmer
import org.junit.Test

internal class PassiveChallengeConfirmationFlowTest {
    @Test
    fun `on launch with New option, should persist parameters & launch using launcher as expected`() = runScenario {
        runLaunchTest(
            confirmationOption = confirmationOption,
            parameters = CONFIRMATION_PARAMETERS,
            definition = definition,
        )
    }

    @Test
    fun `on result with Success, should return NextStep result with token attached for New option`() = runScenario {
        runResultTest(
            confirmationOption = confirmationOption,
            definition = definition,
            launcherResult = PassiveChallengeActivityResult.Success("test_token"),
            parameters = CONFIRMATION_PARAMETERS,
            launcherArgs = launcherArgs,
            definitionResult = ConfirmationDefinition.Result.NextStep(
                confirmationOption = confirmationOption.copy(
                    createParams = confirmationOption.createParams.copy(
                        radarOptions = RadarOptionsFactory.create(verificationObject = null)
                    ),
                    confirmationChallengeState = ConfirmationChallengeState(passiveChallengeComplete = true),
                ),
                arguments = CONFIRMATION_PARAMETERS,
            ),
        )
    }

    @Test
    fun `on result with Failed, should return NextStep result with no token for New option`() = runScenario {
        runResultTest(
            confirmationOption = confirmationOption,
            definition = definition,
            launcherResult = PassiveChallengeActivityResult.Failed(RuntimeException("Challenge failed")),
            parameters = CONFIRMATION_PARAMETERS,
            launcherArgs = launcherArgs,
            definitionResult = ConfirmationDefinition.Result.NextStep(
                confirmationOption = confirmationOption.copy(
                    confirmationChallengeState = ConfirmationChallengeState(passiveChallengeComplete = true),
                ),
                arguments = CONFIRMATION_PARAMETERS,
            ),
        )
    }

    private fun runScenario(block: Scenario.() -> Unit) {
        val passiveChallengeWarmer = FakePassiveChallengeWarmer()
        val errorReporter = FakeErrorReporter()
        val definition = PassiveChallengeConfirmationDefinition(
            errorReporter = errorReporter,
            passiveChallengeWarmer = passiveChallengeWarmer,
            productUsage = setOf("PaymentSheet"),
            isEligibleForConfirmationChallenge = FakeIsEligibleForConfirmationChallenge(),
        )
        val confirmationOption = PaymentMethodConfirmationOption.New(
            createParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
            optionsParams = null,
            extraParams = null,
            shouldSave = false,
        )
        val launcherArgs = PassiveChallengeActivityContract.Args(
            passiveCaptchaParams = PassiveCaptchaParams(
                siteKey = "site_key",
                rqData = null,
                tokenTimeoutSeconds = null,
            ),
            apiConfiguration = DEFAULT_API_CONFIG,
            productUsage = setOf("PaymentSheet"),
        )
        try {
            Scenario(definition, confirmationOption, launcherArgs).block()
        } finally {
            passiveChallengeWarmer.registerCalls.takeItem()
            passiveChallengeWarmer.ensureAllEventsConsumed()
            errorReporter.ensureAllEventsConsumed()
        }
    }

    private data class Scenario(
        val definition: PassiveChallengeConfirmationDefinition,
        val confirmationOption: PaymentMethodConfirmationOption.New,
        val launcherArgs: PassiveChallengeActivityContract.Args,
    )
}
