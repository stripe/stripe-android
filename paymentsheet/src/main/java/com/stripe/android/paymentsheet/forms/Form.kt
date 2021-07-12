package com.stripe.android.paymentsheet.forms

import androidx.annotation.RestrictTo
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import com.stripe.android.paymentsheet.FocusRequesterCount
import com.stripe.android.paymentsheet.FormElement.MandateTextElement
import com.stripe.android.paymentsheet.FormElement.SaveForFutureUseElement
import com.stripe.android.paymentsheet.FormElement.SectionElement
import com.stripe.android.paymentsheet.SectionFieldElementType.DropdownFieldElement
import com.stripe.android.paymentsheet.SectionFieldElementType.TextFieldElement
import com.stripe.android.paymentsheet.elements.DropDown
import com.stripe.android.paymentsheet.elements.Section
import com.stripe.android.paymentsheet.elements.TextField
import com.stripe.android.paymentsheet.specifications.LayoutSpec
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

internal val formElementPadding = 16.dp

@ExperimentalAnimationApi
@Composable
internal fun Form(
    formViewModel: FormViewModel,
) {
    val focusRequesters =
        List(formViewModel.getCountFocusableFields()) { FocusRequester() }
    val optionalIdentifiers by formViewModel.optionalIdentifiers.asLiveData().observeAsState(
        null
    )

    Column(
        modifier = Modifier
            .fillMaxWidth(1f)
    ) {
        formViewModel.elements.forEach { element ->

            AnimatedVisibility(
                optionalIdentifiers?.contains(element.identifier) == false,
                enter = EnterTransition.None,
                exit = ExitTransition.None
            ) {
                when (element) {
                    is SectionElement -> {
                        AnimatedVisibility(
                            optionalIdentifiers?.contains(element.identifier) == false,
                            enter = EnterTransition.None,
                            exit = ExitTransition.None
                        ) {
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
                                        DropDown(
                                            element.field.controller.label,
                                            element.field.controller
                                        )
                                    }
                                }
                            }
                        }
                    }
                    is MandateTextElement -> {
                        Text(
                            stringResource(element.stringResId, element.merchantName ?: ""),
                            fontSize = 10.sp,
                            letterSpacing = .7.sp,
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = element.color
                        )
                    }

                    is SaveForFutureUseElement -> {
                        val controller = element.controller
                        val checked by controller.saveForFutureUse.asLiveData()
                            .observeAsState(true)
                        Row(modifier = Modifier.padding(vertical = 8.dp)) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { controller.onValueChange(it) }
                            )
                            Text(
                                stringResource(controller.label, element.merchantName ?: ""),
                                Modifier.padding(start = 8.dp)
                            )
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
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class FormViewModel(
    layout: LayoutSpec,
    saveForFutureUseInitialValue: Boolean,
    saveForFutureUseInitialVisibility: Boolean,
    merchantName: String,
) : ViewModel() {
    class Factory(
        private val layout: LayoutSpec,
        private val saveForFutureUseValue: Boolean,
        private val saveForFutureUseVisibility: Boolean,
        private val merchantName: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return FormViewModel(
                layout,
                saveForFutureUseValue,
                saveForFutureUseVisibility,
                merchantName
            ) as T
        }
    }

    private var focusIndex = FocusRequesterCount()
    fun getCountFocusableFields() = focusIndex.get()

    internal val elements = transform(
        layout,
        merchantName,
        focusIndex
    )

    private val saveForFutureUseVisible = MutableStateFlow(saveForFutureUseInitialVisibility)

    fun setSaveForFutureUseVisibility(isVisible: Boolean) {
        saveForFutureUseVisible.value = isVisible
    }

    fun setSaveForFutureUse(value: Boolean) {
        elements
            .filterIsInstance<SaveForFutureUseElement>()
            .firstOrNull()?.controller?.onValueChange(value)
    }

    init {
        setSaveForFutureUse(saveForFutureUseInitialValue)
    }

    private val saveForFutureUseElement = elements
        .filterIsInstance<SaveForFutureUseElement>()
        .firstOrNull()

    val saveForFutureUse = saveForFutureUseElement?.controller?.saveForFutureUse
        ?: MutableStateFlow(saveForFutureUseInitialValue)

    val optionalIdentifiers =
        combine(
            saveForFutureUseVisible,
            saveForFutureUseElement?.controller?.optionalIdentifiers
                ?: MutableStateFlow(emptyList())
        ) { showFutureUse, optionalIdentifiers ->
            if (!showFutureUse && saveForFutureUseElement != null) {
                optionalIdentifiers.plus(
                    saveForFutureUseElement.identifier
                )
            } else {
                optionalIdentifiers
            }
        }

    // Mandate is showing if it is an element of the form and it isn't optional
    val showingMandate = optionalIdentifiers.map {
        elements
            .filterIsInstance<MandateTextElement>()
            .firstOrNull()?.let { mandate ->
                !it.contains(mandate.identifier)
            } ?: false
    }

    val completeFormValues = TransformElementToFormFieldValueFlow(
        elements, optionalIdentifiers, showingMandate, saveForFutureUse
    ).transformFlow()

    fun populateFormViewValues(formFieldValues: FormFieldValues) {
        populateWith(elements, formFieldValues)
    }
}
