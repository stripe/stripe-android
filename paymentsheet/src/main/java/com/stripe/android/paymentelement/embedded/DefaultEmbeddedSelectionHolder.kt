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
    init {
        persistPreviousNewSelections(readPreviousNewSelections())
    }

    override val selection: StateFlow<PaymentSelection?> =
        savedStateHandle.getStateFlow(EMBEDDED_SELECTION_KEY, null)
    override val temporarySelection: StateFlow<String?> =
        savedStateHandle.getStateFlow(EMBEDDED_TEMPORARY_SELECTION_KEY, null)
    override val previousNewSelections: PreviousNewSelections
        get() = readPreviousNewSelections()

    override fun setSelection(updatedSelection: PaymentSelection?) {
        savedStateHandle[EMBEDDED_SELECTION_KEY] = updatedSelection
        persistPreviousNewSelections(previousNewSelections.updatedWith(updatedSelection))
    }

    override fun setTemporarySelection(code: PaymentMethodCode?) {
        savedStateHandle[EMBEDDED_TEMPORARY_SELECTION_KEY] = code
    }

    override fun setPreviousNewSelections(selections: PreviousNewSelections) {
        persistPreviousNewSelections(previousNewSelections.mergedWith(selections))
    }

    override fun clearPreviousNewSelections() {
        persistPreviousNewSelections(PreviousNewSelections.empty)
    }

    override fun getPreviousNewSelection(code: PaymentMethodCode): PaymentSelection.New? {
        return previousNewSelections[code]
    }

    private fun readPreviousNewSelections(): PreviousNewSelections {
        return when (val savedSelections = savedStateHandle.get<Any?>(EMBEDDED_PREVIOUS_SELECTIONS_KEY)) {
            is Bundle -> PreviousNewSelections.fromBundle(savedSelections)
            is PreviousNewSelections -> savedSelections
            else -> PreviousNewSelections.empty
        }
    }

    private fun persistPreviousNewSelections(selections: PreviousNewSelections) {
        savedStateHandle[EMBEDDED_PREVIOUS_SELECTIONS_KEY] = selections.toBundle()
    }

    companion object {
        const val EMBEDDED_SELECTION_KEY = "EMBEDDED_SELECTION_KEY"
        const val EMBEDDED_TEMPORARY_SELECTION_KEY = "EMBEDDED_TEMPORARY_SELECTION_KEY"
        const val EMBEDDED_PREVIOUS_SELECTIONS_KEY = "EMBEDDED_PREVIOUS_SELECTIONS_KEY"
    }
}
