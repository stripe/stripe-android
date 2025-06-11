package com.stripe.android.shoppay.webview

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel

@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun MainWebView(
    viewModel: WebViewModel,
    onNavigationStateChange: (canGoBack: Boolean, canGoForward: Boolean) -> Unit
) {
    AndroidView(
        factory = { ctx ->
            EceWebView(
                context = ctx,
                androidJsBridge = viewModel.androidJsBridge,
                webViewClient = PopUpWebViewClient(
                    onPageLoaded = {
                        viewModel.injectJavaScriptBridge(it)
                    }
                ),
                webChromeClient = PopUpWebChromeClient(
                    context = ctx,
                    androidJsBridge = viewModel.androidJsBridge,
                    setPopUpView = {
                        viewModel.setPopupWebView(it)
                    },
                    closeWebView = {
                        viewModel.closePopup()
                    },
                    onPageLoaded = {
                        viewModel.injectJavaScriptBridge(it)
                    }
                ),
            ).apply {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
//                loadUrl("https://unexpected-dune-list.glitch.me/checkout/")
                viewModel.setWebView(this)
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}