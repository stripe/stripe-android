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
        knownDeeplinkActions: Map<String, suspend (DeeplinkPayload) -> Unit>
    ) {
        uriUtils.getQueryParameter(uri, ClickableLinkParameters.EVENT_NAME)?.let { eventName ->
            eventTracker.track(Click(eventName, pane = currentPane))
        }
        when {
            URLUtil.isNetworkUrl(uri) -> onNetworkUrlClicked(uri)
            else -> {
                val clickedEntry = knownDeeplinkActions.entries.firstOrNull { (key, _) ->
                    uriUtils.compareSchemeAuthorityAndPath(key, uri)
                }
                clickedEntry?.value?.invoke(buildPayload(uri)) ?: logger.error("Unrecognized clickable text: $uri")
            }
        }
    }

    private fun buildPayload(uri: String): DeeplinkPayload = DeeplinkPayload(
        nextPaneOrDrawerOnSecondaryCta = uriUtils
            .getQueryParameter(uri, ClickableLinkParameters.NEXT_PANE_OR_DRAWER_ON_SECONDARY_CTA)
            ?.let { internalLinkToPaneId[it] }
    )

    private val internalLinkToPaneId = mapOf(
        InternalLink.MANUAL_ENTRY to Pane.MANUAL_ENTRY.value,
    )

    data class DeeplinkPayload(
        val nextPaneOrDrawerOnSecondaryCta: String?,
    )

    object ClickableLinkParameters {
        const val EVENT_NAME = "eventName"
        const val NEXT_PANE_OR_DRAWER_ON_SECONDARY_CTA = "nextPaneOrDrawerOnSecondaryCta"
    }

    object InternalLink {
        const val MANUAL_ENTRY = "manual-entry"
    }
}
