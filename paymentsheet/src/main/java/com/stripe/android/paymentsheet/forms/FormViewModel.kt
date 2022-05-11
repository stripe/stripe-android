package com.stripe.android.paymentsheet.forms

import android.content.Context
import android.content.res.Resources
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stripe.android.core.injection.Injectable
import com.stripe.android.core.injection.injectWithFallback
import com.stripe.android.paymentsheet.injection.DaggerFormViewModelComponent
import com.stripe.android.paymentsheet.injection.FormViewModelSubcomponent
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.paymentdatacollection.FormFragmentArguments
import com.stripe.android.ui.core.elements.CardBillingAddressElement
import com.stripe.android.ui.core.elements.FormElement
import com.stripe.android.ui.core.elements.IdentifierSpec
import com.stripe.android.ui.core.elements.LayoutSpec
import com.stripe.android.ui.core.elements.MandateTextElement
import com.stripe.android.ui.core.elements.SaveForFutureUseElement
import com.stripe.android.ui.core.elements.SectionElement
import com.stripe.android.ui.core.elements.SectionSpec
import com.stripe.android.ui.core.forms.resources.ResourceRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flattenConcat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider

/**
 * This class stores the visual field layout for the [Form] and then sets up the controller
 * for all the fields on screen.  When all fields are reported as complete, the completedFieldValues
 * holds the resulting values for each field.
 *
 * @param: layout - this contains the visual layout of the fields on the screen used by [Form]
 * to display the UI fields on screen.  It also informs us of the backing fields to be created.
 */
@FlowPreview
internal class FormViewModel @Inject internal constructor(
    layout: LayoutSpec,
    config: FormFragmentArguments,
    private val resourceRepository: ResourceRepository,
    private val transformSpecToElement: TransformSpecToElement
) : ViewModel() {
    internal class Factory(
        val config: FormFragmentArguments,
        val resource: Resources,
        var layout: LayoutSpec,
        private val contextSupplier: () -> Context
    ) : ViewModelProvider.Factory, Injectable<Factory.FallbackInitializeParam> {
        internal data class FallbackInitializeParam(
            val resource: Resources,
            val context: Context
        )

        @Inject
        lateinit var subComponentBuilderProvider: Provider<FormViewModelSubcomponent.Builder>

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val context = contextSupplier()
            injectWithFallback(config.injectorKey, FallbackInitializeParam(resource, context))
            return subComponentBuilderProvider.get()
                .formFragmentArguments(config)
                .layout(layout)
                .build().viewModel as T
        }

        override fun fallbackInitialize(arg: FallbackInitializeParam) {
            DaggerFormViewModelComponent.builder()
                .context(arg.context)
                .resources(arg.resource)
                .build()
                .inject(this)
        }
    }

    // Initial value is null while loading in the background
    internal val elements: StateFlow<List<FormElement>?>

    init {
        if (resourceRepository.isLoaded()) {
            elements = MutableStateFlow(transformSpecToElement.transform(layout.items))
        } else {
            val delayedElements = MutableStateFlow<List<FormElement>?>(null)
            viewModelScope.launch {
                resourceRepository.waitUntilLoaded()
                delayedElements.value = transformSpecToElement.transform(layout.items)
            }
            this.elements = delayedElements
        }
    }

    internal val enabled = MutableStateFlow(true)
    internal fun setEnabled(enabled: Boolean) {
        this.enabled.value = enabled
    }

    @VisibleForTesting
    internal val saveForFutureUseVisible = MutableStateFlow(config.showCheckbox)

    private val saveForFutureUseElement = elements
        .map { elementsList ->
            elementsList?.find { element ->
                element is SaveForFutureUseElement
            } as? SaveForFutureUseElement
        }

    internal val saveForFutureUse = saveForFutureUseElement.map {
        it?.controller?.saveForFutureUse ?: flowOf(false)
    }.flattenConcat()

    private val cardBillingElement = elements
        .map { elementsList ->
            elementsList
                ?.filterIsInstance<SectionElement>()
                ?.flatMap { it.fields }
                ?.filterIsInstance<CardBillingAddressElement>()
                ?.firstOrNull()
        }
    private var externalHiddenIdentifiers = MutableStateFlow(emptyList<IdentifierSpec>())

    @VisibleForTesting
    internal fun addHiddenIdentifiers(identifierSpecs: List<IdentifierSpec>) {
        externalHiddenIdentifiers.value = identifierSpecs
    }

    private val sectionToFieldIdentifierMap = layout.items
        .filterIsInstance<SectionSpec>()
        .associate { sectionSpec ->
            sectionSpec.api_path to sectionSpec.fields.map {
                it.identifier
            }
        }

    internal val hiddenIdentifiers = combine(
        saveForFutureUseVisible,
        cardBillingElement.map {
            it?.hiddenIdentifiers ?: flowOf(emptyList())
        }.flattenConcat(),
        externalHiddenIdentifiers
    ) { showFutureUse, cardBillingIdentifiers, saveFutureUseIdentifiers ->
        val hiddenIdentifiers = saveFutureUseIdentifiers.plus(cardBillingIdentifiers)
        // For hidden *section* identifiers, list of identifiers of elements in the section
        val identifiers = sectionToFieldIdentifierMap
            .filter { idControllerPair ->
                hiddenIdentifiers.contains(idControllerPair.key)
            }
            .flatMap { sectionToSectionFieldEntry ->
                sectionToSectionFieldEntry.value
            }


        val saveForFutureUseElement = saveForFutureUseElement.firstOrNull()
        if (!showFutureUse && saveForFutureUseElement != null) {
            hiddenIdentifiers
                .plus(identifiers)
                .plus(saveForFutureUseElement.identifier)
        } else {
            hiddenIdentifiers
                .plus(identifiers)
        }
    }

    // Mandate is showing if it is an element of the form and it isn't hidden
    private val showingMandate =
        combine(
            hiddenIdentifiers,
            elements.map {
                it ?: emptyList()
            }
        ) { hiddenIdentifiers, formElements ->
            formElements.filterIsInstance<MandateTextElement>().firstOrNull()?.let { mandate ->
                !hiddenIdentifiers.contains(mandate.identifier)
            } ?: false
        }

    // This will convert the save for future use value into a CustomerRequestedSave operation
    private val userRequestedReuse = elements.filterNotNull().map { elementsList ->
        combine(elementsList.map { it.getFormFieldValueFlow() }) { formFieldValues ->
            formFieldValues.toList().flatten()
                .filter { it.first == IdentifierSpec.SaveForFutureUse }
                .map { it.second.value.toBoolean() }
                .map { saveForFutureUse ->
                    if (config.showCheckbox) {
                        if (saveForFutureUse) {
                            PaymentSelection.CustomerRequestedSave.RequestReuse
                        } else {
                            PaymentSelection.CustomerRequestedSave.RequestNoReuse
                        }
                    } else {
                        PaymentSelection.CustomerRequestedSave.NoRequest
                    }
                }
                .firstOrNull() ?: PaymentSelection.CustomerRequestedSave.NoRequest
        }
    }.flattenConcat()

    val completeFormValues =
        CompleteFormFieldValueFilter(
            elements.filterNotNull().map { elementsList ->
                combine(
                    elementsList.map {
                        it.getFormFieldValueFlow()
                    }
                ) {
                    it.toList().flatten().toMap()
                }
            }.flattenConcat(),
            hiddenIdentifiers,
            showingMandate,
            userRequestedReuse
        ).filterFlow()

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
