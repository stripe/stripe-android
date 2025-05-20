package com.stripe.android.link.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import org.junit.Rule
import org.junit.Test

class LinkLoadingScreenKtTest {
    @get:Rule
    val paparazziRule = PaparazziRule(
        listOf(SystemAppearance.DarkTheme),
        boxModifier = Modifier
            .padding(0.dp)
            .fillMaxWidth(),
    )

    @Test
    fun testDefault() {
        paparazziRule.snapshot {
            DefaultLinkTheme {
                LinkLoadingScreen()
            }
        }
    }

    @Test
    fun testWithScreenSize() {
        paparazziRule.snapshot {
            DefaultLinkTheme {
                val intSize = with(LocalDensity.current) {
                    IntSize(300.dp.roundToPx(), 500.dp.roundToPx())
                }
                ProvideLinkScreenSize(intSize) {
                    LinkLoadingScreen()
                }
            }
        }
    }
}
