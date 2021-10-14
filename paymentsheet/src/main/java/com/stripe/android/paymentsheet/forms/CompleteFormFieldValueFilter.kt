package com.stripe.android.paymentsheet.forms

import com.stripe.android.paymentsheet.elements.IdentifierSpec
import com.stripe.android.paymentsheet.model.PaymentSelection
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
    private val userRequestedReuse: Flow<PaymentSelection.UserReuseRequest>
) {
    /**
     * This will return null if any form field values are incomplete, otherwise it is an object
     * representing all the complete, non-hidden fields.
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
        userRequestedReuse: PaymentSelection.UserReuseRequest
    ): FormFieldValues? {
        // This will run twice in a row when the save for future use state changes: once for the
        // saveController changing and once for the the hidden fields changing
        val hiddenFilteredFieldSnapshotMap = idFieldSnapshotMap.filter {
            !hiddenIdentifiers.contains(it.key)
        }

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
