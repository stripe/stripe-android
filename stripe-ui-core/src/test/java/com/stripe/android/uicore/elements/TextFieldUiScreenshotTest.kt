package com.stripe.android.uicore.elements

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.screenshottesting.FontSize
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import com.stripe.android.uicore.R
import com.stripe.android.uicore.elements.TextFieldTest.TestTextFieldConfig
import com.stripe.android.uicore.utils.collectAsState
import org.junit.Rule
import org.junit.Test

class TextFieldUiScreenshotTest {
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
    fun testFilled() {

        paparazziRule.snapshot {
            val simpleTextFieldController = SimpleTextFieldController(
                textFieldConfig = TestTextFieldConfig(
                    maxInputLength = 6
                ),
                initialValue = ""
            )
            val state by simpleTextFieldController.fieldState.collectAsState()
            TextFieldUi(
                label = "Search",
                value = TextFieldValue("John Doe"),
                enabled = true,
                loading = false,
                placeholder = null,
                shouldShowError = false,
                errorMessage = null,
                showOptionalLabel = false,
                trailingIcon = null,
                textFieldController = simpleTextFieldController,
                onTextStateChanged = {},
                fieldState = state
            )
        }
    }

    @Test
    fun testFilledAndDisabled() {
        paparazziRule.snapshot {
            val simpleTextFieldController = SimpleTextFieldController(
                textFieldConfig = TestTextFieldConfig(
                    maxInputLength = 6
                ),
                initialValue = ""
            )
            val state by simpleTextFieldController.fieldState.collectAsState()
            TextFieldUi(
                label = "Search",
                value = TextFieldValue("John Doe"),
                enabled = false,
                loading = false,
                placeholder = null,
                shouldShowError = false,
                errorMessage = null,
                showOptionalLabel = false,
                trailingIcon = null,
                textFieldController = simpleTextFieldController,
                onTextStateChanged = {},
                fieldState = state
            )
        }
    }

    @Test
    fun testFilledWithError() {
        paparazziRule.snapshot {
            val simpleTextFieldController = SimpleTextFieldController(
                textFieldConfig = TestTextFieldConfig(
                    maxInputLength = 6
                ),
                initialValue = ""
            )
            val state by simpleTextFieldController.fieldState.collectAsState()
            TextFieldUi(
                label = "Search",
                value = TextFieldValue("John Doe"),
                enabled = true,
                loading = false,
                placeholder = null,
                shouldShowError = true,
                errorMessage = null,
                showOptionalLabel = false,
                trailingIcon = null,
                textFieldController = simpleTextFieldController,
                onTextStateChanged = {},
                fieldState = state
            )
        }
    }

    @Test
    fun testFilledWithOptionalLabel() {
        paparazziRule.snapshot {
            val simpleTextFieldController = SimpleTextFieldController(
                textFieldConfig = TestTextFieldConfig(
                    maxInputLength = 6
                ),
                initialValue = ""
            )
            val state by simpleTextFieldController.fieldState.collectAsState()
            TextFieldUi(
                label = "Search",
                value = TextFieldValue("John Doe"),
                enabled = true,
                loading = false,
                placeholder = null,
                shouldShowError = false,
                errorMessage = null,
                showOptionalLabel = true,
                trailingIcon = null,
                textFieldController = simpleTextFieldController,
                onTextStateChanged = {},
                fieldState = state
            )
        }
    }

    @Test
    fun testFilledWithTrailingIcon() {
        paparazziRule.snapshot {
            val simpleTextFieldController = SimpleTextFieldController(
                textFieldConfig = TestTextFieldConfig(
                    maxInputLength = 6
                ),
                initialValue = ""
            )
            val state by simpleTextFieldController.fieldState.collectAsState()
            TextFieldUi(
                label = "Search",
                value = TextFieldValue("John Doe"),
                enabled = true,
                loading = false,
                placeholder = null,
                shouldShowError = false,
                errorMessage = null,
                showOptionalLabel = false,
                trailingIcon = TextFieldIcon.Trailing(
                    idRes = R.drawable.stripe_ic_search,
                    isTintable = true,
                ),
                textFieldController = simpleTextFieldController,
                onTextStateChanged = {},
                fieldState = state
            )
        }
    }

