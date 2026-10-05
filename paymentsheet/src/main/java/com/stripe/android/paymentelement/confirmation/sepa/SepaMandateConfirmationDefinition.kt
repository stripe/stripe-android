package com.stripe.android.paymentelement.confirmation.sepa

import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.paymentsheet.ui.SepaMandateContract
import com.stripe.android.paymentsheet.ui.SepaMandateResult
import javax.inject.Inject

internal class SepaMandateConfirmationDefinition @Inject constructor() : ConfirmationDefinition<
    PaymentMethodConfirmationOption.Saved,
    ActivityResultLauncher<SepaMandateContract.Args>,
    SepaMandateContract.Args,
    SepaMandateResult,
    > {
    override val key: String = "AcknowledgeSepaMandate"

    override fun option(
        confirmationOption: ConfirmationHandler.Option,
    ): PaymentMethodConfirmationOption.Saved? {
        return confirmationOption as? PaymentMethodConfirmationOption.Saved
    }

    override fun canConfirm(
        confirmationOption: PaymentMethodConfirmationOption.Saved,
        confirmationArgs: ConfirmationHandler.Args,
    ): Boolean {
        return confirmationOption.paymentMethod.type == PaymentMethod.Type.SepaDebit &&
            !confirmationOption.hasAcknowledgedSepaMandate
    }

    override suspend fun action(
        confirmationOption: PaymentMethodConfirmationOption.Saved,
        confirmationArgs: ConfirmationHandler.Args,
    ): ConfirmationDefinition.Action<SepaMandateContract.Args> {
        return ConfirmationDefinition.Action.Launch(
            launcherArguments = SepaMandateContract.Args(
                merchantName = confirmationArgs.paymentMethodMetadata.merchantName,
                appearance = confirmationArgs.paymentMethodMetadata.appearance,
            ),
            receivesResultInProcess = true,
        )
    }

    override fun createLauncher(
        activityResultCaller: ActivityResultCaller,
        lifecycleOwner: LifecycleOwner,
        onResult: (SepaMandateResult) -> Unit,
    ): ActivityResultLauncher<SepaMandateContract.Args> {
        return activityResultCaller.registerForActivityResult(SepaMandateContract(), onResult)
    }

    override fun launch(
        launcher: ActivityResultLauncher<SepaMandateContract.Args>,
        arguments: SepaMandateContract.Args,
        confirmationOption: PaymentMethodConfirmationOption.Saved,
        confirmationArgs: ConfirmationHandler.Args,
    ) {
        launcher.launch(arguments)
    }

    override fun unregister(launcher: ActivityResultLauncher<SepaMandateContract.Args>) {
        launcher.unregister()
    }

    override fun toResult(
        confirmationOption: PaymentMethodConfirmationOption.Saved,
        confirmationArgs: ConfirmationHandler.Args,
        launcherArgs: SepaMandateContract.Args,
        result: SepaMandateResult,
    ): ConfirmationDefinition.Result {
        return when (result) {
            SepaMandateResult.Acknowledged -> ConfirmationDefinition.Result.NextStep(
                confirmationOption = confirmationOption.copy(hasAcknowledgedSepaMandate = true),
                arguments = confirmationArgs,
            )
            SepaMandateResult.Canceled -> ConfirmationDefinition.Result.Canceled(
                action = ConfirmationHandler.Result.Canceled.Action.InformCancellation,
            )
        }
    }
}
