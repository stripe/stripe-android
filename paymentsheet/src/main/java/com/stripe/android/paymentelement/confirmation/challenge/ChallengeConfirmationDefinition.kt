package com.stripe.android.paymentelement.confirmation.challenge

import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import com.stripe.android.challenge.PassiveChallengeActivityContract
import com.stripe.android.challenge.PassiveChallengeActivityResult
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.paymentelement.confirmation.intent.DeferredIntentConfirmationType
import javax.inject.Inject

internal class ChallengeConfirmationDefinition @Inject constructor(
    private val passiveChallengeActivityContract: PassiveChallengeActivityContract
): ConfirmationDefinition<
    PaymentMethodConfirmationOption.New,
    ActivityResultLauncher<PassiveChallengeActivityContract.Args>,
    Unit,
    PassiveChallengeActivityResult
    > {
    override val key: String = "Challenge"

    override fun canConfirm(
        confirmationOption: PaymentMethodConfirmationOption.New,
        confirmationParameters: ConfirmationDefinition.Parameters
    ): Boolean {
        return confirmationOption.createParams.radarOptions == null
    }

    override fun option(confirmationOption: ConfirmationHandler.Option): PaymentMethodConfirmationOption.New? {
        return confirmationOption as? PaymentMethodConfirmationOption.New
    }

    override fun toResult(
        confirmationOption: PaymentMethodConfirmationOption.New,
        confirmationParameters: ConfirmationDefinition.Parameters,
        deferredIntentConfirmationType: DeferredIntentConfirmationType?,
        result: PassiveChallengeActivityResult
    ): ConfirmationDefinition.Result {
        return when (result) {
            is PassiveChallengeActivityResult.Failed -> {
                ConfirmationDefinition.Result.NextStep(
                    confirmationOption = confirmationOption,
                    parameters = confirmationParameters
                )
            }
            is PassiveChallengeActivityResult.Success -> {
                ConfirmationDefinition.Result.NextStep(
                    confirmationOption = result.newPaymentMethodConfirmationOption,
                    parameters = confirmationParameters
                )
            }
        }
    }

    override fun createLauncher(
        activityResultCaller: ActivityResultCaller,
        onResult: (PassiveChallengeActivityResult) -> Unit
    ): ActivityResultLauncher<PassiveChallengeActivityContract.Args> {
        return activityResultCaller.registerForActivityResult(passiveChallengeActivityContract, onResult)
    }

    override fun launch(
        launcher: ActivityResultLauncher<PassiveChallengeActivityContract.Args>,
        arguments: Unit,
        confirmationOption: PaymentMethodConfirmationOption.New,
        confirmationParameters: ConfirmationDefinition.Parameters
    ) {
        launcher.launch(
            input = PassiveChallengeActivityContract.Args(confirmationOption)
        )
    }

    override suspend fun action(
        confirmationOption: PaymentMethodConfirmationOption.New,
        confirmationParameters: ConfirmationDefinition.Parameters
    ): ConfirmationDefinition.Action<Unit> {
        return ConfirmationDefinition.Action.Launch(
            launcherArguments = Unit,
            receivesResultInProcess = false,
            deferredIntentConfirmationType = null,
        )
    }
}