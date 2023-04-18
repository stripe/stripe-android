package com.stripe.android.ui.core

import androidx.annotation.RestrictTo
import com.stripe.android.ui.core.elements.CardBillingAddressElement
import com.stripe.android.ui.core.elements.LayoutSpec
import com.stripe.android.ui.core.forms.TransformSpecToElements
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.SectionElement
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flattenConcat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Controller that manages the user interaction with the payment method data collection form.
 * When all fields are reported as complete, [completeFormValues] emits the valid payment method.
 *
 * @param: formSpec A representation of the layout which is used to display the UI fields on screen.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class FormController @Inject constructor(
    formSpec: LayoutSpec,
    transformSpecToElement: TransformSpecToElements,
) {
    val elements: StateFlow<List<FormElement>> =
        MutableStateFlow(transformSpecToElement.transform(formSpec.items))

    private val cardBillingElement = elements.map { elementsList ->
        elementsList
            .filterIsInstance<SectionElement>()
            .flatMap { it.fields }
            .filterIsInstance<CardBillingAddressElement>()
            .firstOrNull()
    }

    /**
     * List of field identifiers which should not be visible.
     */
    val hiddenIdentifiers =
        cardBillingElement.map {
            it?.hiddenIdentifiers ?: flowOf(emptySet())
        }.flattenConcat()

    /**
     * Emits a map of the form values when the form content is valid, null otherwise.
     */
    val completeFormValues = combine(
        elements.map { elementsList ->
            combine(
                elementsList.map {
                    it.getFormFieldValueFlow()
                }
            ) {
                it.toList().flatten().toMap()
            }
        }.flattenConcat(),
        hiddenIdentifiers
    ) { elementsList, hiddenIdentifiers ->
        elementsList.filter { mapEntry ->
            !hiddenIdentifiers.contains(mapEntry.key)
        }
    }.map { map ->
        map.takeIf { it.values.all { entry -> entry.isComplete } }
    }

    /**
     * Emits a map of the form values that are complete, empty otherwise.
     */
    val formValues = combine(
        elements.map { elementsList ->
            combine(
                elementsList.map {
                    it.getFormFieldValueFlow()
                }
            ) {
                it.toList().flatten().toMap()
            }
        }.flattenConcat(),
        hiddenIdentifiers
    ) { elementsList, hiddenIdentifiers ->
        elementsList.filter { mapEntry ->
            !hiddenIdentifiers.contains(mapEntry.key)
        }
    }.map { map ->
        map.filter { it.value.isComplete }
    }

    private val textFieldControllerIdsFlow = elements.filterNotNull().map { elementsList ->
        combine(elementsList.map { it.getTextFieldIdentifiers() }) {
            it.toList().flatten()
        }
    }.flattenConcat()

    val lastTextFieldIdentifier = combine(
        hiddenIdentifiers,
        textFieldControllerIdsFlow
    ) { hiddenIds, textFieldControllerIds ->
        textFieldControllerIds.lastOrNull {
            !hiddenIds.contains(it)
        }
    }
}
