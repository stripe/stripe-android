package com.stripe.android.paymentelement.confirmation.samsungpay

import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.PaymentIntent
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.EmptyConfirmationLauncherArgs
import com.stripe.android.payments.samsungpay.Config
import com.stripe.android.payments.samsungpay.SamsungPayLauncherContract
import com.stripe.android.payments.samsungpay.SamsungPayResult
import com.stripe.android.paymentsheet.R
import javax.inject.Inject

internal class SamsungPayConfirmationDefinition @Inject constructor() : ConfirmationDefinition<
    SamsungPayConfirmationOption,
    ActivityResultLauncher<SamsungPayLauncherContract.Args>,
    EmptyConfirmationLauncherArgs,
    SamsungPayResult,
    > {
    override val key: String = "SamsungPay"

    override fun option(confirmationOption: ConfirmationHandler.Option): SamsungPayConfirmationOption? {
        return confirmationOption as? SamsungPayConfirmationOption
    }

    override suspend fun action(
        confirmationOption: SamsungPayConfirmationOption,
        confirmationArgs: ConfirmationHandler.Args,
    ): ConfirmationDefinition.Action<EmptyConfirmationLauncherArgs> {
        return ConfirmationDefinition.Action.Launch(
            launcherArguments = EmptyConfirmationLauncherArgs,
            receivesResultInProcess = true,
        )
    }

    override fun createLauncher(
        activityResultCaller: ActivityResultCaller,
        onResult: (SamsungPayResult) -> Unit,
    ): ActivityResultLauncher<SamsungPayLauncherContract.Args> {
        return activityResultCaller.registerForActivityResult(
            SamsungPayLauncherContract(),
            onResult,
        )
    }

    override fun launch(
        launcher: ActivityResultLauncher<SamsungPayLauncherContract.Args>,
        arguments: EmptyConfirmationLauncherArgs,
        confirmationOption: SamsungPayConfirmationOption,
        confirmationArgs: ConfirmationHandler.Args,
    ) {
        val intent = confirmationArgs.intent
        val amount = (intent as? PaymentIntent)?.amount ?: 0L

        launcher.launch(
            SamsungPayLauncherContract.Args(
                clientSecret = intent.clientSecret.orEmpty(),
                config = Config(amount = amount),
            )
        )
    }

    override fun toResult(
        confirmationOption: SamsungPayConfirmationOption,
        confirmationArgs: ConfirmationHandler.Args,
        launcherArgs: EmptyConfirmationLauncherArgs,
        result: SamsungPayResult,
    ): ConfirmationDefinition.Result {
        return when (result) {
            is SamsungPayResult.Success -> {
                ConfirmationDefinition.Result.Succeeded(
                    intent = confirmationArgs.intent,
                )
            }
            is SamsungPayResult.Cancel -> {
                ConfirmationDefinition.Result.Canceled(
                    action = ConfirmationHandler.Result.Canceled.Action.InformCancellation,
                )
            }
            is SamsungPayResult.Failure -> {
                ConfirmationDefinition.Result.Failed(
                    cause = result.error,
                    message = R.string.stripe_something_went_wrong.resolvableString,
                    type = ConfirmationHandler.Result.Failed.ErrorType.Payment,
                )
            }
        }
    }
}
