package com.stripe.android.paymentsheet.forms

import androidx.annotation.RestrictTo
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Checkbox
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.ExperimentalUnitApi
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import com.stripe.android.paymentsheet.FormElement.MandateTextElement
import com.stripe.android.paymentsheet.FormElement.SaveForFutureUseElement
import com.stripe.android.paymentsheet.FormElement.SectionElement
import com.stripe.android.paymentsheet.SectionFieldElement
import com.stripe.android.paymentsheet.elements.AddressController
import com.stripe.android.paymentsheet.elements.CardStyle
import com.stripe.android.paymentsheet.elements.DropDown
import com.stripe.android.paymentsheet.elements.DropdownFieldController
import com.stripe.android.paymentsheet.elements.InputController
import com.stripe.android.paymentsheet.elements.Section
import com.stripe.android.paymentsheet.elements.TextField
import com.stripe.android.paymentsheet.elements.TextFieldController
import com.stripe.android.paymentsheet.getIdInputControllerMap
import com.stripe.android.paymentsheet.specifications.FormItemSpec
import com.stripe.android.paymentsheet.specifications.IdentifierSpec
import com.stripe.android.paymentsheet.specifications.LayoutSpec
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

internal val formElementPadding = 16.dp

@ExperimentalUnitApi
@ExperimentalAnimationApi
@Composable
internal fun Form(
    formViewModel: FormViewModel,
) {
    val optionalIdentifiers by formViewModel.optionalIdentifiers.asLiveData().observeAsState(
        null
    )
    val enabled by formViewModel.enabled.asLiveData().observeAsState(true)

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
                        SectionElementUI(enabled, element, optionalIdentifiers)
                    }
                    is MandateTextElement -> {
                        MandateElementUI(element)
                    }
                    is SaveForFutureUseElement -> {
                        SaveForFutureUseElementUI(enabled, element)
                    }
                }
            }
        }
    }
}

@ExperimentalUnitApi
@ExperimentalAnimationApi
@Composable
internal fun SectionElementUI(
    enabled: Boolean,
    element: SectionElement,
    optionalIdentifiers: List<IdentifierSpec>?
) {
    AnimatedVisibility(
        optionalIdentifiers?.contains(element.identifier) == false,
        enter = EnterTransition.None,
        exit = ExitTransition.None
    ) {
        val controller = element.controller

        val error by controller.error.asLiveData().observeAsState(null)
        val sectionErrorString =
            error?.let {
                stringResource(
                    it.errorMessage,
                    stringResource(it.errorFieldLabel)
                )
            }

        Section(controller.label, sectionErrorString) {
            element.fields.forEachIndexed { index, field ->
                SectionFieldElementUI(enabled, field)
                if (index != element.fields.size - 1) {
                    val cardStyle = CardStyle(isSystemInDarkTheme())
                    Divider(
                        color = cardStyle.cardBorderColor,
                        thickness = cardStyle.cardBorderWidth,
                        modifier = Modifier.padding(
                            horizontal = cardStyle.cardBorderWidth
                        )
                    )
                }
            }
        }
    }
}

@ExperimentalUnitApi
@ExperimentalAnimationApi
@Composable
internal fun AddressElementUI(
    enabled: Boolean,
    addressSectionElement: SectionFieldElement.AddressElement,
    optionalIdentifiers: List<IdentifierSpec>?,
) {
    val fields by addressSectionElement.fields.asLiveData().observeAsState(emptyList())
    Column {
        fields.forEachIndexed { index, field ->
            SectionFieldElementUI(enabled, field)
            if (index != fields.size - 1) {
                val cardStyle = CardStyle(isSystemInDarkTheme())
                Divider(
                    color = cardStyle.cardBorderColor,
                    thickness = cardStyle.cardBorderWidth,
                    modifier = Modifier.padding(
                        horizontal = cardStyle.cardBorderWidth
                    )
                )
            }
        }
    }
}

@ExperimentalAnimationApi
@ExperimentalUnitApi
@Composable
internal fun SectionFieldElementUI(
    enabled: Boolean,
    field: SectionFieldElement
) {
    when (val controller = field.controller) {
        is TextFieldController -> {
            TextField(
                textFieldController = controller,
                enabled = enabled
            )
        }
        is DropdownFieldController -> {
            DropDown(
                controller.label,
                controller,
                enabled
            )
        }
        is AddressController -> {
            AddressElementUI(
                enabled,
                field as SectionFieldElement.AddressElement,
                emptyList()
            )
        }
    }
}

