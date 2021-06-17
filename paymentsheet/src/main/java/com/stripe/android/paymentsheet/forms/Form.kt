package com.stripe.android.paymentsheet.forms

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Checkbox
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
import com.stripe.android.paymentsheet.elements.common.DropDown
import com.stripe.android.paymentsheet.elements.common.FocusRequesterCount
import com.stripe.android.paymentsheet.elements.common.FormElement
import com.stripe.android.paymentsheet.elements.common.Section
import com.stripe.android.paymentsheet.elements.common.SectionFieldElementType
import com.stripe.android.paymentsheet.elements.common.TextField
import com.stripe.android.paymentsheet.specification.IdentifierSpec
import com.stripe.android.paymentsheet.specification.LayoutSpec
import kotlinx.coroutines.flow.MutableStateFlow

internal val formElementPadding = 16.dp

@ExperimentalAnimationApi
@Composable
internal fun Form(
    formViewModel: FormViewModel,
) {
    val focusRequesters =
        List(formViewModel.getCountFocusableFields()) { FocusRequester() }
    val isOptionalIdentifiers by formViewModel.optionalIdentifiers.asLiveData().observeAsState(
        emptyList()
    )

    Column(
        modifier = Modifier
            .fillMaxWidth(1f)
            .padding(formElementPadding)
    ) {
        formViewModel.elements.forEach { element ->

            AnimatedVisibility(!isOptionalIdentifiers.contains(element.identifier)) {
                when (element) {
                    is FormElement.SectionElement -> {

                        AnimatedVisibility(!isOptionalIdentifiers.contains(element.identifier)) {
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
                                            nextFocus = if (focusRequesterIndex == focusRequesters.size - 1) {
                                                null
                                            } else {
                                                focusRequesters[focusRequesterIndex + 1]
                                            },
                                        )
                                    }
                                    is SectionFieldElementType.DropdownFieldElement -> {
                                        DropDown(element.field.controller)
                                    }
                                }
                            }
                        }
                    }
                    is FormElement.StaticElement -> {
                        Text(
                            stringResource(element.stringResId),
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = element.color
                        )
                    }
                    is FormElement.SaveForFutureUseElement -> {
                        val controller = element.controller
                        val checked by controller.saveForFutureUse.asLiveData().observeAsState(true)
                        Row(modifier = Modifier.padding(vertical = 8.dp)) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { controller.onValueChange(it) }
                            )
                            Text(stringResource(controller.label))
                        }
                    }
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
    internal val elements = specToFormTransform.createElement(layout, focusIndex)

    val optionalIdentifiers = elements
        .filterIsInstance<FormElement.SaveForFutureUseElement>()
        .firstOrNull()?.controller?.optionalIdentifiers
        ?: MutableStateFlow(emptyList())

    // This maps the field type to the controller
    private val idControllerMap = elements
        .filter { it.controller != null }
        .associate { Pair(it.identifier, it.controller!!) }

    val completeFormValues = TransformElementToFormFieldValueFlow(
        elements, optionalIdentifiers
    ).transformFlow()

    internal val populateFormFromFormFieldValues = PopulateFormFromFormFieldValues(elements)
    fun populateFormViewValues(formFieldValues: FormFieldValues) {
        populateFormFromFormFieldValues.populateWith(formFieldValues)
    }
}

data class FieldSnapshot(
    val fieldValue: String,
    val identifier: IdentifierSpec,
    val isComplete: Boolean
)