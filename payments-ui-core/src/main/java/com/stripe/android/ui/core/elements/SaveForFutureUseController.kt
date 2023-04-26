package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import com.stripe.android.ui.core.R
import com.stripe.android.uicore.elements.FieldError
import com.stripe.android.uicore.elements.InputController
import com.stripe.android.uicore.forms.FormFieldEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class SaveForFutureUseController(
    saveForFutureUseInitialValue: Boolean
) : InputController {
    override val label: Flow<Int> = MutableStateFlow(
        R.string.stripe_save_for_future_payments_with_merchant_name
    )
    private val _saveForFutureUse = MutableStateFlow(saveForFutureUseInitialValue)
    val saveForFutureUse: Flow<Boolean> = _saveForFutureUse
    override val fieldValue: Flow<String> = saveForFutureUse.map { it.toString() }
    override val rawFieldValue: Flow<String?> = fieldValue

    override val error: Flow<FieldError?> = MutableStateFlow(null)
    override val showOptionalLabel: Boolean = false
    override val isComplete: Flow<Boolean> = MutableStateFlow(true)
    override val formFieldValue: Flow<FormFieldEntry> =
        combine(isComplete, rawFieldValue) { complete, value ->
            FormFieldEntry(value, complete)
        }

    fun onValueChange(saveForFutureUse: Boolean) {
        _saveForFutureUse.value = saveForFutureUse
    }

    override fun onRawValueChange(rawValue: String) {
        onValueChange(rawValue.toBooleanStrictOrNull() ?: true)
    }
}
