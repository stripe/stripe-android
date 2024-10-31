package com.stripe.android.identity.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.stripe.android.identity.R
import com.stripe.android.identity.networking.Resource
import com.stripe.android.identity.networking.models.DobParam
import com.stripe.android.identity.networking.models.DobParam.Companion.toDob
import com.stripe.android.uicore.elements.FieldError
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.SectionElement
import com.stripe.android.uicore.elements.SectionElementUI
import com.stripe.android.uicore.elements.SimpleTextElement
import com.stripe.android.uicore.elements.SimpleTextFieldConfig
import com.stripe.android.uicore.elements.SimpleTextFieldController
import com.stripe.android.uicore.elements.TextFieldState
import com.stripe.android.uicore.utils.collectAsState
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.absoluteValue

/**
 * Section to collect User's date of birth.
 */
@Composable
internal fun DOBSection(
    enabled: Boolean,
    onDobCollected: (Resource<DobParam>) -> Unit
) {
    val dateController = remember {
        SimpleTextFieldController(
            textFieldConfig = DobTextFieldConfig
        )
    }
    val dateString by dateController.fieldValue.collectAsState()
    val dob: DobParam? by remember {
        derivedStateOf {
            dateString.toDob()
        }
    }

    val dobSectionElement = remember {
        SectionElement.wrap(
            sectionFieldElement = SimpleTextElement(
                identifier = IdentifierSpec.Generic(DOB_SPEC),
                controller = dateController
            ),
            label = R.string.stripe_dob
        )
    }

    LaunchedEffect(dob) {
        onDobCollected(
            dob?.let {
                Resource.success(it)
            } ?: run {
                Resource.error()
            }
        )
    }

    SectionElementUI(
        modifier = Modifier.padding(vertical = 8.dp),
        enabled = enabled,
        element = dobSectionElement,
        hiddenIdentifiers = setOf(),
        lastTextFieldIdentifier = null,
    )
}

internal object DobTextFieldConfig : SimpleTextFieldConfig(
    label = R.string.stripe_dob_placeholder
) {
    /**
     * Check if the string is a valid date and is between 01-01-1990 and now.
     */
    private fun String.isValidDate(): Boolean {
        val dateFormat = SimpleDateFormat(
            DATE_PATTERN,
            Locale.getDefault()
        )
        return try {
            dateFormat.parse(this).between(requireNotNull(dateFormat.parse(START_DATE)), Date())
        } catch (e: ParseException) {
            // Otherwise it's an invalid date, prompt error message and ignore the exception
            false
        }
    }

    private fun Date?.between(from: Date, to: Date) =
        this?.let {
            this == from || this.after(from) && this.before(to)
        } ?: run {
            false
        }

    override val keyboard = KeyboardType.Number
    override val visualTransformation = MaskVisualTransformation(DATE_MASK)

    override fun determineState(input: String): TextFieldState = object : TextFieldState {
        override fun shouldShowError(hasFocus: Boolean) =
            !hasFocus && input.isNotBlank() && !input.isValidDate()

        override fun isValid(): Boolean = input.isNotBlank()

        override fun getError(): FieldError = FieldError(R.string.stripe_invalid_dob_error)

        override fun isFull(): Boolean = input.length == DATE_LENGTH

        override fun isBlank(): Boolean = input.isBlank()
    }

    private const val DATE_PATTERN = "MMddyyyy"
    private const val START_DATE = "01011900"
}

internal class MaskVisualTransformation(private val mask: String) : VisualTransformation {
    private val specialSymbolsIndices = mask.indices.filter { mask[it] != '#' }
    override fun filter(text: AnnotatedString): TransformedText {
        var out = ""
        var maskIndex = 0
        text.forEach { char ->
            while (specialSymbolsIndices.contains(maskIndex)) {
                out += mask[maskIndex]
                maskIndex++
            }
            out += char
            maskIndex++
        }
        return TransformedText(AnnotatedString(out), offsetTranslator())
    }

    private fun offsetTranslator() = object : OffsetMapping {
        override fun originalToTransformed(offset: Int): Int {
            val offsetValue = offset.absoluteValue
            if (offsetValue == 0) return 0
            var numberOfHashtags = 0
            val masked = mask.takeWhile {
                if (it == '#') numberOfHashtags++
                numberOfHashtags < offsetValue
            }
            return masked.length + 1
        }

        override fun transformedToOriginal(offset: Int): Int {
            return mask.take(offset.absoluteValue).count { it == '#' }
        }
    }
}

internal const val DATE_MASK = "## / ## / ####"
internal val DATE_LENGTH = DATE_MASK.count { it == '#' }
internal const val DOB_SPEC = "DobSpec"
