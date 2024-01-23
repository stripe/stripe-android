package com.stripe.android.financialconnections.ui

import android.webkit.URLUtil
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.Click
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.utils.UriUtils
import javax.inject.Inject

internal class HandleClickableUrl @Inject constructor(
    private val uriUtils: UriUtils,
    private val eventTracker: FinancialConnectionsAnalyticsTracker,
    private val logger: Logger,
) {

    suspend operator fun invoke(
        currentPane: Pane,
        uri: String,
        onNetworkUrlClicked: (String) -> Unit,
        knownDeeplinkActions: Map<String, suspend () -> Unit>
    ) {
        uriUtils.getQueryParameter(uri, "eventName")?.let { eventName ->
            eventTracker.track(Click(eventName, pane = currentPane))
        }
        when {
            URLUtil.isNetworkUrl(uri) -> onNetworkUrlClicked(uri)
            else -> {
                val clickedEntry = knownDeeplinkActions.entries.firstOrNull { (key, _) ->
                    uriUtils.compareSchemeAuthorityAndPath(key, uri)
                }
                clickedEntry?.value?.invoke() ?: logger.error("Unrecognized clickable text: $uri")
            }
        }
    }
}
