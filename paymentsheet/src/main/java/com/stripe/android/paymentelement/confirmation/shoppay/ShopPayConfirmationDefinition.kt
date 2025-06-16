package com.stripe.android.paymentelement.confirmation.shoppay

import androidx.activity.result.ActivityResultCaller
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.intent.DeferredIntentConfirmationType
import com.stripe.android.shoppay.ShopPayActivityResult
import com.stripe.android.shoppay.ShopPayLauncher
import javax.inject.Inject

internal class ShopPayConfirmationDefinition @Inject constructor(
    private val shopPayLauncher: ShopPayLauncher,
) : ConfirmationDefinition<ShopPayConfirmationOption, ShopPayLauncher, Unit, ShopPayActivityResult> {
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
        val error = Throwable("ShopPay is not supported yet")
        return ConfirmationDefinition.Result.Failed(
            cause = error,
            message = error.message.orEmpty().resolvableString,
            type = ConfirmationHandler.Result.Failed.ErrorType.Payment,
        )
    }

    override fun createLauncher(
        activityResultCaller: ActivityResultCaller,
        onResult: (ShopPayActivityResult) -> Unit
    ): ShopPayLauncher {
        return shopPayLauncher.apply {
            register(activityResultCaller, onResult)
        }
    }

    override fun launch(
        launcher: ShopPayLauncher,
        arguments: Unit,
        confirmationOption: ShopPayConfirmationOption,
        confirmationParameters: ConfirmationDefinition.Parameters
    ) {
        launcher.present(confirmationOption.shopPayConfiguration)
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

    override fun unregister(launcher: ShopPayLauncher) {
        launcher.unregister()
    }
}
