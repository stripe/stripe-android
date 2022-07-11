package com.stripe.android.financialconnections.analytics

import com.stripe.android.core.networking.AnalyticsEvent

internal data class FinancialConnectionsAnalyticsEvent(
    val eventCode: Code,
    val additionalParams: Map<String, String> = emptyMap()
) : AnalyticsEvent {

    override val eventName: String = eventCode.toString()

    enum class Code(internal val code: String) {
        // General
        PaneLaunched("pane.launched"),
        PaneLoaded("pane.loaded"),
        SheetPresented("sheet.presented"),
        SheetClosed("sheet.closed"),
        SheetFailed("sheet.failed"),
        // Consent
        ClickManualEntry("click.manualentry"),
        ClickLegalLearnMore("click.legal.learn_more"),
        ClickLegalPrivacyPolicy("click.legal.privacy_policy"),
        ClickLegalTerms("click.legal.terms"),
        ClickDataRequested("click.data_requested"),
        ClickDataAccessLearnMore("click.data_access.learn_more"),
        ClickSecurityStripe("click.security_stripe"),
        ClickDisconnectLink("click.disconnect_link");

        override fun toString(): String {
            return "$PREFIX.$code"
        }

        private companion object {
            private const val PREFIX = "stripe_android.connections"
        }
    }
}
