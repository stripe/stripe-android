package com.stripe.android.paymentelement.embedded.content

import androidx.activity.result.ActivityResultCaller
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.common.model.asCommonConfiguration
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.toConfirmationOption
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@ExperimentalEmbeddedPaymentElementApi
internal interface EmbeddedConfirmationMediator {
    val result: Flow<EmbeddedPaymentElement.Result>

    fun register(
        activityResultCaller: ActivityResultCaller,
        lifecycleOwner: LifecycleOwner
    )

    fun confirm(): Boolean
}

@ExperimentalEmbeddedPaymentElementApi
@Singleton
internal class DefaultEmbeddedConfirmationMediator @Inject constructor(
    private val confirmationHandler: ConfirmationHandler,
    private val confirmationStateHolder: EmbeddedConfirmationStateHolder,
    private val selectionHolder: EmbeddedSelectionHolder,
    @ViewModelScope private val coroutineScope: CoroutineScope,
) : EmbeddedConfirmationMediator {
    init {
        coroutineScope.launch {
            confirmationHandler.state.collect { state ->
                when (state) {
                    is ConfirmationHandler.State.Confirming,
                    is ConfirmationHandler.State.Idle -> Unit
                    is ConfirmationHandler.State.Complete -> {
                        _result.send(state.result.asEmbeddedResult())

                        if (state.result is ConfirmationHandler.Result.Succeeded) {
                            confirmationStateHolder.state = null
                            selectionHolder.set(null)
                        }
                    }
                }
            }
        }
    }

    private val _result = Channel<EmbeddedPaymentElement.Result>()
    override val result = _result.receiveAsFlow()

    override fun register(
        activityResultCaller: ActivityResultCaller,
        lifecycleOwner: LifecycleOwner,
    ) {
        confirmationHandler.register(
            activityResultCaller = activityResultCaller,
            lifecycleOwner = lifecycleOwner,
        )
    }

    override fun confirm(): Boolean {
        return confirmationArgs()?.let { confirmationArgs ->
            confirmationHandler.start(confirmationArgs)

            true
        } ?: false
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
