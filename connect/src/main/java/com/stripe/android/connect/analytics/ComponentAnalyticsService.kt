package com.stripe.android.connect.analytics

import com.stripe.android.connect.StripeEmbeddedComponent
import java.util.UUID

/**
 * Service for logging [ConnectAnalyticsEvent] for the Connect SDK. Also keeps track
 * of shared parameters to pass alongside events.
 * There should be one instance of ComponentAnalyticsService per instantiation of [StripeEmbeddedComponent].
 */
internal class ComponentAnalyticsService(
    private val analyticsService: ConnectAnalyticsService,
    private val component: StripeEmbeddedComponent,
    private val publishableKey: String?, // can be null in cases where it should not be logged
) {
    internal var merchantId: String? = null
    private var componentUUID = UUID.randomUUID()

    /**
     * Log an analytic [event].
     */
    fun track(event: ConnectAnalyticsEvent) {
        val params = buildMap {
            // add common params
            put("merchantId", merchantId)
            put("component", component.componentName)
            put("componentInstance", componentUUID.toString())
            put("publishableKey", publishableKey)

            // event-specific params should be included in both the top-level and event_metadata
            // blob so that we can use them in prometheus alerts (which are only available for
            // top-level events).
            if (event.params.isNotEmpty()) {
                putAll(event.params)
                put("event_metadata", event.params)
            }
        }

        analyticsService.track(event.eventName, params)
    }
}