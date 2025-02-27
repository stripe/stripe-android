package com.stripe.android.uicore.elements

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.screenshottesting.FontSize
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import com.stripe.android.uicore.R
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
            TextFieldUi(
                label = "Search",
                value = TextFieldValue("John Doe"),
                enabled = true,
                loading = false,
                placeholder = null,
                shouldShowError = false,
                errorString = null,
                showOptionalLabel = false,
                trailingIcon = null
            )
        }
    }

    @Test
    fun testFilledAndDisabled() {
        paparazziRule.snapshot {
            TextFieldUi(
                label = "Search",
                value = TextFieldValue("John Doe"),
                enabled = false,
                loading = false,
                placeholder = null,
                shouldShowError = false,
                errorString = null,
                showOptionalLabel = false,
                trailingIcon = null
            )
        }
    }

    @Test
    fun testFilledWithError() {
        paparazziRule.snapshot {
            TextFieldUi(
                label = "Search",
                value = TextFieldValue("John Doe"),
                enabled = true,
                loading = false,
                placeholder = null,
                shouldShowError = true,
                errorString = null,
                showOptionalLabel = false,
                trailingIcon = null
            )
        }
    }

    @Test
    fun testFilledWithOptionalLabel() {
        paparazziRule.snapshot {
            TextFieldUi(
                label = "Search",
                value = TextFieldValue("John Doe"),
                enabled = true,
                loading = false,
                placeholder = null,
                shouldShowError = false,
                errorString = null,
                showOptionalLabel = true,
                trailingIcon = null
            )
        }
    }

    @Test
    fun testFilledWithTrailingIcon() {
        paparazziRule.snapshot {
            TextFieldUi(
                label = "Search",
                value = TextFieldValue("John Doe"),
                enabled = true,
                loading = false,
                placeholder = null,
                shouldShowError = false,
                errorString = null,
                showOptionalLabel = false,
                trailingIcon = TextFieldIcon.Trailing(
                    idRes = R.drawable.stripe_ic_search,
                    isTintable = true,
                )
            )
        }
    }

    @Test
    fun testFilledWithEnabledDropdown() {
        paparazziRule.snapshot {
            TextFieldUi(
                label = "Search",
                value = TextFieldValue("John Doe"),
                enabled = true,
                loading = false,
                placeholder = null,
                shouldShowError = false,
                errorString = null,
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
                )
            )
        }
    }

    @Test
    fun testFilledWithDisabledDropdown() {
        paparazziRule.snapshot {
            TextFieldUi(
                label = "Card number",
                value = TextFieldValue("4000 0025 0000 1001"),
                enabled = true,
                loading = false,
                placeholder = null,
                shouldShowError = false,
                errorString = null,
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
                )
            )
        }
    }

    @Test
    fun testEmpty() {
        paparazziRule.snapshot {
            TextFieldUi(
                label = "Search",
                value = TextFieldValue(""),
                enabled = true,
                loading = false,
                placeholder = null,
                shouldShowError = false,
                errorString = null,
                showOptionalLabel = false,
                trailingIcon = null
            )
        }
    }

    @Test
    fun testEmptyAndDisabled() {
        paparazziRule.snapshot {
            TextFieldUi(
                label = "Search",
                value = TextFieldValue("John Doe"),
                enabled = false,
                loading = false,
                placeholder = null,
                shouldShowError = false,
                errorString = null,
                showOptionalLabel = false,
                trailingIcon = null
            )
        }
    }

    @Test
    fun testEmptyWithPlaceholder() {
        paparazziRule.snapshot {
            TextFieldUi(
                label = "Search",
                value = TextFieldValue(""),
                enabled = true,
                loading = false,
                placeholder = "Search for someone...",
                shouldShowError = false,
                errorString = null,
                showOptionalLabel = false,
                trailingIcon = null
            )
        }
    }

    @Test
    fun testEmptyWithPlaceholderDisabled() {
        paparazziRule.snapshot {
            TextFieldUi(
                label = "Search",
                value = TextFieldValue(""),
                enabled = false,
                loading = false,
                placeholder = "Search for someone...",
                shouldShowError = false,
                errorString = null,
                showOptionalLabel = false,
                trailingIcon = null
            )
        }
    }
}
