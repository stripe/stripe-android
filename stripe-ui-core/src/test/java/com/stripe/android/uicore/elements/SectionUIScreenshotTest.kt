package com.stripe.android.uicore.elements

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.unit.dp
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.screenshottesting.FontSize
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import com.stripe.android.uicore.R
import com.stripe.android.uicore.forms.FormFieldEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Rule
import org.junit.Test

class SectionUIScreenshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance.entries,
        FontSize.entries,
        boxModifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(vertical = 16.dp)
            .fillMaxWidth(),
    )

    @Test
    fun testSectionWithTextFieldWarning() {
        paparazziRule.snapshot {
            val textFieldController = FakeTextFieldController(
                fieldValue = "1234",
                validationMsg = FieldValidationMessage.Warning(
                    message = R.string.stripe_address_zip_incomplete
                )
            )

            val sectionElement = SectionElement.wrap(
                sectionFieldElement = SimpleTextElement(
                    identifier = IdentifierSpec.Generic("zip"),
                    controller = textFieldController
                ),
                label = "Billing Address".resolvableString
            )

            SectionElementUI(
                enabled = true,
                element = sectionElement,
                hiddenIdentifiers = emptySet(),
                lastTextFieldIdentifier = null
            )
        }
    }

    @Test
    fun testSectionWithTextFieldError() {
        paparazziRule.snapshot {
            val textFieldController = FakeTextFieldController(
                fieldValue = "123",
                validationMsg = FieldValidationMessage.Error(
                    message = R.string.stripe_address_zip_incomplete
                )
            )

            val sectionElement = SectionElement.wrap(
                sectionFieldElement = SimpleTextElement(
                    identifier = IdentifierSpec.Generic("zip"),
                    controller = textFieldController
                ),
                label = "Billing Address".resolvableString
            )

            SectionElementUI(
                enabled = true,
                element = sectionElement,
                hiddenIdentifiers = emptySet(),
                lastTextFieldIdentifier = null
            )
        }
    }

    @Test
    fun testSectionWithoutError() {
        paparazziRule.snapshot {
            val textFieldController = FakeTextFieldController(
                fieldValue = "12345",
                validationMsg = null
            )

            val sectionElement = SectionElement.wrap(
                sectionFieldElement = SimpleTextElement(
                    identifier = IdentifierSpec.Generic("zip"),
                    controller = textFieldController
                ),
                label = "Billing Address".resolvableString
            )

            SectionElementUI(
                enabled = true,
                element = sectionElement,
                hiddenIdentifiers = emptySet(),
                lastTextFieldIdentifier = null
            )
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    private class FakeTextFieldController(
        fieldValue: String,
        private val validationMsg: FieldValidationMessage?
    ) : TextFieldController {
        override val label: StateFlow<com.stripe.android.core.strings.ResolvableString> =
            MutableStateFlow("ZIP Code".resolvableString)
        override val fieldValue: StateFlow<String> = MutableStateFlow(fieldValue)
        override val rawFieldValue: StateFlow<String?> = MutableStateFlow(fieldValue)
        override val contentDescription: StateFlow<com.stripe.android.core.strings.ResolvableString> =
            MutableStateFlow("ZIP Code".resolvableString)
        override val isComplete: StateFlow<Boolean> = MutableStateFlow(validationMsg == null)
        override val formFieldValue: StateFlow<FormFieldEntry> = MutableStateFlow(
            FormFieldEntry(fieldValue, validationMsg == null)
        )
        override val validationMessage: StateFlow<FieldValidationMessage?> = MutableStateFlow(validationMsg)
        override val visibleValidationMessage: StateFlow<Boolean> = MutableStateFlow(validationMsg != null)
        override val showOptionalLabel: Boolean = false
        override val debugLabel: String = "zip"
        override val initialValue: String? = null
        override val autofillType: ContentType? = null
        override val trailingIcon: StateFlow<TextFieldIcon?> = MutableStateFlow(null)
        override val capitalization: androidx.compose.ui.text.input.KeyboardCapitalization =
            androidx.compose.ui.text.input.KeyboardCapitalization.None
        override val keyboardType: androidx.compose.ui.text.input.KeyboardType =
            androidx.compose.ui.text.input.KeyboardType.Number
        override val layoutDirection: androidx.compose.ui.unit.LayoutDirection? = null
        override val visualTransformation: StateFlow<androidx.compose.ui.text.input.VisualTransformation> =
            MutableStateFlow(androidx.compose.ui.text.input.VisualTransformation.None)
        override val fieldState: StateFlow<TextFieldState> = MutableStateFlow(
            if (validationMsg != null) {
                TextFieldStateConstants.Valid.Full(validationMsg)
            } else {
                TextFieldStateConstants.Valid.Full()
            }
        )
        override val loading: StateFlow<Boolean> = MutableStateFlow(false)

        override fun onRawValueChange(rawValue: String) = Unit
        override fun onValueChange(displayFormatted: String): TextFieldState? = null
        override fun onFocusChange(newHasFocus: Boolean) = Unit
    }
}
