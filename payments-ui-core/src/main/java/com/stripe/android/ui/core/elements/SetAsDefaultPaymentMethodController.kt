package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import com.stripe.android.ui.core.R
import com.stripe.android.uicore.elements.FieldError
import com.stripe.android.uicore.elements.InputController
import com.stripe.android.uicore.forms.FormFieldEntry
import com.stripe.android.uicore.utils.combineAsStateFlow
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class SetAsDefaultPaymentMethodController(
    setAsDefaultPaymentMethodInitialValue: Boolean = false,
    shouldShowElementFlow: StateFlow<Boolean>,
) : InputController {
    override val label: StateFlow<Int> = MutableStateFlow(R.string.stripe_set_as_default_payment_method)

    private val _setAsDefaultPaymentMethod = MutableStateFlow(setAsDefaultPaymentMethodInitialValue)
    val setAsDefaultPaymentMethod: StateFlow<Boolean> = _setAsDefaultPaymentMethod

    override val fieldValue: StateFlow<String> = combineAsStateFlow(
        shouldShowElementFlow,
        setAsDefaultPaymentMethod
    ) { shouldShowElementFlow, setAsDefaultPaymentMethod ->
        (shouldShowElementFlow && setAsDefaultPaymentMethod).toString()
    }

    override val rawFieldValue: StateFlow<String?> = fieldValue

    override val error: StateFlow<FieldError?> = stateFlowOf(null)
    override val showOptionalLabel: Boolean = false
    override val isComplete: StateFlow<Boolean> = stateFlowOf(true)
    override val formFieldValue: StateFlow<FormFieldEntry> =
        combineAsStateFlow(isComplete, rawFieldValue) { complete, value ->
            FormFieldEntry(value, complete)
        }

    fun onValueChange(setAsDefaultPaymentMethod: Boolean) {
        _setAsDefaultPaymentMethod.value = setAsDefaultPaymentMethod
    }

    override fun onRawValueChange(rawValue: String) {
        onValueChange(rawValue.toBooleanStrictOrNull() ?: true)
    }
}
