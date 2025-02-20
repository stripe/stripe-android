package com.stripe.android.uicore.elements

import androidx.annotation.DrawableRes
import androidx.annotation.RestrictTo
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.LayoutDirection
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.uicore.elements.TextFieldStateConstants.Error.Blank
import com.stripe.android.uicore.forms.FormFieldEntry
import com.stripe.android.uicore.utils.combineAsStateFlow
import com.stripe.android.uicore.utils.mapAsStateFlow
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@OptIn(ExperimentalComposeUiApi::class)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
interface TextFieldController : InputController, SectionFieldComposable {
    fun onValueChange(displayFormatted: String): TextFieldState?
    fun onFocusChange(newHasFocus: Boolean)
    fun onDropdownItemClicked(item: TextFieldIcon.Dropdown.Item) {}

    val initialValue: String?
    val autofillType: AutofillType?
    val debugLabel: String
    val trailingIcon: StateFlow<TextFieldIcon?>
    val capitalization: KeyboardCapitalization
    val keyboardType: KeyboardType
    val layoutDirection: LayoutDirection?
    override val label: StateFlow<Int?>
    val visualTransformation: StateFlow<VisualTransformation>
    override val showOptionalLabel: Boolean
    val fieldState: StateFlow<TextFieldState>
    override val fieldValue: StateFlow<String>
    val visibleError: StateFlow<Boolean>
    val loading: StateFlow<Boolean>
    val placeHolder: StateFlow<String?>
        get() = stateFlowOf(null)

    // Whether the TextField should be enabled or not
    val enabled: Boolean
        get() = true

    // This dictates how the accessibility reader reads the text in the field.
    // Default this to _fieldValue to read the field normally
    val contentDescription: StateFlow<String>

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

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    data class Dropdown(
        val title: ResolvableString,
        val hide: Boolean,
        val currentItem: Item,
        val items: List<Item>
    ) : TextFieldIcon() {
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
        data class Item(
            val id: String,
            override val label: ResolvableString,
            override val icon: Int,
            override val enabled: Boolean = true
        ) : SingleChoiceDropdownItem
    }
}

/**
 * This class will provide the onValueChanged and onFocusChanged functionality to the field's
 * composable.  These functions will update the observables as needed.  It is responsible for
 * exposing immutable observers for its data
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
class SimpleTextFieldController(
    val textFieldConfig: TextFieldConfig,
    override val showOptionalLabel: Boolean = false,
    override val initialValue: String? = null
) : TextFieldController, SectionFieldErrorController {
    override val trailingIcon: StateFlow<TextFieldIcon?> = textFieldConfig.trailingIcon
    override val capitalization: KeyboardCapitalization = textFieldConfig.capitalization
    override val keyboardType: KeyboardType = textFieldConfig.keyboard
    override val visualTransformation = stateFlowOf(
        value = textFieldConfig.visualTransformation ?: VisualTransformation.None
    )

    override val label = MutableStateFlow(textFieldConfig.label)
    override val debugLabel = textFieldConfig.debugLabel
    override val layoutDirection: LayoutDirection? = textFieldConfig.layoutDirection

    @OptIn(ExperimentalComposeUiApi::class)
    override val autofillType: AutofillType? = when (textFieldConfig) {
        is DateConfig -> AutofillType.CreditCardExpirationDate
        is PostalCodeConfig -> AutofillType.PostalCode
        is EmailConfig -> AutofillType.EmailAddress
        is NameConfig -> AutofillType.PersonFullName
        else -> null
    }

    override val placeHolder = MutableStateFlow(textFieldConfig.placeHolder)

    /** This is all the information that can be observed on the element */
    private val _fieldValue = MutableStateFlow("")
    override val fieldValue: StateFlow<String> = _fieldValue.asStateFlow()

    override val rawFieldValue: StateFlow<String> = _fieldValue.mapAsStateFlow { textFieldConfig.convertToRaw(it) }

    override val contentDescription: StateFlow<String> = _fieldValue.asStateFlow()

    private val _fieldState = MutableStateFlow<TextFieldState>(Blank)
    override val fieldState: StateFlow<TextFieldState> = _fieldState.asStateFlow()

    override val loading: StateFlow<Boolean> = textFieldConfig.loading

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

    override val formFieldValue: StateFlow<FormFieldEntry> =
        combineAsStateFlow(isComplete, rawFieldValue) { complete, value ->
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
}
