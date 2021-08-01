package com.stripe.android.paymentsheet.forms

import com.stripe.android.paymentsheet.elements.InputController
import com.stripe.android.paymentsheet.specifications.IdentifierSpec
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * This class will take a list of form elements and hidden identifiers.
 * [transformFlow] is the only public method and it will transform
 * the list of form elements into a [FormFieldValues].
 */
internal class TransformElementToFormFieldValueFlow(
    idControllerMap: Map<IdentifierSpec, InputController>,
    private val hiddenIdentifiers: Flow<List<IdentifierSpec>>,
    val showingMandate: Flow<Boolean>,
    val saveForFutureUse: Flow<Boolean>
) {
    private val currentFieldValueMap = combine(
        getCurrentFieldValuePairs(idControllerMap)
    ) {
        it.toMap()
    }

    /**
     * This will return null if any form field values are incomplete, otherwise it is an object
     * representing all the complete, non-hidden fields.
     */
    fun transformFlow() = combine(
        currentFieldValueMap,
        hiddenIdentifiers,
        showingMandate,
        saveForFutureUse
    ) { idFieldSnapshotMap, hiddenIdentifiers, showingMandate, saveForFutureUse ->
        transform(idFieldSnapshotMap, hiddenIdentifiers, showingMandate, saveForFutureUse)
    }

    private fun transform(
        idFieldSnapshotMap: Map<IdentifierSpec, FormFieldEntry>,
        hiddenIdentifiers: List<IdentifierSpec>,
        showingMandate: Boolean,
        saveForFutureUse: Boolean
    ): FormFieldValues? {
        // This will run twice in a row when the save for future use state changes: once for the
        // saveController changing and once for the the hidden fields changing
        val hiddenFilteredFieldSnapshotMap = idFieldSnapshotMap.filter {
            !hiddenIdentifiers.contains(it.key)
        }

        return FormFieldValues(
            hiddenFilteredFieldSnapshotMap,
            showingMandate,
            saveForFutureUse
        ).takeIf {
            hiddenFilteredFieldSnapshotMap.values.map { it.isComplete }
                .none { complete -> !complete }
        }
    }

    private fun getCurrentFieldValuePairs(idControllerMap: Map<IdentifierSpec, InputController>) =
        idControllerMap.map { fieldControllerEntry ->
            getCurrentFieldValuePair(fieldControllerEntry.key, fieldControllerEntry.value)
        }

    private fun getCurrentFieldValuePair(
        identifier: IdentifierSpec,
        controller: InputController
    ) = combine(controller.rawFieldValue, controller.isComplete) { rawFieldValue, isComplete ->
        Pair(
            identifier,
            FormFieldEntry(
                value = rawFieldValue,
                isComplete = isComplete,
            )
        )
    }
}
