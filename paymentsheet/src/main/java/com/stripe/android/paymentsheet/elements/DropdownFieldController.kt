package com.stripe.android.paymentsheet.elements

import com.stripe.android.paymentsheet.ElementType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * This class controls the dropdown view and implements the [Controller] interface.
 * Because it can never be in error the `errorMessage` is always null.  It is also
 * designed to always have a value selected, so isComplete is always true.
 */
internal class DropdownFieldController(
    config: DropdownConfig,
) : Controller {
    val displayItems: List<String> = config.getDisplayItems()
    private val _selectedIndex = MutableStateFlow(0)
    val selectedIndex: Flow<Int> = _selectedIndex
    override val label: Int = config.label
    override val fieldValue = selectedIndex.map { displayItems[it] }
    override val errorMessage: Flow<Int?> = MutableStateFlow(null)
    override val isComplete: Flow<Boolean> = MutableStateFlow(true)
    override val elementType: ElementType = config.elementType

    fun onValueChange(index: Int) {
        _selectedIndex.value = index
    }

    override fun onValueChange(value: String) {
        _selectedIndex.value = displayItems.indexOf(value).takeUnless { it == -1 } ?: 0
    }
}
