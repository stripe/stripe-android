package com.stripe.android.common.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentsheet.utils.ForcedLightAppearance
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import org.junit.Rule
import org.junit.Test

class ForcedLightPrimaryButtonScreenshotTest {

    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance.entries,
        listOf(ForcedLightAppearance),
        boxModifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(vertical = 16.dp)
            .fillMaxWidth(),
    )

    @Test
    fun testForcedLightButton() {
        paparazziRule.snapshot {
            PrimaryButton(isEnabled = true, label = "Pay $50.99", onButtonClick = {})
        }
    }
}
