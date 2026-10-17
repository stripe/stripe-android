package com.stripe.android.lpmfoundations.paymentmethod.definitions

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.ui.core.R
import com.stripe.android.uicore.elements.TextFieldConfig
import com.stripe.android.uicore.elements.TextFieldIcon
import com.stripe.android.uicore.elements.TextFieldState
import com.stripe.android.uicore.elements.TextFieldStateConstants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Text field configuration for a Brazilian CPF or CNPJ. */
internal class PixTaxIdConfig : TextFieldConfig {
    override val capitalization = KeyboardCapitalization.None
    override val debugLabel = "pix_tax_id"
    override val label = resolvableString(R.string.stripe_boleto_tax_id_label)
    override val keyboard = KeyboardType.Number
    override val trailingIcon: StateFlow<TextFieldIcon?> = MutableStateFlow(null)
    override val loading: StateFlow<Boolean> = MutableStateFlow(false)
    override val optional = false

    override val visualTransformation = VisualTransformation { text ->
        val separatorIndices = if (text.length <= CPF_LENGTH) {
            CPF_SEPARATOR_INDICES
        } else {
            CNPJ_SEPARATOR_INDICES
        }
        val output = buildString {
            text.forEachIndexed { index, character ->
                separatorIndices[index]?.let(::append)
                append(character)
            }
        }
        val originalOffsets = buildList {
            add(0)
            output.forEachIndexed { index, character ->
                if (character.isDigit()) {
                    add(index + 1)
                }
            }
        }

        TransformedText(
            text = AnnotatedString(output),
            offsetMapping = object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int = originalOffsets[offset]

                override fun transformedToOriginal(offset: Int): Int =
                    output.take(offset).count { it in '0'..'9' }
            },
        )
    }

    override fun determineState(input: String): TextFieldState = when (input.length) {
        0 -> TextFieldStateConstants.Error.Blank
        CPF_LENGTH -> TextFieldStateConstants.Valid.Limitless
        CNPJ_LENGTH -> TextFieldStateConstants.Valid.Full()
        else -> TextFieldStateConstants.Error.Incomplete(R.string.stripe_id_number_incomplete)
    }

    override fun filter(userTyped: String): String = userTyped.filter { it in '0'..'9' }.take(CNPJ_LENGTH)

    override fun convertToRaw(displayName: String): String = displayName

    override fun convertFromRaw(rawValue: String): String = rawValue

    private companion object {
        const val CPF_LENGTH = 11
        const val CNPJ_LENGTH = 14
        val CPF_SEPARATOR_INDICES = mapOf(3 to '.', 6 to '.', 9 to '-')
        val CNPJ_SEPARATOR_INDICES = mapOf(2 to '.', 5 to '.', 8 to '/', 12 to '-')
    }
}
