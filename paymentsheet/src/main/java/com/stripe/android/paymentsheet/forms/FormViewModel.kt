package com.stripe.android.paymentsheet.forms

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stripe.android.paymentsheet.forms.PlaceholderHelper.connectBillingDetailsFields
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.ui.core.elements.CardBillingAddressElement
import com.stripe.android.ui.core.elements.MandateTextElement
import com.stripe.android.ui.core.elements.SaveForFutureUseElement
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.SectionElement
import com.stripe.android.uicore.forms.FormFieldEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flattenConcat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * This class stores the visual field layout for the [Form] and then sets up the controller
 * for all the fields on screen.  When all fields are reported as complete, the completedFieldValues
 * holds the resulting values for each field.
 *
 * @param: layout - this contains the visual layout of the fields on the screen used by [Form]
 * to display the UI fields on screen.  It also informs us of the backing fields to be created.
 */
internal class FormViewModel @Inject internal constructor(
    val elements: List<FormElement>,
    val formArguments: FormArguments,
    val showCheckboxFlow: Flow<Boolean>
) : ViewModel() {
    internal class Factory(
        val formElements: List<FormElement>,
        val formArguments: FormArguments,
        val showCheckboxFlow: Flow<Boolean>,
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return FormViewModel(
                elements = formElements,
                formArguments = formArguments,
                showCheckboxFlow = showCheckboxFlow,
            ) as T
        }
    }

    private val saveForFutureUseElement = elements.find { element ->
        element is SaveForFutureUseElement
    } as? SaveForFutureUseElement

    internal val saveForFutureUse = saveForFutureUseElement?.controller?.saveForFutureUse ?: flowOf(false)

    private val cardBillingElement = elements.filterIsInstance<SectionElement>()
        .flatMap { it.fields }
        .filterIsInstance<CardBillingAddressElement>()
        .firstOrNull()

    private var externalHiddenIdentifiers = MutableStateFlow(emptySet<IdentifierSpec>())

    init {
        viewModelScope.launch {
            connectBillingDetailsFields(elements)
        }
    }

    @VisibleForTesting
    internal fun addHiddenIdentifiers(identifierSpecs: Set<IdentifierSpec>) {
        externalHiddenIdentifiers.value = identifierSpecs
    }

    internal val hiddenIdentifiers = combine(
        showCheckboxFlow,
        cardBillingElement?.hiddenIdentifiers ?: flowOf(emptySet()),
        externalHiddenIdentifiers
    ) { showFutureUse, cardBillingIdentifiers, externalHiddenIdentifiers ->
        val hiddenIdentifiers = externalHiddenIdentifiers.plus(cardBillingIdentifiers)

        if (!showFutureUse && saveForFutureUseElement != null) {
            hiddenIdentifiers
                .plus(saveForFutureUseElement.identifier)
        } else {
            hiddenIdentifiers
        }
    }

    // Mandate is showing if it is an element of the form and it isn't hidden
    private val showingMandate = hiddenIdentifiers.map { hiddenIdentifiers ->
        elements.filterIsInstance<MandateTextElement>().firstOrNull()?.let { mandate ->
            !hiddenIdentifiers.contains(mandate.identifier)
        } ?: false
    }

    // This will convert the save for future use value into a CustomerRequestedSave operation
    private val userRequestedReuse = showCheckboxFlow.map { showCheckbox ->
        currentFieldValues().map { currentFieldValues ->
            currentFieldValues.filter { it.first == IdentifierSpec.SaveForFutureUse }
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
            currentFieldValues().map { it.toMap() },
            hiddenIdentifiers,
            showingMandate,
            userRequestedReuse,
            defaultValuesToInclude,
        ).filterFlow()

    private fun currentFieldValues(): Flow<List<Pair<IdentifierSpec, FormFieldEntry>>> {
        return if (elements.isEmpty()) {
            flowOf(emptyList())
        } else {
            combine(
                elements.map {
                    it.getFormFieldValueFlow()
                }
            ) {
                it.toList().flatten()
            }
        }
    }

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

    private val textFieldControllerIdsFlow = combine(elements.map { it.getTextFieldIdentifiers() }) {
        it.toList().flatten()
    }

    val lastTextFieldIdentifier = combine(
        hiddenIdentifiers,
        textFieldControllerIdsFlow
    ) { hiddenIds, textFieldControllerIds ->
        textFieldControllerIds.lastOrNull {
            !hiddenIds.contains(it)
        }
    }

    internal val viewDataFlow = combine(
        completeFormValues,
        hiddenIdentifiers,
        lastTextFieldIdentifier,
    ) { completeFormValues, hiddenIdentifiers, lastTextFieldIdentifier ->
        ViewData(
            elements = elements,
            completeFormValues = completeFormValues,
            hiddenIdentifiers = hiddenIdentifiers,
            lastTextFieldIdentifier = lastTextFieldIdentifier,
        )
    }

    internal data class ViewData(
        val elements: List<FormElement> = listOf(),
        val completeFormValues: FormFieldValues? = null,
        val hiddenIdentifiers: Set<IdentifierSpec> = setOf(),
        val lastTextFieldIdentifier: IdentifierSpec? = null,
    )
}
