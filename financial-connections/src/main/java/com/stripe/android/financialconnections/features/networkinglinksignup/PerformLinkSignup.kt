package com.stripe.android.financialconnections.features.networkinglinksignup

import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.Click
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.domain.AttachConsumerToLinkAccountSession
import com.stripe.android.financialconnections.domain.GetCachedAccounts
import com.stripe.android.financialconnections.domain.GetOrFetchSync
import com.stripe.android.financialconnections.domain.GetOrFetchSync.RefetchCondition.Always
import com.stripe.android.financialconnections.domain.SaveAccountToLink
import com.stripe.android.financialconnections.features.common.isDataFlow
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane.NETWORKING_LINK_SIGNUP_PANE
import com.stripe.android.financialconnections.repository.FinancialConnectionsConsumerSessionRepository
import javax.inject.Inject

internal fun interface PerformLinkSignup {
    suspend operator fun invoke(state: NetworkingLinkSignupState)
}

internal class PerformLinkSignupForInstantDebits @Inject constructor(
    private val consumerRepository: FinancialConnectionsConsumerSessionRepository,
    private val attachConsumerToLinkAccountSession: AttachConsumerToLinkAccountSession,
    private val getOrFetchSync: GetOrFetchSync,
) : PerformLinkSignup {

    override suspend fun invoke(state: NetworkingLinkSignupState) {
        val phoneController = state.payload()!!.phoneController

        val signup = consumerRepository.signUp(
            email = state.validEmail!!,
            phoneNumber = phoneController.getE164PhoneNumber(state.validPhone!!),
            country = phoneController.getCountryCode(),
        )

        attachConsumerToLinkAccountSession(
            consumerSessionClientSecret = signup.consumerSession.clientSecret,
        )

        getOrFetchSync(refetchCondition = Always)
    }
}

internal class PerformLinkSignupForNetworking @Inject constructor(
    private val eventTracker: FinancialConnectionsAnalyticsTracker,
    private val getOrFetchSync: GetOrFetchSync,
    private val getCachedAccounts: GetCachedAccounts,
    private val saveAccountToLink: SaveAccountToLink,
) : PerformLinkSignup {

    override suspend fun invoke(state: NetworkingLinkSignupState) {
        eventTracker.track(Click(eventName = "click.save_to_link", pane = NETWORKING_LINK_SIGNUP_PANE))
        val selectedAccounts = getCachedAccounts()
        val manifest = getOrFetchSync().manifest
        val phoneController = state.payload()!!.phoneController
        require(state.valid) { "Form invalid! ${state.validEmail} ${state.validPhone}" }
        saveAccountToLink.new(
            country = phoneController.getCountryCode(),
            email = state.validEmail!!,
            phoneNumber = phoneController.getE164PhoneNumber(state.validPhone!!),
            selectedAccounts = selectedAccounts,
            shouldPollAccountNumbers = manifest.isDataFlow,
        )
    }
}
