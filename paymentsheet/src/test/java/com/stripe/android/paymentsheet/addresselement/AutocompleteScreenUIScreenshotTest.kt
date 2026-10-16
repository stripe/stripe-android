@file:OptIn(com.stripe.android.paymentelement.AppearanceAPIAdditionsPreview::class)

package com.stripe.android.paymentsheet.addresselement

import android.text.SpannableString
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import com.stripe.android.ui.core.elements.autocomplete.model.AutocompletePrediction
import com.stripe.android.uicore.elements.SimpleTextFieldConfig
import com.stripe.android.uicore.elements.SimpleTextFieldController
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
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
                    appearance = PaymentSheet.Appearance(),
                ),
            )
        }
    }

    @Test
    fun `custom payment element appearance renders in every theme mode`(
        @TestParameter themeMode: PaymentSheet.ThemeMode,
    ) {
        paparazziRule.snapshot {
            AutocompleteTestScreen(
                appearanceContext = AutocompleteAppearanceContext.PaymentElement(
                    appearance = PaymentSheet.Appearance(
                        colorsLight = PaymentSheet.Colors.Builder.light()
                            .primary(Color(0xFF0057B8))
                            .surface(Color(0xFFE0ECFF))
                            .onSurface(Color(0xFF071A2F))
                            .build(),
                        colorsDark = PaymentSheet.Colors.Builder.dark()
                            .primary(Color(0xFF8CC8FF))
                            .surface(Color(0xFF203448))
                            .onSurface(Color.White)
                            .build(),
                        shapes = PaymentSheet.Shapes(cornerRadiusDp = 12f, borderStrokeWidthDp = 1f),
                        themeMode = themeMode,
                    ),
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
