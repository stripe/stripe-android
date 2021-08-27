package com.stripe.android.paymentsheet.forms

import com.stripe.android.paymentsheet.elements.IdentifierSpec
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * This class will take a list of form elements and hidden identifiers.
 * [filterFlow] is the only public method and it will transform
 * the list of form elements into a [FormFieldValues].
 */
internal class CompleteFormFieldValueFilter(
    private val currentFieldValueMap: Flow<Map<IdentifierSpec, FormFieldEntry>>,
    private val hiddenIdentifiers: Flow<List<IdentifierSpec>>,
    val showingMandate: Flow<Boolean>,
    val saveForFutureUse: Flow<Boolean>
) {
    /**
     * This will return null if any form field values are incomplete, otherwise it is an object
     * representing all the complete, non-hidden fields.
     */
    fun filterFlow() = combine(
        currentFieldValueMap,
        hiddenIdentifiers,
        showingMandate
    ) { idFieldSnapshotMap, hiddenIdentifiers, showingMandate ->
        filterFlow(idFieldSnapshotMap, hiddenIdentifiers, showingMandate)
    }

    private fun filterFlow(
        idFieldSnapshotMap: Map<IdentifierSpec, FormFieldEntry>,
        hiddenIdentifiers: List<IdentifierSpec>,
        showingMandate: Boolean,
    ): FormFieldValues? {
        // This will run twice in a row when the save for future use state changes: once for the
        // saveController changing and once for the the hidden fields changing
        val hiddenFilteredFieldSnapshotMap = idFieldSnapshotMap.filter {
            !hiddenIdentifiers.contains(it.key)
        }

        return FormFieldValues(
            hiddenFilteredFieldSnapshotMap,
            showingMandate
        ).takeIf {
            hiddenFilteredFieldSnapshotMap.values.map { it.isComplete }
                .none { complete -> !complete }
        }
    }
}
