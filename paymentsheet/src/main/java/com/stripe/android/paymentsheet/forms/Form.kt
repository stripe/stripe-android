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
import com.stripe.android.paymentsheet.FocusRequesterCount
import com.stripe.android.paymentsheet.FormElement
import com.stripe.android.paymentsheet.FormElement.SectionElement
import com.stripe.android.paymentsheet.FormElement.StaticTextElement
import com.stripe.android.paymentsheet.SectionFieldElementType.DropdownFieldElement
import com.stripe.android.paymentsheet.SectionFieldElementType.TextFieldElement
import com.stripe.android.paymentsheet.elements.common.Controller
import com.stripe.android.paymentsheet.elements.common.DropDown
import com.stripe.android.paymentsheet.elements.common.Section
import com.stripe.android.paymentsheet.elements.common.TextField
import com.stripe.android.paymentsheet.specifications.IdentifierSpec
import com.stripe.android.paymentsheet.specifications.LayoutSpec
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine

internal val formElementPadding = 16.dp

@ExperimentalAnimationApi
@Composable
internal fun Form(
    formViewModel: FormViewModel,
) {
    val focusRequesters =
        List(formViewModel.getCountFocusableFields()) { FocusRequester() }
    val optionalIdentifiers by formViewModel.optionalIdentifiers.asLiveData().observeAsState(
        emptyList()
    )

    Column(
        modifier = Modifier
            .fillMaxWidth(1f)
    ) {
        formViewModel.elements.forEach { element ->

            AnimatedVisibility(!optionalIdentifiers.contains(element.identifier)) {
                when (element) {
                    is SectionElement -> {
                        AnimatedVisibility(!optionalIdentifiers.contains(element.identifier)) {
                            val controller = element.controller

                            val error by controller.errorMessage.asLiveData().observeAsState(null)
                            val sectionErrorString =
                                error?.let { stringResource(it, stringResource(controller.label)) }

                            Section(sectionErrorString) {
                                when (element.field) {
                                    is TextFieldElement -> {
                                        val focusRequesterIndex = element.field.focusIndexOrder
                                        TextField(
                                            textFieldController = element.field.controller,
                                            myFocus = focusRequesters[focusRequesterIndex],
                                            nextFocus = focusRequesters.getOrNull(
                                                focusRequesterIndex + 1
                                            )
                                        )
                                    }
                                    is DropdownFieldElement -> {
                                        DropDown(element.field.controller)
                                    }
                                }
                            }
                        }
                    }
                    is StaticTextElement -> {
                        Text(
                            stringResource(element.stringResId),
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = element.color
                        )
                    }

                    is FormElement.SaveForFutureUseElement -> {
                        val controller = element.controller
                        val checked by controller.saveForFutureUse.asLiveData()
                            .observeAsState(true)
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
    internal val elements = specToFormTransform.transform(layout, focusIndex)

    val optionalIdentifiers = elements
        .filterIsInstance<FormElement.SaveForFutureUseElement>()
        .firstOrNull()?.controller?.optionalIdentifiers
        ?: MutableStateFlow(emptyList())

    // This maps the field type to the controller
    private val idControllerMap = elements
        .filter { it.controller != null }
        .associate { Pair(it.identifier, it.controller!!) }

    val currentFieldValueMap = combine(
        getCurrentFieldValuePairs(idControllerMap)
    ) {
        it.toMap()
    }

    // This is null if any form field values are incomplete, otherwise it is an object
    // representing all the complete fields
    val completeFormValues: Flow<FormFieldValues?> = combine(
        currentFieldValueMap,
        optionalIdentifiers
    ) { idFieldSnapshotMap, optionalIdentifiers ->

        // This will hit twice in a row when the save for future use state changes: once for the
        // saveController changing and once for the the optionalFields changing
        val optionalFilteredFieldSnapshotMap = idFieldSnapshotMap.filter {
            !optionalIdentifiers.contains(it.key)
        }

        FormFieldValues(
            optionalFilteredFieldSnapshotMap.mapValues {
                it.value.fieldValue
            }
        ).takeIf {
            optionalFilteredFieldSnapshotMap.values.map { it.isComplete }
                .none { complete -> !complete }
        }
    }

    private fun getCurrentFieldValuePairs(idControllerMap: Map<IdentifierSpec, Controller>) =
        idControllerMap.map { fieldControllerEntry ->
            getCurrentFieldValuePair(fieldControllerEntry.key, fieldControllerEntry.value)
        }

    private fun getCurrentFieldValuePair(
        field: IdentifierSpec,
        value: Controller
    ) = combine(value.fieldValue, value.isComplete) { fieldValue, isComplete ->
        Pair(field, FieldSnapshot(fieldValue, field, isComplete))
    }
}

data class FieldSnapshot(
    val fieldValue: String,
    val identifier: IdentifierSpec,
    val isComplete: Boolean
)
