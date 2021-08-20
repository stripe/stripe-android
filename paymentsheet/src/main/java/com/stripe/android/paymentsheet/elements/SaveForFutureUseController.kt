package com.stripe.android.paymentsheet.elements

import com.stripe.android.paymentsheet.Identifier
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.specifications.IdentifierSpec
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

internal class SaveForFutureUseController(
    identifiersRequiredForFutureUse: List<Identifier> = emptyList()
) : InputController {
    override val label: Int = R.string.stripe_paymentsheet_save_for_future_payments
    private val _saveForFutureUse = MutableStateFlow(true)
    val saveForFutureUse: Flow<Boolean> = _saveForFutureUse
    override val fieldValue: Flow<String> = saveForFutureUse.map { it.toString() }
    override val rawFieldValue: Flow<String?> = fieldValue

    override val error: Flow<FieldError?> = MutableStateFlow(null)
    override val showOptionalLabel: Boolean = false
    override val isComplete: Flow<Boolean> = MutableStateFlow(true)

    val hiddenIdentifiers: Flow<List<Identifier>> =
        saveForFutureUse.map { saveFutureUseInstance ->
            identifiersRequiredForFutureUse.takeUnless { saveFutureUseInstance } ?: emptyList()
        }

    fun onValueChange(saveForFutureUse: Boolean) {
        _saveForFutureUse.value = saveForFutureUse
    }

    override fun onRawValueChange(rawValue: String) {
        onValueChange(rawValue.toBooleanStrictOrNull() ?: true)
    }

    fun toggleValue() {
        _saveForFutureUse.value = !_saveForFutureUse.value
    }
}
