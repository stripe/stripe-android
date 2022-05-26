package com.stripe.android.paymentsheet.forms

import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.ui.core.elements.IdentifierSpec
import com.stripe.android.ui.core.forms.FormFieldEntry
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
    private val userRequestedReuse: Flow<PaymentSelection.CustomerRequestedSave>
) {
    /**
     * This flow does not emit any value until all form field values are complete, then it emits an
     * object representing all the complete, non-hidden fields.
     */
    fun filterFlow() = combine(
        currentFieldValueMap,
        hiddenIdentifiers,
        showingMandate,
        userRequestedReuse
    ) { idFieldSnapshotMap, hiddenIdentifiers, showingMandate, userRequestedReuse ->
        filterFlow(idFieldSnapshotMap, hiddenIdentifiers, showingMandate, userRequestedReuse)
    }

    private fun filterFlow(
        idFieldSnapshotMap: Map<IdentifierSpec, FormFieldEntry>,
        hiddenIdentifiers: List<IdentifierSpec>,
        showingMandate: Boolean,
        userRequestedReuse: PaymentSelection.CustomerRequestedSave
    ): FormFieldValues? {
        // This will run twice in a row when the save for future use state changes: once for the
        // saveController changing and once for the the hidden fields changing
        val hiddenFilteredFieldSnapshotMap = idFieldSnapshotMap.filter {
            !hiddenIdentifiers.contains(it.key)
        }

        val notcomplete = hiddenFilteredFieldSnapshotMap.values.filter { !it.isComplete }

        return FormFieldValues(
            hiddenFilteredFieldSnapshotMap,
            showingMandate,
            userRequestedReuse
        ).takeIf {
            hiddenFilteredFieldSnapshotMap.values.map { it.isComplete }
                .none { complete -> !complete }
        }
    }
}
