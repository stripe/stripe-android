package com.stripe.android.paymentelement.confirmation.intent

import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.ConfirmStripeIntentParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.payments.paymentlauncher.InternalPaymentResult
import com.stripe.android.payments.paymentlauncher.PaymentLauncher
import com.stripe.android.payments.paymentlauncher.PaymentLauncherContract

internal class IntentConfirmationDefinition(
    private val intentConfirmationInterceptor: IntentConfirmationInterceptor,
    private val paymentLauncherFactory: (ActivityResultLauncher<PaymentLauncherContract.Args>) -> PaymentLauncher,
) : ConfirmationDefinition<
    PaymentMethodConfirmationOption,
    PaymentLauncher,
    IntentConfirmationDefinition.Args,
    InternalPaymentResult
    > {
    override val key: String = "IntentConfirmation"

    override fun option(confirmationOption: ConfirmationHandler.Option): PaymentMethodConfirmationOption? {
        return confirmationOption as? PaymentMethodConfirmationOption
    }

    override suspend fun action(
        confirmationOption: PaymentMethodConfirmationOption,
        confirmationParameters: ConfirmationDefinition.Parameters,
    ): ConfirmationDefinition.Action<Args> {
        val nextStep = intentConfirmationInterceptor.intercept(
            confirmationOption = confirmationOption,
            intent = confirmationParameters.intent,
            initializationMode = confirmationParameters.initializationMode,
            shippingDetails = confirmationParameters.shippingDetails,
        )

        val deferredIntentConfirmationType = nextStep.deferredIntentConfirmationType

        return when (nextStep) {
            is IntentConfirmationInterceptor.NextStep.HandleNextAction -> {
                ConfirmationDefinition.Action.Launch(
                    launcherArguments = Args.NextAction(nextStep.clientSecret),
                    deferredIntentConfirmationType = deferredIntentConfirmationType,
                    receivesResultInProcess = false,
                )
            }
            is IntentConfirmationInterceptor.NextStep.Confirm -> {
                ConfirmationDefinition.Action.Launch(
                    launcherArguments = Args.Confirm(nextStep.confirmParams),
                    deferredIntentConfirmationType = deferredIntentConfirmationType,
                    receivesResultInProcess = false,
                )
            }
            is IntentConfirmationInterceptor.NextStep.Fail -> {
                ConfirmationDefinition.Action.Fail(
                    cause = nextStep.cause,
                    message = nextStep.message,
                    errorType = ConfirmationHandler.Result.Failed.ErrorType.Payment,
                )
            }
            is IntentConfirmationInterceptor.NextStep.Complete -> {
                ConfirmationDefinition.Action.Complete(
                    intent = confirmationParameters.intent,
                    confirmationOption = confirmationOption,
                    deferredIntentConfirmationType = deferredIntentConfirmationType,
                    completedFullPaymentFlow = nextStep.completedFullPaymentFlow,
                )
            }
        }
    }

    override fun createLauncher(
        activityResultCaller: ActivityResultCaller,
        onResult: (InternalPaymentResult) -> Unit
    ): PaymentLauncher {
        return paymentLauncherFactory(
            activityResultCaller.registerForActivityResult(
                PaymentLauncherContract(),
                onResult
            )
        )
    }

    override fun launch(
        launcher: PaymentLauncher,
        arguments: Args,
        confirmationOption: PaymentMethodConfirmationOption,
        confirmationParameters: ConfirmationDefinition.Parameters,
    ) {
        when (arguments) {
            is Args.Confirm -> launchConfirm(launcher, arguments.confirmNextParams)
            is Args.NextAction -> launchNextAction(launcher, arguments.clientSecret, confirmationParameters.intent)
        }
    }

    override fun toResult(
        confirmationOption: PaymentMethodConfirmationOption,
        confirmationParameters: ConfirmationDefinition.Parameters,
        deferredIntentConfirmationType: DeferredIntentConfirmationType?,
        result: InternalPaymentResult
    ): ConfirmationDefinition.Result {
        return when (result) {
            is InternalPaymentResult.Completed -> ConfirmationDefinition.Result.Succeeded(
                intent = result.intent,
                deferredIntentConfirmationType = deferredIntentConfirmationType,
            )
            is InternalPaymentResult.Failed -> ConfirmationDefinition.Result.Failed(
                cause = result.throwable,
                message = result.throwable.stripeErrorMessage(),
                type = ConfirmationHandler.Result.Failed.ErrorType.Payment,
            )
            is InternalPaymentResult.Canceled -> ConfirmationDefinition.Result.Canceled(
                action = ConfirmationHandler.Result.Canceled.Action.InformCancellation,
            )
        }
    }

    private fun launchNextAction(
        launcher: PaymentLauncher,
        clientSecret: String,
        intent: StripeIntent,
    ) {
        when (intent) {
            is PaymentIntent -> {
                launcher.handleNextActionForPaymentIntent(clientSecret)
            }
            is SetupIntent -> {
                launcher.handleNextActionForSetupIntent(clientSecret)
            }
        }
    }

    private fun launchConfirm(
        launcher: PaymentLauncher,
        confirmStripeIntentParams: ConfirmStripeIntentParams
    ) {
        when (confirmStripeIntentParams) {
            is ConfirmPaymentIntentParams -> {
                launcher.confirm(confirmStripeIntentParams)
            }
            is ConfirmSetupIntentParams -> {
                launcher.confirm(confirmStripeIntentParams)
            }
        }
    }

    sealed interface Args {
        data class NextAction(val clientSecret: String) : Args

        data class Confirm(val confirmNextParams: ConfirmStripeIntentParams) : Args
    }
}
