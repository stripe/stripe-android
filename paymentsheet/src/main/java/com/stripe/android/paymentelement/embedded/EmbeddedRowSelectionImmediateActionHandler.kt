package com.stripe.android.paymentelement.embedded

import com.stripe.android.core.injection.ViewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider

internal typealias InternalRowSelectionCallback = () -> Unit

internal fun interface EmbeddedRowSelectionImmediateActionHandler {
    fun invoke()
}

internal class DefaultEmbeddedRowSelectionImmediateActionHandler @Inject constructor(
    @ViewModelScope private val coroutineScope: CoroutineScope,
    private val internalRowSelectionCallback: Provider<InternalRowSelectionCallback?>,
) : EmbeddedRowSelectionImmediateActionHandler {

    /**
     * This callback will always be invoked after [DefaultPaymentOptionDisplayDataHolder.paymentOption] updates.
     * The update paymentOption coroutine is always enqueued first because it is always older, it is launched on init.
     */
    override fun invoke() {
        internalRowSelectionCallback.get()?.let { callback -> coroutineScope.launch { callback.invoke() } }
    }
}
