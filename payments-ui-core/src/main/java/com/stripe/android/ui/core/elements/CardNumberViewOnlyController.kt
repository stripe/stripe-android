package com.stripe.android.ui.core.elements

import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import com.stripe.android.model.CardBrand
import com.stripe.android.ui.core.forms.FormFieldEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Controller for the card number field which is view only and never changes.
 *
 * @param initialValues The initial values of the field, which will never change.
 *      Should contain values for both [IdentifierSpec.CardNumber] and [IdentifierSpec.CardBrand]
 */
internal class CardNumberViewOnlyController(
    cardTextFieldConfig: CardNumberConfig,
    initialValues: Map<IdentifierSpec, String?>
) : CardNumberController {

    override val capitalization: KeyboardCapitalization = cardTextFieldConfig.capitalization
    override val keyboardType: KeyboardType = cardTextFieldConfig.keyboard
    override val visualTransformation = VisualTransformation.None
    override val debugLabel = cardTextFieldConfig.debugLabel

    override val label = MutableStateFlow(cardTextFieldConfig.label)

    private val _fieldValue = MutableStateFlow(initialValues[IdentifierSpec.CardNumber] ?: "")
    override val fieldValue: Flow<String> = _fieldValue

    override val rawFieldValue = fieldValue

    override val contentDescription = fieldValue

    override val cardBrandFlow = flowOf(
        initialValues[IdentifierSpec.CardBrand]?.let {
            CardBrand.fromCode(it)
        } ?: CardBrand.Unknown
    )

    override val trailingIcon = cardBrandFlow.map { TextFieldIcon(it.icon, isIcon = false) }

    override val fieldState = flowOf(TextFieldStateConstants.Valid.Full)

    override val enabled = false

    override val showOptionalLabel = false

    override val isComplete = flowOf(true)
    override val formFieldValue =
        combine(isComplete, rawFieldValue) { complete, value ->
            FormFieldEntry(value, complete)
        }

    override val error = flowOf(null)
    override val loading = flowOf(false)
    override val visibleError = flowOf(false)

    override fun onValueChange(displayFormatted: String): TextFieldState? = null

    override fun onRawValueChange(rawValue: String) {}

    override fun onFocusChange(newHasFocus: Boolean) {}
}
