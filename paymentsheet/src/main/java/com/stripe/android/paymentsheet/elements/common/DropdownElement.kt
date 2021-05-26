package com.stripe.android.paymentsheet.elements.common

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations

internal class DropdownElement(
    private val config: DropdownConfig,
) {
    val displayItems: List<String> = config.getDisplayItems()

    private val _selectedIndex = MutableLiveData(0)
    val selectedIndex: LiveData<Int> = _selectedIndex

    val paymentMethodParams = Transformations.map(selectedIndex) {
        config.getPaymentMethodParams()[it]
    }

    fun onValueChange(index: Int) {
        _selectedIndex.value = index
    }
}