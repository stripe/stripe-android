package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.stripe.android.model.CardBrand
import com.stripe.android.ui.core.asIndividualDigits
import com.stripe.android.uicore.elements.FieldError
import com.stripe.android.uicore.elements.SectionFieldErrorController
import com.stripe.android.uicore.elements.TextFieldController
import com.stripe.android.uicore.elements.TextFieldIcon
import com.stripe.android.uicore.elements.TextFieldState
import com.stripe.android.uicore.forms.FormFieldEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import com.stripe.android.R as StripeR

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class CvcController constructor(
    private val cvcTextFieldConfig: CvcConfig = CvcConfig(),
    cardBrandFlow: Flow<CardBrand>,
    initialValue: String? = null,
    override val showOptionalLabel: Boolean = false
) : TextFieldController, SectionFieldErrorController {
    override val capitalization: KeyboardCapitalization = cvcTextFieldConfig.capitalization
    override val keyboardType: KeyboardType = cvcTextFieldConfig.keyboard
    override val visualTransformation = cvcTextFieldConfig.visualTransformation

    private val _label = cardBrandFlow.map { cardBrand ->
        if (cardBrand == CardBrand.AmericanExpress) {
            StripeR.string.stripe_cvc_amex_hint
        } else {
            StripeR.string.stripe_cvc_number_hint
        }
    }
    override val label: Flow<Int> = _label

    override val debugLabel = cvcTextFieldConfig.debugLabel

    @OptIn(ExperimentalComposeUiApi::class)
    override val autofillType: AutofillType = AutofillType.CreditCardSecurityCode

    private val _fieldValue = MutableStateFlow("")
    override val fieldValue: Flow<String> = _fieldValue

    override val rawFieldValue: Flow<String> =
        _fieldValue.map { cvcTextFieldConfig.convertToRaw(it) }

    // This makes the screen reader read out numbers digit by digit
    override val contentDescription: Flow<String> = _fieldValue.map { it.asIndividualDigits() }

    private val _fieldState = combine(cardBrandFlow, _fieldValue) { brand, fieldValue ->
        cvcTextFieldConfig.determineState(brand, fieldValue, brand.maxCvcLength)
    }
    override val fieldState: Flow<TextFieldState> = _fieldState

    private val _hasFocus = MutableStateFlow(false)

    override val visibleError: Flow<Boolean> =
        combine(_fieldState, _hasFocus) { fieldState, hasFocus ->
            fieldState.shouldShowError(hasFocus)
        }.distinctUntilChanged()

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

    override val trailingIcon: Flow<TextFieldIcon?> = cardBrandFlow.map {
        TextFieldIcon.Trailing(it.cvcIcon, isTintable = false)
    }

    override val loading: Flow<Boolean> = MutableStateFlow(false)

    init {
        onRawValueChange(initialValue ?: "")
    }

    /**
     * This is called when the value changed to is a display value.
     */
    override fun onValueChange(displayFormatted: String): TextFieldState? {
        _fieldValue.value = cvcTextFieldConfig.filter(displayFormatted)

        return null
    }

    /**
     * This is called when the value changed to is a raw backing value, not a display value.
     */
    override fun onRawValueChange(rawValue: String) {
        onValueChange(cvcTextFieldConfig.convertFromRaw(rawValue))
    }

    override fun onFocusChange(newHasFocus: Boolean) {
        _hasFocus.value = newHasFocus
    }
}
