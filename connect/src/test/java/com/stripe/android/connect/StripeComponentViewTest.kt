package com.stripe.android.connect

import android.content.Context
import android.graphics.Color
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.stripe.android.connect.appearance.Appearance
import com.stripe.android.connect.appearance.Colors
import com.stripe.android.connect.test.TestComponentView
import com.stripe.android.connect.webview.StripeConnectWebViewContainerState
import com.stripe.android.screenshottesting.PaparazziRule
import org.junit.Rule
import org.junit.Test

@OptIn(PrivateBetaConnectSDK::class)
class StripeComponentViewTest {
    @get:Rule
    val paparazziRule = PaparazziRule(
        boxModifier = Modifier.fillMaxSize(),
        theme = "Theme.AppCompat.Light.NoActionBar"
    )

    @Test
    fun testLoadingSpinner() {
        snapshot(
            applyView = { context ->
                addProgressBar()
                bindViewModelState(
                    StripeConnectWebViewContainerState(
                        isNativeLoadingIndicatorVisible = true,
                    )
                )
            }
        )
    }

    @Test
    fun testLoadingSpinnerWithAppearance() {
        snapshot(
            applyView = {
                addProgressBar()
                bindViewModelState(
                    StripeConnectWebViewContainerState(
                        isNativeLoadingIndicatorVisible = true,
                        appearance = Appearance(
                            colors = Colors(
                                secondaryText = Color.RED
                            )
                        )
                    )
                )
            }
        )
    }

    private fun snapshot(
        applyView: TestComponentView.(Context) -> Unit = {},
    ) {
        paparazziRule.snapshot {
            val context = LocalContext.current
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = {
                    TestComponentView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        applyView(context)
                    }
                }
            )
        }
    }
}
