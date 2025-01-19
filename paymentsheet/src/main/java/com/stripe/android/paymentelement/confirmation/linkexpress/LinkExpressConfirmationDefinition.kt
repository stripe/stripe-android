package com.stripe.android.paymentelement.confirmation.linkexpress

import androidx.activity.result.ActivityResultCaller
import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.link.LinkExpressLauncher
import com.stripe.android.link.LinkExpressResult
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.intent.DeferredIntentConfirmationType
import com.stripe.android.paymentelement.confirmation.link.LinkConfirmationOption

internal class LinkExpressConfirmationDefinition(
    private val linkExpressLauncher: LinkExpressLauncher,
) : ConfirmationDefinition<LinkExpressConfirmationOption, LinkExpressLauncher, Unit, LinkExpressResult> {
    override val key: String = "LinkExpress"

    override fun option(confirmationOption: ConfirmationHandler.Option): LinkExpressConfirmationOption? {
        return confirmationOption as? LinkExpressConfirmationOption
    }

    override fun createLauncher(
        activityResultCaller: ActivityResultCaller,
        onResult: (LinkExpressResult) -> Unit
    ): LinkExpressLauncher {
        return linkExpressLauncher.apply {
            register(activityResultCaller, onResult)
        }
    }

    override fun unregister(launcher: LinkExpressLauncher) {
        launcher.unregister()
    }

    override suspend fun action(
        confirmationOption: LinkExpressConfirmationOption,
        confirmationParameters: ConfirmationDefinition.Parameters,
    ): ConfirmationDefinition.Action<Unit> {
        return ConfirmationDefinition.Action.Launch(
            launcherArguments = Unit,
            receivesResultInProcess = false,
            deferredIntentConfirmationType = null,
        )
    }

    override fun launch(
        launcher: LinkExpressLauncher,
        arguments: Unit,
        confirmationOption: LinkExpressConfirmationOption,
        confirmationParameters: ConfirmationDefinition.Parameters,
    ) {
        launcher.present(
            configuration = confirmationOption.configuration,
            linkAccount = confirmationOption.linkAccount
        )
    }

    override fun toResult(
        confirmationOption: LinkExpressConfirmationOption,
        confirmationParameters: ConfirmationDefinition.Parameters,
        deferredIntentConfirmationType: DeferredIntentConfirmationType?,
        result: LinkExpressResult
    ): ConfirmationDefinition.Result {
        return when (result) {
            is LinkExpressResult.Authenticated -> {
                ConfirmationDefinition.Result.NextStep(
                    parameters = confirmationParameters,
                    confirmationOption = LinkConfirmationOption(
                        configuration = confirmationOption.configuration,
                        linkAccount = result.linkAccount
                    )
                )
            }
            LinkExpressResult.Canceled -> {
                ConfirmationDefinition.Result.Canceled(
                    action = ConfirmationHandler.Result.Canceled.Action.InformCancellation,
                )
            }
            is LinkExpressResult.Failed -> {
                ConfirmationDefinition.Result.Failed(
                    cause = result.error,
                    message = result.error.stripeErrorMessage(),
                    type = ConfirmationHandler.Result.Failed.ErrorType.Payment,
                )
            }
        }
    }
}
