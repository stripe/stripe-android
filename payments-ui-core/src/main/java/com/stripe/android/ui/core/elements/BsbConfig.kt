package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import androidx.annotation.StringRes
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.stripe.android.uicore.elements.TextFieldConfig
import com.stripe.android.uicore.elements.TextFieldIcon
import com.stripe.android.uicore.elements.TextFieldState
import com.stripe.android.uicore.elements.TextFieldStateConstants
import com.stripe.android.view.BecsDebitBanks
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.stripe.android.R as StripeR

/**
 * A text field configuration for a BSB number, or Bank State Branch Number,
 * a six-digit number used to identify the individual branch of an Australian financial institution
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class BsbConfig(private val banks: List<BecsDebitBanks.Bank>) : TextFieldConfig {
    override val capitalization: KeyboardCapitalization = KeyboardCapitalization.None
    override val debugLabel = "bsb"

    override val trailingIcon: StateFlow<TextFieldIcon?> = MutableStateFlow(null)
    override val loading: StateFlow<Boolean> = MutableStateFlow(false)

    @StringRes
    override val label = StripeR.string.stripe_becs_widget_bsb
    override val keyboard = KeyboardType.Number

    // Displays the BSB number in 2 groups of 3 characters with a dash added between them
    override val visualTransformation: VisualTransformation = VisualTransformation { text ->
        val output = StringBuilder()
        val separator = " - "
        text.text.forEachIndexed { i, char ->
            output.append(char)
            if (i == 2) output.append(separator)
        }
        TransformedText(
            AnnotatedString(output.toString()),
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int {
                    return if (offset <= 2) {
                        offset
                    } else {
                        offset + separator.length
                    }
                }

                override fun transformedToOriginal(offset: Int): Int {
                    return if (offset <= 3) {
                        offset
                    } else {
                        offset - separator.length
                    }
                }
            }
        )
    }

    override fun filter(userTyped: String) =
        userTyped.filter { VALID_INPUT_RANGES.contains(it) }.take(LENGTH)

    override fun convertToRaw(displayName: String) = displayName

    override fun convertFromRaw(rawValue: String) = rawValue

    override fun determineState(input: String): TextFieldState {
        input.ifBlank {
            return TextFieldStateConstants.Error.Blank
        }

        if (input.length < LENGTH) {
            return TextFieldStateConstants.Error.Incomplete(
                StripeR.string.stripe_becs_widget_bsb_incomplete
            )
        }

        val bank = banks.firstOrNull {
            input.startsWith(it.prefix)
        }

        if (bank == null || input.length > LENGTH) {
            return TextFieldStateConstants.Error.Invalid(
                StripeR.string.stripe_becs_widget_bsb_invalid
            )
        }

        return TextFieldStateConstants.Valid.Full
    }

    private companion object {
        const val LENGTH = 6
        val VALID_INPUT_RANGES = ('0'..'9')
    }
}
