package com.stripe.android.paymentsheet.elements

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * This class controls the dropdown view and implements the [InputController] interface.
 * Because it can never be in error the `errorMessage` is always null.  It is also
 * designed to always have a value selected, so isComplete is always true.
 */
internal class DropdownFieldController(
    private val config: DropdownConfig,
) : InputController {
    val displayItems: List<String> = config.getDisplayItems()
    private val _selectedIndex = MutableStateFlow(0)
    val selectedIndex: Flow<Int> = _selectedIndex
    override val label: Int = config.label
    override val fieldValue = selectedIndex.map {
        Log.d("STRIPE", "Updating field value. " + it)
        displayItems[it]
    }
    override val rawFieldValue = fieldValue.map {
        Log.d("STRIPE", "Updating field raw value. " + it)
        config.convertToRaw(it)
    }
    override val error: Flow<FieldError?> = MutableStateFlow(null)
    override val isComplete: Flow<Boolean> = MutableStateFlow(true)

    init {
        Log.e("STRIPE", "Dropdown controller created")
    }

    /**
     * This is called when the value changed to is a display value.
     */
    fun onValueChange(index: Int) {
        Log.d("STRIPE", "Dropdown on value change.")
        _selectedIndex.value = index
    }

    /**
     * This is called when the value changed to is a raw backing value, not a display value.
     */
    override fun onRawValueChange(rawValue: String) {
        Log.d("STRIPE", "Dropdown on raw value change.")
        _selectedIndex.value =
            displayItems.indexOf(config.convertFromRaw(rawValue)).takeUnless { it == -1 } ?: 0
    }
}
