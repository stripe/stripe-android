package com.stripe.android.link.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import org.junit.Rule
import org.junit.Test

class MinScreenHeightBoxTest {
    @get:Rule
    val paparazziRule = PaparazziRule(
        listOf(SystemAppearance.DarkTheme),
        boxModifier = Modifier
            .padding(0.dp)
            .fillMaxWidth(),
    )

    @Test
    fun testFull() {
        paparazziRule.snapshot {
            DefaultLinkTheme {
                MinScreenHeightBox(1f) {
                    Square()
                }
            }
        }
    }

    @Test
    fun testHalf() {
        paparazziRule.snapshot {
            DefaultLinkTheme {
                MinScreenHeightBox(.5f) {
                    Square()
                }
            }
        }
    }

    @Composable
    private fun BoxScope.Square() {
        Box(
            Modifier
                .align(Alignment.Center)
                .background(Color.Cyan)
                .size(100.dp)
        )
    }
}
