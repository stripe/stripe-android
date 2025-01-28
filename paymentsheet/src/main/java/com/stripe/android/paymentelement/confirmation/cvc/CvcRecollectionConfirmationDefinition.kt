package com.stripe.android.paymentelement.confirmation.cvc

import androidx.activity.result.ActivityResultCaller
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.paymentelement.confirmation.intent.DeferredIntentConfirmationType
import com.stripe.android.paymentsheet.cvcrecollection.CvcRecollectionHandler
import com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection.CvcRecollectionContract
import com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection.CvcRecollectionLauncher
import com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection.CvcRecollectionLauncherFactory
import com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection.CvcRecollectionResult

internal class CvcRecollectionConfirmationDefinition(
    private val handler: CvcRecollectionHandler,
    private val factory: CvcRecollectionLauncherFactory,
) : ConfirmationDefinition<
    PaymentMethodConfirmationOption.Saved,
    CvcRecollectionLauncher,
    Unit,
    CvcRecollectionResult,
    > {
    override val key: String = "CvcRecollection"

    override fun option(confirmationOption: ConfirmationHandler.Option): PaymentMethodConfirmationOption.Saved? {
        return confirmationOption as? PaymentMethodConfirmationOption.Saved
    }

    override fun canConfirm(
        confirmationOption: PaymentMethodConfirmationOption.Saved,
        confirmationParameters: ConfirmationDefinition.Parameters,
    ): Boolean {
        return !confirmationOption.optionsParams.hasAlreadyRecollectedCvc() && handler.requiresCVCRecollection(
            stripeIntent = confirmationParameters.intent,
            initializationMode = confirmationParameters.initializationMode,
            paymentMethod = confirmationOption.paymentMethod,
            optionsParams = confirmationOption.optionsParams,
        )
    }

    override suspend fun action(
        confirmationOption: PaymentMethodConfirmationOption.Saved,
        confirmationParameters: ConfirmationDefinition.Parameters
    ): ConfirmationDefinition.Action<Unit> {
        return ConfirmationDefinition.Action.Launch(
            launcherArguments = Unit,
            receivesResultInProcess = true,
            deferredIntentConfirmationType = null,
        )
    }

    override fun createLauncher(
        activityResultCaller: ActivityResultCaller,
        onResult: (CvcRecollectionResult) -> Unit
    ): CvcRecollectionLauncher {
        return factory.create(
            activityResultLauncher = activityResultCaller.registerForActivityResult(
                CvcRecollectionContract(),
                onResult,
            ),
        )
    }

    override fun launch(
        launcher: CvcRecollectionLauncher,
        arguments: Unit,
        confirmationOption: PaymentMethodConfirmationOption.Saved,
        confirmationParameters: ConfirmationDefinition.Parameters
    ) {
        handler.launch(confirmationOption.paymentMethod) { recollectionData ->
            launcher.launch(
                data = recollectionData,
                appearance = confirmationParameters.appearance,
                isLiveMode = confirmationParameters.intent.isLiveMode,
            )
        }
    }

    override fun toResult(
        confirmationOption: PaymentMethodConfirmationOption.Saved,
        confirmationParameters: ConfirmationDefinition.Parameters,
        deferredIntentConfirmationType: DeferredIntentConfirmationType?,
        result: CvcRecollectionResult
    ): ConfirmationDefinition.Result {
        return when (result) {
            is CvcRecollectionResult.Confirmed -> ConfirmationDefinition.Result.NextStep(
                confirmationOption = confirmationOption.copy(
                    optionsParams = when (val params = confirmationOption.optionsParams) {
                        is PaymentMethodOptionsParams.Card -> params.copy(cvc = result.cvc)
                        else -> PaymentMethodOptionsParams.Card(cvc = result.cvc)
                    }
                ),
                parameters = confirmationParameters,
            )
            is CvcRecollectionResult.Cancelled -> ConfirmationDefinition.Result.Canceled(
                action = ConfirmationHandler.Result.Canceled.Action.InformCancellation,
            )
        }
    }

    private fun PaymentMethodOptionsParams?.hasAlreadyRecollectedCvc(): Boolean {
        return when (this) {
            is PaymentMethodOptionsParams.Card -> cvc != null
            else -> false
        }
    }
}
