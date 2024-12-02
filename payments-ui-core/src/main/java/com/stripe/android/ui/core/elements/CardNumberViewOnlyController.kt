package com.stripe.android.ui.core.elements

import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import com.stripe.android.model.CardBrand
import com.stripe.android.ui.core.asIndividualDigits
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.TextFieldIcon
import com.stripe.android.uicore.elements.TextFieldState
import com.stripe.android.uicore.elements.TextFieldStateConstants
import com.stripe.android.uicore.forms.FormFieldEntry
import com.stripe.android.uicore.utils.combineAsStateFlow
import com.stripe.android.uicore.utils.mapAsStateFlow
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Controller for the card number field which is view only and never changes.
 * The card number UI element will be shown as disabled to the user.
 *
 * @param initialValues The initial values of the field, which will never change.
 *      Should contain values for both [IdentifierSpec.CardNumber] and [IdentifierSpec.CardBrand].
 *      These are the values that will be emitted in the completed FormFieldEntry flow, which means
 *      empty values will be emitted if the initial values were not set.
 */
internal class CardNumberViewOnlyController(
    cardTextFieldConfig: CardNumberConfig,
    initialValues: Map<IdentifierSpec, String?>
) : CardNumberController() {
    override val capitalization: KeyboardCapitalization = cardTextFieldConfig.capitalization
    override val keyboardType: KeyboardType = cardTextFieldConfig.keyboard
    override val visualTransformation = VisualTransformation.None
    override val debugLabel = cardTextFieldConfig.debugLabel
    override val label = MutableStateFlow(cardTextFieldConfig.label)
    private val _fieldValue = MutableStateFlow(initialValues[IdentifierSpec.CardNumber] ?: "")
    override val fieldValue: StateFlow<String> = _fieldValue
    override val rawFieldValue: StateFlow<String?> = fieldValue
    // This makes the screen reader read out numbers digit by digit
    override val contentDescription: StateFlow<String> = _fieldValue.mapAsStateFlow { it.asIndividualDigits() }
    override val cardBrandFlow: StateFlow<CardBrand> = stateFlowOf(
        initialValues[IdentifierSpec.CardBrand]?.let {
            CardBrand.fromCode(it)
        } ?: CardBrand.Unknown
    )
    override val selectedCardBrandFlow = stateFlowOf(CardBrand.Unknown)
    override val cardScanEnabled = false
    override val trailingIcon: StateFlow<TextFieldIcon?> =
        cardBrandFlow.mapAsStateFlow { TextFieldIcon.Trailing(it.icon, isTintable = false) }
    override val fieldState = stateFlowOf(TextFieldStateConstants.Valid.Full)
    override val enabled = false
    override val showOptionalLabel = false
    override val isComplete = stateFlowOf(true)
    override val formFieldValue =
        combineAsStateFlow(isComplete, rawFieldValue) { complete, value ->
            FormFieldEntry(value, complete)
        }
    override val error = stateFlowOf(null)
    override val loading = stateFlowOf(false)
    override val visibleError = stateFlowOf(false)
    override fun onValueChange(displayFormatted: String): TextFieldState? = null
    override fun onRawValueChange(rawValue: String) {}
    override fun onFocusChange(newHasFocus: Boolean) {}
    override val initialValue: String?
        get() = TODO("Not yet implemented")
}