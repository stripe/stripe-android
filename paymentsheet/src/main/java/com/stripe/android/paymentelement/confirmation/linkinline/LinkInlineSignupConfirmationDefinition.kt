package com.stripe.android.paymentelement.confirmation.linkinline

import android.os.Parcelable
import androidx.activity.result.ActivityResultCaller
import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.link.LinkPaymentDetails
import com.stripe.android.link.account.LinkStore
import com.stripe.android.link.analytics.LinkAnalyticsHelper
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.model.wallets.Wallet
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.paymentelement.confirmation.intent.DeferredIntentConfirmationType
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
        confirmationParameters: ConfirmationDefinition.Parameters
    ): ConfirmationDefinition.Action<LauncherArguments> {
        val nextConfirmationOption = createPaymentMethodConfirmationOption(confirmationOption)

        return ConfirmationDefinition.Action.Launch(
            launcherArguments = LauncherArguments(nextConfirmationOption),
            receivesResultInProcess = true,
            deferredIntentConfirmationType = null,
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
        confirmationParameters: ConfirmationDefinition.Parameters,
    ) {
        launcher.onResult(Result(arguments.nextConfirmationOption))
    }

    override fun toResult(
        confirmationOption: LinkInlineSignupConfirmationOption,
        confirmationParameters: ConfirmationDefinition.Parameters,
        deferredIntentConfirmationType: DeferredIntentConfirmationType?,
        result: Result,
    ): ConfirmationDefinition.Result {
        return ConfirmationDefinition.Result.NextStep(
            confirmationOption = result.nextConfirmationOption,
            parameters = confirmationParameters,
        )
    }

    private suspend fun createPaymentMethodConfirmationOption(
        linkInlineSignupConfirmationOption: LinkInlineSignupConfirmationOption,
    ): PaymentMethodConfirmationOption {
        val configuration = linkInlineSignupConfirmationOption.linkConfiguration
        val userInput = linkInlineSignupConfirmationOption.userInput

        return when (linkConfigurationCoordinator.getAccountStatusFlow(configuration).first()) {
            AccountStatus.Verified -> createOptionAfterAttachingToLink(linkInlineSignupConfirmationOption)
            AccountStatus.VerificationStarted,
            AccountStatus.NeedsVerification -> {
                linkAnalyticsHelper.onLinkPopupSkipped()

                linkInlineSignupConfirmationOption.toNewOption()
            }
            AccountStatus.SignedOut,
            AccountStatus.Error -> {
                linkConfigurationCoordinator.signInWithUserInput(configuration, userInput).fold(
                    onSuccess = {
                        // If successful, the account was fetched or created, so try again
                        createPaymentMethodConfirmationOption(linkInlineSignupConfirmationOption)
                    },
                    onFailure = {
                        linkInlineSignupConfirmationOption.toNewOption()
                    }
                )
            }
        }
    }

    private suspend fun createOptionAfterAttachingToLink(
        linkInlineSignupConfirmationOption: LinkInlineSignupConfirmationOption,
    ): PaymentMethodConfirmationOption {
        val createParams = linkInlineSignupConfirmationOption.createParams
        val saveOption = linkInlineSignupConfirmationOption.saveOption

        val linkPaymentDetails = linkConfigurationCoordinator.attachNewCardToAccount(
            linkInlineSignupConfirmationOption.linkConfiguration,
            createParams,
        ).getOrNull()

        return when (linkPaymentDetails) {
            is LinkPaymentDetails.New -> {
                linkStore.markLinkAsUsed()

                PaymentMethodConfirmationOption.New(
                    createParams = createParams,
                    optionsParams = PaymentMethodOptionsParams.Card(
                        setupFutureUsage = saveOption.setupFutureUsage
                    ),
                    shouldSave = saveOption.shouldSave(),
                )
            }
            is LinkPaymentDetails.Saved -> {
                linkStore.markLinkAsUsed()

                val last4 = linkPaymentDetails.paymentDetails.last4

                PaymentMethodConfirmationOption.Saved(
                    paymentMethod = PaymentMethod.Builder()
                        .setId(linkPaymentDetails.paymentDetails.id)
                        .setCode(createParams.typeCode)
                        .setCard(
                            PaymentMethod.Card(
                                last4 = last4,
                                wallet = Wallet.LinkWallet(last4)
                            )
                        )
                        .setType(PaymentMethod.Type.Card)
                        .build(),
                    optionsParams = PaymentMethodOptionsParams.Card(
                        setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.OffSession?.takeIf {
                            saveOption.shouldSave()
                        } ?: ConfirmPaymentIntentParams.SetupFutureUsage.Blank
                    ),
                )
            }
            null -> linkInlineSignupConfirmationOption.toNewOption()
        }
    }

    private fun LinkInlineSignupConfirmationOption.toNewOption(): PaymentMethodConfirmationOption.New {
        return PaymentMethodConfirmationOption.New(
            createParams = createParams,
            optionsParams = optionsParams,
            shouldSave = saveOption.shouldSave(),
        )
    }

    private fun LinkInlineSignupConfirmationOption.PaymentMethodSaveOption.shouldSave(): Boolean {
        return this == LinkInlineSignupConfirmationOption.PaymentMethodSaveOption.ReuseRequested
    }

    @Parcelize
    data class Result(
        val nextConfirmationOption: PaymentMethodConfirmationOption,
    ) : Parcelable

    @Parcelize
    data class LauncherArguments(
        val nextConfirmationOption: PaymentMethodConfirmationOption,
    ) : Parcelable

    @Parcelize
    class Launcher(
        val onResult: (Result) -> Unit,
    ) : Parcelable
}
