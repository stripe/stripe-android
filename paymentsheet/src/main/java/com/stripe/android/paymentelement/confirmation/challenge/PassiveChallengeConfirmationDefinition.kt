package com.stripe.android.paymentelement.confirmation.challenge

import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import com.stripe.android.challenge.passive.PassiveChallengeActivityContract
import com.stripe.android.challenge.passive.PassiveChallengeActivityResult
import com.stripe.android.challenge.passive.warmer.PassiveChallengeWarmer
import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.RadarOptions
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.paymentelement.confirmation.intent.DeferredIntentConfirmationType
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import javax.inject.Inject
import javax.inject.Named

internal class PassiveChallengeConfirmationDefinition @Inject constructor(
    private val errorReporter: ErrorReporter,
    private val passiveChallengeWarmer: PassiveChallengeWarmer,
    @Named(PUBLISHABLE_KEY) private val publishableKeyProvider: () -> String,
    @Named(PRODUCT_USAGE) private val productUsage: Set<String>
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

    override fun bootstrap(paymentMethodMetadata: PaymentMethodMetadata) {
        val passiveCaptchaParams = paymentMethodMetadata.passiveCaptchaParams ?: return
        passiveChallengeWarmer.start(
            passiveCaptchaParams = passiveCaptchaParams,
            publishableKey = publishableKeyProvider(),
            productUsage = productUsage
        )
    }

    override fun canConfirm(
        confirmationOption: PaymentMethodConfirmationOption.New,
        confirmationArgs: ConfirmationHandler.Args
    ): Boolean {
        return confirmationArgs.paymentMethodMetadata.passiveCaptchaParams != null &&
            confirmationOption.createParams.typeCode == "card" &&
            !confirmationOption.passiveChallengeComplete
    }

    override fun toResult(
        confirmationOption: PaymentMethodConfirmationOption.New,
        confirmationArgs: ConfirmationHandler.Args,
        deferredIntentConfirmationType: DeferredIntentConfirmationType?,
        result: PassiveChallengeActivityResult
    ): ConfirmationDefinition.Result {
        return when (result) {
            is PassiveChallengeActivityResult.Failed -> {
                ConfirmationDefinition.Result.NextStep(
                    confirmationOption = confirmationOption.attachToken(null),
                    arguments = confirmationArgs
                )
            }
            is PassiveChallengeActivityResult.Success -> {
                ConfirmationDefinition.Result.NextStep(
                    confirmationOption = confirmationOption.attachToken(result.token),
                    arguments = confirmationArgs
                )
            }
        }
    }

    override fun createLauncher(
        activityResultCaller: ActivityResultCaller,
        onResult: (PassiveChallengeActivityResult) -> Unit
    ): ActivityResultLauncher<PassiveChallengeActivityContract.Args> {
        passiveChallengeWarmer.register(activityResultCaller)
        return activityResultCaller.registerForActivityResult(PassiveChallengeActivityContract(), onResult)
    }

    override fun launch(
        launcher: ActivityResultLauncher<PassiveChallengeActivityContract.Args>,
        arguments: PassiveChallengeActivityContract.Args,
        confirmationOption: PaymentMethodConfirmationOption.New,
        confirmationArgs: ConfirmationHandler.Args
    ) {
        launcher.launch(arguments)
    }

    override suspend fun action(
        confirmationOption: PaymentMethodConfirmationOption.New,
        confirmationArgs: ConfirmationHandler.Args
    ): ConfirmationDefinition.Action<PassiveChallengeActivityContract.Args> {
        val passiveCaptchaParams = confirmationArgs.paymentMethodMetadata.passiveCaptchaParams
        if (passiveCaptchaParams != null) {
            return ConfirmationDefinition.Action.Launch(
                launcherArguments = PassiveChallengeActivityContract.Args(
                    passiveCaptchaParams,
                    publishableKey = publishableKeyProvider(),
                    productUsage = productUsage
                ),
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

    private fun PaymentMethodConfirmationOption.New.attachToken(token: String?): PaymentMethodConfirmationOption {
        val radarOptions = if (token != null) {
            createParams.radarOptions?.copy(
                hCaptchaToken = token
            ) ?: RadarOptions(
                hCaptchaToken = token,
                androidVerificationObject = null
            )
        } else {
            createParams.radarOptions
        }
        return copy(
            createParams = createParams.copy(
                radarOptions = radarOptions
            ),
            passiveChallengeComplete = true
        )
    }

    override fun unregister(launcher: ActivityResultLauncher<PassiveChallengeActivityContract.Args>) {
        passiveChallengeWarmer.unregister()
        super.unregister(launcher)
    }
}
