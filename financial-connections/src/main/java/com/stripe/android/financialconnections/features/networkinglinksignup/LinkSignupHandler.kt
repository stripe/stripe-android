package com.stripe.android.financialconnections.features.networkinglinksignup

import com.stripe.android.core.Logger
import com.stripe.android.core.exception.StripeException
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.Click
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.analytics.logError
import com.stripe.android.financialconnections.domain.AttachConsumerToLinkAccountSession
import com.stripe.android.financialconnections.domain.GetCachedAccounts
import com.stripe.android.financialconnections.domain.GetOrFetchSync
import com.stripe.android.financialconnections.domain.GetOrFetchSync.RefetchCondition.Always
import com.stripe.android.financialconnections.domain.HandleError
import com.stripe.android.financialconnections.domain.SaveAccountToLink
import com.stripe.android.financialconnections.features.common.isDataFlow
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane.LINK_LOGIN
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane.NETWORKING_LINK_SIGNUP_PANE
import com.stripe.android.financialconnections.navigation.Destination.NetworkingLinkVerification
import com.stripe.android.financialconnections.navigation.Destination.NetworkingSaveToLinkVerification
import com.stripe.android.financialconnections.navigation.Destination.Success
import com.stripe.android.financialconnections.navigation.NavigationManager
import com.stripe.android.financialconnections.repository.FinancialConnectionsConsumerSessionRepository
import com.stripe.android.financialconnections.ui.TextResource
import javax.inject.Inject

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
    private val consumerRepository: FinancialConnectionsConsumerSessionRepository,
    private val attachConsumerToLinkAccountSession: AttachConsumerToLinkAccountSession,
    private val getOrFetchSync: GetOrFetchSync,
    private val navigationManager: NavigationManager,
    private val handleError: HandleError,
) : LinkSignupHandler {

    override suspend fun performSignup(
        state: NetworkingLinkSignupState,
    ): Pane {
        val phoneController = state.payload()!!.phoneController

        val signup = consumerRepository.signUp(
            email = state.validEmail!!,
            phoneNumber = state.validPhone!!,
            country = phoneController.getCountryCode(),
        )

        attachConsumerToLinkAccountSession(
            consumerSessionClientSecret = signup.consumerSession.clientSecret,
        )

        val manifest = getOrFetchSync(refetchCondition = Always).manifest
        return manifest.nextPane
    }

    override fun navigateToVerification() {
        navigationManager.tryNavigateTo(NetworkingLinkVerification(referrer = LINK_LOGIN))
    }

    override fun handleSignupFailure(
        state: NetworkingLinkSignupState,
        error: Throwable,
    ): NetworkingLinkSignupState {
        val phoneError = error.extractPhoneNumberValidationError()

        handleError(
            extraMessage = "Error creating a Link account",
            error = error,
            pane = LINK_LOGIN,
            displayErrorScreen = phoneError == null,
        )

        return state.copy(phoneError = phoneError)
    }

    private fun Throwable.extractPhoneNumberValidationError(): TextResource? {
        val error = (this as? StripeException)?.stripeError?.message
        val isPhoneError = error?.contains("phone") == true
        return if (isPhoneError) {
            TextResource.StringId(R.string.stripe_networking_signup_invalid_phone_number)
        } else {
            null
        }
    }
}

internal class LinkSignupHandlerForNetworking @Inject constructor(
    private val getOrFetchSync: GetOrFetchSync,
    private val getCachedAccounts: GetCachedAccounts,
    private val saveAccountToLink: SaveAccountToLink,
    private val eventTracker: FinancialConnectionsAnalyticsTracker,
    private val navigationManager: NavigationManager,
    private val logger: Logger,
) : LinkSignupHandler {

    override suspend fun performSignup(
        state: NetworkingLinkSignupState,
    ): Pane {
        eventTracker.track(Click(eventName = "click.save_to_link", pane = NETWORKING_LINK_SIGNUP_PANE))
        val selectedAccounts = getCachedAccounts()
        val manifest = getOrFetchSync().manifest
        val phoneController = state.payload()!!.phoneController
        require(state.valid) { "Form invalid! ${state.validEmail} ${state.validPhone}" }
        saveAccountToLink.new(
            country = phoneController.getCountryCode(),
            email = state.validEmail!!,
            phoneNumber = state.validPhone!!,
            selectedAccounts = selectedAccounts,
            shouldPollAccountNumbers = manifest.isDataFlow,
        )

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
