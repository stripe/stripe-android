package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
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
    private val _selectedIndex = MutableStateFlow(0)
    val selectedIndex: StateFlow<Int> = _selectedIndex
    override val label: StateFlow<Int> = MutableStateFlow(config.label)
    override val fieldValue = selectedIndex.mapAsStateFlow { displayItems[it] }
    override val rawFieldValue = selectedIndex.mapAsStateFlow { config.rawItems.getOrNull(it) }
    override val error: StateFlow<FieldError?> = stateFlowOf(null)
    override val showOptionalLabel: Boolean = false // not supported yet
    override val isComplete: StateFlow<Boolean> = MutableStateFlow(true)
    override val formFieldValue: StateFlow<FormFieldEntry> =
        combineAsStateFlow(isComplete, rawFieldValue) { complete, value ->
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
        safelyUpdateSelectedIndex(index)
    }

    /**
     * This is called when the value changed to is a raw backing value, not a display value.
     */
    override fun onRawValueChange(rawValue: String) {
        safelyUpdateSelectedIndex(displayItems.indexOf(config.convertFromRaw(rawValue)).takeUnless { it == -1 } ?: 0)
    }

    private fun safelyUpdateSelectedIndex(index: Int) {
        if (index < displayItems.size) {
            _selectedIndex.value = index
        }
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
        DropDown(
            this,
            enabled
        )
    }
}
