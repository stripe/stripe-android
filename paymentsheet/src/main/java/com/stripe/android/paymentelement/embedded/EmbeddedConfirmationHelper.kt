package com.stripe.android.paymentelement.embedded

import androidx.activity.result.ActivityResultCaller
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.stripe.android.common.model.asCommonConfiguration
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.toConfirmationOption
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import kotlinx.coroutines.launch

@ExperimentalEmbeddedPaymentElementApi
internal class EmbeddedConfirmationHelper(
    private val confirmationHandler: ConfirmationHandler,
    private val resultCallback: EmbeddedPaymentElement.ResultCallback,
    private val activityResultCaller: ActivityResultCaller,
    private val lifecycleOwner: LifecycleOwner,
    private val confirmationStateSupplier: () -> State?,
) {
    init {
        confirmationHandler.register(
            activityResultCaller = activityResultCaller,
            lifecycleOwner = lifecycleOwner
        )

        lifecycleOwner.lifecycleScope.launch {
            confirmationHandler.state.collect { state ->
                when (state) {
                    is ConfirmationHandler.State.Complete -> {
                        resultCallback.onResult(state.result.asEmbeddedResult())
                    }
                    is ConfirmationHandler.State.Confirming, ConfirmationHandler.State.Idle -> Unit
                }
            }
        }
    }

    fun confirm() {
        confirmationArgs()?.let { confirmationArgs ->
            confirmationHandler.start(confirmationArgs)
        } ?: run {
            resultCallback.onResult(
                EmbeddedPaymentElement.Result.Failed(IllegalStateException("Not in a state that's confirmable."))
            )
        }
    }

    private fun confirmationArgs(): ConfirmationHandler.Args? {
        val loadedState = confirmationStateSupplier() ?: return null
        val confirmationOption = loadedState.selection?.toConfirmationOption(
            initializationMode = loadedState.initializationMode,
            configuration = loadedState.configuration.asCommonConfiguration(),
            appearance = loadedState.configuration.appearance,
        ) ?: return null

        return ConfirmationHandler.Args(
            intent = loadedState.paymentMethodMetadata.stripeIntent,
            confirmationOption = confirmationOption,
        )
    }

    data class State(
        val paymentMethodMetadata: PaymentMethodMetadata,
        val selection: PaymentSelection?,
        val initializationMode: PaymentElementLoader.InitializationMode,
        val configuration: EmbeddedPaymentElement.Configuration,
    )
}
