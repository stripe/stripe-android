package com.stripe.android.paymentsheet.forms

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stripe.android.core.injection.Injectable
import com.stripe.android.core.injection.injectWithFallback
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentsheet.injection.DaggerFormViewModelComponent
import com.stripe.android.paymentsheet.injection.FormViewModelSubcomponent
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.paymentdatacollection.FormFragmentArguments
import com.stripe.android.ui.core.address.AddressRepository
import com.stripe.android.ui.core.elements.CardBillingAddressElement
import com.stripe.android.ui.core.elements.FormElement
import com.stripe.android.ui.core.elements.FormItemSpec
import com.stripe.android.ui.core.elements.IdentifierSpec
import com.stripe.android.ui.core.elements.MandateTextElement
import com.stripe.android.ui.core.elements.SaveForFutureUseElement
import com.stripe.android.ui.core.elements.SectionElement
import com.stripe.android.ui.core.forms.resources.LpmRepository
import com.stripe.android.ui.core.forms.resources.ResourceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flattenConcat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    paymentMethodCode: PaymentMethodCode,
    config: FormFragmentArguments,
    internal val lpmResourceRepository: ResourceRepository<LpmRepository>,
    internal val addressResourceRepository: ResourceRepository<AddressRepository>,
    private val transformSpecToElement: TransformSpecToElement
) : ViewModel() {
    internal class Factory(
        val config: FormFragmentArguments,
        var paymentMethodCode: PaymentMethodCode,
        private val contextSupplier: () -> Context
    ) : ViewModelProvider.Factory, Injectable<Factory.FallbackInitializeParam> {
        internal data class FallbackInitializeParam(
            val context: Context
        )

        @Inject
        lateinit var subComponentBuilderProvider: Provider<FormViewModelSubcomponent.Builder>

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val context = contextSupplier()
            injectWithFallback(config.injectorKey, FallbackInitializeParam(context))
            return subComponentBuilderProvider.get()
                .formFragmentArguments(config)
                .paymentMethodCode(paymentMethodCode)
                .build().viewModel as T
        }

        override fun fallbackInitialize(arg: FallbackInitializeParam) {
            DaggerFormViewModelComponent.builder()
                .context(arg.context)
                .build()
                .inject(this)
        }
    }

    // Initial value is null while loading in the background
    internal val elements: StateFlow<List<FormElement>?>

    private val resourceRepositories = listOf(lpmResourceRepository, addressResourceRepository)

    init {
        if (resourceRepositories.all { it.isLoaded() }) {
            elements = MutableStateFlow(
                transformSpecToElement.transform(
                    getLpmItems(paymentMethodCode)
                )
            )
        } else {
            val delayedElements = MutableStateFlow<List<FormElement>?>(null)
            viewModelScope.launch {
                // The coroutine scope is needed to do work off the UI thread so that the
                // repository ready event can be observed in the ComposeFormDataCollection
                // Fragment and the fragment repository will be updated and ready
                CoroutineScope(Dispatchers.IO).launch {

                    // If after we complete waiting for the repository things are still
                    // active, then update the elements
                    resourceRepositories.forEach { it.waitUntilLoaded() }
                    if (resourceRepositories.all { it.isLoaded() } && isActive) {
                        // When open payment options with returning customer with saved cards, then
                        // click on Add, then kill, then re-open, ComposeFormDataCollectionFragment
                        // is no longer listening for the resource repository to be ready and so
                        // the resource repository is not ready!
                        val values = transformSpecToElement.transform(
                            getLpmItems(paymentMethodCode)
                        )
                        withContext(Dispatchers.Main) {
                            delayedElements.value = values
                        }
                    }
                }
            }
            this.elements = delayedElements
        }
    }

    private fun getLpmItems(paymentMethodCode: PaymentMethodCode): List<FormItemSpec> {
        require(resourceRepositories.all { it.isLoaded() })
        return requireNotNull(
            lpmResourceRepository.getRepository().fromCode(
                paymentMethodCode
            )
        ).formSpec.items
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

    internal val hiddenIdentifiers = combine(
        saveForFutureUseVisible,
        cardBillingElement.map {
            it?.hiddenIdentifiers ?: flowOf(emptyList())
        }.flattenConcat(),
        externalHiddenIdentifiers
    ) { showFutureUse, cardBillingIdentifiers, saveFutureUseIdentifiers ->
        val hiddenIdentifiers = saveFutureUseIdentifiers.plus(cardBillingIdentifiers)

        val saveForFutureUseElement = saveForFutureUseElement.firstOrNull()
        if (!showFutureUse && saveForFutureUseElement != null) {
            hiddenIdentifiers
                .plus(saveForFutureUseElement.identifier)
        } else {
            hiddenIdentifiers
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
