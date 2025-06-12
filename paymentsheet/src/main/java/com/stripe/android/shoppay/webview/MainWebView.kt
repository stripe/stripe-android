package com.stripe.android.shoppay.webview

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun MainWebView(
    viewModel: WebViewModel,
) {
    AndroidView(
        factory = { ctx ->
            EceWebView(
                context = ctx,
                androidJsBridge = viewModel.androidJsBridge,
                webViewClient = PopUpWebViewClient(
                    assetLoader = viewModel.assetLoader(ctx),
                    onPageLoaded = {
                        viewModel.injectJavaScriptBridge(it)
                    }
                ),
                webChromeClient = PopUpWebChromeClient(
                    context = ctx,
                    assetLoader = viewModel.assetLoader(ctx),
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
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                viewModel.setWebView(this)
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}