package com.stripe.android.financialconnections.analytics

import android.content.Context
import com.stripe.android.core.Logger
import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.networking.AnalyticsRequestV2Executor
import com.stripe.android.core.networking.AnalyticsRequestV2Factory
import com.stripe.android.financialconnections.FinancialConnections
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.ErrorCode
import com.stripe.android.financialconnections.analytics.FinancialConnectionsResponseEventEmitter.Companion.EVENTS_TO_EMIT
import com.stripe.android.financialconnections.domain.GetOrFetchSync
import com.stripe.android.financialconnections.exception.AppInitializationError
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Event tracker for Financial Connections.
 */
internal interface FinancialConnectionsAnalyticsTracker {
    fun track(event: FinancialConnectionsAnalyticsEvent)
}

internal fun FinancialConnectionsAnalyticsTracker.logError(
    extraMessage: String,
    error: Throwable,
    logger: Logger,
    pane: Pane
) {
    // log error to analytics.
    track(
        FinancialConnectionsAnalyticsEvent.Error(
            extraMessage = extraMessage,
            pane = pane,
            exception = error
        )
    )
    // log error locally.
    logger.error(extraMessage, error)

    // log error to live events listener if needed.
    emitPublicClientErrorEventIfNeeded(error)
}

/**
 * Emits client error events to the live events listener. Backend errors should be emitted
 * by the response handler.
 *
 * @see [com.stripe.android.financialconnections.analytics.FinancialConnectionsResponseEventEmitter]
 */
private fun emitPublicClientErrorEventIfNeeded(error: Throwable) {
    val isStripeErrorWithEvents = (error as? StripeException)
        ?.stripeError?.extraFields
        ?.get(EVENTS_TO_EMIT)
        ?.isNotEmpty() == true

    // only emit events for client errors.
    if (isStripeErrorWithEvents.not()) {
        when (error) {
            // client-specific error: flow was launched without a browser installed.
            is AppInitializationError -> FinancialConnections.emitEvent(
                name = FinancialConnectionsEvent.Name.ERROR,
                metadata = FinancialConnectionsEvent.Metadata(
                    errorCode = ErrorCode.WEB_BROWSER_UNAVAILABLE
                )
            )

            // any non-backend error should be emitted as an unexpected error.
            else -> FinancialConnections.emitEvent(
                name = FinancialConnectionsEvent.Name.ERROR,
                metadata = FinancialConnectionsEvent.Metadata(
                    errorCode = ErrorCode.UNEXPECTED_ERROR
                )
            )
        }
    }
}

internal class FinancialConnectionsAnalyticsTrackerImpl(
    private val getOrFetchSync: GetOrFetchSync,
    private val configuration: FinancialConnectionsSheet.Configuration,
    private val locale: Locale,
    context: Context,
    private val requestExecutor: AnalyticsRequestV2Executor,
) : FinancialConnectionsAnalyticsTracker {

    private val requestFactory = AnalyticsRequestV2Factory(
        context = context,
        clientId = CLIENT_ID,
        origin = ORIGIN
    )

    @OptIn(DelicateCoroutinesApi::class)
    override fun track(event: FinancialConnectionsAnalyticsEvent) {
//        GlobalScope.launch(Dispatchers.IO) {
//            val request = requestFactory.createRequest(
//                eventName = event.eventName,
//                additionalParams = event.params.orEmpty() + commonParams(),
//                includeSDKParams = true,
//            )
//            requestExecutor.enqueue(request)
//        }
    }

    private suspend fun commonParams(): Map<String, String?> {
        val manifest = getOrFetchSync().manifest
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
