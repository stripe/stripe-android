package com.stripe.android.paymentelement.embedded

import androidx.lifecycle.SavedStateHandle
import com.stripe.android.paymentsheet.model.PaymentSelection
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class EmbeddedSelectionHolder @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
) {
    val selection: StateFlow<PaymentSelection?> = savedStateHandle.getStateFlow(EMBEDDED_SELECTION_KEY, null)

    fun set(updatedSelection: PaymentSelection?) {
        savedStateHandle[EMBEDDED_SELECTION_KEY] = updatedSelection
    }

    companion object {
        const val EMBEDDED_SELECTION_KEY = "EMBEDDED_SELECTION_KEY"
    }
}
