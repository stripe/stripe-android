package com.stripe.android.paymentelement.embedded

import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentsheet.model.PaymentSelection
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DefaultEmbeddedSelectionHolder @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
) : EmbeddedSelectionHolder {
    override val selection: StateFlow<PaymentSelection?> =
        savedStateHandle.getStateFlow(EMBEDDED_SELECTION_KEY, null)
    override val temporarySelection: StateFlow<String?> =
        savedStateHandle.getStateFlow(EMBEDDED_TEMPORARY_SELECTION_KEY, null)
    override val previousNewSelections: Bundle = savedStateHandle[EMBEDDED_PREVIOUS_SELECTIONS_KEY]
        ?: Bundle().also {
            savedStateHandle[EMBEDDED_PREVIOUS_SELECTIONS_KEY] = it
        }

    override fun set(updatedSelection: PaymentSelection?) {
        savedStateHandle[EMBEDDED_SELECTION_KEY] = updatedSelection
        previousNewSelections.stashNewSelection(updatedSelection)
        savedStateHandle[EMBEDDED_PREVIOUS_SELECTIONS_KEY] = previousNewSelections
    }

    override fun setTemporary(code: PaymentMethodCode?) {
        savedStateHandle[EMBEDDED_TEMPORARY_SELECTION_KEY] = code
    }

    override fun setPreviousNewSelections(bundle: Bundle) {
        this.previousNewSelections.putAll(bundle)
        savedStateHandle[EMBEDDED_PREVIOUS_SELECTIONS_KEY] = previousNewSelections
    }

    override fun getPreviousNewSelection(code: PaymentMethodCode): PaymentSelection.New? {
        return previousNewSelections.previousNewSelection(code)
    }

    companion object {
        const val EMBEDDED_SELECTION_KEY = "EMBEDDED_SELECTION_KEY"
        const val EMBEDDED_TEMPORARY_SELECTION_KEY = "EMBEDDED_TEMPORARY_SELECTION_KEY"
        const val EMBEDDED_PREVIOUS_SELECTIONS_KEY = "EMBEDDED_PREVIOUS_SELECTIONS_KEY"
    }
}
