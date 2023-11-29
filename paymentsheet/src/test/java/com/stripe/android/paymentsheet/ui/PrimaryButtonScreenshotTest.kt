package com.stripe.android.paymentsheet.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stripe.android.paymentsheet.R
import com.stripe.android.utils.screenshots.FontSize
import com.stripe.android.utils.screenshots.PaparazziRule
import com.stripe.android.utils.screenshots.PaymentSheetAppearance
import com.stripe.android.utils.screenshots.SystemAppearance
import org.junit.Rule
import org.junit.Test

class PrimaryButtonScreenshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance.values(),
        PaymentSheetAppearance.values(),
        FontSize.values(),
        boxModifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
    )

    @get:Rule
    val customThemePaparazziRule = PaparazziRule(
        arrayOf(SystemAppearance.LightTheme),
        arrayOf(PaymentSheetAppearance.DefaultAppearance),
        arrayOf(FontSize.DefaultFont),
        boxModifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(vertical = 16.dp)
            .fillMaxWidth(),
    )

    @Test
    fun testIdle() {
        paparazziRule.snapshot {
            PrimaryButton(
                label = "Pay $50.99",
                locked = false,
                enabled = true,
            ) {}
        }
    }

    @Test
    fun testIdleDisabled() {
        paparazziRule.snapshot {
            PrimaryButton(
                label = "Pay $50.99",
                locked = false,
                enabled = false,
            ) {}
        }
    }

    @Test
    fun testIdleLocked() {
        paparazziRule.snapshot {
            PrimaryButton(
                label = "Pay $50.99",
                locked = true,
                enabled = true,
            ) {}
        }
    }

    @Test
    fun testIdleLockedAndDisabled() {
        paparazziRule.snapshot {
            PrimaryButton(
                label = "Pay $50.99",
                locked = true,
                enabled = false,
            ) {}
        }
    }

    @Test
    fun testProcessing() {
        paparazziRule.snapshot {
            PrimaryButton(
                label = "Pay $50.99",
                processingState = PrimaryButtonProcessingState.Processing,
                locked = false,
                enabled = true
            ) {}
        }
    }

    @Test
    fun testProcessingDisabled() {
        paparazziRule.snapshot {
            PrimaryButton(
                label = "Pay $50.99",
                processingState = PrimaryButtonProcessingState.Processing,
                locked = false,
                enabled = false
            ) {}
        }
    }

    @Test
    fun testCompleted() {
        paparazziRule.snapshot {
            PrimaryButton(
                label = "Pay $50.99",
                processingState = PrimaryButtonProcessingState.Completed,
                locked = false,
                enabled = false
            ) {}
        }
    }

    @Test
    fun testEnabledWithCustomTheme() {
        customThemePaparazziRule.snapshotWithCustomTheme {
            PrimaryButton(
                label = "Pay $50.99",
                processingState = PrimaryButtonProcessingState.Idle(null),
                locked = false,
                enabled = true
            ) {}
        }
    }

    @Test
    fun testDisabledWithCustomTheme() {
        customThemePaparazziRule.snapshotWithCustomTheme {
            PrimaryButton(
                label = "Pay $50.99",
                processingState = PrimaryButtonProcessingState.Idle(null),
                locked = false,
                enabled = false
            ) {}
        }
    }

    @Test
    fun testProcessingWithCustomTheme() {
        customThemePaparazziRule.snapshotWithCustomTheme {
            PrimaryButton(
                label = "Pay $50.99",
                processingState = PrimaryButtonProcessingState.Processing,
                locked = false,
                enabled = true
            ) {}
        }
    }

    @Test
    fun testCompletedWithCustomTheme() {
        customThemePaparazziRule.snapshotWithCustomTheme {
            PrimaryButton(
                label = "Pay $50.99",
                processingState = PrimaryButtonProcessingState.Completed,
                locked = false,
                enabled = true
            ) {}
        }
    }

    private fun PaparazziRule.snapshotWithCustomTheme(
        content: @Composable () -> Unit
    ) {
        snapshot {
            PrimaryButtonTheme(
                colors = PrimaryButtonColors(
                    background = colorResource(
                        id = R.color.stripe_paymentsheet_googlepay_primary_button_background_color
                    ),
                    onBackground = colorResource(
                        id = R.color.stripe_paymentsheet_googlepay_primary_button_tint_color
                    ),
                    successBackground = colorResource(
                        id = R.color.stripe_paymentsheet_googlepay_primary_button_background_color
                    ),
                    onSuccessBackground = colorResource(
                        id = R.color.stripe_paymentsheet_googlepay_primary_button_tint_color
                    ),
                    border = colorResource(
                        id = R.color.stripe_paymentsheet_card_stroke
                    )
                ),
                shape = PrimaryButtonShape(
                    cornerRadius = 16.dp,
                    borderStrokeWidth = 3.dp
                ),
                typography = PrimaryButtonTypography(
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 18.sp
                )
            ) {
                content()
            }
        }
    }
}
