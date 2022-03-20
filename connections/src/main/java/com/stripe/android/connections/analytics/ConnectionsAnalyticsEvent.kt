package com.stripe.android.connections.analytics

internal data class ConnectionsAnalyticsEvent(
    val eventCode: Code,
    val additionalParams: Map<String, String> = emptyMap()
) {
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
