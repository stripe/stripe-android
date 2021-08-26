package com.stripe.android.paymentsheet.forms

import android.content.Intent
import android.content.res.Resources
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Checkbox
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.google.accompanist.flowlayout.FlowCrossAxisAlignment
import com.google.accompanist.flowlayout.FlowRow
import com.stripe.android.paymentsheet.FormElement
import com.stripe.android.paymentsheet.FormElement.AfterpayClearpayHeaderElement
import com.stripe.android.paymentsheet.FormElement.MandateTextElement
import com.stripe.android.paymentsheet.FormElement.SaveForFutureUseElement
import com.stripe.android.paymentsheet.FormElement.SectionElement
import com.stripe.android.paymentsheet.R
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
import com.stripe.android.paymentsheet.injection.DaggerFormViewModelComponent
import com.stripe.android.paymentsheet.paymentdatacollection.FormFragmentArguments
import com.stripe.android.paymentsheet.specifications.FormItemSpec
import com.stripe.android.paymentsheet.specifications.IdentifierSpec
import com.stripe.android.paymentsheet.specifications.LayoutSpec
import com.stripe.android.paymentsheet.specifications.ResourceRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Composable
internal fun Form(formViewModel: FormViewModel) {
    FormInternal(
        formViewModel.hiddenIdentifiers,
        formViewModel.enabled,
        formViewModel.elements
    )
}

@Composable
internal fun FormInternal(
    hiddenIdentifiersFlow: Flow<List<IdentifierSpec>>,
    enabledFlow: Flow<Boolean>,
    elements: List<FormElement>
) {
    val hiddenIdentifiers by hiddenIdentifiersFlow.asLiveData().observeAsState(
        emptyList()
    )
    val enabled by enabledFlow.asLiveData().observeAsState(true)

    Column(
        modifier = Modifier
            .fillMaxWidth(1f)
    ) {
        elements.forEach { element ->
            if (!hiddenIdentifiers.contains(element.identifier)) {
                when (element) {
                    is SectionElement -> SectionElementUI(enabled, element, hiddenIdentifiers)
                    is MandateTextElement -> MandateElementUI(element)
                    is SaveForFutureUseElement -> SaveForFutureUseElementUI(enabled, element)
                    is AfterpayClearpayHeaderElement -> AfterpayClearpayElementUI(enabled, element)
                }
            }
        }
    }
}

