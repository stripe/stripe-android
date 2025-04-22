package com.stripe.android.paymentelement.embedded

import androidx.lifecycle.SavedStateHandle
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentsheet.model.PaymentSelection
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class EmbeddedSelectionHolder @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
) {
    val selection: StateFlow<PaymentSelection?> = savedStateHandle.getStateFlow(EMBEDDED_SELECTION_KEY, null)
    val temporarySelection: StateFlow<String?> = savedStateHandle.getStateFlow(EMBEDDED_TEMPORARY_SELECTION_KEY, null)

    fun set(updatedSelection: PaymentSelection?) {
        savedStateHandle[EMBEDDED_SELECTION_KEY] = updatedSelection
    }

    fun setTemporary(code: PaymentMethodCode?) {
        savedStateHandle[EMBEDDED_TEMPORARY_SELECTION_KEY] = code
    }

    companion object {
        const val EMBEDDED_SELECTION_KEY = "EMBEDDED_SELECTION_KEY"
        const val EMBEDDED_TEMPORARY_SELECTION_KEY = "EMBEDDED_TEMPORARY_SELECTION_KEY"
    }
}
