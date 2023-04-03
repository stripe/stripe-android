package com.stripe.android.paymentsheet.forms

import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.forms.FormFieldEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * This class will take a list of form elements, hidden identifiers and default values.
 * [filterFlow] is the only public method and it will transform
 * the list of form elements into a [FormFieldValues].
 */
internal class CompleteFormFieldValueFilter(
    private val currentFieldValueMap: Flow<Map<IdentifierSpec, FormFieldEntry>>,
    private val hiddenIdentifiers: Flow<Set<IdentifierSpec>>,
    val showingMandate: Flow<Boolean>,
    private val userRequestedReuse: Flow<PaymentSelection.CustomerRequestedSave>,
    private val defaultValues: Map<IdentifierSpec, String>,
) {
    /**
     * This flow does not emit any value until all form field values are complete, then it emits an
     * object representing all the complete, non-hidden fields.
     */
    fun filterFlow() = combine(
        currentFieldValueMap,
        hiddenIdentifiers,
        showingMandate,
        userRequestedReuse,
    ) { idFieldSnapshotMap, hiddenIdentifiers, showingMandate, userRequestedReuse ->
        filterFlow(
            idFieldSnapshotMap,
            hiddenIdentifiers,
            showingMandate,
            userRequestedReuse,
            defaultValues,
        )
    }

    private fun filterFlow(
        idFieldSnapshotMap: Map<IdentifierSpec, FormFieldEntry>,
        hiddenIdentifiers: Set<IdentifierSpec>,
        showingMandate: Boolean,
        userRequestedReuse: PaymentSelection.CustomerRequestedSave,
        defaultValues: Map<IdentifierSpec, String>,
    ): FormFieldValues? {
        // This will run twice in a row when the save for future use state changes: once for the
        // saveController changing and once for the the hidden fields changing
        var processedFieldsMap = idFieldSnapshotMap.filter {
            !hiddenIdentifiers.contains(it.key)
        }.toMutableMap()

        // Apply defaults for fields with no value.
        // Default values are added even if the field is hidden.
        for (entry in defaultValues) {
            val formValue = processedFieldsMap[entry.key]
            if (formValue?.value.isNullOrBlank() && !entry.value.isNullOrBlank()) {
                processedFieldsMap[entry.key] = FormFieldEntry(entry.value, true)
            }
        }

        return FormFieldValues(
            processedFieldsMap,
            showingMandate,
            userRequestedReuse
        ).takeIf {
            processedFieldsMap.values.map { it.isComplete }
                .none { complete -> !complete }
        }
    }
}
