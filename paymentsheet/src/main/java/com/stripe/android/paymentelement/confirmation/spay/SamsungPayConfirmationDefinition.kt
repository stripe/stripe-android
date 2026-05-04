package com.stripe.android.paymentelement.confirmation.spay

import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.EmptyConfirmationLauncherArgs
import com.stripe.android.payments.samsungpay.SamsungPayLauncher
import com.stripe.android.payments.samsungpay.SamsungPayLauncherContract
import com.stripe.android.payments.samsungpay.SamsungPayTokenRequest
import com.stripe.android.payments.samsungpay.TokenExchangeHandler
import com.stripe.android.paymentsheet.R
import javax.inject.Inject

internal class SamsungPayConfirmationDefinition @Inject constructor(
) : ConfirmationDefinition<
    SamsungPayConfirmationOption,
    ActivityResultLauncher<SamsungPayLauncherContract.Args>,
    EmptyConfirmationLauncherArgs,
    SamsungPayLauncher.Result,
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
        onResult: (SamsungPayLauncher.Result) -> Unit,
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
        val config = confirmationOption.config
        val clientSecret = confirmationArgs.intent.clientSecret ?: return

        // Store stub token exchange handler for the Activity to pick up
        SamsungPayLauncher.tokenExchangeHandlerHolder = STUB_TOKEN_EXCHANGE_HANDLER

        val args = if (clientSecret.startsWith("seti_")) {
            SamsungPayLauncherContract.Args.SetupIntentArgs(
                clientSecret = clientSecret,
                config = config,
                currencyCode = "USD",
            )
        } else {
            SamsungPayLauncherContract.Args.PaymentIntentArgs(
                clientSecret = clientSecret,
                config = config,
            )
        }

        launcher.launch(args)
    }

    override fun toResult(
        confirmationOption: SamsungPayConfirmationOption,
        confirmationArgs: ConfirmationHandler.Args,
        launcherArgs: EmptyConfirmationLauncherArgs,
        result: SamsungPayLauncher.Result,
    ): ConfirmationDefinition.Result {
        return when (result) {
            is SamsungPayLauncher.Result.Completed -> {
                ConfirmationDefinition.Result.Succeeded(
                    intent = confirmationArgs.intent,
                )
            }
            is SamsungPayLauncher.Result.Failed -> {
                ConfirmationDefinition.Result.Failed(
                    cause = result.error,
                    message = R.string.stripe_something_went_wrong.resolvableString,
                    type = ConfirmationHandler.Result.Failed.ErrorType.Payment,
                )
            }
            is SamsungPayLauncher.Result.Canceled -> {
                ConfirmationDefinition.Result.Canceled(
                    action = ConfirmationHandler.Result.Canceled.Action.InformCancellation,
                )
            }
        }
    }

    companion object {
        /**
         * Stub token exchange handler for testing the flow.
         * In production, this would be provided by the merchant.
         */
        private val STUB_TOKEN_EXCHANGE_HANDLER = TokenExchangeHandler { _ ->
            "tok_stub_samsung_pay"
        }
    }
}
