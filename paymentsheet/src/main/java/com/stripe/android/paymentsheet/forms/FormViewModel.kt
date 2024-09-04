package com.stripe.android.paymentsheet.forms

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stripe.android.paymentsheet.forms.PlaceholderHelper.connectBillingDetailsFields
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.ui.core.elements.CardBillingAddressElement
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.SectionElement
import com.stripe.android.uicore.utils.combineAsStateFlow
import com.stripe.android.uicore.utils.flatMapLatestAsStateFlow
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * This class stores the visual field layout for the [Form] and then sets up the controller
 * for all the fields on screen.  When all fields are reported as complete, the completedFieldValues
 * holds the resulting values for each field.
 *
 * @param: layout - this contains the visual layout of the fields on the screen used by [Form]
 * to display the UI fields on screen.  It also informs us of the backing fields to be created.
 */
internal class FormViewModel(
    initialElements: List<FormElement>,
    private val formArguments: FormArguments,
) : ViewModel() {

    private val _elements = MutableStateFlow(initialElements)
    val elements: StateFlow<List<FormElement>> = _elements.asStateFlow()

    private val currentFieldValues = _elements.flatMapLatestAsStateFlow { elements ->
        if (elements.isEmpty()) {
            stateFlowOf(emptyList())
        } else {
            combineAsStateFlow(elements.map(FormElement::getFormFieldValueFlow)) {
                it.toList().flatten()
            }
        }
    }

    private val cardBillingElement = initialElements.filterIsInstance<SectionElement>()
        .flatMap { it.fields }
        .filterIsInstance<CardBillingAddressElement>()
        .firstOrNull()

    private var externalHiddenIdentifiers = MutableStateFlow(emptySet<IdentifierSpec>())

    init {
        viewModelScope.launch {
            connectBillingDetailsFields(initialElements)
        }
    }

    fun updateFormElements(elements: List<FormElement>) {
        _elements.value = elements.toList()
    }

    @VisibleForTesting
    internal fun addHiddenIdentifiers(identifierSpecs: Set<IdentifierSpec>) {
        externalHiddenIdentifiers.value = identifierSpecs
    }

    internal val hiddenIdentifiers = combineAsStateFlow(
        cardBillingElement?.hiddenIdentifiers ?: stateFlowOf(emptySet()),
        externalHiddenIdentifiers
    ) { cardBillingIdentifiers, externalHiddenIdentifiers ->
        externalHiddenIdentifiers.plus(cardBillingIdentifiers)
    }

    // This will convert the save for future use value into a CustomerRequestedSave operation
    private val userRequestedReuse = currentFieldValues.map { values ->
        values.filter { it.first == IdentifierSpec.SaveForFutureUse }
            .map { it.second.value.toBoolean() }
            .map { saveForFutureUse ->
                if (saveForFutureUse) {
                    PaymentSelection.CustomerRequestedSave.RequestReuse
                } else {
                    PaymentSelection.CustomerRequestedSave.RequestNoReuse
                }
            }
            .firstOrNull() ?: PaymentSelection.CustomerRequestedSave.NoRequest
    }

    val completeFormValues =
        CompleteFormFieldValueFilter(
            currentFieldValues.map { it.toMap() },
            hiddenIdentifiers,
            userRequestedReuse,
            defaultValuesToInclude,
        ).filterFlow()

    @VisibleForTesting
    val defaultValuesToInclude
        get(): Map<IdentifierSpec, String> {
            val defaults = mutableMapOf<IdentifierSpec, String>()

            if (formArguments.billingDetailsCollectionConfiguration.attachDefaultsToPaymentMethod) {
                formArguments.billingDetails?.let { billingDetails ->
                    billingDetails.name?.let { defaults[IdentifierSpec.Name] = it }
                    billingDetails.email?.let { defaults[IdentifierSpec.Email] = it }
                    billingDetails.phone?.let { defaults[IdentifierSpec.Phone] = it }
                    billingDetails.address?.line1?.let { defaults[IdentifierSpec.Companion.Line1] = it }
                    billingDetails.address?.line2?.let { defaults[IdentifierSpec.Companion.Line2] = it }
                    billingDetails.address?.city?.let { defaults[IdentifierSpec.Companion.City] = it }
                    billingDetails.address?.state?.let { defaults[IdentifierSpec.Companion.State] = it }
                    billingDetails.address?.postalCode?.let {
                        defaults[IdentifierSpec.Companion.PostalCode] = it
                    }
                    billingDetails.address?.country?.let {
                        defaults[IdentifierSpec.Companion.Country] = it
                    }
                }
            }
            return defaults
        }

    private val textFieldControllerIdsFlow = combineAsStateFlow(initialElements.map { it.getTextFieldIdentifiers() }) {
        it.toList().flatten()
    }

    val lastTextFieldIdentifier = combineAsStateFlow(
        hiddenIdentifiers,
        textFieldControllerIdsFlow
    ) { hiddenIds, textFieldControllerIds ->
        textFieldControllerIds.lastOrNull {
            !hiddenIds.contains(it)
        }
    }

    internal class Factory(
        val formElements: List<FormElement>,
        val formArguments: FormArguments,
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return FormViewModel(
                initialElements = formElements,
                formArguments = formArguments,
            ) as T
        }
    }
}
