package com.stripe.android.ui.core.elements

import androidx.annotation.DrawableRes
import androidx.annotation.RestrictTo
import androidx.annotation.StringRes
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import com.stripe.android.ui.core.elements.TextFieldStateConstants.Error.Blank
import com.stripe.android.ui.core.forms.FormFieldEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
interface TextFieldController : InputController {
    fun onValueChange(displayFormatted: String): TextFieldState?
    fun onFocusChange(newHasFocus: Boolean)

    val debugLabel: String
    val trailingIcon: Flow<TextFieldIcon?>
    val capitalization: KeyboardCapitalization
    val keyboardType: KeyboardType
    override val label: Flow<Int>
    val visualTransformation: VisualTransformation
    override val showOptionalLabel: Boolean
    val fieldState: Flow<TextFieldState>
    override val fieldValue: Flow<String>
    val visibleError: Flow<Boolean>
    val loading: Flow<Boolean>
    // This dictates how the accessibility reader reads the text in the field.
    // Default this to _fieldValue to read the field normally
    val contentDescription: Flow<String>
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
        val isIcon: Boolean
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
    private val textFieldConfig: TextFieldConfig,
    override val showOptionalLabel: Boolean = false,
    initialValue: String? = null
) : TextFieldController, SectionFieldErrorController {
    override val trailingIcon: Flow<TextFieldIcon?> = textFieldConfig.trailingIcon
    override val capitalization: KeyboardCapitalization = textFieldConfig.capitalization
    override val keyboardType: KeyboardType = textFieldConfig.keyboard
    override val visualTransformation =
        textFieldConfig.visualTransformation ?: VisualTransformation.None

    override val label: Flow<Int> = MutableStateFlow(textFieldConfig.label)
    override val debugLabel = textFieldConfig.debugLabel

    /** This is all the information that can be observed on the element */
    private val _fieldValue = MutableStateFlow("")
    override val fieldValue: Flow<String> = _fieldValue

    override val rawFieldValue: Flow<String> = _fieldValue.map { textFieldConfig.convertToRaw(it) }

    override val contentDescription: Flow<String> = _fieldValue

    private val _fieldState = MutableStateFlow<TextFieldState>(Blank)
    override val fieldState: Flow<TextFieldState> = _fieldState

    override val loading: Flow<Boolean> = textFieldConfig.loading

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

    override val formFieldValue: Flow<FormFieldEntry> =
        combine(isComplete, rawFieldValue) { complete, value ->
            FormFieldEntry(value, complete)
        }

    init {
        initialValue?.let { onRawValueChange(it) }
    }

    /**
     * This is called when the value changed to is a display value.
     */
    override fun onValueChange(displayFormatted: String): TextFieldState? {
        val originalTextStateValue = _fieldState.value
        _fieldValue.value = textFieldConfig.filter(displayFormatted)

        // Should be filtered value
        _fieldState.value = textFieldConfig.determineState(_fieldValue.value)

        return if (_fieldState.value != originalTextStateValue) {
            _fieldState.value
        } else {
            null
        }
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

    companion object {
        fun createEmailSectionController(initialValue: String?) = SimpleTextFieldController(
            EmailConfig(),
            initialValue = initialValue
        )

        fun createNameSectionController(initialValue: String?) = SimpleTextFieldController(
            NameConfig(),
            initialValue = initialValue
        )
    }
}
