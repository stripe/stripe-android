package com.stripe.android.financialconnections.features.partnerauth

import android.annotation.SuppressLint
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksActivityViewModel
import com.airbnb.mvrx.compose.mavericksViewModel
import com.google.accompanist.web.AccompanistWebViewClient
import com.google.accompanist.web.WebView
import com.google.accompanist.web.WebViewState
import com.google.accompanist.web.rememberWebViewState
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.navigation.NavigationDirections
import com.stripe.android.financialconnections.presentation.FinancialConnectionsSheetNativeViewModel
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme
import java.net.URL

@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun PartnerAuthScreen() {
    // get shared configuration from activity state
    val activityViewModel: FinancialConnectionsSheetNativeViewModel = mavericksActivityViewModel()
    val authSession = activityViewModel.collectAsState { it.manifest }
    var url by remember { mutableStateOf(authSession.value.hostedAuthUrl) }
    val webViewState: WebViewState = rememberWebViewState(url)
    val authority = remember(webViewState.content.getCurrentUrl()) {
        webViewState.content.getCurrentUrl()?.let { URL(it).authority } ?: ""
    }

    // update step state when manifest changes
    val viewModel: PartnerAuthViewModel = mavericksViewModel()

    LaunchedEffect(webViewState.content.getCurrentUrl()) {
//        webViewState.content.getCurrentUrl()?.let {
//            if (it.contains(other = "connections-auth.stripe.com", ignoreCase = true)) {
//                url = authSession.value.hostedAuthUrl
//            }
//        }
       viewModel.onUrlChanged(webViewState.content.getCurrentUrl())
    }

    val webViewClient = remember {
        object : AccompanistWebViewClient() {
        }
    }

    Column {
        Text(
            webViewState.content.getCurrentUrl() ?: "",
            style = FinancialConnectionsTheme.typography.heading
        )
        WebView(
            state = webViewState,
            captureBackPresses = false,
            client = webViewClient,
            onCreated = {
                it.settings.javaScriptEnabled = true
                it.settings.javaScriptCanOpenWindowsAutomatically = true
                it.settings.domStorageEnabled = true
                it.settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK;

            }
        )
    }
}
