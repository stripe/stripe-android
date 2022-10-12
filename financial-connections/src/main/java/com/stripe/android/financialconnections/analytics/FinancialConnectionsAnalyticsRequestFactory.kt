package com.stripe.android.financialconnections.analytics

import android.content.Context
import com.stripe.android.core.Logger
import com.stripe.android.core.networking.AnalyticsRequestV2Factory
import com.stripe.android.core.networking.StripeNetworkClient
import javax.inject.Inject

/**
 * Event tracker for Financial Connections.
 */
internal class FinancialConnectionsAnalyticsTracker @Inject constructor(
    private val stripeNetworkClient: StripeNetworkClient,
    private val logger: Logger,
    context: Context,
) {
    private val requestFactory = AnalyticsRequestV2Factory(
        context = context,
        clientId = CLIENT_ID,
        origin = ORIGIN
    )

    suspend fun track(event: FinancialConnectionsEvent): Result<Unit> {
        return runCatching {

            logger.debug("tracked event: $event")
            val eventRequest = requestFactory.createRequest(
                event.name,
                event.params ?: emptyMap(),
                includeSDKParams = true
            )
            stripeNetworkClient.executeRequest(eventRequest)
            Unit
        }.onFailure {
            logger.error("Exception while making analytics request", it)
        }
    }

    //TODO@carlosmuvi temporary configuration 
    internal companion object {
        const val CLIENT_ID = "mobile-financialconnections-sdk"
        const val ORIGIN = "stripe-financialconnections-android"
        const val ID = "id"
    }
}
