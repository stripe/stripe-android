package com.stripe.android.paymentelement.confirmation.challenge

import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import com.stripe.android.challenge.PassiveChallengeActivityContract
import com.stripe.android.challenge.PassiveChallengeActivityResult
import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.paymentelement.confirmation.intent.DeferredIntentConfirmationType
import com.stripe.android.payments.core.analytics.ErrorReporter
import javax.inject.Inject

internal class PassiveChallengeConfirmationDefinition @Inject constructor(
    private val errorReporter: ErrorReporter,
) : ConfirmationDefinition<
    PaymentMethodConfirmationOption.New,
    ActivityResultLauncher<PassiveChallengeActivityContract.Args>,
    PassiveChallengeActivityContract.Args,
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
        arguments: PassiveChallengeActivityContract.Args,
        confirmationOption: PaymentMethodConfirmationOption.New,
        confirmationParameters: ConfirmationDefinition.Parameters
    ) {
        launcher.launch(arguments)
    }

    override suspend fun action(
        confirmationOption: PaymentMethodConfirmationOption.New,
        confirmationParameters: ConfirmationDefinition.Parameters
    ): ConfirmationDefinition.Action<PassiveChallengeActivityContract.Args> {
        if (confirmationOption.passiveCaptchaParams != null) {
            return ConfirmationDefinition.Action.Launch(
                launcherArguments = PassiveChallengeActivityContract.Args(confirmationOption.passiveCaptchaParams),
                receivesResultInProcess = false,
                deferredIntentConfirmationType = null,
            )
        }

        val error = IllegalArgumentException("Passive challenge params are null")
        errorReporter.report(
            ErrorReporter.UnexpectedErrorEvent.INTENT_CONFIRMATION_HANDLER_PASSIVE_CHALLENGE_PARAMS_NULL,
            stripeException = StripeException.create(error)
        )

        return ConfirmationDefinition.Action.Fail(
            cause = error,
            message = "Passive challenge params are null".resolvableString,
            errorType = ConfirmationHandler.Result.Failed.ErrorType.Internal
        )
    }
}
