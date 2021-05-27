package com.stripe.android.paymentsheet.elements.common

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

internal class DropdownElement(
    private val config: DropdownConfig,
) {
    val displayItems: List<String> = config.getDisplayItems()

    private val _selectedIndex = MutableStateFlow(0)
    val selectedIndex: Flow<Int> = _selectedIndex

    val paymentMethodParams = selectedIndex.map { config.getPaymentMethodParams()[it] }

    fun onValueChange(index: Int) {
        _selectedIndex.value = index
    }
}