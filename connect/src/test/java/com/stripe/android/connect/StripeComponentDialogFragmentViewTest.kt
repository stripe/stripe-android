package com.stripe.android.connect

import android.content.Context
import android.graphics.Typeface
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.apps.common.testing.accessibility.framework.utils.contrast.Color
import com.stripe.android.connect.appearance.Appearance
import com.stripe.android.connect.appearance.Colors
import com.stripe.android.connect.appearance.Typography
import com.stripe.android.connect.appearance.fonts.CustomFontSource
import com.stripe.android.connect.test.TestComponentView
import com.stripe.android.screenshottesting.Orientation
import com.stripe.android.screenshottesting.PaparazziRule
import org.junit.Rule
import org.junit.Test

@OptIn(PrivateBetaConnectSDK::class)
class StripeComponentDialogFragmentViewTest {
    @get:Rule
    val paparazziRule = PaparazziRule(
        Orientation.entries,
        boxModifier = Modifier.fillMaxSize(),
        theme = "Theme.AppCompat.Light.NoActionBar"
    )

    @Test
    fun testDefault() {
        snapshot()
    }

    @Test
    fun testLongTitle() {
        snapshot(title = "This is a pretty long component name for a title")
    }

    @Test
    fun testAppearance() {
        snapshot(appearance = createAppearance())
    }

    @Test
    fun testAppearanceWithCustomFont() {
        snapshot(appearance = createAppearance(includeFont = true))
    }

    @Test
    fun testAppearanceWithComponentView() {
        snapshot(
            appearance = createAppearance(),
            applyView = { context ->
                componentView = TestComponentView(context).apply {
                    setBackgroundColor(Color.argb(255, 0, 255, 255))
                    addView(
                        TextView(context).apply {
                            text = "Hello world"
                            setTextColor(Color.BLACK)
                        }
                    )
                }
            }
        )
    }

    private fun snapshot(
        title: String = "Account Onboarding",
        appearance: Appearance = Appearance(),
        applyView: StripeComponentDialogFragmentView<TestComponentView>.(Context) -> Unit = {},
    ) {
        paparazziRule.snapshot {
            val context = LocalContext.current
            val customFonts = listOf(CustomFontSource("fonts/doto.ttf", "doto"))
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = {
                    TestStripeComponentDialogFragmentView(context).apply {
                        this.title = title
                        bindAppearance(appearance, customFonts)
                        applyView(context)
                    }
                }
            )
        }
    }

    private fun createAppearance(includeFont: Boolean = false) =
        Appearance(
            colors = Colors(
                text = Color.argb(255, 255, 0, 0),
                border = Color.argb(255, 0, 255, 0),
                background = Color.argb(255, 0, 0, 255),
            ),
            typography = Typography(fontFamily = "doto".takeIf { includeFont })
        )

    private class TestStripeComponentDialogFragmentView(
        context: Context
    ) : StripeComponentDialogFragmentView<TestComponentView>(context) {
        // The real implementation uses `Typeface.createFromAsset` which is not functional in
        // Paparazzi tests due to AssetManager not being fully implemented in in layoutlib.
        // Workaround this by creating from a file on the classpath. We lose some integration
        // fidelity but at least we can still verify that the view is rendering correctly.
        override fun createTypeface(customFontSource: CustomFontSource): Typeface {
            val fontFileUrl = this.javaClass.classLoader!!.getResource(customFontSource.assetsFilePath)
            return Typeface.createFromFile(fontFileUrl.path)
        }
    }
}
