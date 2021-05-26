package com.stripe.android.paymentsheet.elements.common

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.mapLatest

@ExperimentalCoroutinesApi
internal class DropdownElement(
    private val config: DropdownConfig,
) {
    val displayItems: List<String> = config.getDisplayItems()

    private val _selectedIndex = MutableStateFlow(0)
    val selectedIndex: Flow<Int> = _selectedIndex

    val paymentMethodParams = selectedIndex.mapLatest { config.getPaymentMethodParams()[it] }

    fun onValueChange(index: Int) {
        _selectedIndex.value = index
    }
}