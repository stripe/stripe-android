package com.stripe.android.paymentsheet.addresselement

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class AddressElementActivityProcessingState {
    private val _isProcessing = MutableStateFlow(false)

    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    fun tryStartProcessing(): Boolean {
        if (_isProcessing.value) return false

        _isProcessing.value = true
        return true
    }

    fun finishProcessing() {
        _isProcessing.value = false
    }
}
