package com.stripe.android.paymentelement.embedded

import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentelement.embedded.content.DefaultPaymentOptionDisplayDataHolder
import com.stripe.android.paymentsheet.model.PaymentSelection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

internal typealias InternalRowSelectionCallback = () -> Unit

@Singleton
internal interface EmbeddedRowSelectionImmediateActionHandler {
    fun handleImmediateRowSelectionCallback(oldSelection: PaymentSelection?, newSelection: PaymentSelection?)
}

@OptIn(ExperimentalEmbeddedPaymentElementApi::class)
@Singleton
internal class DefaultEmbeddedRowSelectionImmediateActionHandler @Inject constructor(
    @ViewModelScope private val coroutineScope: CoroutineScope,
    private val internalRowSelectionCallback: Provider<InternalRowSelectionCallback?>,
    private val paymentOptionDisplayDataHolder: DefaultPaymentOptionDisplayDataHolder,
) : EmbeddedRowSelectionImmediateActionHandler {

    override fun handleImmediateRowSelectionCallback(oldSelection: PaymentSelection?, newSelection: PaymentSelection?) {
        internalRowSelectionCallback.get()?.let { callback ->
            newSelection?.let {
                if (oldSelection != null && oldSelection == newSelection) {
                    callback.invoke()
                } else {
                    coroutineScope.launch {
                        paymentOptionDisplayDataHolder.paymentOption.firstOrNull()?.let {
                            callback.invoke()
                        }
                    }
                }
            }
        }
    }
}
