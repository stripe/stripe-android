package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import com.stripe.android.uicore.R
import com.stripe.android.uicore.forms.FormFieldEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class SameAsShippingController(
    initialValue: Boolean
) : InputController, SectionFieldComposable {
    override val label: Flow<Int> = MutableStateFlow(R.string.stripe_billing_same_as_shipping)
    private val _value = MutableStateFlow(initialValue)
    val value: Flow<Boolean> = _value
    override val fieldValue: Flow<String> = value.map { it.toString() }
    override val rawFieldValue: Flow<String?> = fieldValue

    override val error: Flow<FieldError?> = flowOf(null)
    override val showOptionalLabel: Boolean = false
    override val isComplete: Flow<Boolean> = flowOf(true)
    override val formFieldValue: Flow<FormFieldEntry> =
        rawFieldValue.map { value ->
            FormFieldEntry(value, true)
        }

    fun onValueChange(value: Boolean) {
        _value.value = value
    }

    override fun onRawValueChange(rawValue: String) {
        onValueChange(rawValue.toBooleanStrictOrNull() ?: true)
    }

    @Composable
    override fun ComposeUI(
        enabled: Boolean,
        field: SectionFieldElement,
        modifier: Modifier,
        hiddenIdentifiers: Set<IdentifierSpec>,
        lastTextFieldIdentifier: IdentifierSpec?,
        nextFocusDirection: FocusDirection,
        previousFocusDirection: FocusDirection
    ) {
        SameAsShippingElementUI(this)
    }
}
