package com.stripe.android.financialconnections.features.networkinglinksignup

import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.analytics.logError
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane.LINK_ACCOUNT_PICKER
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane.LINK_LOGIN
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane.NETWORKING_LINK_SIGNUP_PANE
import com.stripe.android.financialconnections.navigation.Destination.NetworkingLinkVerification
import com.stripe.android.financialconnections.navigation.Destination.NetworkingSaveToLinkVerification
import com.stripe.android.financialconnections.navigation.Destination.Success
import com.stripe.android.financialconnections.navigation.NavigationManager
import javax.inject.Inject
import javax.inject.Provider

internal class LinkSignupHandlerFactory @Inject constructor(
    private val performLinkSignupForNetworking: Provider<PerformLinkSignupForNetworking>,
    private val performLinkSignupForInstantDebits: Provider<PerformLinkSignupForInstantDebits>,
    private val navigationManager: NavigationManager,
    private val eventTracker: FinancialConnectionsAnalyticsTracker,
    private val logger: Logger,
) {

    fun create(isInstantDebits: Boolean): LinkSignupHandler {
        return if (isInstantDebits) {
            LinkSignupHandlerForInstantDebits(
                performLinkSignup = performLinkSignupForInstantDebits.get(),
                navigationManager = navigationManager,
                eventTracker = eventTracker,
                logger = logger,
            )
        } else {
            LinkSignupHandlerForNetworking(
                performLinkSignup = performLinkSignupForNetworking.get(),
                navigationManager = navigationManager,
                eventTracker = eventTracker,
                logger = logger,
            )
        }
    }
}

internal interface LinkSignupHandler {

    suspend fun performSignup(
        state: NetworkingLinkSignupState,
    ): Pane

    fun handleSignupFailure(
        state: NetworkingLinkSignupState,
        error: Throwable,
    ): NetworkingLinkSignupState

    fun navigateToVerification()
}

internal class LinkSignupHandlerForInstantDebits @Inject constructor(
    private val performLinkSignup: PerformLinkSignup,
    private val navigationManager: NavigationManager,
    private val eventTracker: FinancialConnectionsAnalyticsTracker,
    private val logger: Logger,
) : LinkSignupHandler {

    override suspend fun performSignup(
        state: NetworkingLinkSignupState,
    ): Pane {
        performLinkSignup(state)
        return LINK_ACCOUNT_PICKER
    }

    override fun navigateToVerification() {
        navigationManager.tryNavigateTo(NetworkingLinkVerification(referrer = LINK_LOGIN))
    }

    override fun handleSignupFailure(
        state: NetworkingLinkSignupState,
        error: Throwable,
    ): NetworkingLinkSignupState {
        eventTracker.logError(
            extraMessage = "Error creating a Link account",
            error = error,
            logger = logger,
            pane = LINK_LOGIN,
        )

        // TODO(tillh-stripe) Display error inline

        return state
    }
}

internal class LinkSignupHandlerForNetworking @Inject constructor(
    private val performLinkSignup: PerformLinkSignup,
    private val eventTracker: FinancialConnectionsAnalyticsTracker,
    private val navigationManager: NavigationManager,
    private val logger: Logger,
) : LinkSignupHandler {

    override suspend fun performSignup(
        state: NetworkingLinkSignupState,
    ): Pane {
        performLinkSignup(state)
        return Pane.SUCCESS
    }

    override fun navigateToVerification() {
        navigationManager.tryNavigateTo(NetworkingSaveToLinkVerification(referrer = NETWORKING_LINK_SIGNUP_PANE))
    }

    override fun handleSignupFailure(
        state: NetworkingLinkSignupState,
        error: Throwable,
    ): NetworkingLinkSignupState {
        eventTracker.logError(
            extraMessage = "Error saving account to Link",
            error = error,
            logger = logger,
            pane = NETWORKING_LINK_SIGNUP_PANE,
        )

        navigationManager.tryNavigateTo(Success(referrer = NETWORKING_LINK_SIGNUP_PANE))
        return state
    }
}
