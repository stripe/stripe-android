package com.stripe.android.paymentsheet.elements.common

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.distinctUntilChanged

internal class DropdownElement(
    private val config: DropdownConfig,
) {
    val items: List<String> = config.getItems()

    /** This is all the information that can be observed on the element */
    private val _paramValue: MutableLiveData<String?> = MutableLiveData(null)
    val paramValue: LiveData<String?> = _paramValue.distinctUntilChanged()
    val displayValue: LiveData<String> = Transformations.map(paramValue) {
        config.convertToDisplay(it)
    }

    init {
        onValueChange(items[0])
    }

    fun onValueChange(displayFormatted: String) {
        _paramValue.value =
            config.convertToPaymentMethodParam(displayFormatted)
    }
}