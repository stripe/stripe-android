package com.stripe.android.financialconnections.analytics

import android.content.Context
import com.stripe.android.core.Logger
import com.stripe.android.core.networking.AnalyticsRequestV2Factory
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.Error
import com.stripe.android.financialconnections.domain.GetManifest
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import java.util.Locale

/**
 * Event tracker for Financial Connections.
 */
internal interface FinancialConnectionsAnalyticsTracker {

    suspend fun track(event: FinancialConnectionsAnalyticsEvent): Result<Unit>
}

internal suspend fun FinancialConnectionsAnalyticsTracker.logError(
    extraMessage: String,
    error: Throwable,
    logger: Logger,
    pane: Pane
) {
    // log error to analytics.
    track(
        Error(
            extraMessage = extraMessage,
            pane = pane,
            exception = error
        )
    )
    // log error locally.
    logger.error(extraMessage, error)
}

internal class FinancialConnectionsAnalyticsTrackerImpl(
    private val stripeNetworkClient: StripeNetworkClient,
    private val getManifest: GetManifest,
    private val configuration: FinancialConnectionsSheet.Configuration,
    private val logger: Logger,
    private val locale: Locale,
    context: Context,
) : FinancialConnectionsAnalyticsTracker {

    private val requestFactory = AnalyticsRequestV2Factory(
        context = context,
        clientId = CLIENT_ID,
        origin = ORIGIN
    )

    override suspend fun track(event: FinancialConnectionsAnalyticsEvent): Result<Unit> {
        return runCatching {
            val eventParams: Map<out String, Any?> = event.params ?: emptyMap()
            val commonParams = commonParams()
            val request = requestFactory.createRequest(
                eventName = event.eventName,
                additionalParams = eventParams + commonParams,
                includeSDKParams = true
            )
            stripeNetworkClient.executeRequest(
                request
            )
            logger.debug("EVENT: ${request.eventName}: ${request.params}")
        }.onFailure {
            logger.error("Exception while making analytics request", it)
        }
    }

    private suspend fun commonParams(): Map<String, String?> {
        val manifest = getManifest()
        return mapOf(
            "las_client_secret" to configuration.financialConnectionsSessionClientSecret,
//            "las_creator_client_secret": this.linkAccountSessionCreatorClientSecret,
//        "las_creator_type": this.linkAccountSessionCreatorType,
//        "las_creator_id": this.linkAccountSessionCreatorId,
            "key" to configuration.publishableKey,
            "stripe_account" to configuration.stripeAccountId,
            "navigator_language" to locale.toLanguageTag(),
            "is_webview" to false.toString(),
            "livemode" to manifest.livemode.toString(),
            "product" to manifest.product.value,
            "is_stripe_direct" to manifest.isStripeDirect.toString(),
            "single_account" to manifest.singleAccount.toString(),
            "allow_manual_entry" to manifest.allowManualEntry.toString(),
            "account_holder_id" to manifest.accountholderToken,
        )
    }

    internal companion object {
        const val CLIENT_ID = "mobile-clients-linked-accounts"
        const val ORIGIN = "stripe-linked-accounts-android"
    }
}
