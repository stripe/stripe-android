package com.stripe.android.ui.core

import androidx.annotation.RestrictTo
import com.stripe.android.ui.core.elements.CardBillingAddressElement
import com.stripe.android.ui.core.elements.FormElement
import com.stripe.android.ui.core.elements.LayoutSpec
import com.stripe.android.ui.core.elements.SectionElement
import com.stripe.android.ui.core.forms.TransformSpecToElements
import com.stripe.android.ui.core.forms.resources.ResourceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flattenConcat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Controller that manages the user interaction with the payment method data collection form.
 * When all fields are reported as complete, [completeFormValues] emits the valid payment method.
 *
 * @param: formSpec A representation of the layout which is used to display the UI fields on screen.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class FormController @Inject constructor(
    private val formSpec: LayoutSpec,
    private val resourceRepository: ResourceRepository,
    private val transformSpecToElement: TransformSpecToElements,
    viewModelScope: CoroutineScope
) {
    // Initial value is null while loading in the background
    val elements: StateFlow<List<FormElement>?>

    init {
        if (resourceRepository.isLoaded()) {
            elements =
                MutableStateFlow(transformSpecToElement.transform(formSpec.items))
        } else {
            val delayedElements = MutableStateFlow<List<FormElement>?>(null)
            viewModelScope.launch {
                resourceRepository.waitUntilLoaded()
                delayedElements.value =
                    transformSpecToElement.transform(formSpec.items)
            }
            this.elements = delayedElements
        }
    }

    private val cardBillingElement = elements.map { elementsList ->
        elementsList
            ?.filterIsInstance<SectionElement>()
            ?.flatMap { it.fields }
            ?.filterIsInstance<CardBillingAddressElement>()
            ?.firstOrNull()
    }

    /**
     * List of field identifiers which should not be visible.
     */
    val hiddenIdentifiers =
        cardBillingElement.map {
            it?.hiddenIdentifiers ?: flowOf(emptyList())
        }.flattenConcat()

    /**
     * Emits a map of the form values when the form content is valid, null otherwise.
     */
    val completeFormValues = combine(
        elements.filterNotNull().map { elementsList ->
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
