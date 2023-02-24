package com.stripe.android.uicore.elements

import androidx.annotation.DrawableRes
import androidx.annotation.RestrictTo
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import com.stripe.android.uicore.elements.TextFieldStateConstants.Error.Blank
import com.stripe.android.uicore.forms.FormFieldEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
interface TextFieldController : InputController, SectionFieldComposable {
    fun onValueChange(displayFormatted: String): TextFieldState?
    fun onFocusChange(newHasFocus: Boolean)

    val debugLabel: String
    val trailingIcon: Flow<TextFieldIcon?>
    val capitalization: KeyboardCapitalization
    val keyboardType: KeyboardType
    override val label: Flow<Int?>
    val visualTransformation: VisualTransformation
    override val showOptionalLabel: Boolean
    val fieldState: Flow<TextFieldState>
    override val fieldValue: Flow<String>
    val visibleError: Flow<Boolean>
    val loading: Flow<Boolean>
    val placeHolder: Flow<String?>
        get() = flowOf(null)

    // Whether the TextField should be enabled or not
    val enabled: Boolean
        get() = true

    // This dictates how the accessibility reader reads the text in the field.
    // Default this to _fieldValue to read the field normally
    val contentDescription: Flow<String>

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
        TextField(
            textFieldController = this,
            enabled = enabled,
            imeAction = if (lastTextFieldIdentifier == field.identifier) {
                ImeAction.Done
            } else {
                ImeAction.Next
            },
            modifier = modifier,
            nextFocusDirection = nextFocusDirection,
            previousFocusDirection = previousFocusDirection
        )
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
sealed class TextFieldIcon {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    data class Trailing(
        @DrawableRes
        val idRes: Int,
        @StringRes
        val contentDescription: Int? = null,

        /** If it is an icon that should be tinted to match the text the value should be true */
        val isTintable: Boolean,
        val onClick: (() -> Unit)? = null
    ) : TextFieldIcon()

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    data class MultiTrailing(
        val staticIcons: List<Trailing>,
        val animatedIcons: List<Trailing>
    ) : TextFieldIcon()
}

/**
 * This class will provide the onValueChanged and onFocusChanged functionality to the field's
 * composable.  These functions will update the observables as needed.  It is responsible for
 * exposing immutable observers for its data
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
class SimpleTextFieldController constructor(
    val textFieldConfig: TextFieldConfig,
    override val showOptionalLabel: Boolean = false,
    initialValue: String? = null
) : TextFieldController, SectionFieldErrorController {
    override val trailingIcon: Flow<TextFieldIcon?> = textFieldConfig.trailingIcon
    override val capitalization: KeyboardCapitalization = textFieldConfig.capitalization
    override val keyboardType: KeyboardType = textFieldConfig.keyboard
    override val visualTransformation =
        textFieldConfig.visualTransformation ?: VisualTransformation.None

    override val label = MutableStateFlow(textFieldConfig.label)
    override val debugLabel = textFieldConfig.debugLabel

    override val placeHolder = MutableStateFlow(textFieldConfig.placeHolder)

    /** This is all the information that can be observed on the element */
    private val _textField: MutableStateFlow<TextField> = MutableStateFlow(
        TextField(value = "", state = Blank)
    )

    override val fieldValue: Flow<String> = _textField.map { it.value }.distinctUntilChanged()

    override val rawFieldValue: Flow<String> = fieldValue.map { textFieldConfig.convertToRaw(it) }

    override val contentDescription: Flow<String> = fieldValue

    override val fieldState: Flow<TextFieldState> = _textField.map { it.state }.distinctUntilChanged()

    override val loading: Flow<Boolean> = textFieldConfig.loading

    private val _hasFocus = MutableStateFlow(false)

    override val visibleError: Flow<Boolean> =
        combine(fieldState, _hasFocus) { fieldState, hasFocus ->
            fieldState.shouldShowError(hasFocus)
        }

    /**
     * An error must be emitted if it is visible or not visible.
     **/
    override val error: Flow<FieldError?> = visibleError.map { visibleError ->
        _textField.value.state.getError()?.takeIf { visibleError }
    }

    override val isComplete: Flow<Boolean> = _textField.map { it.state }.map { it.isComplete() }

    override val formFieldValue: Flow<FormFieldEntry> =
        _textField.map {
            FormFieldEntry(
                it.value,
                it.state.isComplete()
            )
        }

    init {
        initialValue?.let { onRawValueChange(it) }
    }

    /**
     * This is called when the value changed to is a display value.
     */
    override fun onValueChange(displayFormatted: String): TextFieldState? {
        val originalState = _textField.value.state
        val newValue = textFieldConfig.filter(displayFormatted)
        val newState = textFieldConfig.determineState(newValue)
        _textField.value = TextField(
            value = newValue,
            state = newState
        )

        return if (newState != originalState) newState else null
    }

    /**
     * This is called when the value changed to is a raw backing value, not a display value.
     */
    override fun onRawValueChange(rawValue: String) {
        onValueChange(textFieldConfig.convertFromRaw(rawValue))
    }

    override fun onFocusChange(newHasFocus: Boolean) {
        _hasFocus.value = newHasFocus
    }

    private fun TextFieldState.isComplete() =
        isValid() || (!isValid() && showOptionalLabel && isBlank())

    private data class TextField(
        val value: String,
        val state: TextFieldState
    )
}
