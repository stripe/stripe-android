package com.stripe.android.crypto.onramp.ui.theme

import android.content.res.Configuration
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.LinkAppearance
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.createComposeCleanupRule
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
internal class OnrampThemeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val composeCleanupRule = createComposeCleanupRule()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(UnconfinedTestDispatcher())

    @Test
    @Config(qualifiers = "notnight")
    fun `forced dark appearance applies dark colors and resources`() {
        assertAppearance(LinkAppearance.Style.ALWAYS_DARK, Configuration.UI_MODE_NIGHT_YES, Color.Blue)
    }

    @Test
    @Config(qualifiers = "night")
    fun `forced light appearance applies light colors and resources`() {
        assertAppearance(LinkAppearance.Style.ALWAYS_LIGHT, Configuration.UI_MODE_NIGHT_NO, Color.Red)
    }

    private fun assertAppearance(style: LinkAppearance.Style, expectedUiMode: Int, expectedPrimary: Color) {
        val appearance = LinkAppearance()
            .style(style)
            .lightColors(LinkAppearance.Colors().primary(Color.Red).contentOnPrimary(Color.White))
            .darkColors(LinkAppearance.Colors().primary(Color.Blue).contentOnPrimary(Color.White))
            .primaryButton(LinkAppearance.PrimaryButton().heightDp(64f).cornerRadiusDp(20f))
            .build()
        var actualColors: OnrampColors? = null
        var actualShapes: OnrampShapes? = null
        var actualUiMode: Int? = null

        composeRule.setContent {
            DefaultOnrampTheme(appearance) {
                actualColors = OnrampTheme.colors
                actualShapes = OnrampTheme.shapes
                actualUiMode = LocalContext.current.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            }
        }

        composeRule.runOnIdle {
            assertThat(actualColors?.buttonBrand).isEqualTo(expectedPrimary)
            assertThat(actualColors?.onButtonBrand).isEqualTo(Color.White)
            assertThat(actualShapes?.primaryButtonHeight).isEqualTo(64.dp)
            assertThat(actualUiMode).isEqualTo(expectedUiMode)
        }
    }
}
