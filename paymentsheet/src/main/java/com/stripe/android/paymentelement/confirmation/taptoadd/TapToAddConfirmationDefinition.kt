package com.stripe.android.paymentelement.confirmation.taptoadd

import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import com.stripe.android.common.taptoadd.TapToAddContract
import com.stripe.android.common.taptoadd.TapToAddResult
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackIdentifier
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.MutableConfirmationMetadata
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import javax.inject.Inject
import javax.inject.Named

internal class TapToAddConfirmationDefinition @Inject constructor(
    @PaymentElementCallbackIdentifier private val paymentElementCallbackIdentifier: String,
    @Named(PRODUCT_USAGE) private val productUsage: Set<String>
) :
    ConfirmationDefinition<
        TapToAddConfirmationOption,
        ActivityResultLauncher<TapToAddContract.Args>,
        TapToAddContract.Args,
        TapToAddResult,
    > {
    override val key: String = "TapToAdd"

    override fun option(confirmationOption: ConfirmationHandler.Option): TapToAddConfirmationOption? {
        return confirmationOption as? TapToAddConfirmationOption
    }

    override fun canConfirm(
        confirmationOption: TapToAddConfirmationOption,
        confirmationArgs: ConfirmationHandler.Args
    ): Boolean {
        return confirmationArgs.paymentMethodMetadata.isTapToAddSupported
    }

    override suspend fun action(
        confirmationOption: TapToAddConfirmationOption,
        confirmationArgs: ConfirmationHandler.Args
    ): ConfirmationDefinition.Action<TapToAddContract.Args> {
        return ConfirmationDefinition.Action.Launch(
            launcherArguments = TapToAddContract.Args(
                paymentMethodMetadata = confirmationArgs.paymentMethodMetadata,
                paymentElementCallbackIdentifier = paymentElementCallbackIdentifier,
                productUsage = productUsage,
                tapToAddMode = confirmationOption.mode,
            ),
            receivesResultInProcess = false,
        )
    }

    override fun launch(
        launcher: ActivityResultLauncher<TapToAddContract.Args>,
        arguments: TapToAddContract.Args,
        confirmationOption: TapToAddConfirmationOption,
        confirmationArgs: ConfirmationHandler.Args
    ) {
        launcher.launch(arguments)
    }

    override fun createLauncher(
        activityResultCaller: ActivityResultCaller,
        onResult: (TapToAddResult) -> Unit
    ): ActivityResultLauncher<TapToAddContract.Args> {
        return activityResultCaller.registerForActivityResult(
            contract = TapToAddContract,
            callback = onResult,
        )
    }

    override fun toResult(
        confirmationOption: TapToAddConfirmationOption,
        confirmationArgs: ConfirmationHandler.Args,
        launcherArgs: TapToAddContract.Args,
        result: TapToAddResult
    ): ConfirmationDefinition.Result {
        return when (result) {
            is TapToAddResult.Complete -> {
                ConfirmationDefinition.Result.Succeeded(
                    intent = result.intent,
                )
            }
            is TapToAddResult.Canceled -> {
                ConfirmationDefinition.Result.Canceled(
                    action = ConfirmationHandler.Result.Canceled.Action.None,
                    metadata = MutableConfirmationMetadata().apply {
                        result.paymentMethod?.let {
                            set(TapToAddCollectedPaymentMethodMetadataKey, it)
                        }
                    }
                )
            }
        }
    }
}