@Composable
internal fun SectionElementUI(
    enabled: Boolean,
    element: SectionElement,
    hiddenIdentifiers: List<IdentifierSpec>?,
) {
    if (hiddenIdentifiers?.contains(element.identifier) == false) {
        val controller = element.controller

        val error by controller.error.asLiveData().observeAsState(null)
        val sectionErrorString = error?.let {
            it.formatArgs?.let { args ->
                stringResource(
                    it.errorMessage,
                    *args
                )
            } ?: stringResource(it.errorMessage)
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

@Composable
internal fun AddressElementUI(
    enabled: Boolean,
    controller: AddressController
) {
    val fields by controller.fieldsFlowable.asLiveData().observeAsState(emptyList())
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

@Composable
internal fun SectionFieldElementUI(
    enabled: Boolean,
    field: SectionFieldElement
) {
    when (val controller = field.sectionFieldErrorController()) {
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
                controller
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

@Composable
internal fun AfterpayClearpayElementUI(
    enabled: Boolean,
    element: AfterpayClearpayHeaderElement
) {
    val context = LocalContext.current

    FlowRow(
        modifier = Modifier.padding(4.dp, 8.dp, 4.dp, 4.dp),
        crossAxisAlignment = FlowCrossAxisAlignment.Center
    ) {
        Text(
            element.getLabel(context.resources),
            Modifier
                .padding(end = 4.dp),
            color = element.getTextColor()
        )
        Image(
            painter = painterResource(R.drawable.stripe_ic_afterpay_clearpay_logo),
            contentDescription = stringResource(
                R.string.stripe_paymentsheet_payment_method_afterpay_clearpay
            ),
            colorFilter = if (isSystemInDarkTheme()) {
                ColorFilter.tint(Color.White)
            } else {
                null
            }
        )
        TextButton(
            onClick = {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(element.infoUrl))
                )
            },
            modifier = Modifier.size(32.dp),
            enabled = enabled,
            contentPadding = PaddingValues(4.dp)
        ) {
            Text(
                text = "ⓘ",
                modifier = Modifier.padding(0.dp),
                style = TextStyle(fontWeight = FontWeight.Bold),
                color = element.getTextColor()
            )
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
@Singleton
internal class FormViewModel @Inject internal constructor(
    layout: LayoutSpec,
    config: FormFragmentArguments,
    private val resourceRepository: ResourceRepository
) : ViewModel() {
    internal class Factory(
        private val resources: Resources,
        private val layout: LayoutSpec,
        private val formArguments: FormFragmentArguments
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {

            // This is where we will call Dagger:
            return DaggerFormViewModelComponent.builder()
                .resources(resources)
                .layout(layout)
                .formFragmentArguments(formArguments)
                .build()
                .viewModel as T
        }
    }

    private val transformSpecToElement = TransformSpecToElement(resourceRepository, config)

    init {
        viewModelScope.launch {
            resourceRepository.init()
            elements = transformSpecToElement.transform(layout.items)
        }
    }

    internal val enabled = MutableStateFlow(true)
    internal fun setEnabled(enabled: Boolean) {
        this.enabled.value = enabled
    }

    internal lateinit var elements: List<FormElement>

    private val saveForFutureUseVisible = MutableStateFlow(config.saveForFutureUseInitialVisibility)

    internal fun setSaveForFutureUseVisibility(isVisible: Boolean) {
        saveForFutureUseVisible.value = isVisible
    }

    internal fun setSaveForFutureUse(value: Boolean) {
        elements
            .filterIsInstance<SaveForFutureUseElement>()
            .firstOrNull()?.controller?.onValueChange(value)
    }

    private val saveForFutureUseElement = elements
        .filterIsInstance<SaveForFutureUseElement>()
        .firstOrNull()

    internal val saveForFutureUse = saveForFutureUseElement?.controller?.saveForFutureUse
        ?: MutableStateFlow(config.saveForFutureUseInitialValue)

    private val sectionToFieldIdentifierMap = layout.items
        .filterIsInstance<FormItemSpec.SectionSpec>()
        .associate { sectionSpec ->
            sectionSpec.identifier to sectionSpec.fields.map {
                it.identifier
            }
        }

    internal val hiddenIdentifiers =
        combine(
            saveForFutureUseVisible,
            saveForFutureUseElement?.controller?.hiddenIdentifiers
                ?: MutableStateFlow(emptyList())
        ) { showFutureUse, hiddenIdentifiers ->

            // For hidden *section* identifiers, list of identifiers of elements in the section
            val identifiers = sectionToFieldIdentifierMap
                .filter { idControllerPair ->
                    hiddenIdentifiers.contains(idControllerPair.key)
                }
                .flatMap { sectionToSectionFieldEntry ->
                    sectionToSectionFieldEntry.value
                }

            if (!showFutureUse && saveForFutureUseElement != null) {
                hiddenIdentifiers
                    .plus(identifiers)
                    .plus(saveForFutureUseElement.identifier)
            } else {
                hiddenIdentifiers
                    .plus(identifiers)
            }
        }

    // Mandate is showing if it is an element of the form and it isn't hidden
    internal val showingMandate = hiddenIdentifiers.map {
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

    @ExperimentalCoroutinesApi
    val completeFormValues = addressSectionFields.map { addressSectionFields ->
        addressSectionFields
            ?.filter { it.controller is InputController }
            ?.associate { sectionFieldElement ->
                sectionFieldElement.identifier to sectionFieldElement.controller as InputController
            }
            ?.plus(
                elements.getIdInputControllerMap()
            ) ?: elements.getIdInputControllerMap()
    }
        .flatMapLatest { value ->
            TransformElementToFormFieldValueFlow(
                value,
                hiddenIdentifiers,
                showingMandate,
                saveForFutureUse
            ).transformFlow()
        }
}
