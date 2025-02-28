package com.stripe.android.uicore.elements

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import com.stripe.android.screenshottesting.PaparazziConfigOption
import com.stripe.android.screenshottesting.PaparazziRule
import org.junit.Rule
import org.junit.Test

internal class CenteredTextLayoutTest {
    private enum class FontSize(val scaleFactor: Float) : PaparazziConfigOption {
        SmallFont(scaleFactor = 0.5f),
        DefaultFont(scaleFactor = 1f),
        LargeFont(scaleFactor = 1.5f),
        MegaFont(scaleFactor = 2f);

        override fun apply(deviceConfig: DeviceConfig): DeviceConfig {
            return deviceConfig.copy(
                fontScale = scaleFactor,
            )
        }
    }

    @get:Rule
    val paparazziRule = PaparazziRule(FontSize.entries)

    @Test
    fun testSingleLineTextWithSmallStartContent() {
        test("Some random text") {
            Spacer(
                modifier = Modifier
                    .background(color = Color.Red)
                    .size(6.dp)
            )
        }
    }

    @Test
    fun testSingleLineTextWithLargeStartContent() {
        test("Some random text") {
            Spacer(
                modifier = Modifier
                    .background(color = Color.Red)
                    .size(36.dp)
            )
        }
    }

    @Test
    fun testMultiLineTextWithSmallStartContent() {
        test("Some multiline text\nSome multiline text") {
            Spacer(
                modifier = Modifier
                    .background(color = Color.Red)
                    .size(6.dp)
            )
        }
    }

    @Test
    fun testMultiLineTextWithLargeStartContent() {
        test("Some multiline text\nSome multiline text") {
            Spacer(
                modifier = Modifier
                    .background(color = Color.Red)
                    .size(36.dp)
            )
        }
    }

    private fun test(
        text: String,
        startContent: @Composable () -> Unit,
    ) {
        paparazziRule.snapshot {
            CenteredTextLayout(
                startContent = {
                    startContent()
                },
                textContent = {
                    H6Text(
                        text = text,
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .border(width = 1.dp, color = Color.Red),
                        includeFontPadding = false,
                    )
                },
            )
        }
    }
}
