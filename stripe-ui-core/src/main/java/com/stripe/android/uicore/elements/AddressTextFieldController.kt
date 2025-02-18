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
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.uicore.forms.FormFieldEntry
import com.stripe.android.uicore.utils.combineAsStateFlow
import com.stripe.android.uicore.utils.mapAsStateFlow
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class AddressTextFieldController(
    private val config: TextFieldConfig,
    private val onNavigation: (() -> Unit)? = null,
    override val initialValue: String? = null
) : TextFieldController, InputController, SectionFieldErrorController, SectionFieldComposable {

    init {
        initialValue?.let { onRawValueChange(it) }
    }

    override val trailingIcon: StateFlow<TextFieldIcon?> = config.trailingIcon
    override val capitalization: KeyboardCapitalization = config.capitalization
    override val keyboardType: KeyboardType = config.keyboard
    override val visualTransformation = stateFlowOf(
        value = config.visualTransformation ?: VisualTransformation.None,
    )
    override val showOptionalLabel: Boolean = false

    override val label = MutableStateFlow(config.label)
    override val debugLabel = config.debugLabel

    @ExperimentalComposeUiApi
    override val autofillType: AutofillType? = null

    /** This is all the information that can be observed on the element */
    private val _fieldValue = MutableStateFlow("")
    override val fieldValue: StateFlow<String> = _fieldValue.asStateFlow()

    override val rawFieldValue: StateFlow<String> = _fieldValue.mapAsStateFlow { config.convertToRaw(it) }

    override val contentDescription: StateFlow<ResolvableString> = _fieldValue.mapAsStateFlow { it.resolvableString }

    private val _fieldState = MutableStateFlow<TextFieldState>(TextFieldStateConstants.Error.Blank)
    override val fieldState: StateFlow<TextFieldState> = _fieldState.asStateFlow()

    override val loading: StateFlow<Boolean> = config.loading

    private val _hasFocus = MutableStateFlow(false)

    override val visibleError: StateFlow<Boolean> =
        combineAsStateFlow(_fieldState, _hasFocus) { fieldState, hasFocus ->
            fieldState.shouldShowError(hasFocus)
        }

    /**
     * An error must be emitted if it is visible or not visible.
     **/
    override val error: StateFlow<FieldError?> = visibleError.mapAsStateFlow { visibleError ->
        _fieldState.value.getError()?.takeIf { visibleError }
    }

    override val isComplete: StateFlow<Boolean> = _fieldState.mapAsStateFlow {
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

    override val formFieldValue: StateFlow<FormFieldEntry> =
        combineAsStateFlow(isComplete, rawFieldValue) { complete, value ->
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
