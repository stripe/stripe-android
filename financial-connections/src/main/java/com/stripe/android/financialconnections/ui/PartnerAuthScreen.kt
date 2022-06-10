package com.stripe.android.financialconnections.ui

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksActivityViewModel
import com.stripe.android.financialconnections.presentation.CreateBrowserIntentForUrl
import com.stripe.android.financialconnections.presentation.FinancialConnectionsSheetNativeViewModel

@Composable
fun PartnerAuthScreen() {
    val activityViewModel: FinancialConnectionsSheetNativeViewModel = mavericksActivityViewModel()
    val url = activityViewModel.collectAsState(mapper = { it.authorizationSession?.url })
    val context = LocalContext.current
    OpenBrowserLauncherEffect(url.value, context)
}

@Composable
private fun OpenBrowserLauncherEffect(
    url: String?,
    context: Context
) {
    LaunchedEffect(url) {
        if (url != null) {
            val intent = CreateBrowserIntentForUrl(context, Uri.parse(url))
            context.startActivity(intent)
        }
    }
}
