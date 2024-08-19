package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import com.stripe.android.ui.core.R
import com.stripe.android.uicore.elements.FieldError
import com.stripe.android.uicore.elements.InputController
import com.stripe.android.uicore.forms.FormFieldEntry
import com.stripe.android.uicore.utils.combineAsStateFlow
import com.stripe.android.uicore.utils.mapAsStateFlow
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class SaveForFutureUseController(
    saveForFutureUseInitialValue: Boolean
) : InputController {
    override val label: StateFlow<Int> = MutableStateFlow(
        R.string.stripe_save_payment_details_to_merchant_name
    )
    private val _saveForFutureUse = MutableStateFlow(saveForFutureUseInitialValue)
    val saveForFutureUse: StateFlow<Boolean> = _saveForFutureUse
    override val fieldValue: StateFlow<String> = saveForFutureUse.mapAsStateFlow { it.toString() }
    override val rawFieldValue: StateFlow<String?> = fieldValue

    override val error: StateFlow<FieldError?> = stateFlowOf(null)
    override val showOptionalLabel: Boolean = false
    override val isComplete: StateFlow<Boolean> = stateFlowOf(true)
    override val formFieldValue: StateFlow<FormFieldEntry> =
        combineAsStateFlow(isComplete, rawFieldValue) { complete, value ->
            FormFieldEntry(value, complete)
        }

    fun onValueChange(saveForFutureUse: Boolean) {
        _saveForFutureUse.value = saveForFutureUse
    }

    override fun onRawValueChange(rawValue: String) {
        onValueChange(rawValue.toBooleanStrictOrNull() ?: true)
    }
}
