@file:OptIn(ExperimentalEmbeddedPaymentElementApi::class)

package com.stripe.android.paymentelement.embedded.content

import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.core.mainthread.MainThreadOnlyMutableStateFlow
import com.stripe.android.paymentelement.EmbeddedPaymentElement.PaymentOptionDisplayData
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
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
) : PaymentOptionDisplayDataHolder {
    private val _paymentOption = MainThreadOnlyMutableStateFlow<PaymentOptionDisplayData?>(null)
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
                }
            }
        }
    }
}
