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

internal class PassiveChallengeConfirmationDefinition @Inject constructor() : ConfirmationDefinition<
    PaymentMethodConfirmationOption.New,
    ActivityResultLauncher<PassiveChallengeActivityContract.Args>,
    Unit,
    PassiveChallengeActivityResult
    > {
    override val key = "ChallengePassive"

    override fun option(confirmationOption: ConfirmationHandler.Option): PaymentMethodConfirmationOption.New? {
        return confirmationOption as? PaymentMethodConfirmationOption.New
    }

    override fun canConfirm(
        confirmationOption: PaymentMethodConfirmationOption.New,
        confirmationParameters: ConfirmationDefinition.Parameters
    ): Boolean {
        return confirmationOption.passiveCaptchaParams != null
    }

    override fun toResult(
        confirmationOption: PaymentMethodConfirmationOption.New,
        confirmationParameters: ConfirmationDefinition.Parameters,
        deferredIntentConfirmationType: DeferredIntentConfirmationType?,
        result: PassiveChallengeActivityResult
    ): ConfirmationDefinition.Result {
        return ConfirmationDefinition.Result.NextStep(
            confirmationOption = confirmationOption.copy(
                passiveCaptchaParams = null
            ),
            parameters = confirmationParameters
        )
    }

    override fun createLauncher(
        activityResultCaller: ActivityResultCaller,
        onResult: (PassiveChallengeActivityResult) -> Unit
    ): ActivityResultLauncher<PassiveChallengeActivityContract.Args> {
        return activityResultCaller.registerForActivityResult(PassiveChallengeActivityContract(), onResult)
    }

    override fun launch(
        launcher: ActivityResultLauncher<PassiveChallengeActivityContract.Args>,
        arguments: Unit,
        confirmationOption: PaymentMethodConfirmationOption.New,
        confirmationParameters: ConfirmationDefinition.Parameters
    ) {
        requireNotNull(confirmationOption.passiveCaptchaParams)
        launcher.launch(PassiveChallengeActivityContract.Args(confirmationOption.passiveCaptchaParams))
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
