package com.stripe.android.financialconnections.ui

import android.app.Activity
import android.net.Uri
import androidx.compose.ui.platform.UriHandler
import com.stripe.android.financialconnections.browser.CustomTabsManager
import com.stripe.android.financialconnections.presentation.CreateBrowserIntentForUrl

/**
 * Alternative [UriHandler] that opens uris in a Custom tab when available
 * using [CreateBrowserIntentForUrl].
 */
internal class CustomTabUriHandler(
    private val activity: Activity,
    private val customTabsManager: CustomTabsManager
) : UriHandler {
    override fun openUri(uri: String) {
        customTabsManager.openCustomTab(
            activity = activity,
            uri = Uri.parse(uri)
        )
    }
}
