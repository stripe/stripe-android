package com.stripe.android.paymentsheet.elements.common

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

internal class DropdownElement(
    private val config: DropdownConfig,
) : Element {
    val displayItems: List<String> = config.getDisplayItems()
    private val _selectedIndex = MutableStateFlow(0)
    val selectedIndex: Flow<Int> = _selectedIndex
    override val label: Int = config.label
    override val fieldValue = selectedIndex.map { displayItems[it] }
    override val errorMessage: Flow<Int?> = MutableStateFlow(null)
    override val isComplete: Flow<Boolean> = MutableStateFlow(true)

    fun onValueChange(index: Int) {
        _selectedIndex.value = index
    }
}