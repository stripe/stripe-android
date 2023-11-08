package com.stripe.android.paymentsheet.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
}
