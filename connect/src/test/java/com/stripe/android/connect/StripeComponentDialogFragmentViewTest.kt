package com.stripe.android.connect

import android.content.Context
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.apps.common.testing.accessibility.framework.utils.contrast.Color
import com.stripe.android.connect.appearance.Appearance
import com.stripe.android.connect.appearance.Colors
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

    private val testAppearance =
        Appearance(
            colors = Colors(
                text = Color.argb(255, 255, 0, 0),
                border = Color.argb(255, 0, 255, 0),
                background = Color.argb(255, 0, 0, 255),
            )
        )

    @Test
    fun testDefault() {
        snapshot()
    }

    @Test
    fun testLongTitle() {
        snapshot(title = "This is a pretty long component name")
    }

    @Test
    fun testAppearance() {
        snapshot(appearance = testAppearance)
    }

    @Test
    fun testAppearanceWithComponentView() {
        snapshot(
            appearance = testAppearance,
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
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = {
                    StripeComponentDialogFragmentView<TestComponentView>(context).apply {
                        this.title = title
                        bindAppearance(appearance)
                        applyView(context)
                    }
                }
            )
        }
    }
}
