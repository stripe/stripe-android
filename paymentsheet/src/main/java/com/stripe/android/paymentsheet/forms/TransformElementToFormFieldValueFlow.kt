package com.stripe.android.paymentsheet.forms

import com.stripe.android.paymentsheet.elements.common.Controller
import com.stripe.android.paymentsheet.elements.common.FormElement
import com.stripe.android.paymentsheet.specification.IdentifierSpec
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

internal class TransformElementToFormFieldValueFlow(
    val elements: List<FormElement>,
    val optionalIdentifiers: Flow<List<IdentifierSpec>>
) {

    // This maps the field type to the controller
    private val idControllerMap = elements
        .filter { it.controller != null }
        .associate { Pair(it.identifier, it.controller!!) }

    private val currentFieldValueMap = combine(
        getCurrentFieldValuePairs(idControllerMap)
    ) {
        it.toMap()
    }

    // This is null if any form field values are incomplete, otherwise it is an object
    // representing all the complete fields
    fun transformFlow() = combine(
        currentFieldValueMap,
        optionalIdentifiers
    ) { idFieldSnapshotMap, optionalIdentifiers ->
        transform(idFieldSnapshotMap, optionalIdentifiers)
    }

    private fun transform(
        idFieldSnapshotMap: Map<IdentifierSpec, FieldSnapshot>,
        optionalIdentifiers: List<IdentifierSpec>
    ): FormFieldValues? {
        // This will hit twice in a row when the save for future use state changes: once for the
        // saveController changing and once for the the optionalFields changing
        val optionalFilteredFieldSnapshotMap = idFieldSnapshotMap.filter {
            !optionalIdentifiers.contains(it.key)
        }

        return FormFieldValues(
            optionalFilteredFieldSnapshotMap.mapValues {
                it.value.fieldValue
            }
        ).takeIf {
            optionalFilteredFieldSnapshotMap.values.map { it.isComplete }
                .none { complete -> !complete }
        }
    }

    private fun getCurrentFieldValuePairs(idControllerMap: Map<IdentifierSpec, Controller>) =
        idControllerMap.map { fieldControllerEntry ->
            getCurrentFieldValuePair(fieldControllerEntry.key, fieldControllerEntry.value)
        }

    private fun getCurrentFieldValuePair(
        field: IdentifierSpec,
        value: Controller
    ) = combine(value.fieldValue, value.isComplete) { fieldValue, isComplete ->
        Pair(field, FieldSnapshot(fieldValue, field, isComplete))
    }
}