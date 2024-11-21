package com.stripe.android.paymentelement.confirmation.bacs
import androidx.activity.result.ActivityResultCaller
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.paymentelement.confirmation.intent.DeferredIntentConfirmationType
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.paymentdatacollection.bacs.BacsMandateConfirmationContract
import com.stripe.android.paymentsheet.paymentdatacollection.bacs.BacsMandateConfirmationLauncher
import com.stripe.android.paymentsheet.paymentdatacollection.bacs.BacsMandateConfirmationLauncherFactory
import com.stripe.android.paymentsheet.paymentdatacollection.bacs.BacsMandateConfirmationResult
import com.stripe.android.paymentsheet.paymentdatacollection.bacs.BacsMandateData

internal class BacsConfirmationDefinition(
    private val bacsMandateConfirmationLauncherFactory: BacsMandateConfirmationLauncherFactory,
) : ConfirmationDefinition<
    BacsConfirmationOption,
    BacsMandateConfirmationLauncher,
    BacsMandateData,
    BacsMandateConfirmationResult,
    > {
    override val key: String = "Bacs"

    override fun option(confirmationOption: ConfirmationHandler.Option): BacsConfirmationOption? {
        return confirmationOption as? BacsConfirmationOption
    }

    override suspend fun action(
        confirmationOption: BacsConfirmationOption,
        intent: StripeIntent
    ): ConfirmationDefinition.Action<BacsMandateData> {
        return BacsMandateData.fromConfirmationOption(confirmationOption)?.let { data ->
            ConfirmationDefinition.Action.Launch(
                launcherArguments = data,
                deferredIntentConfirmationType = null,
                receivesResultInProcess = true,
            )
        } ?: run {
            ConfirmationDefinition.Action.Fail(
                cause = IllegalArgumentException(
                    "Given confirmation option does not have expected Bacs data!"
                ),
                message = R.string.stripe_something_went_wrong.resolvableString,
                errorType = ConfirmationHandler.Result.Failed.ErrorType.Internal
            )
        }
    }

    override fun createLauncher(
        activityResultCaller: ActivityResultCaller,
        onResult: (BacsMandateConfirmationResult) -> Unit
    ): BacsMandateConfirmationLauncher {
        return bacsMandateConfirmationLauncherFactory.create(
            activityResultCaller.registerForActivityResult(
                BacsMandateConfirmationContract(),
                onResult,
            )
        )
    }

    override fun launch(
        launcher: BacsMandateConfirmationLauncher,
        arguments: BacsMandateData,
        confirmationOption: BacsConfirmationOption,
        intent: StripeIntent,
    ) {
        launcher.launch(
            data = arguments,
            appearance = confirmationOption.appearance
        )
    }

    override fun toResult(
        confirmationOption: BacsConfirmationOption,
        deferredIntentConfirmationType: DeferredIntentConfirmationType?,
        intent: StripeIntent,
        result: BacsMandateConfirmationResult,
    ): ConfirmationDefinition.Result {
        return when (result) {
            is BacsMandateConfirmationResult.Confirmed -> {
                val nextConfirmationOption = PaymentMethodConfirmationOption.New(
                    initializationMode = confirmationOption.initializationMode,
                    shippingDetails = confirmationOption.shippingDetails,
                    createParams = confirmationOption.createParams,
                    optionsParams = null,
                    shouldSave = false,
                )

                ConfirmationDefinition.Result.NextStep(
                    intent = intent,
                    confirmationOption = nextConfirmationOption,
                )
            }
            is BacsMandateConfirmationResult.ModifyDetails -> ConfirmationDefinition.Result.Canceled(
                action = ConfirmationHandler.Result.Canceled.Action.ModifyPaymentDetails,
            )
            is BacsMandateConfirmationResult.Cancelled -> ConfirmationDefinition.Result.Canceled(
                action = ConfirmationHandler.Result.Canceled.Action.None,
            )
        }
    }
}
