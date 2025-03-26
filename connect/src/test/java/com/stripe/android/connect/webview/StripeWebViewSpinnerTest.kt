package com.stripe.android.connect.webview

import android.graphics.Color
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.stripe.android.connect.test.TestClock
import com.stripe.android.screenshottesting.PaparazziRule
import org.junit.Rule
import org.junit.Test

class StripeWebViewSpinnerTest {
    @get:Rule
    val paparazziRule = PaparazziRule(
        boxModifier = Modifier.wrapContentSize(),
        theme = "Theme.AppCompat.Light.NoActionBar"
    )

    @Test
    fun testSpinnerColor() {
        paparazziRule.snapshot {
            val context = LocalContext.current

            AndroidView(
                modifier = Modifier.wrapContentSize(),
                factory = {
                    StripeWebViewSpinner(context, clock = TestClock()).apply {
                        setColor(Color.RED)
                    }
                }
            )
        }
    }

    @Test
    fun testSpinnerRotation() {
        paparazziRule.snapshot {
            val context = LocalContext.current
            Column(modifier = Modifier.wrapContentSize()) {
                repeat(10) { i ->
                    AndroidView(
                        modifier = Modifier.wrapContentSize(),
                        factory = {
                            StripeWebViewSpinner(
                                context = context,
                                clock = TestClock(i * 50L)
                            )
                        }
                    )
                }
            }
        }
    }
}
