package com.stripe.android.paymentsheet.forms

import android.content.res.Resources
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stripe.android.core.injection.Injectable
import com.stripe.android.core.injection.injectWithFallback
import com.stripe.android.paymentsheet.elements.Form
import com.stripe.android.paymentsheet.elements.FormElement
import com.stripe.android.paymentsheet.elements.LayoutSpec
import com.stripe.android.paymentsheet.elements.SaveForFutureUseElement
import com.stripe.android.paymentsheet.elements.SectionSpec
import com.stripe.android.paymentsheet.elements.StaticTextElement
import com.stripe.android.paymentsheet.forms.resources.ResourceRepository
import com.stripe.android.paymentsheet.injection.DaggerFormViewModelComponent
import com.stripe.android.paymentsheet.injection.FormViewModelSubcomponent
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.paymentdatacollection.FormFragmentArguments
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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
        var layout: LayoutSpec
    ) : ViewModelProvider.Factory, Injectable<Factory.FallbackInitializeParam> {
        internal data class FallbackInitializeParam(
            val resource: Resources
        )

        @Inject
        lateinit var subComponentBuilderProvider: Provider<FormViewModelSubcomponent.Builder>

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            injectWithFallback(config.injectorKey, FallbackInitializeParam(resource))
            return subComponentBuilderProvider.get()
                .formFragmentArguments(config)
                .layout(layout)
                .build().viewModel as T
        }

        override fun fallbackInitialize(arg: FallbackInitializeParam) {
            DaggerFormViewModelComponent.builder()
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

    private val saveForFutureUseVisible = MutableStateFlow(config.showCheckbox)

    internal fun setSaveForFutureUseVisibility(isVisible: Boolean) {
        saveForFutureUseVisible.value = isVisible
    }

    internal suspend fun setSaveForFutureUse(value: Boolean) {
        elements
            .firstOrNull()
            ?.filterIsInstance<SaveForFutureUseElement>()
            ?.firstOrNull()?.controller?.onValueChange(value)
    }

    private val saveForFutureUseElement = elements
        .map { elementsList ->
            elementsList?.find { element ->
                element is SaveForFutureUseElement
            } as? SaveForFutureUseElement
        }

    internal val saveForFutureUse = saveForFutureUseElement.map {
        it?.controller?.saveForFutureUse ?: flowOf(false)
    }.flattenConcat()

    private val sectionToFieldIdentifierMap = layout.items
        .filterIsInstance<SectionSpec>()
        .associate { sectionSpec ->
            sectionSpec.identifier to sectionSpec.fields.map {
                it.identifier
            }
        }

    internal val hiddenIdentifiers =
        combine(
            saveForFutureUseVisible,
            saveForFutureUseElement.map {
                it?.controller?.hiddenIdentifiers ?: flowOf(emptyList())
            }.flattenConcat()
        ) { showFutureUse, hiddenIdentifiers ->
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
            formElements.filterIsInstance<StaticTextElement>().firstOrNull()?.let { mandate ->
                !hiddenIdentifiers.contains(mandate.identifier)
            } ?: false
        }

    private val userRequestedReuse =
        saveForFutureUseElement.map {
            it?.controller?.saveForFutureUse?.map { saveForFutureUse ->
                if (config.showCheckbox) {
                    if (saveForFutureUse) {
                        PaymentSelection.CustomerRequestedSave.RequestReuse
                    } else {
                        PaymentSelection.CustomerRequestedSave.RequestNoReuse
                    }
                } else {
                    PaymentSelection.CustomerRequestedSave.NoRequest
                }
            }?.firstOrNull() ?: PaymentSelection.CustomerRequestedSave.NoRequest
        }

    val completeFormValues =
        CompleteFormFieldValueFilter(
            elements.map { nullableElementsList ->
                nullableElementsList?.let { elementsList ->
                    combine(
                        elementsList.map {
                            it.getFormFieldValueFlow()
                        }
                    ) {
                        it.toList().flatten().toMap()
                    }
                } ?: flowOf(emptyMap())
            }.flattenConcat(),
            hiddenIdentifiers,
            showingMandate,
            userRequestedReuse
        ).filterFlow()
}
