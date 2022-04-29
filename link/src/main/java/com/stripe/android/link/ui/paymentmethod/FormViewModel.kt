package com.stripe.android.link.ui.paymentmethod

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stripe.android.link.injection.FormViewModelSubcomponent
import com.stripe.android.link.injection.NonFallbackInjectable
import com.stripe.android.link.injection.NonFallbackInjector
import com.stripe.android.ui.core.elements.CardBillingAddressElement
import com.stripe.android.ui.core.elements.FormElement
import com.stripe.android.ui.core.elements.LayoutSpec
import com.stripe.android.ui.core.elements.SectionElement
import com.stripe.android.ui.core.elements.SectionSpec
import com.stripe.android.ui.core.forms.TransformSpecToElements
import com.stripe.android.ui.core.forms.resources.ResourceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flattenConcat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider

/**
 * This class stores the visual field layout for the [Form] and then sets up the controller
 * for all the fields on screen.  When all fields are reported as complete, the completeFormValues
 * emits the valid payment method.
 *
 * @param: paymentMethod holds a representation of the layout in [SupportedPaymentMethod.formSpec],
 * which is used to display the UI fields on screen.
 */
internal class FormViewModel @Inject internal constructor(
    private val formSpec: LayoutSpec,
    private val resourceRepository: ResourceRepository,
    private val transformSpecToElement: TransformSpecToElements
) : ViewModel() {

    // Initial value is null while loading in the background
    internal val elements: StateFlow<List<FormElement>?>

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

    private val sectionToFieldIdentifierMap = formSpec.items
        .filterIsInstance<SectionSpec>()
        .associate { sectionSpec ->
            sectionSpec.identifier to sectionSpec.fields.map {
                it.identifier
            }
        }

    private val cardBillingElement = elements.map { elementsList ->
        elementsList
            ?.filterIsInstance<SectionElement>()
            ?.flatMap { it.fields }
            ?.filterIsInstance<CardBillingAddressElement>()
            ?.firstOrNull()
    }

    val hiddenIdentifiers = cardBillingElement.map {
        it?.hiddenIdentifiers ?: flowOf(emptyList())
    }.flattenConcat().map {
        // For hidden *section* identifiers, list of identifiers of elements in the section
        val identifiers = sectionToFieldIdentifierMap
            .filter { idControllerPair ->
                it.contains(idControllerPair.key)
            }
            .flatMap { sectionToSectionFieldEntry ->
                sectionToSectionFieldEntry.value
            }

        it.plus(identifiers)
    }

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

    class Factory(
        private val formSpec: LayoutSpec,
        private val injector: NonFallbackInjector
    ) : ViewModelProvider.Factory, NonFallbackInjectable {

        @Inject
        lateinit var subComponentBuilderProvider:
            Provider<FormViewModelSubcomponent.Builder>

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            injector.inject(this)
            return subComponentBuilderProvider.get()
                .formSpec(formSpec)
                .build().formViewModel as T
        }
    }
}
