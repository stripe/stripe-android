package com.stripe.android.paymentelement.embedded.form

import androidx.activity.result.ActivityResultCaller
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.common.model.asCommonConfiguration
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.toConfirmationOption
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

internal interface FormActivityConfirmationHandler {
    fun confirm()
    fun register(activityResultCaller: ActivityResultCaller, lifecycleOwner: LifecycleOwner)
}

@OptIn(ExperimentalEmbeddedPaymentElementApi::class)
internal class DefaultFormActivityConfirmationHandler @Inject constructor(
    private val initializationMode: PaymentElementLoader.InitializationMode,
    private val paymentMethodMetadata: PaymentMethodMetadata,
    @ViewModelScope private val coroutineScope: CoroutineScope,
    confirmationHandlerFactory: ConfirmationHandler.Factory,
    private val configuration: EmbeddedPaymentElement.Configuration,
    private val selectionHolder: EmbeddedSelectionHolder,
    private val uiStateHolder: FormActivityUiStateHolder,
) : FormActivityConfirmationHandler {

    private val confirmationHandler = confirmationHandlerFactory.create(coroutineScope)

    init {
        coroutineScope.launch {
            confirmationHandler.state.collectLatest {
                uiStateHolder.updateProcessingState(it)
            }
        }
    }

    override fun register(activityResultCaller: ActivityResultCaller, lifecycleOwner: LifecycleOwner) {
        confirmationHandler.register(activityResultCaller, lifecycleOwner)
    }

    override fun confirm() {
        confirmationArgs()?.let { args ->
            confirmationHandler.start(args)
        }
    }

    private fun confirmationArgs(): ConfirmationHandler.Args? {
        val confirmationOption = selectionHolder.selection.value?.toConfirmationOption(
            configuration = configuration.asCommonConfiguration(),
            linkConfiguration = paymentMethodMetadata.linkState?.configuration
        ) ?: return null
        return ConfirmationHandler.Args(
            intent = paymentMethodMetadata.stripeIntent,
            confirmationOption = confirmationOption,
            appearance = configuration.appearance,
            initializationMode = initializationMode,
            shippingDetails = paymentMethodMetadata.shippingDetails
        )
    }
}
