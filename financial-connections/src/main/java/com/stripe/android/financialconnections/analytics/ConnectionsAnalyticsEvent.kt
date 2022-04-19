package com.stripe.android.financialconnections.analytics

import com.stripe.android.core.networking.AnalyticsEvent

internal data class ConnectionsAnalyticsEvent(
    val eventCode: Code,
    val additionalParams: Map<String, String> = emptyMap()
) : AnalyticsEvent {

    override val eventName: String = eventCode.toString()

    enum class Code(internal val code: String) {

        // Connections Sheet Events
        SheetPresented("sheet.presented"),
        SheetClosed("sheet.closed"),
        SheetFailed("sheet.failed");

        override fun toString(): String {
            return "$PREFIX.$code"
        }

        private companion object {
            private const val PREFIX = "stripe_android.connections"
        }
    }
}
