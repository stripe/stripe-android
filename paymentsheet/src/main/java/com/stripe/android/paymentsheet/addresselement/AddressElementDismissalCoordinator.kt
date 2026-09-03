package com.stripe.android.paymentsheet.addresselement

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class AddressElementDismissalCoordinator @Inject constructor() {
    private val _isDirty = MutableStateFlow(false)
    val isDirty: StateFlow<Boolean> = _isDirty.asStateFlow()

    private val _showDiscardConfirmation = MutableStateFlow(false)
    val showDiscardConfirmation: StateFlow<Boolean> = _showDiscardConfirmation.asStateFlow()

    fun setDirty(isDirty: Boolean) {
        _isDirty.value = isDirty
    }

    fun requestDismiss(): Boolean {
        if (isDirty.value) {
            _showDiscardConfirmation.value = true
            return false
        }

        return true
    }

    fun keepEditing() {
        _showDiscardConfirmation.value = false
    }

    fun discardChanges() {
        _showDiscardConfirmation.value = false
        _isDirty.value = false
    }

    fun markSaved() {
        _showDiscardConfirmation.value = false
        _isDirty.value = false
    }
}
