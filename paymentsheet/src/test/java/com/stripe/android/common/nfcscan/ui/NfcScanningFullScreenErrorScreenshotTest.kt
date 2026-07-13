package com.stripe.android.common.nfcscan.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.screenshottesting.Orientation
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
internal class NfcScanningFullScreenErrorScreenshotTest(
    private val deviceRotation: DeviceRotation,
    paparazziOrientation: Orientation,
) {
    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance.entries,
        listOf(paparazziOrientation),
        boxModifier = Modifier.fillMaxSize(),
        includeStripeTheme = false,
    )

    @Test
    fun snapshot() {
        paparazziRule.snapshot {
            NfcScanningTheme {
                NfcScanningFullScreenErrorLayout(
                    message = "Turn developer options off and try again.".resolvableString,
                    deviceRotation = deviceRotation,
                    onClose = {},
                )
            }
        }
    }

    private companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun parameters(): List<Array<Any>> = listOf(
            arrayOf(
                DeviceRotation.Portrait,
                Orientation.Portrait,
            ),
            arrayOf(
                DeviceRotation.LandscapeRight,
                Orientation.Landscape,
            ),
        )
    }
}
