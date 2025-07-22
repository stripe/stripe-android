package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.uicore.forms.FormFieldEntry
import com.stripe.android.uicore.utils.combineAsStateFlow
import com.stripe.android.uicore.utils.mapAsStateFlow
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * This class controls the dropdown view and implements the [InputController] interface.
 * Because it can never be in error the `errorMessage` is always null.  It is also
 * designed to always have a value selected, so isComplete is always true.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class DropdownFieldController(
    private val config: DropdownConfig,
    initialValue: String? = null
) : InputController, SectionFieldErrorController, SectionFieldComposable {
    val displayItems: List<String> = config.displayItems
    val disableDropdownWithSingleElement = config.disableDropdownWithSingleElement
    private val dropdownMode = config.mode
    private val initialIndex = (0).takeIf {
        dropdownMode !is DropdownConfig.Mode.Full || dropdownMode.selectsFirstOptionAsDefault
    }
    private val _selectedIndex = MutableStateFlow(initialIndex)
    val selectedIndex: StateFlow<Int?> = _selectedIndex
    override val label: StateFlow<ResolvableString> = MutableStateFlow(config.label)
    override val fieldValue = selectedIndex.mapAsStateFlow { it?.let { displayItems[it] } ?: "" }
    override val rawFieldValue = selectedIndex.mapAsStateFlow { it?.let { config.rawItems.getOrNull(it) } }
    override val error: StateFlow<FieldError?> = stateFlowOf(null)
    override val showOptionalLabel: Boolean = false // not supported yet
    override val isComplete: StateFlow<Boolean> = selectedIndex.mapAsStateFlow { it != null }
    override val formFieldValue: StateFlow<FormFieldEntry> =
        combineAsStateFlow(isComplete, rawFieldValue) { complete, value ->
            FormFieldEntry(value, complete)
        }

    val tinyMode = config.mode is DropdownConfig.Mode.Condensed

    init {
        initialValue?.let { onRawValueChange(it) }
    }

    /**
     * Get the label for the selected item, shown when the dropdown list is collapsed.
     */
    fun getSelectedItemLabel(index: Int?) = index?.let { config.getSelectedItemLabel(index) } ?: ""

    /**
     * This is called when the value changed to is a display value.
     */
    fun onValueChange(index: Int) {
        safelyUpdateSelectedIndex(index)
    }

    /**
     * This is called when the value changed to is a raw backing value, not a display value.
     */
    override fun onRawValueChange(rawValue: String) {
        safelyUpdateSelectedIndex(
            displayItems.indexOf(config.convertFromRaw(rawValue)).takeUnless { it == -1 } ?: initialIndex
        )
    }

    private fun safelyUpdateSelectedIndex(index: Int?) {
        index?.let {
            if (it < displayItems.size) {
                _selectedIndex.value = it
            }
        } ?: run {
            _selectedIndex.value = initialIndex
        }
    }

    @Composable
    override fun ComposeUI(
        enabled: Boolean,
        field: SectionFieldElement,
        modifier: Modifier,
        hiddenIdentifiers: Set<IdentifierSpec>,
        lastTextFieldIdentifier: IdentifierSpec?
    ) {
        DropDown(
            this,
            enabled,
            modifier = modifier,
        )
    }
}
