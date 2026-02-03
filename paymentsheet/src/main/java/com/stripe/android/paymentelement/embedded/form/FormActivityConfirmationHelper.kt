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
import com.stripe.android.paymentsheet.model.PaymentSelection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

internal interface FormActivityConfirmationHelper {
    val result: SharedFlow<FormResult>

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
    lifecycleOwner: LifecycleOwner,
    activityResultCaller: ActivityResultCaller,
    @ViewModelScope private val coroutineScope: CoroutineScope,
    formActivityConfirmationHandlerRegistrar: FormActivityConfirmationHandlerRegistrar
) : FormActivityConfirmationHelper {
    private val _result = MutableSharedFlow<FormResult>()
    override val result: SharedFlow<FormResult> = _result.asSharedFlow()

    init {
        formActivityConfirmationHandlerRegistrar.registerAndBootstrap(
            activityResultCaller,
            lifecycleOwner,
            paymentMethodMetadata
        )

        lifecycleOwner.lifecycleScope.launch {
            confirmationHandler.state.collectLatest { state ->
                if (
                    configuration.formSheetAction == EmbeddedPaymentElement.FormSheetAction.Continue &&
                    state is ConfirmationHandler.State.Complete &&
                    state.result is ConfirmationHandler.Result.Succeeded
                ) {
                    _result.emit(
                        FormResult.Complete(
                            selection = state.result.intent.paymentMethod?.let {
                                PaymentSelection.Saved(it)
                            },
                            hasBeenConfirmed = false
                        )
                    )
                } else {
                    stateHelper.updateConfirmationState(state)
                }
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
                    coroutineScope.launch {
                        _result.emit(
                            FormResult.Complete(selectionHolder.selection.value, false)
                        )
                    }
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
