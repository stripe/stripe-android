package com.stripe.android.paymentsheet.elements.common

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.distinctUntilChanged

internal class DropdownElement(
    private val config: DropdownConfig,
) {
    val displayItems: List<String> = config.getDisplayItems()

    private val _selectedIndex = MutableLiveData<Int>()
    val selectedIndex: LiveData<Int> = MutableLiveData()

    val paymentMethodParams = Transformations.map(selectedIndex) {
        config.getPaymentMethodParams()[it]
    }.distinctUntilChanged()

    init {
        onValueChange(0)
    }

    fun onValueChange(index: Int) {
        _selectedIndex.value = index
    }
}