package com.stripe.android.paymentelement.confirmation

import androidx.activity.result.ActivityResultCaller
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.paymentdatacollection.bacs.BacsMandateConfirmationContract
import com.stripe.android.paymentsheet.paymentdatacollection.bacs.BacsMandateConfirmationLauncher
import com.stripe.android.paymentsheet.paymentdatacollection.bacs.BacsMandateConfirmationLauncherFactory
import com.stripe.android.paymentsheet.paymentdatacollection.bacs.BacsMandateConfirmationResult
import com.stripe.android.paymentsheet.paymentdatacollection.bacs.BacsMandateData

internal class BacsConfirmationDefinition(
    private val bacsMandateConfirmationLauncherFactory: BacsMandateConfirmationLauncherFactory,
) : ConfirmationDefinition<
    ConfirmationHandler.Option.BacsPaymentMethod,
    BacsMandateConfirmationLauncher,
    BacsMandateData,
    BacsMandateConfirmationResult,
    > {
    override val key: String = "Bacs"

    override val confirmationFlow: ConfirmationDefinition.ConfirmationFlow =
        ConfirmationDefinition.ConfirmationFlow.Preconfirm

    override fun option(confirmationOption: ConfirmationHandler.Option): ConfirmationHandler.Option.BacsPaymentMethod? {
        return confirmationOption as? ConfirmationHandler.Option.BacsPaymentMethod
    }

    override fun canConfirm(
        confirmationOption: ConfirmationHandler.Option.BacsPaymentMethod,
        intent: StripeIntent
    ): Boolean = true

    override suspend fun action(
        confirmationOption: ConfirmationHandler.Option.BacsPaymentMethod,
        intent: StripeIntent
    ): ConfirmationDefinition.ConfirmationAction<BacsMandateData> {
        return BacsMandateData.fromConfirmationOption(confirmationOption)?.let { data ->
            ConfirmationDefinition.ConfirmationAction.Launch(
                launcherArguments = data,
                deferredIntentConfirmationType = null,
            )
        } ?: run {
            ConfirmationDefinition.ConfirmationAction.Fail(
                cause = IllegalArgumentException(
                    "Given payment selection could not be converted to Bacs data!"
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
        confirmationOption: ConfirmationHandler.Option.BacsPaymentMethod,
        intent: StripeIntent,
    ) {
        launcher.launch(
            data = arguments,
            appearance = confirmationOption.appearance
        )
    }

    override fun toResult(
        confirmationOption: ConfirmationHandler.Option.BacsPaymentMethod,
        deferredIntentConfirmationType: DeferredIntentConfirmationType?,
        intent: StripeIntent,
        result: BacsMandateConfirmationResult,
    ): ConfirmationDefinition.Result {
        return when (result) {
            is BacsMandateConfirmationResult.Confirmed -> {
                val nextConfirmationOption = ConfirmationHandler.Option.PaymentMethod.New(
                    initializationMode = confirmationOption.initializationMode,
                    shippingDetails = confirmationOption.shippingDetails,
                    createParams = confirmationOption.createParams,
                    optionsParams = null,
                    shouldSave = false,
                )

                ConfirmationDefinition.Result.Succeeded(
                    intent = intent,
                    confirmationOption = nextConfirmationOption,
                    deferredIntentConfirmationType = null,
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
