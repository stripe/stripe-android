package com.stripe.android.paymentsheet.forms

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import com.stripe.android.paymentsheet.elements.EmailConfig
import com.stripe.android.paymentsheet.elements.NameConfig
import com.stripe.android.paymentsheet.elements.common.DropDown
import com.stripe.android.paymentsheet.elements.common.DropdownElement
import com.stripe.android.paymentsheet.elements.common.Element
import com.stripe.android.paymentsheet.elements.common.Section
import com.stripe.android.paymentsheet.elements.common.TextField
import com.stripe.android.paymentsheet.elements.common.TextFieldElement
import com.stripe.android.paymentsheet.elements.country.CountryConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

// TODO: Manadate type
// TODO: Save for future usage.

@ExperimentalAnimationApi
@Composable
internal fun Form(
    formViewModel: FormViewModel,
) {
    val form = formViewModel.fieldLayout

    // There is only a single field in a section, some of those might not require focus like
    // country so we will create an extra one
    var focusRequesterIndex = 0
    val focusRequesters = List(formViewModel.getNumberTextFieldElements()) { FocusRequester() }

    Column(
        modifier = Modifier
            .fillMaxWidth(1f)
            .padding(16.dp)
    ) {
        form.visualFieldLayout.forEach { section ->
            val element = formViewModel.getElement(section.field)
            val error by element.errorMessage.asLiveData().observeAsState(null)
            val sectionErrorString =
                error?.let { stringResource(it, stringResource(element.label)) }

            Section(sectionErrorString) {
                if (element is TextFieldElement) {
                    TextField(
                        textFieldElement = element,
                        myFocus = focusRequesters[focusRequesterIndex],
                        nextFocus = if (focusRequesterIndex == focusRequesters.size - 1) {
                            null
                        } else {
                            focusRequesters[focusRequesterIndex + 1]
                        },
                    )
                    focusRequesterIndex++
                } else if (element is DropdownElement) {
                    DropDown(element)
                }
            }
        }
    }
}

/**
 * This class stores the visual field layout for the [Form] and then sets up the controller
 * for all the fields on screen.  When all fields are reported as complete, the completedFieldValues
 * holds the resulting values for each field.
 *
 * @param: visualFieldLayout - this contains the visual layout of the fields on the screen used by [Form] to display the UI fields on screen.  It also informs us of the backing fields to be created.
 */
class FormViewModel(
    val fieldLayout: FieldLayout,
) : ViewModel() {
    class Factory(
        private val fieldLayout: FieldLayout,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return FormViewModel(fieldLayout) as T
        }
    }

    // This maps the field type to the element
    private val fieldElementMap: Map<Field, Element> =
        fieldLayout.allFields.associateWith { field ->
            when (field) {
                Field.NameInput -> TextFieldElement(NameConfig()) // All configs should have the label passed in for consistency
                Field.EmailInput -> TextFieldElement(EmailConfig())
                Field.CountryInput -> DropdownElement(CountryConfig())
            }
        }

    // This find the element based on the field type
    internal fun getElement(type: Field) = requireNotNull(fieldElementMap[type])

    fun getNumberTextFieldElements() = fieldElementMap.count { it.value is TextFieldElement }

    // This is null if any form field values are incomplete, otherwise it is an object
    // representing all the complete fields
    val completeFormValues: Flow<FormFieldValues?> = combine(
        currentFormFieldValuesFlow(fieldElementMap),
        allFormFieldsComplete(fieldElementMap)
    ) { formFieldValue, isComplete ->
        formFieldValue.takeIf { isComplete }
    }

    companion object {
        // Flows of FormFieldValues for each of the elements as the field is updated
        fun currentFormFieldValuesFlow(fieldElementMap: Map<Field, Element>) =
            combine(getCurrentFieldValuePairs(fieldElementMap))
            {
                transformToFormFieldValues(it)
            }

        fun transformToFormFieldValues(allFormFieldValues: Array<Pair<Field, String>>) =
            FormFieldValues(allFormFieldValues.toMap())

        fun getCurrentFieldValuePairs(fieldElementMap: Map<Field, Element>): List<Flow<Pair<Field, String>>> {
            return fieldElementMap.map { fieldElementEntry ->
                getCurrentFieldValuePair(fieldElementEntry.key, fieldElementEntry.value)
            }
        }

        fun getCurrentFieldValuePair(field: Field, value: Element): Flow<Pair<Field, String>> {
            return value.fieldValue.map {
                Pair(field, it)
            }
        }

        fun allFormFieldsComplete(fieldElementMap: Map<Field, Element>) =
            combine(fieldElementMap.values.map { it.isComplete }) { elementCompleteStates ->
                elementCompleteStates.none { complete -> !complete }
            }
    }
}
