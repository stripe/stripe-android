package com.stripe.android.paymentelement.embedded.content

import androidx.activity.result.ActivityResultCaller
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.stripe.android.common.model.asCommonConfiguration
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.toConfirmationOption
import com.stripe.android.paymentelement.embedded.EmbeddedStateHelper
import kotlinx.coroutines.launch
import javax.inject.Inject

internal interface EmbeddedConfirmationHelper {
    fun confirm()
}

@ExperimentalEmbeddedPaymentElementApi
@EmbeddedPaymentElementScope
internal class DefaultEmbeddedConfirmationHelper @Inject constructor(
    private val confirmationStarter: EmbeddedConfirmationStarter,
    private val resultCallback: EmbeddedPaymentElement.ResultCallback,
    private val activityResultCaller: ActivityResultCaller,
    private val lifecycleOwner: LifecycleOwner,
    private val confirmationStateHolder: EmbeddedConfirmationStateHolder,
    private val stateHelper: EmbeddedStateHelper,
) : EmbeddedConfirmationHelper {
    init {
        confirmationStarter.register(
            activityResultCaller = activityResultCaller,
            lifecycleOwner = lifecycleOwner
        )

        lifecycleOwner.lifecycleScope.launch {
            confirmationStarter.result.collect { result ->
                resultCallback.onResult(result.asEmbeddedResult())

                if (result is ConfirmationHandler.Result.Succeeded) {
                    stateHelper.state = null
                }
            }
        }
    }

    override fun confirm() {
        confirmationArgs()?.let { confirmationArgs ->
            confirmationStarter.start(confirmationArgs)
        } ?: run {
            resultCallback.onResult(
                EmbeddedPaymentElement.Result.Failed(IllegalStateException("Not in a state that's confirmable."))
            )
        }
    }

    private fun confirmationArgs(): ConfirmationHandler.Args? {
        val confirmationState = confirmationStateHolder.state ?: return null
        val confirmationOption = confirmationState.selection?.toConfirmationOption(
            configuration = confirmationState.configuration.asCommonConfiguration(),
            linkConfiguration = confirmationState.paymentMethodMetadata.linkState?.configuration,
        ) ?: return null

        return ConfirmationHandler.Args(
            intent = confirmationState.paymentMethodMetadata.stripeIntent,
            confirmationOption = confirmationOption,
            initializationMode = confirmationState.initializationMode,
            appearance = confirmationState.configuration.appearance,
            shippingDetails = confirmationState.configuration.shippingDetails,
        )
    }
}

@ExperimentalEmbeddedPaymentElementApi
private fun ConfirmationHandler.Result.asEmbeddedResult(): EmbeddedPaymentElement.Result = when (this) {
    is ConfirmationHandler.Result.Canceled -> {
        EmbeddedPaymentElement.Result.Canceled()
    }
    is ConfirmationHandler.Result.Failed -> {
        EmbeddedPaymentElement.Result.Failed(cause)
    }
    is ConfirmationHandler.Result.Succeeded -> {
        EmbeddedPaymentElement.Result.Completed()
    }
}
