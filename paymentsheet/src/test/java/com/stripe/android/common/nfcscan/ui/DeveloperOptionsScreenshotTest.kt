package com.stripe.android.common.nfcscan.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.screenshottesting.LayoutDirection
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import org.junit.Rule
import org.junit.Test

class DeveloperOptionsScreenshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule(
        LayoutDirection.entries,
        SystemAppearance.entries,
        boxModifier = Modifier.fillMaxSize(),
        includeStripeTheme = false,
    )

    @Test
    fun default() {
        paparazziRule.snapshot {
            NfcScanningTheme(appearance = PaymentSheet.Appearance()) {
                DeveloperOptionsScreen(onOpenSettings = {})
            }
        }
    }
}