@Composable
internal fun MandateElementUI(
    element: MandateTextElement
) {
    Text(
        stringResource(element.stringResId, element.merchantName ?: ""),
        fontSize = 10.sp,
        letterSpacing = .7.sp,
        modifier = Modifier.padding(vertical = 8.dp),
        color = element.color
    )
}

@Composable
internal fun SaveForFutureUseElementUI(
    enabled: Boolean,
    element: SaveForFutureUseElement
) {
    val controller = element.controller
    val checked by controller.saveForFutureUse.asLiveData()
        .observeAsState(true)
    Row(modifier = Modifier.padding(vertical = 8.dp)) {
        Checkbox(
            checked = checked,
            onCheckedChange = { controller.onValueChange(it) },
            enabled = enabled
        )
        Text(
            stringResource(controller.label, element.merchantName ?: ""),
            Modifier
                .padding(start = 4.dp)
                .align(Alignment.CenterVertically)
                .clickable(
                    enabled, null,
                    null
                ) {
                    controller.toggleValue()
                },
            color = if (isSystemInDarkTheme()) {
                Color.LightGray
            } else {
                Color.Black
            }
        )
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
    internal class Factory(
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

    internal val enabled = MutableStateFlow(true)
    internal fun setEnabled(enabled: Boolean) {
        this.enabled.value = enabled
    }

    internal val elements = layout.items.transform(merchantName)

    private val saveForFutureUseVisible = MutableStateFlow(saveForFutureUseInitialVisibility)

    internal fun setSaveForFutureUseVisibility(isVisible: Boolean) {
        saveForFutureUseVisible.value = isVisible
    }

    internal fun setSaveForFutureUse(value: Boolean) {
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

    internal val saveForFutureUse = saveForFutureUseElement?.controller?.saveForFutureUse
        ?: MutableStateFlow(saveForFutureUseInitialValue)

    internal val sectionToFieldIdentifierMap = layout.items
        .filterIsInstance<FormItemSpec.SectionSpec>()
        .associate { sectionSpec ->
            sectionSpec.identifier to sectionSpec.fields.map {
                it.identifier
            }
        }

    internal val optionalIdentifiers =
        combine(
            saveForFutureUseVisible,
            saveForFutureUseElement?.controller?.optionalIdentifiers
                ?: MutableStateFlow(emptyList())
        ) { showFutureUse, optionalIdentifiers ->

            // For optional *section* identifiers, list of identifiers of elements in the section
            val identifiers = sectionToFieldIdentifierMap
                .filter { idControllerPair ->
                    optionalIdentifiers.contains(idControllerPair.key)
                }
                .flatMap { sectionToSectionFieldEntry ->
                    sectionToSectionFieldEntry.value
                }

            if (!showFutureUse && saveForFutureUseElement != null) {
                optionalIdentifiers
                    .plus(identifiers)
                    .plus(saveForFutureUseElement.identifier)
            } else {
                optionalIdentifiers
                    .plus(identifiers)
            }
        }

    // Mandate is showing if it is an element of the form and it isn't optional
    internal val showingMandate = optionalIdentifiers.map {
        elements
            .filterIsInstance<MandateTextElement>()
            .firstOrNull()?.let { mandate ->
                !it.contains(mandate.identifier)
            } ?: false
    }

    private val addressSectionFields = elements
        .filterIsInstance<SectionElement>()
        .flatMap { it.fields }
        .filterIsInstance<SectionFieldElement.AddressElement>()
        .firstOrNull()
        ?.fields
        ?: MutableStateFlow(null)

    val completeFormValues = addressSectionFields.map { addressSectionFields ->
        val addressInputControllers = addressSectionFields
            ?.filter { it.controller is InputController }
            ?.associate { sectionFieldElement ->
                sectionFieldElement.identifier to sectionFieldElement.controller as InputController
            }

        addressInputControllers?.plus(
            elements.getIdInputControllerMap()
        ) ?: elements.getIdInputControllerMap()
    }
        .flatMapLatest { value ->
            TransformElementToFormFieldValueFlow(
                value,
                optionalIdentifiers,
                showingMandate,
                saveForFutureUse
            ).transformFlow()
        }

    internal fun populateFormViewValues(formFieldValues: FormFieldValues) {
        populateWith(elements, formFieldValues)
    }
}
