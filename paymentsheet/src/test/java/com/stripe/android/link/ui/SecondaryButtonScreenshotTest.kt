package com.stripe.android.link.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.screenshottesting.FontSize
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import org.junit.Rule
import org.junit.Test

internal class SecondaryButtonScreenshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance.entries,
        FontSize.entries,
        boxModifier = Modifier
            .padding(0.dp)
            .fillMaxWidth(),
    )

    @Test
    fun testEnabledState() {
        snapshot(enabled = true)
    }

    @Test
    fun testDisabledState() {
        snapshot(enabled = false)
    }

    private fun snapshot(
        enabled: Boolean
    ) {
        paparazziRule.snapshot {
            DefaultLinkTheme {
                SecondaryButton(
                    enabled = enabled,
                    label = "Pay Another Way",
                    onClick = {}
                )
            }
        }
    }
}
