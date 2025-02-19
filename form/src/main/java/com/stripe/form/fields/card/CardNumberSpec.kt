package com.stripe.form.fields.card

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.stripe.android.model.CardBrand
import com.stripe.form.ContentBox
import com.stripe.form.ContentSpec
import com.stripe.form.FormFieldSpec
import com.stripe.form.FormFieldState
import com.stripe.form.ValidationResult
import com.stripe.form.ValueChange
import com.stripe.form.fields.TextFieldSpec
import com.stripe.form.key
import com.stripe.form.text.TextSpec

@Stable
data class CardNumberSpec(
    override val state: State
) : FormFieldSpec<String> {
    @Composable
    override fun Content(modifier: Modifier) {
        var cardNumber by rememberSaveable("cardNumber") {
            mutableStateOf(state.initialValue)
        }
        ContentBox(
            modifier = modifier,
            spec = TextFieldSpec(
                state = TextFieldSpec.TextFieldState(
                    key = key("cardNumberInput"),
                    label = state.label,
                    initialValue = TextFieldValue(state.initialValue),
                    validator = {
                        state.validator(it.text)
                    },
                    onValueChange = { change ->
                        cardNumber = change.value.text
                        state.onValueChange(
                            ValueChange(
                                key = change.key,
                                value = change.value.text,
                                isComplete = change.isComplete
                            )
                        )
                    },
                    trailing = CardNumberBrandSpec(
                        state = CardNumberBrandSpec.State(
                            cardBrand = state.getCardBrand(cardNumber)
                        )
                    ),
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Number
                    ),
                    maxLength = state.getCardBrand(cardNumber)
                        .getMaxLengthForCardNumber(cardNumber),
                    visualTransformation = state.visualTransformation,
                    readOnly = state.readOnly
                )
            )
        )
    }

    @Stable
    data class State(
        val initialValue: String = "",
        val label: ContentSpec? = TextSpec("Card Number"),
        val getCardBrand: (String) -> CardBrand = { CardBrand.Visa },
        val readOnly: Boolean = false,
        val visualTransformation: VisualTransformation = CardNumberVisualTransformation,
        override val onValueChange: (ValueChange<String>) -> Unit,
        override val validator: (String) -> ValidationResult = { ValidationResult.Valid }
    ) : FormFieldState<String> {
        override val key = KEY

    }

    companion object {
        val KEY = key<String>("cardNumber")
    }
}

private object CardNumberVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        var out = ""
        for (i in text.indices) {
            out += text[i]
            if (i % 4 == 3 && i < 15) out += ' '
        }

        /**
         * The offset translator should ignore the hyphen characters, so conversion from
         *  original offset to transformed text works like
         *  - The 4th char of the original text is 5th char in the transformed text.
         *  - The 13th char of the original text is 15th char in the transformed text.
         *  Similarly, the reverse conversion works like
         *  - The 5th char of the transformed text is 4th char in the original text.
         *  - The 12th char of the transformed text is 10th char in the original text.
         */
        val creditCardOffsetTranslator = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 3) return offset
                if (offset <= 7) return offset + 1
                if (offset <= 11) return offset + 2
                return offset + 3
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 4) return offset
                if (offset <= 9) return offset - 1
                if (offset <= 14) return offset - 2
                return offset - 3
            }
        }

        return TransformedText(AnnotatedString(out), creditCardOffsetTranslator)
    }

}