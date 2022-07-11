package com.stripe.android.financialconnections.analytics

import com.stripe.android.core.Logger
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityResult
import com.stripe.android.financialconnections.navigation.NavigationCommand
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

internal class DefaultFinancialConnectionsEventReporter @Inject constructor(
    private val analyticsRequestExecutor: AnalyticsRequestExecutor,
    private val analyticsRequestFactory: AnalyticsRequestFactory,
    private val logger: Logger,
    private val configuration: FinancialConnectionsSheet.Configuration,
    @IOContext private val workContext: CoroutineContext
) : FinancialConnectionsEventReporter {

    override fun onPresented(configuration: FinancialConnectionsSheet.Configuration) {
        fireEvent(
            FinancialConnectionsAnalyticsEvent(
                FinancialConnectionsAnalyticsEvent.Code.SheetPresented,
                mapOf(PARAM_CLIENT_SECRET to configuration.financialConnectionsSessionClientSecret)
            )
        )
    }

    override fun onPaneLaunched(
        current: NavigationCommand,
        next: NavigationCommand
    ) {
        logger.debug("Navigating from ${current.destination} to ${next.destination}")
        fireEvent(
            FinancialConnectionsAnalyticsEvent(
                FinancialConnectionsAnalyticsEvent.Code.PaneLaunched,
                mapOf(PARAM_CLIENT_SECRET to configuration.financialConnectionsSessionClientSecret)
            )
        )
    }

    override fun onPaneLoaded(
        pane: NavigationCommand,
    ) {
        fireEvent(
            FinancialConnectionsAnalyticsEvent(
                FinancialConnectionsAnalyticsEvent.Code.PaneLoaded,
                mapOf(PARAM_CLIENT_SECRET to configuration.financialConnectionsSessionClientSecret)
            )
        )
    }

    override fun onResult(
        configuration: FinancialConnectionsSheet.Configuration,
        financialConnectionsSheetResult: FinancialConnectionsSheetActivityResult
    ) {
        val event = when (financialConnectionsSheetResult) {
            is FinancialConnectionsSheetActivityResult.Completed ->
                FinancialConnectionsAnalyticsEvent(
                    FinancialConnectionsAnalyticsEvent.Code.SheetClosed,
                    mapOf(
                        PARAM_CLIENT_SECRET to configuration.financialConnectionsSessionClientSecret,
                        PARAM_SESSION_RESULT to "completed"
                    )
                )
            is FinancialConnectionsSheetActivityResult.Canceled ->
                FinancialConnectionsAnalyticsEvent(
                    FinancialConnectionsAnalyticsEvent.Code.SheetClosed,
                    mapOf(
                        PARAM_CLIENT_SECRET to configuration.financialConnectionsSessionClientSecret,
                        PARAM_SESSION_RESULT to "cancelled"
                    )
                )
            is FinancialConnectionsSheetActivityResult.Failed ->
                FinancialConnectionsAnalyticsEvent(
                    FinancialConnectionsAnalyticsEvent.Code.SheetFailed,
                    mapOf(
                        PARAM_CLIENT_SECRET to configuration.financialConnectionsSessionClientSecret,
                        PARAM_SESSION_RESULT to "failure"
                    )
                )
        }
        fireEvent(event)
    }

    override fun onClickSecurityStripe(
        pane: NavigationCommand,
    ) {
        fireEvent(
            FinancialConnectionsAnalyticsEvent(
                FinancialConnectionsAnalyticsEvent.Code.ClickSecurityStripe,
                mapOf(
                    PARAM_CLIENT_SECRET to configuration.financialConnectionsSessionClientSecret,
                )
            )
        )
    }

    override fun onClickDataRequested(
        pane: NavigationCommand,
    ) {
        fireEvent(
            FinancialConnectionsAnalyticsEvent(
                FinancialConnectionsAnalyticsEvent.Code.ClickDataRequested,
                mapOf(
                    PARAM_CLIENT_SECRET to configuration.financialConnectionsSessionClientSecret,
                )
            )
        )
    }

    override fun onClickLegalLearnMore(
        pane: NavigationCommand,
    ) {
        fireEvent(
            FinancialConnectionsAnalyticsEvent(
                FinancialConnectionsAnalyticsEvent.Code.ClickLegalLearnMore,
                mapOf(
                    PARAM_CLIENT_SECRET to configuration.financialConnectionsSessionClientSecret,
                )
            )
        )
    }

    override fun onClickDataAccessLearnMore(
        pane: NavigationCommand,
    ) {
        fireEvent(
            FinancialConnectionsAnalyticsEvent(
                FinancialConnectionsAnalyticsEvent.Code.ClickDataAccessLearnMore,
                mapOf(
                    PARAM_CLIENT_SECRET to configuration.financialConnectionsSessionClientSecret,
                )
            )
        )
    }

    override fun onClickLegalPrivacyPolicy(
        pane: NavigationCommand,
    ) {
        fireEvent(
            FinancialConnectionsAnalyticsEvent(
                FinancialConnectionsAnalyticsEvent.Code.ClickLegalPrivacyPolicy,
                mapOf(
                    PARAM_CLIENT_SECRET to configuration.financialConnectionsSessionClientSecret,
                )
            )
        )
    }

    override fun onClickLegalTerms(
        pane: NavigationCommand,
    ) {
        fireEvent(
            FinancialConnectionsAnalyticsEvent(
                FinancialConnectionsAnalyticsEvent.Code.ClickLegalTerms,
                mapOf(
                    PARAM_CLIENT_SECRET to configuration.financialConnectionsSessionClientSecret,
                )
            )
        )
    }

    override fun onClickAgree(
        pane: NavigationCommand
    ) {
        fireEvent(
            FinancialConnectionsAnalyticsEvent(
                FinancialConnectionsAnalyticsEvent.Code.ClickDisconnectLink,
                mapOf(
                    PARAM_CLIENT_SECRET to configuration.financialConnectionsSessionClientSecret,
                )
            )
        )
    }

    override fun onClickDisconnect(
        pane: NavigationCommand
    ) {
        fireEvent(
            FinancialConnectionsAnalyticsEvent(
                FinancialConnectionsAnalyticsEvent.Code.ClickDisconnectLink,
                mapOf(
                    PARAM_CLIENT_SECRET to configuration.financialConnectionsSessionClientSecret,
                )
            )
        )
    }

    private fun fireEvent(event: FinancialConnectionsAnalyticsEvent) {
        CoroutineScope(workContext).launch {
            analyticsRequestExecutor.executeAsync(
                analyticsRequestFactory.createRequest(
                    event = event,
                    additionalParams = event.additionalParams
                )
            )
        }
    }

    internal companion object {
        const val PARAM_CLIENT_SECRET = "las_client_secret"
        const val PARAM_SESSION_RESULT = "session_result"
    }
}
