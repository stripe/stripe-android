package com.stripe.android.paymentsheet.addresselement

import android.text.SpannableString
import androidx.compose.runtime.Composable
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.elements.Appearance
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import com.stripe.android.ui.core.elements.autocomplete.model.AutocompletePrediction
import com.stripe.android.uicore.elements.SimpleTextFieldConfig
import com.stripe.android.uicore.elements.SimpleTextFieldController
import org.junit.Rule
import org.junit.Test

class AutocompleteScreenUIScreenshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance.entries,
    )

    @Test
    fun withPaymentElement() {
        paparazziRule.snapshot {
            AutocompleteTestScreen(
                appearanceContext = AutocompleteAppearanceContext.PaymentElement(
                    appearance = Appearance(),
                ),
            )
        }
    }

    @Test
    fun withLink() {
        paparazziRule.snapshot {
            AutocompleteTestScreen(
                appearanceContext = AutocompleteAppearanceContext.Link,
            )
        }
    }

    @Composable
    private fun AutocompleteTestScreen(
        appearanceContext: AutocompleteAppearanceContext,
    ) {
        appearanceContext.Theme {
            AutocompleteScreenUI(
                predictions = listOf(
                    AutocompletePrediction(
                        primaryText = SpannableString("123 Apple Street"),
                        secondaryText = SpannableString("123 Apple Street, CA, US 99999"),
                        placeId = "placeId1"
                    ),
                    AutocompletePrediction(
                        primaryText = SpannableString("123 Popcorn Street"),
                        secondaryText = SpannableString("123 Popcorn Street, CA, US 88888"),
                        placeId = "placeId2"
                    ),
                ),
                loading = false,
                queryController = SimpleTextFieldController(
                    textFieldConfig = SimpleTextFieldConfig(
                        label = "Address".resolvableString,
                    ),
                    initialValue = "123"
                ),
                appearanceContext = appearanceContext,
                isRootScreen = true,
                attributionDrawable = null,
                onBackPressed = {},
                onEnterManually = {},
                onSelectPrediction = { _ -> }
            )
        }
    }
}
