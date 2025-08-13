package com.stripe.android.paymentelement.confirmation.shoppay

import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import com.stripe.android.SharedPaymentTokenSessionPreview
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.intent.DeferredIntentConfirmationType
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.shoppay.ShopPayActivityContract
import com.stripe.android.shoppay.ShopPayActivityResult
import javax.inject.Inject

internal class ShopPayConfirmationDefinition @Inject constructor(
    private val shopPayActivityContract: ShopPayActivityContract
) : ConfirmationDefinition<
    ShopPayConfirmationOption,
    ActivityResultLauncher<ShopPayActivityContract.Args>,
    Unit,
    ShopPayActivityResult
    > {
    override val key = "ShopPay"

    override fun option(confirmationOption: ConfirmationHandler.Option): ShopPayConfirmationOption? {
        return confirmationOption as? ShopPayConfirmationOption
    }

    override fun toResult(
        confirmationOption: ShopPayConfirmationOption,
        confirmationParameters: ConfirmationDefinition.Parameters,
        deferredIntentConfirmationType: DeferredIntentConfirmationType?,
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
                    intent = confirmationParameters.intent,
                    deferredIntentConfirmationType = deferredIntentConfirmationType,
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
        arguments: Unit,
        confirmationOption: ShopPayConfirmationOption,
        confirmationParameters: ConfirmationDefinition.Parameters
    ) {
        launcher.launch(
            ShopPayActivityContract.Args(
                shopPayConfiguration = confirmationOption.shopPayConfiguration,
                customerSessionClientSecret = confirmationOption.customerSessionClientSecret,
                businessName = merchantName(
                    confirmationOption = confirmationOption,
                    confirmationParameters = confirmationParameters,
                )
            )
        )
    }

    override suspend fun action(
        confirmationOption: ShopPayConfirmationOption,
        confirmationParameters: ConfirmationDefinition.Parameters
    ): ConfirmationDefinition.Action<Unit> {
        return ConfirmationDefinition.Action.Launch(
            launcherArguments = Unit,
            receivesResultInProcess = false,
            deferredIntentConfirmationType = null,
        )
    }

    @OptIn(SharedPaymentTokenSessionPreview::class)
    private fun merchantName(
        confirmationOption: ShopPayConfirmationOption,
        confirmationParameters: ConfirmationDefinition.Parameters,
    ): String {
        val initializationMode = confirmationParameters.initializationMode

        if (initializationMode !is PaymentElementLoader.InitializationMode.DeferredIntent) {
            return confirmationOption.merchantDisplayName
        }

        val intentBehavior = initializationMode.intentConfiguration.intentBehavior

        if (intentBehavior !is PaymentSheet.IntentConfiguration.IntentBehavior.SharedPaymentToken) {
            return confirmationOption.merchantDisplayName
        }

        return intentBehavior.sellerDetails?.businessName ?: confirmationOption.merchantDisplayName
    }
}
