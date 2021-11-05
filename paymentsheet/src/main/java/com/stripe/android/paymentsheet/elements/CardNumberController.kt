package com.stripe.android.paymentsheet.elements

import androidx.annotation.StringRes
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.stripe.android.model.CardBrand
import com.stripe.android.paymentsheet.forms.FormFieldEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

internal class CardNumberController constructor(
    private val creditTextFieldConfig: CardNumberConfig,
    override val showOptionalLabel: Boolean = false
) : TextFieldController, SectionFieldErrorController {
    override val capitalization: KeyboardCapitalization = creditTextFieldConfig.capitalization
    override val keyboardType: KeyboardType = creditTextFieldConfig.keyboard
    override val visualTransformation = creditTextFieldConfig.visualTransformation

    @StringRes
    // TODO: This should change to a flow and be based in the card brand
    override val label: Int = creditTextFieldConfig.label

    override val debugLabel = creditTextFieldConfig.debugLabel

    /** This is all the information that can be observed on the element */
    private val _fieldValue = MutableStateFlow("")
    override val fieldValue: Flow<String> = _fieldValue

    override val rawFieldValue: Flow<String> =
        _fieldValue.map { creditTextFieldConfig.convertToRaw(it) }

    internal val cardBrandFlow = _fieldValue.map {
        CardBrand.getCardBrands(it).firstOrNull() ?: CardBrand.Unknown
    }

    private val _fieldState = combine(cardBrandFlow, _fieldValue) { brand, fieldValue ->
        // This should also take a list of strings based on CVV or CVC
        creditTextFieldConfig.determineState(brand, fieldValue)
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

    val isFull: Flow<Boolean> = _fieldState.map { it.isFull() }

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
        _fieldValue.value = creditTextFieldConfig.filter(displayFormatted)
    }

    /**
     * This is called when the value changed to is a raw backing value, not a display value.
     */
    override fun onRawValueChange(rawValue: String) {
        onValueChange(creditTextFieldConfig.convertFromRaw(rawValue))
    }

    override fun onFocusChange(newHasFocus: Boolean) {
        _hasFocus.value = newHasFocus
    }
}
