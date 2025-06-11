package com.stripe.android.paymentelement.embedded

import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.paymentMethodType
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class EmbeddedSelectionHolder @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
) {
    val selection: StateFlow<PaymentSelection?> = savedStateHandle.getStateFlow(EMBEDDED_SELECTION_KEY, null)
    val temporarySelection: StateFlow<String?> = savedStateHandle.getStateFlow(EMBEDDED_TEMPORARY_SELECTION_KEY, null)
    val previousNewSelections: Bundle = savedStateHandle[EMBEDDED_PREVIOUS_SELECTIONS_KEY] ?: Bundle()

    fun set(updatedSelection: PaymentSelection?) {
        savedStateHandle[EMBEDDED_SELECTION_KEY] = updatedSelection

        if (updatedSelection != null && updatedSelection is PaymentSelection.New) {
            previousNewSelections.putParcelable(updatedSelection.paymentMethodType, updatedSelection)
        }
    }

    fun setTemporary(code: PaymentMethodCode?) {
        savedStateHandle[EMBEDDED_TEMPORARY_SELECTION_KEY] = code
    }

    fun setPreviousNewSelections(bundle: Bundle) {
        this.previousNewSelections.putAll(bundle)
    }

    fun getPreviousNewSelection(code: PaymentMethodCode): PaymentSelection.New? {
        @Suppress("DEPRECATION")
        return previousNewSelections.getParcelable(code) as PaymentSelection.New?
    }

    companion object {
        const val EMBEDDED_SELECTION_KEY = "EMBEDDED_SELECTION_KEY"
        const val EMBEDDED_TEMPORARY_SELECTION_KEY = "EMBEDDED_TEMPORARY_SELECTION_KEY"
        const val EMBEDDED_PREVIOUS_SELECTIONS_KEY = "EMBEDDED_PREVIOUS_SELECTIONS_KEY"
    }
}
