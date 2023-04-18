package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import com.stripe.android.uicore.forms.FormFieldEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class AddressTextFieldController(
    private val config: TextFieldConfig,
    private val onNavigation: (() -> Unit)? = null,
    initialValue: String? = null
) : TextFieldController, InputController, SectionFieldErrorController, SectionFieldComposable {

    init {
        initialValue?.let { onRawValueChange(it) }
    }

    override val trailingIcon: Flow<TextFieldIcon?> = config.trailingIcon
    override val capitalization: KeyboardCapitalization = config.capitalization
    override val keyboardType: KeyboardType = config.keyboard
    override val visualTransformation =
        config.visualTransformation ?: VisualTransformation.None
    override val showOptionalLabel: Boolean = false

    override val label = MutableStateFlow(config.label)
    override val debugLabel = config.debugLabel

    @ExperimentalComposeUiApi
    override val autofillType: AutofillType? = null

    /** This is all the information that can be observed on the element */
    private val _fieldValue = MutableStateFlow("")
    override val fieldValue: Flow<String> = _fieldValue

    override val rawFieldValue: Flow<String> = _fieldValue.map { config.convertToRaw(it) }

    override val contentDescription: Flow<String> = _fieldValue

    private val _fieldState = MutableStateFlow<TextFieldState>(TextFieldStateConstants.Error.Blank)
    override val fieldState: Flow<TextFieldState> = _fieldState

    override val loading: Flow<Boolean> = config.loading

    private val _hasFocus = MutableStateFlow(false)

    override val visibleError: Flow<Boolean> =
        combine(_fieldState, _hasFocus) { fieldState, hasFocus ->
            fieldState.shouldShowError(hasFocus)
        }

    /**
     * An error must be emitted if it is visible or not visible.
     **/
    override val error: Flow<FieldError?> = visibleError.map { visibleError ->
        _fieldState.value.getError()?.takeIf { visibleError }
    }

    override val isComplete: Flow<Boolean> = _fieldState.map {
        it.isValid() || (!it.isValid() && showOptionalLabel && it.isBlank())
    }

    /**
     * This is called when the value changed to is a display value.
     */
    override fun onValueChange(displayFormatted: String): TextFieldState? {
        val originalTextStateValue = _fieldState.value
        _fieldValue.value = config.filter(displayFormatted)

        // Should be filtered value
        _fieldState.value = config.determineState(_fieldValue.value)

        return if (_fieldState.value != originalTextStateValue) {
            _fieldState.value
        } else {
            null
        }
    }

    override fun onFocusChange(newHasFocus: Boolean) {
        _hasFocus.value = newHasFocus
    }

    override val formFieldValue: Flow<FormFieldEntry> =
        combine(isComplete, rawFieldValue) { complete, value ->
            FormFieldEntry(value, complete)
        }

    override fun onRawValueChange(rawValue: String) {
        onValueChange(config.convertFromRaw(rawValue))
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
        AddressTextFieldUI(this)
    }

    fun launchAutocompleteScreen() {
        onNavigation?.invoke()
    }
}
