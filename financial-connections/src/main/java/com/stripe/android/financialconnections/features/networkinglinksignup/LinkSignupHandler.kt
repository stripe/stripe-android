package com.stripe.android.financialconnections.features.networkinglinksignup

import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.FinancialConnectionsSheet.ElementsSessionContext.PrefillDetails
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.AttestationEndpoint.SIGNUP
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.Click
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.analytics.logError
import com.stripe.android.financialconnections.di.APPLICATION_ID
import com.stripe.android.financialconnections.domain.AttachConsumerToLinkAccountSession
import com.stripe.android.financialconnections.domain.GetCachedAccounts
import com.stripe.android.financialconnections.domain.GetOrFetchSync
import com.stripe.android.financialconnections.domain.GetOrFetchSync.RefetchCondition.Always
import com.stripe.android.financialconnections.domain.HandleError
import com.stripe.android.financialconnections.domain.RequestIntegrityToken
import com.stripe.android.financialconnections.domain.SaveAccountToLink
import com.stripe.android.financialconnections.features.common.isDataFlow
import com.stripe.android.financialconnections.features.error.toAttestationErrorIfApplicable
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane.LINK_LOGIN
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane.NETWORKING_LINK_SIGNUP_PANE
import com.stripe.android.financialconnections.navigation.Destination.NetworkingLinkVerification
import com.stripe.android.financialconnections.navigation.Destination.NetworkingSaveToLinkVerification
import com.stripe.android.financialconnections.navigation.Destination.Success
import com.stripe.android.financialconnections.repository.FinancialConnectionsConsumerSessionRepository
import com.stripe.android.uicore.navigation.NavigationManager
import javax.inject.Inject
import javax.inject.Named

internal interface LinkSignupHandler {

    suspend fun performSignup(
        state: NetworkingLinkSignupState
    ): Pane

    fun handleSignupFailure(
        state: NetworkingLinkSignupState,
        error: Throwable
    )

    fun navigateToVerification()
}

internal class LinkSignupHandlerForInstantDebits @Inject constructor(
    private val consumerRepository: FinancialConnectionsConsumerSessionRepository,
    private val attachConsumerToLinkAccountSession: AttachConsumerToLinkAccountSession,
    private val requestIntegrityToken: RequestIntegrityToken,
    private val getOrFetchSync: GetOrFetchSync,
    private val navigationManager: NavigationManager,
    @Named(APPLICATION_ID) private val applicationId: String,
    private val handleError: HandleError,
) : LinkSignupHandler {

    override suspend fun performSignup(
        state: NetworkingLinkSignupState,
    ): Pane {
        val phoneController = state.payload()!!.phoneController

        val manifest = getOrFetchSync().manifest
        val signup = if (manifest.appVerificationEnabled) {
            val token = requestIntegrityToken(
                endpoint = SIGNUP,
                pane = LINK_LOGIN
            )
            consumerRepository.mobileSignUp(
                email = state.validEmail!!,
                phoneNumber = state.validPhone!!,
                country = phoneController.getCountryCode(),
                verificationToken = token,
                appId = applicationId
            )
        } else {
            consumerRepository.signUp(
                email = state.validEmail!!,
                phoneNumber = state.validPhone!!,
                country = phoneController.getCountryCode(),
            )
        }

        attachConsumerToLinkAccountSession(
            consumerSessionClientSecret = signup.consumerSession.clientSecret,
        )

        // Refresh manifest to get the next pane
        return getOrFetchSync(refetchCondition = Always).manifest.nextPane
    }

    override fun navigateToVerification() {
        navigationManager.tryNavigateTo(NetworkingLinkVerification(referrer = LINK_LOGIN))
    }

    override fun handleSignupFailure(
        state: NetworkingLinkSignupState,
        error: Throwable
    ) {
        handleError(
            extraMessage = "Error creating a Link account",
            error = error.toAttestationErrorIfApplicable(
                PrefillDetails(
                    state.validEmail!!,
                    state.validPhone,
                    state.payload()!!.phoneController.getCountryCode()
                )
            ),
            pane = LINK_LOGIN,
            displayErrorScreen = true,
        )
    }
}

internal class LinkSignupHandlerForNetworking @Inject constructor(
    private val consumerRepository: FinancialConnectionsConsumerSessionRepository,
    private val getOrFetchSync: GetOrFetchSync,
    private val getCachedAccounts: GetCachedAccounts,
    private val requestIntegrityToken: RequestIntegrityToken,
    private val saveAccountToLink: SaveAccountToLink,
    private val eventTracker: FinancialConnectionsAnalyticsTracker,
    private val navigationManager: NavigationManager,
    @Named(APPLICATION_ID) private val applicationId: String,
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

        if (manifest.appVerificationEnabled) {
            // ** New signup flow on verified flows: 2 requests **
            // 1. Mobile signup endpoint providing email + phone number
            // 2. Separately call SaveAccountToLink with the newly created account.
            val token = requestIntegrityToken(
                endpoint = SIGNUP,
                pane = NETWORKING_LINK_SIGNUP_PANE
            )
            val signup = consumerRepository.mobileSignUp(
                email = state.validEmail!!,
                phoneNumber = state.validPhone!!,
                country = phoneController.getCountryCode(),
                verificationToken = token,
                appId = applicationId,
            )
            saveAccountToLink.existing(
                consumerSessionClientSecret = signup.consumerSession.clientSecret,
                selectedAccounts = selectedAccounts,
                shouldPollAccountNumbers = manifest.isDataFlow,
            )
        } else {
            // ** Legacy signup endpoint on unverified flows: 1 request **
            // SaveAccountToLink endpoint Signs up when providing email + phone number
            // **and** saves accounts to Link in the same request.
            saveAccountToLink.new(
                country = phoneController.getCountryCode(),
                email = state.validEmail!!,
                phoneNumber = state.validPhone!!,
                selectedAccounts = selectedAccounts,
                shouldPollAccountNumbers = manifest.isDataFlow,
            )
        }
        return Pane.SUCCESS
    }

    override fun navigateToVerification() {
        navigationManager.tryNavigateTo(NetworkingSaveToLinkVerification(referrer = NETWORKING_LINK_SIGNUP_PANE))
    }

    override fun handleSignupFailure(
        state: NetworkingLinkSignupState,
        error: Throwable
    ) {
        eventTracker.logError(
            extraMessage = "Error saving account to Link",
            error = error.toAttestationErrorIfApplicable(
                PrefillDetails(
                    state.validEmail!!,
                    state.validPhone,
                    state.payload()!!.phoneController.getCountryCode()
                )
            ),
            logger = logger,
            pane = NETWORKING_LINK_SIGNUP_PANE,
        )

        navigationManager.tryNavigateTo(Success(referrer = NETWORKING_LINK_SIGNUP_PANE))
    }
}
