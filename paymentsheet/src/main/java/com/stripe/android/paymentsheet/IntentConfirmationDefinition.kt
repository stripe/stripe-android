package com.stripe.android.paymentsheet

import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.ConfirmStripeIntentParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.payments.paymentlauncher.InternalPaymentResult
import com.stripe.android.payments.paymentlauncher.PaymentLauncher
import com.stripe.android.payments.paymentlauncher.PaymentLauncherContract

internal class IntentConfirmationDefinition(
    private val intentConfirmationInterceptor: IntentConfirmationInterceptor,
    private val paymentLauncherFactory: (ActivityResultLauncher<PaymentLauncherContract.Args>) -> PaymentLauncher,
) : PaymentConfirmationDefinition<
    PaymentConfirmationOption.PaymentMethod,
    PaymentLauncher,
    IntentConfirmationDefinition.Args,
    InternalPaymentResult
    > {
    override val key: String = "IntentConfirmation"

    override fun option(confirmationOption: PaymentConfirmationOption): PaymentConfirmationOption.PaymentMethod? {
        return confirmationOption as? PaymentConfirmationOption.PaymentMethod
    }

    override suspend fun action(
        confirmationOption: PaymentConfirmationOption.PaymentMethod,
        intent: StripeIntent
    ): PaymentConfirmationDefinition.ConfirmationAction<Args> {
        val nextStep = intentConfirmationInterceptor.intercept(confirmationOption = confirmationOption)

        val deferredIntentConfirmationType = nextStep.deferredIntentConfirmationType

        return when (nextStep) {
            is IntentConfirmationInterceptor.NextStep.HandleNextAction -> {
                PaymentConfirmationDefinition.ConfirmationAction.Launch(
                    launcherArguments = Args.NextAction(nextStep.clientSecret),
                    deferredIntentConfirmationType = deferredIntentConfirmationType,
                )
            }
            is IntentConfirmationInterceptor.NextStep.Confirm -> {
                PaymentConfirmationDefinition.ConfirmationAction.Launch(
                    launcherArguments = Args.Confirm(nextStep.confirmParams),
                    deferredIntentConfirmationType = deferredIntentConfirmationType,
                )
            }
            is IntentConfirmationInterceptor.NextStep.Fail -> {
                PaymentConfirmationDefinition.ConfirmationAction.Fail(
                    cause = nextStep.cause,
                    message = nextStep.message,
                    errorType = PaymentConfirmationErrorType.Payment,
                )
            }
            is IntentConfirmationInterceptor.NextStep.Complete -> {
                PaymentConfirmationDefinition.ConfirmationAction.Complete(
                    intent = intent,
                    confirmationOption = confirmationOption,
                    deferredIntentConfirmationType = deferredIntentConfirmationType,
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
        confirmationOption: PaymentConfirmationOption.PaymentMethod,
        intent: StripeIntent,
    ) {
        when (arguments) {
            is Args.Confirm -> launchConfirm(launcher, arguments.confirmNextParams)
            is Args.NextAction -> launchNextAction(launcher, arguments.clientSecret, intent)
        }
    }

    override fun toPaymentConfirmationResult(
        confirmationOption: PaymentConfirmationOption.PaymentMethod,
        deferredIntentConfirmationType: DeferredIntentConfirmationType?,
        intent: StripeIntent,
        result: InternalPaymentResult
    ): PaymentConfirmationResult {
        return when (result) {
            is InternalPaymentResult.Completed -> PaymentConfirmationResult.Succeeded(
                intent = result.intent,
                deferredIntentConfirmationType = deferredIntentConfirmationType,
            )
            is InternalPaymentResult.Failed -> PaymentConfirmationResult.Failed(
                cause = result.throwable,
                message = result.throwable.stripeErrorMessage(),
                type = PaymentConfirmationErrorType.Payment,
            )
            is InternalPaymentResult.Canceled -> PaymentConfirmationResult.Canceled(
                action = PaymentCancellationAction.InformCancellation
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
