package com.stripe.android.paymentelement.confirmation.linkinline

import android.os.Parcelable
import androidx.activity.result.ActivityResultCaller
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.link.LinkPaymentDetails
import com.stripe.android.link.account.LinkStore
import com.stripe.android.link.analytics.LinkAnalyticsHelper
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.ui.inline.UserInput
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.PaymentMethodExtraParams
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import kotlinx.coroutines.flow.first
import kotlinx.parcelize.Parcelize

internal class LinkInlineSignupConfirmationDefinition(
    private val linkConfigurationCoordinator: LinkConfigurationCoordinator,
    private val linkAnalyticsHelper: LinkAnalyticsHelper,
    private val linkStore: LinkStore,
) : ConfirmationDefinition<
    LinkInlineSignupConfirmationOption,
    LinkInlineSignupConfirmationDefinition.Launcher,
    LinkInlineSignupConfirmationDefinition.LauncherArguments,
    LinkInlineSignupConfirmationDefinition.Result,
    > {
    override val key: String = "LinkInlineSignup"

    override fun option(confirmationOption: ConfirmationHandler.Option): LinkInlineSignupConfirmationOption? {
        return confirmationOption as? LinkInlineSignupConfirmationOption
    }

    override suspend fun action(
        confirmationOption: LinkInlineSignupConfirmationOption,
        confirmationArgs: ConfirmationHandler.Args
    ): ConfirmationDefinition.Action<LauncherArguments> {
        val nextConfirmationOption = createPaymentMethodConfirmationOption(confirmationOption)

        return ConfirmationDefinition.Action.Launch(
            launcherArguments = LauncherArguments(nextConfirmationOption),
            receivesResultInProcess = true,
        )
    }

    override fun createLauncher(
        activityResultCaller: ActivityResultCaller,
        onResult: (Result) -> Unit
    ): Launcher {
        return Launcher(onResult)
    }

    override fun launch(
        launcher: Launcher,
        arguments: LauncherArguments,
        confirmationOption: LinkInlineSignupConfirmationOption,
        confirmationArgs: ConfirmationHandler.Args,
    ) {
        launcher.onResult(Result(arguments.nextConfirmationOption))
    }

    override fun toResult(
        confirmationOption: LinkInlineSignupConfirmationOption,
        confirmationArgs: ConfirmationHandler.Args,
        launcherArgs: LauncherArguments,
        result: Result,
    ): ConfirmationDefinition.Result {
        return ConfirmationDefinition.Result.NextStep(
            confirmationOption = result.nextConfirmationOption,
            arguments = confirmationArgs,
        )
    }

    private suspend fun createPaymentMethodConfirmationOption(
        linkInlineSignupConfirmationOption: LinkInlineSignupConfirmationOption,
    ): PaymentMethodConfirmationOption {
        val configuration = linkInlineSignupConfirmationOption.linkConfiguration
        val userInput = linkInlineSignupConfirmationOption.sanitizedUserInput

        return when (linkConfigurationCoordinator.getAccountStatusFlow(configuration).first()) {
            is AccountStatus.Verified -> createOptionAfterAttachingToLink(linkInlineSignupConfirmationOption, userInput)
            AccountStatus.VerificationStarted,
            is AccountStatus.NeedsVerification -> {
                linkAnalyticsHelper.onLinkPopupSkipped()

                linkInlineSignupConfirmationOption.toOption()
            }
            AccountStatus.SignedOut,
            is AccountStatus.Error -> {
                linkConfigurationCoordinator.signInWithUserInput(configuration, userInput).fold(
                    onSuccess = {
                        // If successful, the account was fetched or created, so try again
                        createPaymentMethodConfirmationOption(linkInlineSignupConfirmationOption)
                    },
                    onFailure = {
                        linkInlineSignupConfirmationOption.toOption()
                    }
                )
            }
        }
    }

    private suspend fun createOptionAfterAttachingToLink(
        linkInlineSignupConfirmationOption: LinkInlineSignupConfirmationOption,
        userInput: UserInput,
    ): PaymentMethodConfirmationOption {
        if (linkInlineSignupConfirmationOption !is LinkInlineSignupConfirmationOption.New) {
            return linkInlineSignupConfirmationOption.toOption()
        }

        if (userInput is UserInput.SignIn) {
            linkAnalyticsHelper.onLinkPopupSkipped()

            return linkInlineSignupConfirmationOption.toOption()
        }

        val createParams = linkInlineSignupConfirmationOption.createParams
        val saveOption = linkInlineSignupConfirmationOption.saveOption
        val extraParams = linkInlineSignupConfirmationOption.extraParams
        val configuration = linkInlineSignupConfirmationOption.linkConfiguration

        val linkPaymentDetails = linkConfigurationCoordinator.attachNewCardToAccount(
            configuration,
            createParams,
        ).getOrNull()

        return when (linkPaymentDetails) {
            is LinkPaymentDetails.New -> {
                linkStore.markLinkAsUsed()

                linkPaymentDetails.toNewOption(saveOption, configuration, extraParams)
            }
            is LinkPaymentDetails.Saved -> {
                linkStore.markLinkAsUsed()

                linkPaymentDetails.toSavedOption(saveOption)
            }
            null -> linkInlineSignupConfirmationOption.toOption()
        }
    }

    private fun LinkPaymentDetails.Saved.toSavedOption(
        saveOption: LinkInlineSignupConfirmationOption.PaymentMethodSaveOption,
    ): PaymentMethodConfirmationOption.Saved {
        return PaymentMethodConfirmationOption.Saved(
            paymentMethod = paymentMethod,
            optionsParams = PaymentMethodOptionsParams.Card(
                setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.OffSession.takeIf {
                    saveOption.shouldSave()
                } ?: ConfirmPaymentIntentParams.SetupFutureUsage.Blank
            ),
            originatedFromWallet = true,
            newPMTransformedForConfirmation = true
        )
    }

    private fun LinkPaymentDetails.New.toNewOption(
        saveOption: LinkInlineSignupConfirmationOption.PaymentMethodSaveOption,
        configuration: LinkConfiguration,
        extraParams: PaymentMethodExtraParams?,
    ): PaymentMethodConfirmationOption.New {
        val passthroughMode = configuration.passthroughModeEnabled

        val optionsParams = if (passthroughMode) {
            PaymentMethodOptionsParams.Card(setupFutureUsage = saveOption.setupFutureUsage)
        } else {
            PaymentMethodOptionsParams.Link(setupFutureUsage = saveOption.setupFutureUsage)
        }

        return PaymentMethodConfirmationOption.New(
            createParams = confirmParams,
            optionsParams = optionsParams,
            extraParams = extraParams,
            shouldSave = saveOption.shouldSave(),
        )
    }

    private fun LinkInlineSignupConfirmationOption.toOption(): PaymentMethodConfirmationOption {
        return when (this) {
            is LinkInlineSignupConfirmationOption.New -> PaymentMethodConfirmationOption.New(
                createParams = createParams,
                optionsParams = optionsParams,
                extraParams = extraParams,
                shouldSave = saveOption.shouldSave(),
            )
            is LinkInlineSignupConfirmationOption.Saved -> PaymentMethodConfirmationOption.Saved(
                paymentMethod = paymentMethod,
                optionsParams = optionsParams,
            )
        }
    }

    private fun LinkInlineSignupConfirmationOption.PaymentMethodSaveOption.shouldSave(): Boolean {
        return this == LinkInlineSignupConfirmationOption.PaymentMethodSaveOption.RequestedReuse
    }

    @Parcelize
    data class Result(
        val nextConfirmationOption: PaymentMethodConfirmationOption,
    ) : Parcelable

    @Parcelize
    data class LauncherArguments(
        val nextConfirmationOption: PaymentMethodConfirmationOption,
    ) : Parcelable

    class Launcher(
        val onResult: (Result) -> Unit,
    )
}
