package com.stripe.android.paymentsheet.addresselement

import com.stripe.android.lpmfoundations.luxe.TransformSpecToElements
import com.stripe.android.ui.core.elements.CardBillingAddressElement
import com.stripe.android.ui.core.elements.LayoutSpec
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.SectionElement
import com.stripe.android.uicore.utils.combineAsStateFlow
import com.stripe.android.uicore.utils.flatMapLatestAsStateFlow
import com.stripe.android.uicore.utils.mapAsStateFlow
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * Controller that manages the user interaction with the payment method data collection form.
 * When all fields are reported as complete, [completeFormValues] emits the valid payment method.
 *
 * @param: formSpec A representation of the layout which is used to display the UI fields on screen.
 */
internal class FormController @Inject constructor(
    formSpec: LayoutSpec,
    transformSpecToElement: TransformSpecToElements,
) {
    val elements: StateFlow<List<FormElement>> =
        stateFlowOf(transformSpecToElement.transform(null, formSpec.items))

    private val cardBillingElement = elements.mapAsStateFlow { elementsList ->
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
        cardBillingElement.flatMapLatestAsStateFlow {
            it?.hiddenIdentifiers ?: stateFlowOf(emptySet())
        }

    /**
     * Emits a map of the form values when the form content is valid, null otherwise.
     */
    val completeFormValues = combineAsStateFlow(
        elements.flatMapLatestAsStateFlow { elementsList ->
            combineAsStateFlow(
                elementsList.map {
                    it.getFormFieldValueFlow()
                }
            ) {
                it.toList().flatten().toMap()
            }
        },
        hiddenIdentifiers
    ) { elementsList, hiddenIdentifiers ->
        elementsList.filter { mapEntry ->
            !hiddenIdentifiers.contains(mapEntry.key)
        }
    }.mapAsStateFlow { map ->
        map.takeIf { it.values.all { entry -> entry.isComplete } }
    }

    /**
     * Emits a map of the form values that are complete, empty otherwise.
     */
    val formValues = combineAsStateFlow(
        elements.flatMapLatestAsStateFlow { elementsList ->
            combineAsStateFlow(
                elementsList.map {
                    it.getFormFieldValueFlow()
                }
            ) {
                it.toList().flatten().toMap()
            }
        },
        hiddenIdentifiers
    ) { elementsList, hiddenIdentifiers ->
        elementsList.filter { mapEntry ->
            !hiddenIdentifiers.contains(mapEntry.key)
        }
    }.mapAsStateFlow { map ->
        map.filter { it.value.isComplete }
    }

    private val textFieldControllerIdsFlow = elements.flatMapLatestAsStateFlow { elementsList ->
        combineAsStateFlow(elementsList.map { it.getTextFieldIdentifiers() }) {
            it.toList().flatten()
        }
    }

    val lastTextFieldIdentifier = combineAsStateFlow(
        hiddenIdentifiers,
        textFieldControllerIdsFlow
    ) { hiddenIds, textFieldControllerIds ->
        textFieldControllerIds.lastOrNull {
            !hiddenIds.contains(it)
        }
    }
}
