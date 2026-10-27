package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.uicore.R
import com.stripe.android.uicore.forms.FormFieldEntry
import com.stripe.android.uicore.utils.combineAsStateFlow
import com.stripe.android.uicore.utils.mapAsStateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Keeps the selected raw value independent of option ordering, so collapsed dropdowns do not need
 * to load their complete list of display labels.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class DropdownFieldController(
    private val config: DropdownConfig,
    initialValue: String? = null,
) : InputController, SectionFieldValidationController, SectionFieldComposable {
    val autofillType = config.autofillType
    val displayItems: List<String>
        get() = config.displayItems
    val itemCount: Int
        get() = config.itemCount
    val disableDropdownWithSingleElement = config.disableDropdownWithSingleElement
    private val dropdownMode = config.mode
    private val initialItem by lazy {
        if (dropdownMode !is DropdownConfig.Mode.Full || dropdownMode.selectsFirstOptionAsDefault) {
            config.defaultItem
        } else {
            null
        }
    }
    private val _validating = MutableStateFlow(false)
    private val _selection = MutableStateFlow(initialValue?.let(config::itemForRaw) ?: initialItem)
    override val rawFieldValue = _selection.mapAsStateFlow { it?.rawValue }

    // Resolving an index can require loading all options; only read it when displaying the menu.
    val selectedIndex: StateFlow<Int?> = _selection.mapAsStateFlow { item ->
        item?.let {
            config.rawItems.indices.firstOrNull { index ->
                config.rawItems[index] == it.rawValue && displayItems[index] == it.displayText
            }
        }
    }
    val selectedItemLabel: StateFlow<String> = _selection.mapAsStateFlow { it?.selectedLabel.orEmpty() }
    override val label: StateFlow<ResolvableString> = MutableStateFlow(config.label)
    override val fieldValue = _selection.mapAsStateFlow { it?.displayText.orEmpty() }
    override val validationMessage: StateFlow<FieldValidationMessage?> = combineAsStateFlow(
        _validating,
        _selection,
    ) { validating, value ->
        if (validating && value == null) {
            FieldValidationMessage.Error(R.string.stripe_blank_and_required)
        } else {
            null
        }
    }
    override val showOptionalLabel: Boolean = false // not supported yet
    override val isComplete: StateFlow<Boolean> = _selection.mapAsStateFlow { it != null }
    override val formFieldValue: StateFlow<FormFieldEntry> =
        combineAsStateFlow(isComplete, rawFieldValue) { complete, value ->
            FormFieldEntry(value, complete)
        }

    val tinyMode = config.mode is DropdownConfig.Mode.Condensed

    /**
     * Get the label for the selected item, shown when the dropdown list is collapsed.
     */
    fun getSelectedItemLabel(index: Int?) = index?.let { config.getSelectedItemLabel(index) } ?: ""

    /**
     * This is called when the value changed to is a display value.
     */
    fun onValueChange(index: Int) {
        if (index < itemCount) {
            _selection.value = config.getItem(index) ?: initialItem
        }
    }

    override fun onValidationStateChanged(isValidating: Boolean) {
        _validating.value = isValidating
    }

    /**
     * This is called when the value changed to is a raw backing value, not a display value.
     */
    override fun onRawValueChange(rawValue: String) {
        _selection.value = config.itemForRaw(rawValue) ?: initialItem
    }

    fun onAutofillValue(value: String) {
        _selection.value = config.itemForAutofill(value) ?: initialItem
    }

    @Composable
    override fun ComposeUI(
        enabled: Boolean,
        field: SectionFieldElement,
        modifier: Modifier,
        hiddenIdentifiers: Set<FormFieldId>,
        lastTextFieldIdentifier: FormFieldId?
    ) {
        DropDown(
            this,
            enabled,
            modifier = modifier,
        )
    }
}
