package com.stripe.android.paymentsheet.forms

import android.content.res.Resources
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stripe.android.payments.core.injection.Injectable
import com.stripe.android.payments.core.injection.injectWithFallback
import com.stripe.android.paymentsheet.elements.Form
import com.stripe.android.paymentsheet.elements.FormElement
import com.stripe.android.paymentsheet.elements.LayoutSpec
import com.stripe.android.paymentsheet.elements.MandateTextElement
import com.stripe.android.paymentsheet.elements.ResourceRepository
import com.stripe.android.paymentsheet.elements.SaveForFutureUseElement
import com.stripe.android.paymentsheet.elements.SectionSpec
import com.stripe.android.paymentsheet.injection.DaggerFormViewModelComponent
import com.stripe.android.paymentsheet.injection.FormViewModelSubcomponent
import com.stripe.android.paymentsheet.paymentdatacollection.FormFragmentArguments
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
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
internal class FormViewModel @Inject internal constructor(
    layout: LayoutSpec,
    config: FormFragmentArguments,
    resourceRepository: ResourceRepository,
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

    init {
        viewModelScope.launch {
            elements = transformSpecToElement.transform(layout.items)
        }
    }

    internal val enabled = MutableStateFlow(true)
    internal fun setEnabled(enabled: Boolean) {
        this.enabled.value = enabled
    }

    internal lateinit var elements: List<FormElement>

    private val saveForFutureUseVisible = MutableStateFlow(config.saveForFutureUseInitialVisibility)

    internal fun setSaveForFutureUseVisibility(isVisible: Boolean) {
        saveForFutureUseVisible.value = isVisible
    }

    internal fun setSaveForFutureUse(value: Boolean) {
        elements
            .filterIsInstance<SaveForFutureUseElement>()
            .firstOrNull()?.controller?.onValueChange(value)
    }

    private val saveForFutureUseElement = elements
        .filterIsInstance<SaveForFutureUseElement>()
        .firstOrNull()

    internal val saveForFutureUse = saveForFutureUseElement?.controller?.saveForFutureUse
        ?: MutableStateFlow(config.saveForFutureUseInitialValue)

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
            saveForFutureUseElement?.controller?.hiddenIdentifiers
                ?: MutableStateFlow(emptyList())
        ) { showFutureUse, hiddenIdentifiers ->

            // For hidden *section* identifiers, list of identifiers of elements in the section
            val identifiers = sectionToFieldIdentifierMap
                .filter { idControllerPair ->
                    hiddenIdentifiers.contains(idControllerPair.key)
                }
                .flatMap { sectionToSectionFieldEntry ->
                    sectionToSectionFieldEntry.value
                }

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
    internal val showingMandate = hiddenIdentifiers.map {
        elements
            .filterIsInstance<MandateTextElement>()
            .firstOrNull()?.let { mandate ->
                !it.contains(mandate.identifier)
            } ?: false
    }

    val completeFormValues =
        CompleteFormFieldValueFilter(
            combine(
                elements.map { it.getFormFieldValueFlow() }
            ) {
                it.toList().flatten().toMap()
            },
            hiddenIdentifiers,
            showingMandate,
            saveForFutureUse
        ).filterFlow()
}
