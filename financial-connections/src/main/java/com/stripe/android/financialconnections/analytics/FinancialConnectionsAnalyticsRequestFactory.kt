package com.stripe.android.financialconnections.analytics

import android.content.Context
import com.stripe.android.core.Logger
import com.stripe.android.core.networking.AnalyticsRequestV2Factory
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.domain.GetManifest
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

/**
 * Event tracker for Financial Connections.
 */
internal class FinancialConnectionsAnalyticsTracker @Inject constructor(
    private val stripeNetworkClient: StripeNetworkClient,
    private val getManifest: GetManifest,
    private val configuration: FinancialConnectionsSheet.Configuration,
    private val logger: Logger,
    private val locale: Locale,
    context: Context,
) {

    // Assumes [FinancialConnectionsAnalyticsTracker] is a singleton.
    private val loggerId = UUID.randomUUID().toString()

    private val requestFactory = AnalyticsRequestV2Factory(
        context = context,
        clientId = CLIENT_ID,
        origin = ORIGIN
    )

    suspend fun track(event: FinancialConnectionsEvent): Result<Unit> {
        return runCatching {

            val eventParams: Map<out String, Any?> = event.params ?: emptyMap()
            val commonParams = commonParams()
            logger.debug(
                "tracked event: ${event.name}" +
                    "\nparams: $eventParams" +
                    "\ncommonParams: $commonParams"
            )
            val eventRequest = requestFactory.createRequest(
                eventName = event.name,
                additionalParams = eventParams + commonParams,
                includeSDKParams = true
            )
            //stripeNetworkClient.executeRequest(eventRequest)
        }.onFailure {
            logger.error("Exception while making analytics request", it)
        }
    }

    private suspend fun commonParams(): Map<String, Any?> {
        val manifest = getManifest()
        return mapOf(
            "las_client_secret" to configuration.financialConnectionsSessionClientSecret,
//            "las_creator_client_secret": this.linkAccountSessionCreatorClientSecret,
//        "las_creator_type": this.linkAccountSessionCreatorType,
//        "las_creator_id": this.linkAccountSessionCreatorId,
            "key" to configuration.publishableKey,
            "stripe_account" to configuration.stripeAccountId,
            "logger_id" to loggerId,
            "navigator_language" to locale.language,
            "is_webview" to false,
            "livemode" to manifest.livemode,
            "product" to manifest.product,
            "is_stripe_direct" to manifest.isStripeDirect,
            "single_account" to manifest.singleAccount,
            "allow_manual_entry" to manifest.allowManualEntry,
            "account_holder_id" to manifest.accountHolderToken,
        )
    }

    //TODO@carlosmuvi temporary configuration 
    internal companion object {
        const val CLIENT_ID = "mobile-financialconnections-sdk"
        const val ORIGIN = "stripe-financialconnections-android"
        const val ID = "id"
    }
}
