package com.stripe.android.paymentsheet.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.stripe.android.screenshottesting.FontSize
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import com.stripe.android.utils.screenshots.PaymentSheetAppearance
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
internal class PayButtonIconScreenshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance.entries,
        listOf(PaymentSheetAppearance.DefaultAppearance),
        listOf(FontSize.DefaultFont),
        boxModifier = Modifier.padding(16.dp).fillMaxWidth(),
    )

    @Test
    fun idle(@TestParameter icon: PrimaryButtonIcon, @TestParameter enabled: Boolean) {
        snapshot(icon, enabled, PrimaryButtonProcessingState.Idle(null))
    }

    @Test
    fun processing(@TestParameter icon: PrimaryButtonIcon) {
        snapshot(icon, false, PrimaryButtonProcessingState.Processing)
    }

    @Test
    fun completed(@TestParameter icon: PrimaryButtonIcon) {
        snapshot(icon, false, PrimaryButtonProcessingState.Completed)
    }

    private fun snapshot(icon: PrimaryButtonIcon, enabled: Boolean, state: PrimaryButtonProcessingState) {
        paparazziRule.snapshot {
            PrimaryButton(
                label = "Pay $50.99",
                icon = icon,
                enabled = enabled,
                processingState = state,
                onClick = {},
            )
        }
    }
}
