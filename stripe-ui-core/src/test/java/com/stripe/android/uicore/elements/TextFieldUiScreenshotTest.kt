package com.stripe.android.uicore.elements

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.uicore.R
import com.stripe.android.uicore.utils.FontSize
import com.stripe.android.uicore.utils.PaparazziRule
import com.stripe.android.uicore.utils.SystemAppearance
import org.junit.Rule
import org.junit.Test

class TextFieldUiScreenshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance.values(),
        FontSize.values(),
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
                value = "John Doe",
                enabled = true,
                loading = false,
                placeholder = null,
                shouldShowError = false,
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
                value = "John Doe",
                enabled = false,
                loading = false,
                placeholder = null,
                shouldShowError = false,
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
                value = "John Doe",
                enabled = true,
                loading = false,
                placeholder = null,
                shouldShowError = true,
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
                value = "John Doe",
                enabled = true,
                loading = false,
                placeholder = null,
                shouldShowError = false,
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
                value = "John Doe",
                enabled = true,
                loading = false,
                placeholder = null,
                shouldShowError = false,
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
                value = "John Doe",
                enabled = true,
                loading = false,
                placeholder = null,
                shouldShowError = false,
                showOptionalLabel = false,
                trailingIcon = TextFieldIcon.Dropdown(
                    title = resolvableString("Select an option"),
                    hide = false,
                    currentItem = TextFieldIcon.Dropdown.Item(
                        id = "visa",
                        label = resolvableString("Visa"),
                        icon = R.drawable.stripe_ic_card_visa
                    ),
                    items = listOf(
                        TextFieldIcon.Dropdown.Item(
                            id = "visa",
                            label = resolvableString("Visa"),
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
                value = "4000 0025 0000 1001",
                enabled = true,
                loading = false,
                placeholder = null,
                shouldShowError = false,
                showOptionalLabel = false,
                trailingIcon = TextFieldIcon.Dropdown(
                    title = resolvableString("Select an option"),
                    hide = true,
                    currentItem = TextFieldIcon.Dropdown.Item(
                        id = "visa",
                        label = resolvableString("Visa"),
                        icon = R.drawable.stripe_ic_card_visa
                    ),
                    items = listOf(
                        TextFieldIcon.Dropdown.Item(
                            id = "visa",
                            label = resolvableString("Visa"),
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
                value = "",
                enabled = true,
                loading = false,
                placeholder = null,
                shouldShowError = false,
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
                value = "John Doe",
                enabled = false,
                loading = false,
                placeholder = null,
                shouldShowError = false,
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
                value = "",
                enabled = true,
                loading = false,
                placeholder = "Search for someone...",
                shouldShowError = false,
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
                value = "",
                enabled = false,
                loading = false,
                placeholder = "Search for someone...",
                shouldShowError = false,
                showOptionalLabel = false,
                trailingIcon = null
            )
        }
    }
}
