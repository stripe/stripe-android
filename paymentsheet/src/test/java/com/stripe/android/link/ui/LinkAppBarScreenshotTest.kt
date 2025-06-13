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
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
internal class LinkAppBarScreenshotTest(
    private val testCase: TestCase
) {
    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance.entries,
        FontSize.entries,
        boxModifier = Modifier
            .padding(0.dp)
            .fillMaxWidth(),
    )

    @Test
    fun testLinkAppBarState() {
        paparazziRule.snapshot {
            DefaultLinkTheme {
                LinkAppBar(
                    state = testCase.state,
                    onBackPressed = {},
                )
            }
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): List<TestCase> {
            return listOf(
                TestCase(
                    name = "LinkAppBarWithLogoAndCloseButton",
                    state = LinkAppBarState(
                        canNavigateBack = false,
                        showHeader = true,
                    )
                ),
                TestCase(
                    name = "LinkAppBarWithoutLogoAndWithBackButton",
                    state = LinkAppBarState(
                        canNavigateBack = true,
                        showHeader = false,
                    )
                ),
            )
        }
    }

    internal data class TestCase(val name: String, val state: LinkAppBarState) {
        override fun toString(): String = name
    }
}
