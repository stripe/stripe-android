package com.stripe.android.paymentsheet.addresselement

import com.stripe.android.uicore.utils.flatMapLatestAsStateFlow
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class AddressElementDismissalCoordinator @Inject constructor() {
    private val changes = MutableStateFlow<StateFlow<Boolean>>(stateFlowOf(false))
    val isDirty: StateFlow<Boolean> = changes.flatMapLatestAsStateFlow { it }

    var isSaving: Boolean = false
        private set

    private val _showDiscardConfirmation = MutableStateFlow(false)
    val showDiscardConfirmation: StateFlow<Boolean> = _showDiscardConfirmation.asStateFlow()

    fun observeChanges(changes: StateFlow<Boolean>) {
        this.changes.value = changes
    }

    fun setSaving(isSaving: Boolean) {
        this.isSaving = isSaving
        if (isSaving) keepEditing()
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
        changes.value = stateFlowOf(false)
    }

    fun markSaved() {
        _showDiscardConfirmation.value = false
        changes.value = stateFlowOf(false)
        isSaving = false
    }
}
