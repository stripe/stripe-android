package com.stripe.android.paymentelement.embedded.form

import androidx.activity.result.ActivityResultCaller
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.stripe.android.common.model.asCommonConfiguration
import com.stripe.android.common.taptoadd.TapToAddHelper
import com.stripe.android.common.taptoadd.TapToAddResult
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.toConfirmationOption
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentsheet.analytics.EventReporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

internal interface FormActivityConfirmationHelper {
    fun confirm()
}

@FormActivityScope
internal class DefaultFormActivityConfirmationHelper @Inject constructor(
    private val paymentMethodMetadata: PaymentMethodMetadata,
    private val confirmationHandler: ConfirmationHandler,
    private val configuration: EmbeddedPaymentElement.Configuration,
    private val selectionHolder: EmbeddedSelectionHolder,
    private val stateHelper: FormActivityStateHelper,
    private val onClickDelegate: OnClickOverrideDelegate,
    private val eventReporter: EventReporter,
    private val tapToAddHelper: TapToAddHelper,
    lifecycleOwner: LifecycleOwner,
    activityResultCaller: ActivityResultCaller,
    @ViewModelScope private val coroutineScope: CoroutineScope,
    formActivityRegistrar: FormActivityRegistrar
) : FormActivityConfirmationHelper {

    init {
        formActivityRegistrar.registerAndBootstrap(
            activityResultCaller,
            lifecycleOwner,
            paymentMethodMetadata
        )

        lifecycleOwner.lifecycleScope.launch {
            tapToAddHelper.result.collect { result ->
                val formResult = when (result) {
                    is TapToAddResult.Canceled -> {
                        // Do nothing.
                        null
                    }
                    TapToAddResult.Complete -> {
                        FormResult.Complete(
                            selection = null,
                            hasBeenConfirmed = true,
                        )
                    }
                    is TapToAddResult.Continue -> {
                        FormResult.Complete(
                            selection = result.paymentSelection,
                            hasBeenConfirmed = false,
                        )
                    }
                }
                formResult?.let {
                    stateHelper.setResult(it)
                }
            }
        }

        lifecycleOwner.lifecycleScope.launch {
            confirmationHandler.state.collectLatest {
                stateHelper.updateConfirmationState(it)
            }
        }
    }

    override fun confirm() {
        if (onClickDelegate.onClickOverride != null) {
            onClickDelegate.onClickOverride?.invoke()
        } else {
            selectionHolder.selection.value?.let { paymentSelection ->
                eventReporter.onPressConfirmButton(paymentSelection)
            }

            when (configuration.formSheetAction) {
                EmbeddedPaymentElement.FormSheetAction.Continue -> {
                    stateHelper.setResult(
                        FormResult.Complete(selectionHolder.selection.value, false)
                    )
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
    }

    private fun confirmationArgs(): ConfirmationHandler.Args? {
        val confirmationOption = selectionHolder.selection.value?.toConfirmationOption(
            configuration = configuration.asCommonConfiguration(),
            linkConfiguration = paymentMethodMetadata.linkState?.configuration,
            cardFundingFilter = paymentMethodMetadata.cardFundingFilter
        ) ?: return null
        return ConfirmationHandler.Args(
            confirmationOption = confirmationOption,
            paymentMethodMetadata = paymentMethodMetadata,
        )
    }
}
