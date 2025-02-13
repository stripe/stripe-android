package com.stripe.android.paymentelement.confirmation.link

import android.os.Parcelable
import androidx.activity.result.ActivityResultCaller
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.parsers.PaymentMethodJsonParser
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.paymentelement.confirmation.intent.DeferredIntentConfirmationType
import com.stripe.android.paymentsheet.R
import kotlinx.parcelize.Parcelize
import org.json.JSONObject
import javax.inject.Inject

internal class LinkPassthroughConfirmationDefinition @Inject constructor(
    private val linkAccountManager: LinkAccountManager,
) : ConfirmationDefinition<
    LinkPassthroughConfirmationOption,
    LinkPassthroughConfirmationDefinition.Launcher,
    LinkPassthroughConfirmationDefinition.LauncherArguments,
    LinkPassthroughConfirmationDefinition.Result,
    > {
    override val key: String = "LinkPassthrough"

    override fun option(confirmationOption: ConfirmationHandler.Option): LinkPassthroughConfirmationOption? {
        return confirmationOption as? LinkPassthroughConfirmationOption
    }

    override suspend fun action(
        confirmationOption: LinkPassthroughConfirmationOption,
        confirmationParameters: ConfirmationDefinition.Parameters
    ): ConfirmationDefinition.Action<LauncherArguments> {
        return createPaymentMethodConfirmationOption(confirmationOption).fold(
            onSuccess = { nextConfirmationOption ->
                ConfirmationDefinition.Action.Launch(
                    launcherArguments = LauncherArguments(nextConfirmationOption),
                    receivesResultInProcess = true,
                    deferredIntentConfirmationType = null,
                )
            },
            onFailure = { error ->
                ConfirmationDefinition.Action.Fail(
                    cause = error,
                    message = resolvableString(R.string.stripe_something_went_wrong),
                    errorType = ConfirmationHandler.Result.Failed.ErrorType.Internal,
                )
            },
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
        confirmationOption: LinkPassthroughConfirmationOption,
        confirmationParameters: ConfirmationDefinition.Parameters,
    ) {
        launcher.onResult(Result(arguments.nextConfirmationOption))
    }

    override fun toResult(
        confirmationOption: LinkPassthroughConfirmationOption,
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
        confirmationOption: LinkPassthroughConfirmationOption,
    ): kotlin.Result<PaymentMethodConfirmationOption> {
        return linkAccountManager.sharePaymentDetails(
            paymentDetailsId = confirmationOption.paymentDetailsId,
            expectedPaymentMethodType = confirmationOption.expectedPaymentMethodType,
        ).mapCatching {
            requireNotNull(it.encodedPaymentMethod.parsePaymentMethod())
        }.map { paymentMethod ->
            PaymentMethodConfirmationOption.Saved(
                paymentMethod = paymentMethod,
                optionsParams = null,
            )
        }
    }

    @Parcelize
    data class Result(
        val nextConfirmationOption: PaymentMethodConfirmationOption,
    ) : Parcelable

    data class LauncherArguments(
        val nextConfirmationOption: PaymentMethodConfirmationOption,
    )

    class Launcher(
        val onResult: (Result) -> Unit,
    )
}

private fun String.parsePaymentMethod(): PaymentMethod? = runCatching {
    val json = JSONObject(this)
    val paymentMethod = PaymentMethodJsonParser().parse(json)
    paymentMethod
}.getOrNull()
