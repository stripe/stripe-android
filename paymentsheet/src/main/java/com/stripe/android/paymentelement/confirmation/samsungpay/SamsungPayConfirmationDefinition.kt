package com.stripe.android.paymentelement.confirmation.samsungpay

import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.PaymentIntent
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.EmptyConfirmationLauncherArgs
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.payments.samsungpay.DefaultGetSamsungPayStatus
import com.stripe.android.payments.samsungpay.SamsungPayPaymentMethodLauncher
import com.stripe.android.payments.samsungpay.SamsungPayPaymentMethodLauncherContract
import com.stripe.android.paymentsheet.R
import javax.inject.Inject
import com.stripe.android.payments.samsungpay.Config as SamsungPayConfig

internal class SamsungPayConfirmationDefinition @Inject constructor() : ConfirmationDefinition<
    SamsungPayConfirmationOption,
    ActivityResultLauncher<SamsungPayPaymentMethodLauncherContract.Args>,
    EmptyConfirmationLauncherArgs,
    SamsungPayPaymentMethodLauncher.Result,
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
        onResult: (SamsungPayPaymentMethodLauncher.Result) -> Unit,
    ): ActivityResultLauncher<SamsungPayPaymentMethodLauncherContract.Args> {
        return activityResultCaller.registerForActivityResult(
            SamsungPayPaymentMethodLauncherContract(),
            onResult,
        )
    }

    override fun launch(
        launcher: ActivityResultLauncher<SamsungPayPaymentMethodLauncherContract.Args>,
        arguments: EmptyConfirmationLauncherArgs,
        confirmationOption: SamsungPayConfirmationOption,
        confirmationArgs: ConfirmationHandler.Args,
    ) {
        val intent = confirmationArgs.intent
        val amount = (intent as? PaymentIntent)?.amount ?: 0L
        val currencyCode = (intent as? PaymentIntent)?.currency ?: "USD"

        launcher.launch(
            SamsungPayPaymentMethodLauncherContract.Args(
                config = SamsungPayConfig(
                    serviceId = DefaultGetSamsungPayStatus.DEFAULT_SERVICE_ID,
                    merchantId = confirmationArgs.paymentMethodMetadata.merchantName,
                    merchantName = confirmationArgs.paymentMethodMetadata.merchantName,
                ),
                currencyCode = currencyCode,
                amount = amount,
                orderNumber = intent.id.orEmpty(),
            )
        )
    }

    override fun toResult(
        confirmationOption: SamsungPayConfirmationOption,
        confirmationArgs: ConfirmationHandler.Args,
        launcherArgs: EmptyConfirmationLauncherArgs,
        result: SamsungPayPaymentMethodLauncher.Result,
    ): ConfirmationDefinition.Result {
        return when (result) {
            is SamsungPayPaymentMethodLauncher.Result.Completed -> {
                ConfirmationDefinition.Result.NextStep(
                    confirmationOption = PaymentMethodConfirmationOption.Saved(
                        paymentMethod = result.paymentMethod,
                        optionsParams = null,
                        originatedFromWallet = true,
                    ),
                    arguments = confirmationArgs,
                )
            }
            is SamsungPayPaymentMethodLauncher.Result.Canceled -> {
                ConfirmationDefinition.Result.Canceled(
                    action = ConfirmationHandler.Result.Canceled.Action.InformCancellation,
                )
            }
            is SamsungPayPaymentMethodLauncher.Result.Failed -> {
                ConfirmationDefinition.Result.Failed(
                    cause = result.error,
                    message = R.string.stripe_something_went_wrong.resolvableString,
                    type = ConfirmationHandler.Result.Failed.ErrorType.Payment,
                )
            }
        }
    }
}
