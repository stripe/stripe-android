package com.stripe.android.paymentelement.confirmation.link

import androidx.activity.result.ActivityResultCaller
import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.LinkPaymentLauncher
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.link.account.LinkStore
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.paymentelement.confirmation.intent.DeferredIntentConfirmationType
import javax.inject.Inject

internal class LinkConfirmationDefinition @Inject constructor(
    private val linkPaymentLauncher: LinkPaymentLauncher,
    private val linkStore: LinkStore,
    private val linkAccountHolder: LinkAccountHolder
) : ConfirmationDefinition<LinkConfirmationOption, LinkPaymentLauncher, Unit, LinkActivityResult> {
    override val key: String = "Link"

    override fun option(confirmationOption: ConfirmationHandler.Option): LinkConfirmationOption? {
        return confirmationOption as? LinkConfirmationOption
    }

    override fun createLauncher(
        activityResultCaller: ActivityResultCaller,
        onResult: (LinkActivityResult) -> Unit
    ): LinkPaymentLauncher {
        return linkPaymentLauncher.apply {
            register(activityResultCaller, onResult)
        }
    }

    override fun unregister(launcher: LinkPaymentLauncher) {
        launcher.unregister()
    }

    override suspend fun action(
        confirmationOption: LinkConfirmationOption,
        confirmationParameters: ConfirmationDefinition.Parameters,
    ): ConfirmationDefinition.Action<Unit> {
        return ConfirmationDefinition.Action.Launch(
            launcherArguments = Unit,
            receivesResultInProcess = false,
            deferredIntentConfirmationType = null,
        )
    }

    override fun launch(
        launcher: LinkPaymentLauncher,
        arguments: Unit,
        confirmationOption: LinkConfirmationOption,
        confirmationParameters: ConfirmationDefinition.Parameters,
    ) {
        launcher.present(
            configuration = confirmationOption.configuration,
            linkAccount = linkAccountHolder.linkAccount.value
        )
    }

    override fun toResult(
        confirmationOption: LinkConfirmationOption,
        confirmationParameters: ConfirmationDefinition.Parameters,
        deferredIntentConfirmationType: DeferredIntentConfirmationType?,
        result: LinkActivityResult
    ): ConfirmationDefinition.Result {
        if (
            result !is LinkActivityResult.Canceled ||
            result.reason != LinkActivityResult.Canceled.Reason.BackPressed
        ) {
            linkStore.markLinkAsUsed()
        }

        return when (result) {
            is LinkActivityResult.PaymentMethodObtained -> {
                ConfirmationDefinition.Result.NextStep(
                    confirmationOption = PaymentMethodConfirmationOption.Saved(
                        paymentMethod = result.paymentMethod,
                        optionsParams = null,
                    ),
                    parameters = confirmationParameters,
                )
            }
            is LinkActivityResult.Failed -> {
                result.linkAccountUpdate.updateLinkAccount()
                ConfirmationDefinition.Result.Failed(
                    cause = result.error,
                    message = result.error.stripeErrorMessage(),
                    type = ConfirmationHandler.Result.Failed.ErrorType.Payment,
                )
            }
            is LinkActivityResult.Canceled -> {
                result.linkAccountUpdate.updateLinkAccount()
                ConfirmationDefinition.Result.Canceled(
                    action = ConfirmationHandler.Result.Canceled.Action.InformCancellation,
                )
            }
            is LinkActivityResult.Completed -> {
                result.linkAccountUpdate.updateLinkAccount()
                ConfirmationDefinition.Result.Succeeded(
                    intent = confirmationParameters.intent,
                    deferredIntentConfirmationType = deferredIntentConfirmationType
                )
            }
        }
    }

    private fun LinkAccountUpdate.updateLinkAccount() {
        when (this) {
            is LinkAccountUpdate.Value -> {
                linkAccountHolder.set(linkAccount)
            }
            LinkAccountUpdate.None -> Unit
        }
    }
}
