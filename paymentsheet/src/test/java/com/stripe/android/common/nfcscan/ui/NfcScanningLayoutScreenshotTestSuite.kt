package com.stripe.android.common.nfcscan.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.stripe.android.common.nfcscan.tapzone.TapZone
import com.stripe.android.screenshottesting.Orientation
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import kotlinx.coroutines.delay
import org.junit.Rule
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Enclosed::class)
internal class NfcScanningLayoutScreenshotTestSuite {
    @RunWith(Parameterized::class)
    class Static(
        private val deviceRotation: DeviceRotation,
        private val tapZone: TapZone,
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
                    NfcScanningLayout(
                        status = NfcScanningStatus.Idle,
                        tapZone = tapZone,
                        deviceRotation = deviceRotation,
                        onClose = {},
                        onSuccessShown = {},
                    )
                }
            }
        }

        private companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "{0}_{1}")
            fun parameters(): List<Array<Any>> = listOf(
                arrayOf(DeviceRotation.Portrait, LOW_TAP_ZONE, Orientation.Portrait),
                arrayOf(DeviceRotation.Portrait, HIGH_TAP_ZONE, Orientation.Portrait),
                arrayOf(DeviceRotation.UpsideDown, LOW_TAP_ZONE, Orientation.Portrait),
                arrayOf(DeviceRotation.UpsideDown, HIGH_TAP_ZONE, Orientation.Portrait),
                arrayOf(
                    DeviceRotation.LandscapeLeft,
                    LANDSCAPE_LEFT_TOP_END_CLOSE_TAP_ZONE,
                    Orientation.Landscape,
                ),
                arrayOf(
                    DeviceRotation.LandscapeLeft,
                    LANDSCAPE_LEFT_TOP_START_CLOSE_TAP_ZONE,
                    Orientation.Landscape,
                ),
                arrayOf(
                    DeviceRotation.LandscapeRight,
                    LANDSCAPE_RIGHT_TOP_END_CLOSE_TAP_ZONE,
                    Orientation.Landscape,
                ),
                arrayOf(
                    DeviceRotation.LandscapeRight,
                    LANDSCAPE_RIGHT_TOP_START_CLOSE_TAP_ZONE,
                    Orientation.Landscape,
                ),
            )

            val LOW_TAP_ZONE = TapZone(xBias = 0.2f, yBias = 0.2f)
            val HIGH_TAP_ZONE = TapZone(xBias = 0.2f, yBias = 0.85f)
            val LANDSCAPE_LEFT_TOP_END_CLOSE_TAP_ZONE = TapZone(xBias = 0.8f, yBias = 0.1f)
            val LANDSCAPE_LEFT_TOP_START_CLOSE_TAP_ZONE = TapZone(xBias = 0.1f, yBias = 0.5f)
            val LANDSCAPE_RIGHT_TOP_END_CLOSE_TAP_ZONE = TapZone(xBias = 0.2f, yBias = 0.85f)
            val LANDSCAPE_RIGHT_TOP_START_CLOSE_TAP_ZONE = TapZone(xBias = 0.1f, yBias = 0.5f)
        }
    }

    class Animated {
        @get:Rule
        val paparazziRule = PaparazziRule(
            boxModifier = Modifier.fillMaxSize(),
            includeStripeTheme = false,
        )

        @Test
        fun idle() {
            paparazziRule.gif(end = 1500L) {
                NfcScanningTheme {
                    NfcScanningLayout(
                        status = NfcScanningStatus.Idle,
                        tapZone = TapZone(xBias = 0.5f, yBias = 0.35f),
                        deviceRotation = DeviceRotation.Portrait,
                        onClose = {},
                        onSuccessShown = {},
                    )
                }
            }
        }

        @Test
        fun scanned() {
            paparazziRule.gif(end = 2400L) {
                NfcScanningTheme {
                    var status by remember { mutableStateOf<NfcScanningStatus>(NfcScanningStatus.Idle) }

                    LaunchedEffect(Unit) {
                        delay(800L)
                        status = NfcScanningStatus.Scanned
                    }

                    NfcScanningLayout(
                        status = status,
                        tapZone = TapZone(xBias = 0.5f, yBias = 0.35f),
                        deviceRotation = DeviceRotation.Portrait,
                        onClose = {},
                        onSuccessShown = {},
                    )
                }
            }
        }
    }
}