    @Test
    fun testFilledWithEnabledDropdown() {
        paparazziRule.snapshot {
            val simpleTextFieldController = SimpleTextFieldController(
                textFieldConfig = TestTextFieldConfig(
                    maxInputLength = 6
                ),
                initialValue = ""
            )
            val state by simpleTextFieldController.fieldState.collectAsState()
            TextFieldUi(
                label = "Search",
                value = TextFieldValue("John Doe"),
                enabled = true,
                loading = false,
                placeholder = null,
                shouldShowError = false,
                errorMessage = null,
                showOptionalLabel = false,
                trailingIcon = TextFieldIcon.Dropdown(
                    title = "Select an option".resolvableString,
                    hide = false,
                    currentItem = TextFieldIcon.Dropdown.Item(
                        id = "visa",
                        label = "Visa".resolvableString,
                        icon = R.drawable.stripe_ic_card_visa
                    ),
                    items = listOf(
                        TextFieldIcon.Dropdown.Item(
                            id = "visa",
                            label = "Visa".resolvableString,
                            icon = R.drawable.stripe_ic_card_visa
                        )
                    )
                ),
                textFieldController = simpleTextFieldController,
                onTextStateChanged = {},
                fieldState = state
            )
        }
    }

    @Test
    fun testFilledWithDisabledDropdown() {
        paparazziRule.snapshot {
            val simpleTextFieldController = SimpleTextFieldController(
                textFieldConfig = TestTextFieldConfig(
                    maxInputLength = 6
                ),
                initialValue = ""
            )
            val state by simpleTextFieldController.fieldState.collectAsState()
            TextFieldUi(
                label = "Card number",
                value = TextFieldValue("4000 0025 0000 1001"),
                enabled = true,
                loading = false,
                placeholder = null,
                shouldShowError = false,
                errorMessage = null,
                showOptionalLabel = false,
                trailingIcon = TextFieldIcon.Dropdown(
                    title = "Select an option".resolvableString,
                    hide = true,
                    currentItem = TextFieldIcon.Dropdown.Item(
                        id = "visa",
                        label = "Visa".resolvableString,
                        icon = R.drawable.stripe_ic_card_visa
                    ),
                    items = listOf(
                        TextFieldIcon.Dropdown.Item(
                            id = "visa",
                            label = "Visa".resolvableString,
                            icon = R.drawable.stripe_ic_card_visa
                        )
                    )
                ),
                textFieldController = simpleTextFieldController,
                onTextStateChanged = {},
                fieldState = state
            )
        }
    }

    @Test
    fun testEmpty() {
        paparazziRule.snapshot {
            val simpleTextFieldController = SimpleTextFieldController(
                textFieldConfig = TestTextFieldConfig(
                    maxInputLength = 6
                ),
                initialValue = ""
            )
            val state by simpleTextFieldController.fieldState.collectAsState()
            TextFieldUi(
                label = "Search",
                value = TextFieldValue(""),
                enabled = true,
                loading = false,
                placeholder = null,
                shouldShowError = false,
                errorMessage = null,
                showOptionalLabel = false,
                trailingIcon = null,
                textFieldController = simpleTextFieldController,
                onTextStateChanged = {},
                fieldState = state
            )
        }
    }

    @Test
    fun testEmptyAndDisabled() {
        paparazziRule.snapshot {
            val simpleTextFieldController = SimpleTextFieldController(
                textFieldConfig = TestTextFieldConfig(
                    maxInputLength = 6
                ),
                initialValue = ""
            )
            val state by simpleTextFieldController.fieldState.collectAsState()
            TextFieldUi(
                label = "Search",
                value = TextFieldValue("John Doe"),
                enabled = false,
                loading = false,
                placeholder = null,
                shouldShowError = false,
                errorMessage = null,
                showOptionalLabel = false,
                trailingIcon = null,
                textFieldController = simpleTextFieldController,
                onTextStateChanged = {},
                fieldState = state
            )
        }
    }

    @Test
    fun testEmptyWithPlaceholder() {
        paparazziRule.snapshot {
            val simpleTextFieldController = SimpleTextFieldController(
                textFieldConfig = TestTextFieldConfig(
                    maxInputLength = 6
                ),
                initialValue = ""
            )
            val state by simpleTextFieldController.fieldState.collectAsState()
            TextFieldUi(
                label = "Search",
                value = TextFieldValue(""),
                enabled = true,
                loading = false,
                placeholder = "Search for someone...",
                shouldShowError = false,
                errorMessage = null,
                showOptionalLabel = false,
                trailingIcon = null,
                textFieldController = simpleTextFieldController,
                onTextStateChanged = {},
                fieldState = state
            )
        }
    }

    @Test
    fun testEmptyWithPlaceholderDisabled() {
        paparazziRule.snapshot {
            val simpleTextFieldController = SimpleTextFieldController(
                textFieldConfig = TestTextFieldConfig(
                    maxInputLength = 6
                ),
                initialValue = ""
            )
            val state by simpleTextFieldController.fieldState.collectAsState()
            TextFieldUi(
                label = "Search",
                value = TextFieldValue(""),
                enabled = false,
                loading = false,
                placeholder = "Search for someone...",
                shouldShowError = false,
                errorMessage = null,
                showOptionalLabel = false,
                trailingIcon = null,
                textFieldController = simpleTextFieldController,
                onTextStateChanged = {},
                fieldState = state
            )
        }
    }
}
