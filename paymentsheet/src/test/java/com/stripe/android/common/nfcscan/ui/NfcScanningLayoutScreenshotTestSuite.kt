package com.stripe.android.common.nfcscan.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.stripe.android.common.nfcscan.tapzone.TapZone
import com.stripe.android.core.strings.resolvableString
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
        private val screenshotStatus: NfcScanningScreenshotStatus,
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
                        status = screenshotStatus.status,
                        tapZone = tapZone,
                        deviceRotation = deviceRotation,
                        onClose = {},
                        onSuccessShown = {},
                        onErrorShown = {},
                    )
                }
            }
        }

        sealed interface NfcScanningScreenshotStatus {
            val status: NfcScanningStatus

            data object Idle : NfcScanningScreenshotStatus {
                override val status = NfcScanningStatus.Idle()
            }

            data class Error(val message: String) : NfcScanningScreenshotStatus {
                override val status = NfcScanningStatus.Idle(message.resolvableString)

                override fun toString(): String {
                    return "withError(${message.filter { !it.isWhitespace() }})"
                }
            }
        }

        private companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "{0}_{1}_{2}_{3}")
            fun parameters(): List<Array<out Any?>> = listOf(
                arrayOf(
                    DeviceRotation.Portrait,
                    DEFAULT_TAP_ZONE,
                    Orientation.Portrait,
                    NfcScanningScreenshotStatus.Idle
                ),
                *HIGH_LOW_CASES,
                *CLOSE_TOP_START_END_CASES,
                *LANDSCAPE_CORNER_CASES,
                *ERROR_CASES,
            )

            val DEFAULT_TAP_ZONE = TapZone(xBias = 0.5f, yBias = 0.3f)

            val HIGH_TAP_ZONE = TapZone(xBias = 0.2f, yBias = 0.2f)
            val LOW_TAP_ZONE = TapZone(xBias = 0.2f, yBias = 0.85f)

            val HIGH_LOW_CASES = arrayOf(
                arrayOf(
                    DeviceRotation.Portrait,
                    HIGH_TAP_ZONE,
                    Orientation.Portrait,
                    NfcScanningScreenshotStatus.Idle
                ),
                arrayOf(
                    DeviceRotation.Portrait,
                    LOW_TAP_ZONE,
                    Orientation.Portrait,
                    NfcScanningScreenshotStatus.Idle
                ),
                arrayOf(
                    DeviceRotation.UpsideDown,
                    HIGH_TAP_ZONE,
                    Orientation.Portrait,
                    NfcScanningScreenshotStatus.Idle
                ),
                arrayOf(
                    DeviceRotation.UpsideDown,
                    LOW_TAP_ZONE,
                    Orientation.Portrait,
                    NfcScanningScreenshotStatus.Idle
                ),
            )

            val PORTRAIT_CLOSE_TOP_TAP_ZONE = TapZone(xBias = 0.05f, yBias = 0.05f)
            val PORTRAIT_CLOSE_START_TAP_ZONE = TapZone(xBias = 0.05f, yBias = 0.3f)
            val PORTRAIT_CLOSE_END_TAP_ZONE = TapZone(xBias = 0.95f, yBias = 0.3f)
            val LANDSCAPE_CLOSE_START_TAP_ZONE = TapZone(xBias = 0.5f, yBias = 0.05f)
            val LANDSCAPE_CLOSE_END_TAP_ZONE = TapZone(xBias = 0.5f, yBias = 0.95f)

            val CLOSE_TOP_START_END_CASES = arrayOf(
                arrayOf(
                    DeviceRotation.Portrait,
                    PORTRAIT_CLOSE_TOP_TAP_ZONE,
                    Orientation.Portrait,
                    NfcScanningScreenshotStatus.Idle,
                ),
                arrayOf(
                    DeviceRotation.Portrait,
                    PORTRAIT_CLOSE_START_TAP_ZONE,
                    Orientation.Portrait,
                    NfcScanningScreenshotStatus.Idle,
                ),
                arrayOf(
                    DeviceRotation.Portrait,
                    PORTRAIT_CLOSE_END_TAP_ZONE,
                    Orientation.Portrait,
                    NfcScanningScreenshotStatus.Idle,
                ),
                arrayOf(
                    DeviceRotation.LandscapeRight,
                    LANDSCAPE_CLOSE_START_TAP_ZONE,
                    Orientation.Landscape,
                    NfcScanningScreenshotStatus.Idle,
                ),
                arrayOf(
                    DeviceRotation.LandscapeRight,
                    LANDSCAPE_CLOSE_END_TAP_ZONE,
                    Orientation.Landscape,
                    NfcScanningScreenshotStatus.Idle,
                ),
            )

            val LANDSCAPE_LEFT_TOP_END_CLOSE_TAP_ZONE = TapZone(xBias = 0.8f, yBias = 0.1f)
            val LANDSCAPE_LEFT_TOP_START_CLOSE_TAP_ZONE = TapZone(xBias = 0.1f, yBias = 0.5f)
            val LANDSCAPE_RIGHT_TOP_END_CLOSE_TAP_ZONE = TapZone(xBias = 0.2f, yBias = 0.85f)
            val LANDSCAPE_RIGHT_TOP_START_CLOSE_TAP_ZONE = TapZone(xBias = 0.1f, yBias = 0.5f)

            val LANDSCAPE_CORNER_CASES = arrayOf(
                arrayOf(
                    DeviceRotation.LandscapeLeft,
                    LANDSCAPE_LEFT_TOP_END_CLOSE_TAP_ZONE,
                    Orientation.Landscape,
                    NfcScanningScreenshotStatus.Idle,
                ),
                arrayOf(
                    DeviceRotation.LandscapeLeft,
                    LANDSCAPE_LEFT_TOP_START_CLOSE_TAP_ZONE,
                    Orientation.Landscape,
                    NfcScanningScreenshotStatus.Idle,
                ),
                arrayOf(
                    DeviceRotation.LandscapeRight,
                    LANDSCAPE_RIGHT_TOP_END_CLOSE_TAP_ZONE,
                    Orientation.Landscape,
                    NfcScanningScreenshotStatus.Idle,
                ),
                arrayOf(
                    DeviceRotation.LandscapeRight,
                    LANDSCAPE_RIGHT_TOP_START_CLOSE_TAP_ZONE,
                    Orientation.Landscape,
                    NfcScanningScreenshotStatus.Idle,
                ),
            )

            val ERROR_CASES = arrayOf(
                arrayOf(
                    DeviceRotation.Portrait,
                    DEFAULT_TAP_ZONE,
                    Orientation.Portrait,
                    NfcScanningScreenshotStatus.Error(ERROR_TEXT),
                ),
                arrayOf(
                    DeviceRotation.Portrait,
                    PORTRAIT_CLOSE_START_TAP_ZONE,
                    Orientation.Portrait,
                    NfcScanningScreenshotStatus.Error(ERROR_TEXT),
                ),
                arrayOf(
                    DeviceRotation.Portrait,
                    PORTRAIT_CLOSE_END_TAP_ZONE,
                    Orientation.Portrait,
                    NfcScanningScreenshotStatus.Error(ERROR_TEXT),
                ),
                arrayOf(
                    DeviceRotation.Portrait,
                    LOW_TAP_ZONE,
                    Orientation.Portrait,
                    NfcScanningScreenshotStatus.Error(ERROR_TEXT),
                ),
            )
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
                        status = NfcScanningStatus.Idle(),
                        tapZone = TapZone(xBias = 0.5f, yBias = 0.35f),
                        deviceRotation = DeviceRotation.Portrait,
                        onClose = {},
                        onSuccessShown = {},
                        onErrorShown = {},
                    )
                }
            }
        }

        @Test
        fun error() {
            paparazziRule.gif(end = 1500L) {
                NfcScanningTheme {
                    var status by remember {
                        mutableStateOf<NfcScanningStatus>(NfcScanningStatus.Idle())
                    }

                    LaunchedEffect(Unit) {
                        delay(800L)
                        status = NfcScanningStatus.Idle(ERROR_TEXT.resolvableString)
                    }

                    NfcScanningLayout(
                        status = status,
                        tapZone = TapZone(xBias = 0.5f, yBias = 0.35f),
                        deviceRotation = DeviceRotation.Portrait,
                        onClose = {},
                        onSuccessShown = {},
                        onErrorShown = {},
                    )
                }
            }
        }

        @Test
        fun scanned() {
            paparazziRule.gif(end = 2500L) {
                NfcScanningTheme {
                    var status by remember {
                        mutableStateOf<NfcScanningStatus>(NfcScanningStatus.Idle())
                    }

                    LaunchedEffect(Unit) {
                        delay(600L)
                        status = NfcScanningStatus.Scanning
                        delay(400L)
                        status = NfcScanningStatus.Scanned
                    }

                    NfcScanningLayout(
                        status = status,
                        tapZone = TapZone(xBias = 0.5f, yBias = 0.35f),
                        deviceRotation = DeviceRotation.Portrait,
                        onClose = {},
                        onSuccessShown = {},
                        onErrorShown = {},
                    )
                }
            }
        }
    }

    private companion object {
        const val ERROR_TEXT = "Card expired. Try another card."
    }
}
