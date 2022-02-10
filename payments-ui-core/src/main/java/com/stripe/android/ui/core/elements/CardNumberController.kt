package com.stripe.android.ui.core.elements

import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.stripe.android.model.CardBrand
import com.stripe.android.ui.core.forms.FormFieldEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

internal class CardNumberController constructor(
    private val cardTextFieldConfig: CardNumberConfig,
    override val showOptionalLabel: Boolean = false
) : TextFieldController, SectionFieldErrorController {
    override val capitalization: KeyboardCapitalization = cardTextFieldConfig.capitalization
    override val keyboardType: KeyboardType = cardTextFieldConfig.keyboard
    override val visualTransformation = cardTextFieldConfig.visualTransformation
    override val debugLabel = cardTextFieldConfig.debugLabel

    override val label: Flow<Int> = MutableStateFlow(cardTextFieldConfig.label)

    private val _fieldValue = MutableStateFlow("")
    override val fieldValue: Flow<String> = _fieldValue

    override val rawFieldValue: Flow<String> =
        _fieldValue.map { cardTextFieldConfig.convertToRaw(it) }

    internal val cardBrandFlow = _fieldValue.map {
        CardBrand.getCardBrands(it).firstOrNull() ?: CardBrand.Unknown
    }

    override val trailingIcon: Flow<TextFieldIcon?> = cardBrandFlow.map {
        TextFieldIcon(it.icon, isIcon = false)
    }

    private val _fieldState = combine(cardBrandFlow, _fieldValue) { brand, fieldValue ->
        cardTextFieldConfig.determineState(brand, fieldValue)
    }
    override val fieldState: Flow<TextFieldState> = _fieldState

    private val _hasFocus = MutableStateFlow(false)

    override val visibleError: Flow<Boolean> =
        combine(_fieldState, _hasFocus) { fieldState, hasFocus ->
            fieldState.shouldShowError(hasFocus)
        }

    /**
     * An error must be emitted if it is visible or not visible.
     **/
    override val error: Flow<FieldError?> =
        combine(visibleError, _fieldState) { visibleError, fieldState ->
            fieldState.getError()?.takeIf { visibleError }
        }

    override val isComplete: Flow<Boolean> = _fieldState.map { it.isValid() }

    override val formFieldValue: Flow<FormFieldEntry> =
        combine(isComplete, rawFieldValue) { complete, value ->
            FormFieldEntry(value, complete)
        }

    init {
        onValueChange("")
    }

    /**
     * This is called when the value changed to is a display value.
     */
    override fun onValueChange(displayFormatted: String) {
        _fieldValue.value = cardTextFieldConfig.filter(displayFormatted)
    }

    /**
     * This is called when the value changed to is a raw backing value, not a display value.
     */
    override fun onRawValueChange(rawValue: String) {
        onValueChange(cardTextFieldConfig.convertFromRaw(rawValue))
    }

    override fun onFocusChange(newHasFocus: Boolean) {
        _hasFocus.value = newHasFocus
    }
}
