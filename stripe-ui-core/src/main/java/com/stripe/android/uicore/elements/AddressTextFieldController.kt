package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.uicore.R
import com.stripe.android.uicore.forms.FormFieldEntry
import com.stripe.android.uicore.utils.combineAsStateFlow
import com.stripe.android.uicore.utils.mapAsStateFlow
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class AddressTextFieldState(
    val value: String,
    val isEditable: Boolean,
    val fieldDisplayState: FieldDisplayState,
    val showDisabledErrorIndicator: Boolean,
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class AddressTextFieldController(
    label: ResolvableString,
    private val onNavigation: (() -> Unit)? = null,
    isInlineAutocompleteEnabled: Boolean = false,
) : InputController, SectionFieldValidationController, SectionFieldComposable {
    private val _isValidating = MutableStateFlow(false)
    private val _inlineQuery = MutableStateFlow("")
    private val isEditable = isInlineAutocompleteEnabled

    override val showOptionalLabel: Boolean = false
    override val label = stateFlowOf(label)
    override val fieldValue: StateFlow<String> = stateFlowOf("")
    override val rawFieldValue: StateFlow<String> = stateFlowOf("")
    override val isComplete: StateFlow<Boolean> = stateFlowOf(false)

    override val validationMessage: StateFlow<FieldValidationMessage?> = _isValidating.mapAsStateFlow { isValidating ->
        FieldValidationMessage.Error(R.string.stripe_blank_and_required).takeIf { isValidating }
    }

    override val formFieldValue: StateFlow<FormFieldEntry> =
        combineAsStateFlow(isComplete, rawFieldValue) { complete, value ->
            FormFieldEntry(value, complete)
        }

    val textFieldState: StateFlow<AddressTextFieldState> =
        combineAsStateFlow(_inlineQuery, validationMessage) { query, error ->
            val isError = error != null
            AddressTextFieldState(
                value = if (isEditable) query else "",
                isEditable = isEditable,
                fieldDisplayState = if (isError) FieldDisplayState.ERROR else FieldDisplayState.NORMAL,
                showDisabledErrorIndicator = !isEditable && isError,
            )
        }

    override fun onRawValueChange(rawValue: String) {
        // No-op, this field does not support direct input manipulation
    }

    fun onInlineQueryChanged(query: String) {
        if (isEditable) {
            _inlineQuery.value = query
        }
    }

    override fun onValidationStateChanged(isValidating: Boolean) {
        _isValidating.value = isValidating
    }

    @Composable
    override fun ComposeUI(
        enabled: Boolean,
        field: SectionFieldElement,
        modifier: Modifier,
        hiddenIdentifiers: Set<IdentifierSpec>,
        lastTextFieldIdentifier: IdentifierSpec?
    ) {
        AddressTextFieldUI(controller = this, enabled = enabled, modifier = modifier)
    }

    fun launchAutocompleteScreen() {
        onNavigation?.invoke()
    }
}
