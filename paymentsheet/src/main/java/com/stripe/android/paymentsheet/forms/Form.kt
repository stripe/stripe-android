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
import com.stripe.android.paymentsheet.elements.common.Controller
import com.stripe.android.paymentsheet.elements.common.DropDown
import com.stripe.android.paymentsheet.elements.common.Element.FocusRequesterCount
import com.stripe.android.paymentsheet.elements.common.Element.FormElement
import com.stripe.android.paymentsheet.elements.common.Element.SectionFieldElementType
import com.stripe.android.paymentsheet.elements.common.Section
import com.stripe.android.paymentsheet.elements.common.TextField
import com.stripe.android.paymentsheet.specifications.IdentifierSpec
import com.stripe.android.paymentsheet.specifications.LayoutSpec
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

// TODO: Save for future usage.

internal val formElementPadding = 16.dp

@ExperimentalAnimationApi
@Composable
internal fun Form(
    formViewModel: FormViewModel,
) {
    val focusRequesters =
        List(formViewModel.getCountFocusableFields()) { FocusRequester() }

    Column(
        modifier = Modifier
            .fillMaxWidth(1f)
            .padding(formElementPadding)
    ) {
        formViewModel.elements.forEach { element ->
            when (element) {
                is FormElement.SectionElement -> {
                    val controller = element.controller
                    val error by controller.errorMessage.asLiveData().observeAsState(null)
                    val sectionErrorString =
                        error?.let { stringResource(it, stringResource(controller.label)) }

                    Section(sectionErrorString) {
                        when (element.field) {
                            is SectionFieldElementType.TextFieldElement -> {
                                val focusRequesterIndex = element.field.focusIndexOrder
                                TextField(
                                    textFieldController = element.field.controller,
                                    myFocus = focusRequesters[focusRequesterIndex],
                                    nextFocus = focusRequesters.getOrNull(
                                        focusRequesterIndex + 1
                                    ),
                                )
                            }
                            is SectionFieldElementType.DropdownFieldElement -> {
                                DropDown(element.field.controller)
                            }
                        }
                    }
                }
                is FormElement.StaticTextElement -> {
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
 * @param: layout - this contains the visual layout of the fields on the screen used by [Form]
 * to display the UI fields on screen.  It also informs us of the backing fields to be created.
 */
class FormViewModel(
    layout: LayoutSpec,
) : ViewModel() {
    class Factory(
        private val layout: LayoutSpec,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return FormViewModel(layout) as T
        }
    }

    private var focusIndex = FocusRequesterCount()
    fun getCountFocusableFields() = focusIndex.get()

    private val specToFormTransform = TransformSpecToElement()
    internal val elements = specToFormTransform.transform(layout, focusIndex)

    // This maps the field type to the controller
    private val idControllerMap = elements
        .filter { it.controller != null }
        .associate { Pair(it.identifier, it.controller!!) }

    // This is null if any form field values are incomplete, otherwise it is an object
    // representing all the complete fields
    val completeFormValues: Flow<FormFieldValues?> = combine(
        currentFormFieldValuesFlow(idControllerMap),
        allFormFieldsComplete(idControllerMap)
    ) { formFieldValue, isComplete ->
        formFieldValue.takeIf { isComplete }
    }

    // Flows of FormFieldValues for each of the fields as they are updated
    fun currentFormFieldValuesFlow(idControllerMap: Map<IdentifierSpec, Controller>) =
        combine(getCurrentFieldValuePairs(idControllerMap))
        {
            transformToFormFieldValues(it)
        }

    fun transformToFormFieldValues(allFormFieldValues: Array<Pair<IdentifierSpec, String>>) =
        FormFieldValues(allFormFieldValues.toMap())

    fun getCurrentFieldValuePairs(idControllerMap: Map<IdentifierSpec, Controller>): List<Flow<Pair<IdentifierSpec, String>>> {
        return idControllerMap.map { fieldControllerEntry ->
            getCurrentFieldValuePair(fieldControllerEntry.key, fieldControllerEntry.value)
        }
    }

    fun getCurrentFieldValuePair(
        field: IdentifierSpec,
        value: Controller
    ): Flow<Pair<IdentifierSpec, String>> {
        return value.fieldValue.map {
            Pair(field, it)
        }
    }

    fun allFormFieldsComplete(fieldControllerMap: Map<IdentifierSpec, Controller>) =
        combine(fieldControllerMap.values.map { it.isComplete }) { fieldCompleteStates ->
            fieldCompleteStates.none { complete -> !complete }
        }
}
