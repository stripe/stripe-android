package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import com.stripe.android.uicore.R
import com.stripe.android.uicore.forms.FormFieldEntry
import com.stripe.android.uicore.utils.mapAsStateFlow
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class SameAsShippingController(
    initialValue: Boolean
) : InputController {
    override val label: StateFlow<Int> = stateFlowOf(R.string.stripe_billing_same_as_shipping)
    private val _value = MutableStateFlow(initialValue)
    val value: StateFlow<Boolean> = _value.asStateFlow()
    override val fieldValue: StateFlow<String> = value.mapAsStateFlow { it.toString() }
    override val rawFieldValue: StateFlow<String?> = fieldValue

    override val error: StateFlow<FieldError?> = stateFlowOf(null)
    override val showOptionalLabel: Boolean = false
    override val isComplete: StateFlow<Boolean> = stateFlowOf(true)
    override val formFieldValue: StateFlow<FormFieldEntry> =
        rawFieldValue.mapAsStateFlow { value ->
            FormFieldEntry(value, true)
        }

    fun onValueChange(value: Boolean) {
        _value.value = value
    }

    override fun onRawValueChange(rawValue: String) {
        onValueChange(rawValue.toBooleanStrictOrNull() ?: true)
    }
}
