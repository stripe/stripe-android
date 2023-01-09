package com.stripe.android.paymentsheet.forms

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.stripe.android.core.injection.NonFallbackInjectable
import com.stripe.android.core.injection.NonFallbackInjector
import com.stripe.android.paymentsheet.addresselement.toIdentifierMap
import com.stripe.android.paymentsheet.injection.FormViewModelSubcomponent
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.paymentdatacollection.FormFragmentArguments
import com.stripe.android.paymentsheet.paymentdatacollection.getInitialValuesMap
import com.stripe.android.ui.core.address.AddressRepository
import com.stripe.android.ui.core.elements.CardBillingAddressElement
import com.stripe.android.ui.core.elements.IdentifierSpec
import com.stripe.android.ui.core.elements.MandateTextElement
import com.stripe.android.ui.core.elements.SaveForFutureUseElement
import com.stripe.android.ui.core.elements.SectionElement
import com.stripe.android.ui.core.forms.TransformSpecToElements
import com.stripe.android.ui.core.forms.resources.LpmRepository
import com.stripe.android.ui.core.forms.resources.ResourceRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flattenConcat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
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
    context: Context,
    formFragmentArguments: FormFragmentArguments,
    lpmResourceRepository: ResourceRepository<LpmRepository>,
    addressResourceRepository: ResourceRepository<AddressRepository>,
    val showCheckboxFlow: Flow<Boolean>
) : ViewModel() {
    internal class Factory(
        val config: FormFragmentArguments,
        val showCheckboxFlow: Flow<Boolean>,
        private val injector: NonFallbackInjector
    ) : ViewModelProvider.Factory, NonFallbackInjectable {
        @Inject
        lateinit var subComponentBuilderProvider: Provider<FormViewModelSubcomponent.Builder>

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            injector.inject(this)
            return subComponentBuilderProvider.get()
                .formFragmentArguments(config)
                .showCheckboxFlow(showCheckboxFlow)
                .build()
                .viewModel as T
        }
    }

    val elementsFlow = flowOf(
        TransformSpecToElements(
            addressResourceRepository = addressResourceRepository,
            initialValues = formFragmentArguments.getInitialValuesMap(),
            amount = formFragmentArguments.amount,
            saveForFutureUseInitialValue = formFragmentArguments.showCheckboxControlledFields,
            merchantName = formFragmentArguments.merchantName,
            context = context,
            shippingValues = formFragmentArguments.shippingDetails
                ?.toIdentifierMap(formFragmentArguments.billingDetails)
        ).transform(
            requireNotNull(
                lpmResourceRepository.getRepository()
                    .fromCode(formFragmentArguments.paymentMethodCode)
            ).formSpec.items
        )
    )

    private val saveForFutureUseElement = elementsFlow
        .map { elementsList ->
            elementsList.find { element ->
                element is SaveForFutureUseElement
            } as? SaveForFutureUseElement
        }

    internal val saveForFutureUse = saveForFutureUseElement.map {
        it?.controller?.saveForFutureUse ?: flowOf(false)
    }.flattenConcat()

    private val cardBillingElement = elementsFlow
        .map { elementsList ->
            elementsList
                .filterIsInstance<SectionElement>()
                .flatMap { it.fields }
                .filterIsInstance<CardBillingAddressElement>()
                .firstOrNull()
        }
    private var externalHiddenIdentifiers = MutableStateFlow(emptySet<IdentifierSpec>())

    @VisibleForTesting
    internal fun addHiddenIdentifiers(identifierSpecs: Set<IdentifierSpec>) {
        externalHiddenIdentifiers.value = identifierSpecs
    }

    internal val hiddenIdentifiers = combine(
        showCheckboxFlow,
        cardBillingElement.map {
            it?.hiddenIdentifiers ?: flowOf(emptySet())
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
            elementsFlow.map {
                it ?: emptyList()
            }
        ) { hiddenIdentifiers, formElements ->
            formElements.filterIsInstance<MandateTextElement>().firstOrNull()?.let { mandate ->
                !hiddenIdentifiers.contains(mandate.identifier)
            } ?: false
        }

    // This will convert the save for future use value into a CustomerRequestedSave operation
    private val userRequestedReuse =
        combine(
            elementsFlow.filterNotNull(),
            showCheckboxFlow
        ) { elementsList, showCheckbox ->
            combine(
                elementsList.map { it.getFormFieldValueFlow() },
            ) { formFieldValues ->
                formFieldValues.toList().flatten()
                    .filter { it.first == IdentifierSpec.SaveForFutureUse }
                    .map { it.second.value.toBoolean() }
                    .map { saveForFutureUse ->
                        if (showCheckbox) {
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
            elementsFlow.filterNotNull().map { elementsList ->
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

    private val textFieldControllerIdsFlow = elementsFlow.filterNotNull().map { elementsList ->
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
