package com.stripe.android.paymentelement.embedded

import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.paymentsheet.model.PaymentSelection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

internal typealias InternalRowSelectionCallback = () -> Unit

@Singleton
internal interface EmbeddedRowSelectionImmediateActionHandler {
    fun handleImmediateRowSelectionCallback(oldSelection: PaymentSelection?, newSelection: PaymentSelection?)
}

@Singleton
internal class DefaultEmbeddedRowSelectionImmediateActionHandler @Inject constructor(
    @ViewModelScope private val coroutineScope: CoroutineScope,
    private val internalRowSelectionCallback: Provider<InternalRowSelectionCallback?>,
) : EmbeddedRowSelectionImmediateActionHandler {

    /**
     * This callback will always be invoked after [DefaultPaymentOptionDisplayDataHolder.paymentOption] updates.
     * The update paymentOption coroutine is always enqueued first because it is always older, it is launched on init.
     */
    override fun handleImmediateRowSelectionCallback(oldSelection: PaymentSelection?, newSelection: PaymentSelection?) {
        internalRowSelectionCallback.get()?.let { callback -> coroutineScope.launch { callback.invoke() } }
    }
}
