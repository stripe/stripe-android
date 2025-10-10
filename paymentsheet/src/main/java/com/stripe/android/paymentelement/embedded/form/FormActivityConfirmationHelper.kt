package com.stripe.android.paymentelement.embedded.form

import androidx.activity.result.ActivityResultCaller
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.stripe.android.common.model.asCommonConfiguration
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.toConfirmationOption
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

internal interface FormActivityConfirmationHelper {
    fun confirm(): FormResult?
}

@FormActivityScope
internal class DefaultFormActivityConfirmationHelper @Inject constructor(
    private val initializationMode: PaymentElementLoader.InitializationMode,
    private val paymentMethodMetadata: PaymentMethodMetadata,
    private val confirmationHandler: ConfirmationHandler,
    private val configuration: EmbeddedPaymentElement.Configuration,
    private val selectionHolder: EmbeddedSelectionHolder,
    private val stateHelper: FormActivityStateHelper,
    private val onClickDelegate: OnClickOverrideDelegate,
    private val eventReporter: EventReporter,
    lifecycleOwner: LifecycleOwner,
    activityResultCaller: ActivityResultCaller,
    @ViewModelScope private val coroutineScope: CoroutineScope,
    formActivityConfirmationHandlerRegistrar: FormActivityConfirmationHandlerRegistrar
) : FormActivityConfirmationHelper {

    init {
        formActivityConfirmationHandlerRegistrar.registerAndBootstrap(
            activityResultCaller,
            lifecycleOwner,
            paymentMethodMetadata
        )

        lifecycleOwner.lifecycleScope.launch {
            confirmationHandler.state.collectLatest {
                stateHelper.updateConfirmationState(it)
            }
        }
    }

    override fun confirm(): FormResult? {
        if (onClickDelegate.onClickOverride != null) {
            onClickDelegate.onClickOverride?.invoke()
        } else {
            selectionHolder.selection.value?.let { paymentSelection ->
                eventReporter.onPressConfirmButton(paymentSelection)
            }

            when (configuration.formSheetAction) {
                EmbeddedPaymentElement.FormSheetAction.Continue -> {
                    return FormResult.Complete(selectionHolder.selection.value, false)
                }
                EmbeddedPaymentElement.FormSheetAction.Confirm -> {
                    confirmationArgs()?.let { args ->
                        coroutineScope.launch {
                            confirmationHandler.start(args)
                        }
                    }
                }
            }
        }

        return null
    }

    private fun confirmationArgs(): ConfirmationHandler.Args? {
        val confirmationOption = selectionHolder.selection.value?.toConfirmationOption(
            configuration = configuration.asCommonConfiguration(),
            linkConfiguration = paymentMethodMetadata.linkState?.configuration,
            passiveCaptchaParams = paymentMethodMetadata.passiveCaptchaParams,
            clientAttributionMetadata = paymentMethodMetadata.clientAttributionMetadata,
        ) ?: return null
        return ConfirmationHandler.Args(
            intent = paymentMethodMetadata.stripeIntent,
            confirmationOption = confirmationOption,
            appearance = configuration.appearance,
            initializationMode = initializationMode,
            shippingDetails = paymentMethodMetadata.shippingDetails,
            ephemeralKeySecret = confirmationHandler.ephemeralKeySecret,
        )
    }
}
