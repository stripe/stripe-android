@file:OptIn(ExperimentalEmbeddedPaymentElementApi::class)

package com.stripe.android.paymentelement.embedded.content

import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.EmbeddedPaymentElement.PaymentOptionDisplayData
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentelement.InternalRowSelectionCallback
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentsheet.analytics.code
import com.stripe.android.paymentsheet.model.PaymentSelection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

internal interface PaymentOptionDisplayDataHolder {
    val paymentOption: StateFlow<PaymentOptionDisplayData?>
}

@Singleton
internal class DefaultPaymentOptionDisplayDataHolder @Inject constructor(
    @ViewModelScope private val coroutineScope: CoroutineScope,
    private val selectionHolder: EmbeddedSelectionHolder,
    private val confirmationStateSupplier: () -> EmbeddedConfirmationStateHolder.State?,
    private val paymentOptionDisplayDataFactory: PaymentOptionDisplayDataFactory,
    private val internalRowSelectionCallback: Provider<InternalRowSelectionCallback?>,
) : PaymentOptionDisplayDataHolder {
    private val _paymentOption = MutableStateFlow<PaymentOptionDisplayData?>(null)
    override val paymentOption: StateFlow<PaymentOptionDisplayData?> = _paymentOption.asStateFlow()

    init {
        coroutineScope.launch {
            selectionHolder.selection.collect { selection ->
                val state = confirmationStateSupplier()
                if (state == null) {
                    _paymentOption.value = null
                } else {
                    _paymentOption.value = paymentOptionDisplayDataFactory.create(
                        selection = selection,
                        paymentMethodMetadata = state.paymentMethodMetadata,
                    )
                    handleImmediateRowActionCallback(
                        selection = selection,
                        state = state,
                    )
                }
            }
        }
    }

    private fun handleImmediateRowActionCallback(
        selection: PaymentSelection?,
        state: EmbeddedConfirmationStateHolder.State
    ) {
        internalRowSelectionCallback.get()?.let { callback ->
            selection?.code()?.let { code ->
                val hasForms = state.paymentMethodMetadata.formElementsForCode(
                    code = code,
                    uiDefinitionFactoryArgumentsFactory = NullUiDefinitionFactoryHelper.nullEmbeddedUiDefinitionFactory
                )?.isNotEmpty() ?: false

                if (
                    !hasForms ||
                    state.configuration.formSheetAction == EmbeddedPaymentElement.FormSheetAction.Continue
                ) {
                    callback.invoke()
                }
            }
        }
    }
}
