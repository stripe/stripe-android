package com.stripe.android.paymentmethodmessaging.element

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.elements.bottomsheet.StripeBottomSheetLayout
import com.stripe.android.uicore.elements.bottomsheet.rememberStripeBottomSheetLayoutInfo
import com.stripe.android.uicore.elements.bottomsheet.rememberStripeBottomSheetState

internal class LearnMoreActivity : AppCompatActivity() {
    private val args: LearnMoreActivityArgs? by lazy {
        LearnMoreActivityArgs.fromIntent(intent)
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val termsArgs = args
        if (termsArgs == null) {
            finish()
            return
        }

        setContent {
            StripeTheme {
                val bottomSheetState = rememberStripeBottomSheetState()
                val layoutInfo = rememberStripeBottomSheetLayoutInfo()
                StripeBottomSheetLayout(
                    state = bottomSheetState,
                    layoutInfo = layoutInfo,
                    modifier = Modifier,
                    onDismissed = {
                        finish()
                    }
                ) {
                    AndroidView(
                        factory = { context ->
                            WebView(context).apply {
                                webViewClient = WebViewClient()
                                settings.javaScriptEnabled = true
                                loadUrl(termsArgs.learnMoreUrl)
                            }
                        }
                    )
                }
            }
        }
    }
}
