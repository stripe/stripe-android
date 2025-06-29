package com.stripe.android.shoppay.webview

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.stripe.android.shoppay.ShopPayViewModel

@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun MainWebView(
    viewModel: ShopPayViewModel,
) {
    val context = LocalContext.current
    val assetLoader = remember {
        viewModel.assetLoader(context)
    }
    AndroidView(
        factory = { ctx ->
            EceWebView(
                context = ctx,
                bridgeHandler = viewModel.bridgeHandler,
                webViewClient = PopUpWebViewClient(
                    assetLoader = assetLoader,
                    onPageLoaded = viewModel::onPageLoaded
                ),
                webChromeClient = PopUpWebChromeClient(
                    context = ctx,
                    bridgeHandler = viewModel.bridgeHandler,
                    assetLoader = assetLoader,
                    setPopUpView = viewModel::setPopupWebView,
                    closeWebView = viewModel::closePopup,
                    onPageLoaded = viewModel::onPageLoaded,
                )
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
