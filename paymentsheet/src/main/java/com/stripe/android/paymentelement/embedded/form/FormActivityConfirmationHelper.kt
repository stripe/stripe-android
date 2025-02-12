package com.stripe.android.paymentelement.embedded.form

import androidx.activity.result.ActivityResultCaller
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.stripe.android.common.model.asCommonConfiguration
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.toConfirmationOption
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

internal interface FormActivityConfirmationHelper {
    fun confirm()
}

@OptIn(ExperimentalEmbeddedPaymentElementApi::class)
@FormActivityScope
internal class DefaultFormActivityConfirmationHelper @Inject constructor(
    private val initializationMode: PaymentElementLoader.InitializationMode,
    private val paymentMethodMetadata: PaymentMethodMetadata,
    private val confirmationHandler: ConfirmationHandler,
    private val configuration: EmbeddedPaymentElement.Configuration,
    private val selectionHolder: EmbeddedSelectionHolder,
    private val stateHelper: FormActivityStateHelper,
    lifecycleOwner: LifecycleOwner,
    activityResultCaller: ActivityResultCaller
) : FormActivityConfirmationHelper {

    init {
        confirmationHandler.register(activityResultCaller, lifecycleOwner)
        lifecycleOwner.lifecycleScope.launch {
            confirmationHandler.state.collectLatest {
                stateHelper.updateConfirmationState(it)
            }
        }
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
