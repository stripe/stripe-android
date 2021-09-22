package com.stripe.android.paymentsheet.forms

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stripe.android.Logger
import com.stripe.android.payments.core.injection.Injectable
import com.stripe.android.payments.core.injection.WeakMapInjectorRegistry
import com.stripe.android.paymentsheet.elements.*
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
    private val resourceRepository: ResourceRepository
) : ViewModel() {
    internal class Factory(
        val enableLogging: Boolean,
        val config: FormFragmentArguments,
        val application: Application,
    ) : ViewModelProvider.Factory, Injectable<Factory.FallbackInitializeParam> {
        internal data class FallbackInitializeParam(
            val enableLogging: Boolean,
            val config: FormFragmentArguments,
            val application: Application,
            var layout: LayoutSpec
        )

        @Inject
        lateinit var subComponentBuilderProvider:
            Provider<FormViewModelSubcomponent.Builder>

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {

            val logger = Logger.getInstance(enableLogging)
            WeakMapInjectorRegistry.retrieve(config.injectorKey)?.let {
                logger.info(
                    "Injector available, " +
                        "injecting dependencies into PaymentOptionsViewModel.Factory"
                )
                it.inject(this)
            } ?: run {
                logger.info(
                    "Injector unavailable, " +
                        "initializing dependencies of PaymentOptionsViewModel.Factory"
                )
                fallbackInitialize(
                    FallbackInitializeParam(
                        enableLogging, config, application, layout
                    )
                )
            }
            return subComponentBuilderProvider.get()
                .formFragmentArguments(config)
                .layout(layout)
                .build().viewModel as T
        }

        override fun fallbackInitialize(arg: FallbackInitializeParam) {
            DaggerFormViewModelComponent.builder()
                .layout(arg.layout)
                .resources(arg.application.resources)
                .formFragmentArguments(arg.config)
                .build()
                .inject(this)
        }
    }

    private val transformSpecToElement = TransformSpecToElement(resourceRepository, config)

    init {
        viewModelScope.launch {
            resourceRepository.init()
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
