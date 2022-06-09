package com.stripe.android.financialconnections.ui

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.viewinterop.AndroidView
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksActivityViewModel
import com.stripe.android.financialconnections.presentation.FinancialConnectionsSheetNativeViewModel

@Composable
fun PartnerAuthScreen() {
    val activityViewModel: FinancialConnectionsSheetNativeViewModel = mavericksActivityViewModel()
    val url = activityViewModel.collectAsState(mapper = { it.authorizationSession?.url })
    PartnerAuthContent(url.value ?: "")
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PartnerAuthContent(url: String) {
    AndroidView(factory = {
        WebView(it).apply {
            settings.javaScriptEnabled = true
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            webViewClient = WebViewClient()
            loadUrl(url)
        }
    }, update = {
        it.loadUrl(url)
    })
}
