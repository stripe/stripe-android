package com.stripe.android.paymentelement.confirmation.shoppay

import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.EmptyConfirmationLauncherArgs
import com.stripe.android.shoppay.ShopPayActivityContract
import com.stripe.android.shoppay.ShopPayActivityResult
import javax.inject.Inject

internal class ShopPayConfirmationDefinition @Inject constructor(
    private val shopPayActivityContract: ShopPayActivityContract
) : ConfirmationDefinition<
    ShopPayConfirmationOption,
    ActivityResultLauncher<ShopPayActivityContract.Args>,
    EmptyConfirmationLauncherArgs,
    ShopPayActivityResult
    > {
    override val key = "ShopPay"

    override fun option(confirmationOption: ConfirmationHandler.Option): ShopPayConfirmationOption? {
        return confirmationOption as? ShopPayConfirmationOption
    }

    override fun toResult(
        confirmationOption: ShopPayConfirmationOption,
        confirmationArgs: ConfirmationHandler.Args,
        launcherArgs: EmptyConfirmationLauncherArgs,
        result: ShopPayActivityResult
    ): ConfirmationDefinition.Result {
        return when (result) {
            ShopPayActivityResult.Canceled -> {
                ConfirmationDefinition.Result.Canceled(
                    action = ConfirmationHandler.Result.Canceled.Action.None
                )
            }
            is ShopPayActivityResult.Completed -> {
                ConfirmationDefinition.Result.Succeeded(
                    intent = confirmationArgs.intent,
                    // Shop Pay is handed off for `preparePaymentMethod` purposes
                    completedFullPaymentFlow = false,
                )
            }
            is ShopPayActivityResult.Failed -> {
                ConfirmationDefinition.Result.Failed(
                    cause = result.error,
                    message = result.error.message.orEmpty().resolvableString,
                    type = ConfirmationHandler.Result.Failed.ErrorType.Payment,
                )
            }
        }
    }

    override fun createLauncher(
        activityResultCaller: ActivityResultCaller,
        onResult: (ShopPayActivityResult) -> Unit
    ): ActivityResultLauncher<ShopPayActivityContract.Args> {
        return activityResultCaller.registerForActivityResult(
            shopPayActivityContract,
            onResult
        )
    }

    override fun launch(
        launcher: ActivityResultLauncher<ShopPayActivityContract.Args>,
        arguments: EmptyConfirmationLauncherArgs,
        confirmationOption: ShopPayConfirmationOption,
        confirmationArgs: ConfirmationHandler.Args
    ) {
        launcher.launch(
            ShopPayActivityContract.Args(
                paymentMethodMetadata = confirmationArgs.paymentMethodMetadata,
                shopPayConfiguration = confirmationOption.shopPayConfiguration,
                customerSessionClientSecret = confirmationOption.customerSessionClientSecret,
                businessName = confirmationArgs.paymentMethodMetadata.sellerBusinessName
                    ?: confirmationOption.merchantDisplayName
            )
        )
    }

    override suspend fun action(
        confirmationOption: ShopPayConfirmationOption,
        confirmationArgs: ConfirmationHandler.Args
    ): ConfirmationDefinition.Action<EmptyConfirmationLauncherArgs> {
        return ConfirmationDefinition.Action.Launch(
            launcherArguments = EmptyConfirmationLauncherArgs,
            receivesResultInProcess = false,
        )
    }
}
