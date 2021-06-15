package com.stripe.android.paymentsheet.forms

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
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
import com.stripe.android.paymentsheet.elements.common.DropdownFieldController
import com.stripe.android.paymentsheet.elements.common.FieldController
import com.stripe.android.paymentsheet.elements.common.Section
import com.stripe.android.paymentsheet.elements.common.TextField
import com.stripe.android.paymentsheet.elements.common.TextFieldController
import com.stripe.android.paymentsheet.elements.country.CountryConfig
import com.stripe.android.paymentsheet.forms.FormElementSpec.SectionSpec
import com.stripe.android.paymentsheet.forms.FormElementSpec.SectionSpec.SectionFieldSpec
import com.stripe.android.paymentsheet.forms.FormElementSpec.SectionSpec.SectionFieldSpec.Country
import com.stripe.android.paymentsheet.forms.FormElementSpec.SectionSpec.SectionFieldSpec.Email
import com.stripe.android.paymentsheet.forms.FormElementSpec.SectionSpec.SectionFieldSpec.Name
import com.stripe.android.paymentsheet.forms.FormElementSpec.StaticSpec.TextSpec
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

// TODO: Save for future usage.

@ExperimentalAnimationApi
@Composable
internal fun Form(
    formViewModel: FormViewModel,
) {
    val fieldLayout = formViewModel.layout

    var focusRequesterIndex = 0
    val focusRequesters = List(formViewModel.getNumberTextFields()) { FocusRequester() }

    Column(
        modifier = Modifier
            .fillMaxWidth(1f)
            .padding(16.dp)
    ) {
        fieldLayout.elements.forEach { element ->
            when (element) {
                is SectionSpec -> {
                    val controller = formViewModel.getController(element.field)
                    val error by controller.errorMessage.asLiveData().observeAsState(null)
                    val sectionErrorString =
                        error?.let { stringResource(it, stringResource(controller.label)) }

                    Section(sectionErrorString) {
                        when (controller) {
                            is TextFieldController -> {
                                TextField(
                                    textFieldController = controller,
                                    myFocus = focusRequesters[focusRequesterIndex],
                                    nextFocus = if (focusRequesterIndex == focusRequesters.size - 1) {
                                        null
                                    } else {
                                        focusRequesters[focusRequesterIndex + 1]
                                    },
                                )
                                focusRequesterIndex++
                            }
                            is DropdownFieldController -> {
                                DropDown(controller)
                            }
                        }
                    }
                }
                is TextSpec -> {
                    Text(
                        stringResource(element.stringResId),
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = element.color
                    )
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
 * @param: fieldLayout - this contains the visual layout of the fields on the screen used by [Form] to display the UI fields on screen.  It also informs us of the backing fields to be created.
 */
class FormViewModel(
    val layout: Layout,
) : ViewModel() {
    class Factory(
        private val layout: Layout,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return FormViewModel(layout) as T
        }
    }

    // This maps the field type to the controller
    private val fieldControllerMap: Map<SectionFieldSpec, FieldController> =
        layout.allFields.associateWith { field ->
            when (field) {
                Name -> TextFieldController(NameConfig()) // All configs should have the label passed in for consistency
                Email -> TextFieldController(EmailConfig())
                Country -> DropdownFieldController(CountryConfig())
            }
        }

    // This find the controller based on the field type
    internal fun getController(type: SectionFieldSpec) =
        requireNotNull(fieldControllerMap[type])

    fun getNumberTextFields() = fieldControllerMap.count { it.value is TextFieldController }

    // This is null if any form field values are incomplete, otherwise it is an object
    // representing all the complete fields
    val completeFormValues: Flow<FormFieldValues?> = combine(
        currentFormFieldValuesFlow(fieldControllerMap),
        allFormFieldsComplete(fieldControllerMap)
    ) { formFieldValue, isComplete ->
        formFieldValue.takeIf { isComplete }
    }

    companion object {
        // Flows of FormFieldValues for each of the fields as they are updated
        fun currentFormFieldValuesFlow(fieldControllerMap: Map<SectionFieldSpec, FieldController>) =
            combine(getCurrentFieldValuePairs(fieldControllerMap))
            {
                transformToFormFieldValues(it)
            }

        fun transformToFormFieldValues(allFormFieldValues: Array<Pair<SectionFieldSpec, String>>) =
            FormFieldValues(allFormFieldValues.toMap())

        fun getCurrentFieldValuePairs(fieldControllerMap: Map<SectionFieldSpec, FieldController>): List<Flow<Pair<SectionFieldSpec, String>>> {
            return fieldControllerMap.map { fieldControllerEntry ->
                getCurrentFieldValuePair(fieldControllerEntry.key, fieldControllerEntry.value)
            }
        }

        fun getCurrentFieldValuePair(
            field: SectionFieldSpec,
            value: FieldController
        ): Flow<Pair<SectionFieldSpec, String>> {
            return value.fieldValue.map {
                Pair(field, it)
            }
        }

        fun allFormFieldsComplete(fieldControllerMap: Map<SectionFieldSpec, FieldController>) =
            combine(fieldControllerMap.values.map { it.isComplete }) { fieldCompleteStates ->
                fieldCompleteStates.none { complete -> !complete }
            }
    }
}
