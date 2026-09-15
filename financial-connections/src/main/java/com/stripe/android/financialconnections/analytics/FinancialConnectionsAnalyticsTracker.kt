package com.stripe.android.financialconnections.analytics

import com.stripe.android.core.Logger
import com.stripe.android.core.exception.StripeException
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.ErrorCode
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.Metadata
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.Name
import com.stripe.android.financialconnections.analytics.FinancialConnectionsResponseEventEmitter.Companion.EVENTS_TO_EMIT
import com.stripe.android.financialconnections.domain.GetOrFetchSync
import com.stripe.android.financialconnections.exception.AppInitializationError
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * Event tracker for Financial Connections.
 */
internal interface FinancialConnectionsAnalyticsTracker {
    fun track(event: FinancialConnectionsAnalyticsEvent)
    fun emitEvent(name: Name, metadata: Metadata)
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
private fun FinancialConnectionsAnalyticsTracker.emitPublicClientErrorEventIfNeeded(error: Throwable) {
    val isStripeErrorWithEvents = (error as? StripeException)
        ?.stripeError?.extraFields
        ?.get(EVENTS_TO_EMIT)
        ?.isNotEmpty() == true

    // only emit events for client errors.
    if (isStripeErrorWithEvents.not()) {
        when (error) {
            // client-specific error: flow was launched without a browser installed.
            is AppInitializationError -> emitEvent(
                name = FinancialConnectionsEvent.Name.ERROR,
                metadata = FinancialConnectionsEvent.Metadata(
                    errorCode = ErrorCode.WEB_BROWSER_UNAVAILABLE
                )
            )

            // any non-backend error should be emitted as an unexpected error.
            else -> emitEvent(
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
    private val analyticsSender: FinancialConnectionsAnalyticsEventSender,
    private val eventEmitter: FinancialConnectionsEventEmitter,
) : FinancialConnectionsAnalyticsTracker {

    @OptIn(DelicateCoroutinesApi::class)
    override fun track(event: FinancialConnectionsAnalyticsEvent) {
        GlobalScope.launch(Dispatchers.IO) {
            analyticsSender.send(event, getOrFetchSync().manifest)
        }
    }

    override fun emitEvent(name: Name, metadata: Metadata) {
        eventEmitter.emit(name, metadata)
    }

    internal companion object {
        const val CLIENT_ID = "mobile-clients-linked-accounts"
        const val ORIGIN = "stripe-linked-accounts-android"
    }
}
