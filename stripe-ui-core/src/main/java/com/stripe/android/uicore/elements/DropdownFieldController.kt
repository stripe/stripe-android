package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import com.stripe.android.uicore.forms.FormFieldEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

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
    private val _selectedIndex = MutableStateFlow(0)
    val selectedIndex: StateFlow<Int> = _selectedIndex
    override val label: Flow<Int> = MutableStateFlow(config.label)
    override val fieldValue = selectedIndex.map { displayItems[it] }
    override val rawFieldValue = selectedIndex.map { config.rawItems[it] }
    override val error: Flow<FieldError?> = MutableStateFlow(null)
    override val showOptionalLabel: Boolean = false // not supported yet
    override val isComplete: Flow<Boolean> = MutableStateFlow(true)
    override val formFieldValue: Flow<FormFieldEntry> =
        combine(isComplete, rawFieldValue) { complete, value ->
            FormFieldEntry(value, complete)
        }

    val tinyMode = config.tinyMode

    init {
        initialValue?.let { onRawValueChange(it) }
    }

    /**
     * Get the label for the selected item, shown when the dropdown list is collapsed.
     */
    fun getSelectedItemLabel(index: Int) = config.getSelectedItemLabel(index)

    /**
     * This is called when the value changed to is a display value.
     */
    fun onValueChange(index: Int) {
        _selectedIndex.value = index
    }

    /**
     * This is called when the value changed to is a raw backing value, not a display value.
     */
    override fun onRawValueChange(rawValue: String) {
        _selectedIndex.value =
            displayItems.indexOf(config.convertFromRaw(rawValue)).takeUnless { it == -1 } ?: 0
    }

    @Composable
    override fun ComposeUI(
        enabled: Boolean,
        field: SectionFieldElement,
        modifier: Modifier,
        hiddenIdentifiers: Set<IdentifierSpec>,
        lastTextFieldIdentifier: IdentifierSpec?,
        nextFocusDirection: FocusDirection,
        previousFocusDirection: FocusDirection
    ) {
        Dropdown(
            this,
            enabled
        )
    }
}
