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
import com.stripe.android.uicore.utils.combineAsStateFlow
import com.stripe.android.uicore.utils.mapAsStateFlow
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.stripe.android.R as StripeR

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class CvcController constructor(
    private val cvcTextFieldConfig: CvcConfig = CvcConfig(),
    cardBrandFlow: StateFlow<CardBrand>,
    override val initialValue: String? = null,
    override val showOptionalLabel: Boolean = false
) : TextFieldController, SectionFieldErrorController {
    override val capitalization: KeyboardCapitalization = cvcTextFieldConfig.capitalization
    override val keyboardType: KeyboardType = cvcTextFieldConfig.keyboard

    private val _label = cardBrandFlow.mapAsStateFlow { cardBrand ->
        if (cardBrand == CardBrand.AmericanExpress) {
            StripeR.string.stripe_cvc_amex_hint
        } else {
            StripeR.string.stripe_cvc_number_hint
        }
    }
    override val label: StateFlow<Int> = _label

    override val debugLabel = cvcTextFieldConfig.debugLabel

    @OptIn(ExperimentalComposeUiApi::class)
    override val autofillType: AutofillType = AutofillType.CreditCardSecurityCode

    private val _fieldValue = MutableStateFlow("")
    override val fieldValue: StateFlow<String> = _fieldValue.asStateFlow()

    override val visualTransformation = _fieldValue.mapAsStateFlow { number ->
        cvcTextFieldConfig.determineVisualTransformation(number = number, panLength = 0)
    }

    override val rawFieldValue: StateFlow<String> =
        _fieldValue.mapAsStateFlow { cvcTextFieldConfig.convertToRaw(it) }

    // This makes the screen reader read out numbers digit by digit
    override val contentDescription: StateFlow<String> = _fieldValue.mapAsStateFlow { it.asIndividualDigits() }

    private val _fieldState = combineAsStateFlow(cardBrandFlow, _fieldValue) { brand, fieldValue ->
        cvcTextFieldConfig.determineState(brand, fieldValue, brand.maxCvcLength)
    }
    override val fieldState: StateFlow<TextFieldState> = _fieldState

    private val _hasFocus = MutableStateFlow(false)

    override val visibleError: StateFlow<Boolean> =
        combineAsStateFlow(_fieldState, _hasFocus) { fieldState, hasFocus ->
            fieldState.shouldShowError(hasFocus)
        }

    /**
     * An error must be emitted if it is visible or not visible.
     **/
    override val error: StateFlow<FieldError?> =
        combineAsStateFlow(visibleError, _fieldState) { visibleError, fieldState ->
            fieldState.getError()?.takeIf { visibleError }
        }

    override val isComplete: StateFlow<Boolean> = _fieldState.mapAsStateFlow { it.isValid() }

    override val formFieldValue: StateFlow<FormFieldEntry> =
        combineAsStateFlow(isComplete, rawFieldValue) { complete, value ->
            FormFieldEntry(value, complete)
        }

    override val trailingIcon: StateFlow<TextFieldIcon?> = cardBrandFlow.mapAsStateFlow {
        TextFieldIcon.Trailing(it.cvcIcon, isTintable = false)
    }

    override val loading: StateFlow<Boolean> = stateFlowOf(false)

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
