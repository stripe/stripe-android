package com.stripe.android.financialconnections.domain

import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.analytics.logError
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator.Message.CloseWithError
import com.stripe.android.financialconnections.features.error.isAttestationError
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.navigation.Destination
import com.stripe.android.financialconnections.navigation.NavigationManager
import com.stripe.android.financialconnections.repository.FinancialConnectionsErrorRepository
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

internal interface HandleError {
    operator fun invoke(
        extraMessage: String,
        error: Throwable,
        pane: FinancialConnectionsSessionManifest.Pane,
        displayErrorScreen: Boolean
    )
}

internal class RealHandleError @Inject constructor(
    private val errorRepository: FinancialConnectionsErrorRepository,
    private val analyticsTracker: FinancialConnectionsAnalyticsTracker,
    private val nativeAuthFlowCoordinator: NativeAuthFlowCoordinator,
    private val logger: Logger,
    private val navigationManager: NavigationManager
) : HandleError {

    /**
     * Handle an error by logging it and navigating to the error screen if necessary.
     *
     *  - logs error to analytics.
     *  - logs error locally.
     *  - logs error to live events listener if needed.
     *
     * @param extraMessage a message to include in the analytics event
     * @param error the error to handle
     * @param pane the pane where the error occurred
     * @param displayErrorScreen whether to navigate to the error screen
     *
     */
    override operator fun invoke(
        extraMessage: String,
        error: Throwable,
        pane: FinancialConnectionsSessionManifest.Pane,
        displayErrorScreen: Boolean,
    ) {
        analyticsTracker.logError(
            extraMessage = extraMessage,
            error = error,
            logger = logger,
            pane = pane
        )

        if (error.isAttestationError) {
            /*
            An attestation error (verification token generation error, unsatisfactory attestation verdict, etc)
            Happened mid flow -> Close the native flow with the error (right after we'll open a web browser to finish
            the flow)
             */
            GlobalScope.launch { nativeAuthFlowCoordinator().emit(CloseWithError(cause = error)) }
        } else if (displayErrorScreen) {
            // Navigate to error screen
            errorRepository.set(error)
            navigationManager.tryNavigateTo(route = Destination.Error(referrer = pane))
        }
    }
}
