package com.stripe.android.financialconnections.domain

import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.VerificationError
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.VerificationError.Error.MarkLinkVerifiedError
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.VerificationSuccess
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.analytics.logError
import com.stripe.android.financialconnections.features.common.isDataFlow
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.model.PaymentAccountParams
import com.stripe.android.financialconnections.navigation.Destination
import com.stripe.android.financialconnections.navigation.Destination.Success
import com.stripe.android.financialconnections.navigation.NavigationManager
import com.stripe.android.financialconnections.navigation.destination
import com.stripe.android.financialconnections.repository.AttachedPaymentAccountRepository
import javax.inject.Inject

internal class CompleteVerification @Inject constructor(
    private val cachedAccounts: GetCachedAccounts,
    private val saveAccountToLink: SaveAccountToLink,
    private val getOrFetchSync: GetOrFetchSync,
    private val analyticsTracker: FinancialConnectionsAnalyticsTracker,
    private val navigationManager: NavigationManager,
    private val logger: Logger,
    private val handleError: HandleError,
    private val markLinkVerified: MarkLinkVerified,
    private val attachedPaymentAccountRepository: AttachedPaymentAccountRepository,
) {
    suspend operator fun invoke(
        pane: Pane,
        consumerSessionClientSecret: String,
    ) {
        val manifest = getOrFetchSync().manifest
        val accounts = cachedAccounts()
        if (accounts.isNotEmpty()) {
            // If there are accounts to share (ie. user linked accounts first and then logged in) then save those
            // when we have already connected accounts, we first saveToNetworkAndLink and then markLinkVerified
            saveToNetworkAndLinkRequest(
                accounts = accounts,
                consumerSessionClientSecret = consumerSessionClientSecret,
                isDataFlow = manifest.isDataFlow,
                pane = pane
            )
        } else if (attachedPaymentAccountRepository.get()?.attachedPaymentAccount is PaymentAccountParams.BankAccount) {
            // we come here if the user has already attached their bank account and has then logged in
            // to an existing Link account. now we want network that bank account
            // we do not need to pass anything here since the bank account is already attached
            // and the backend pulls it from the LAS
            saveToNetworkAndLinkRequest(
                accounts = null,
                consumerSessionClientSecret = consumerSessionClientSecret,
                isDataFlow = manifest.isDataFlow,
                pane = pane
            )
        } else {
            // if the user is verifying at the beginning of the flow and hasn't connected accounts yet,
            // we just markLinkVerified and move onto returning user account picker.
            runCatching { markLinkVerified() }.fold(
                // TODO(carlosmuvi): once `/link_verified` is updated to return correct next_pane we should consume that
                onSuccess = {
                    analyticsTracker.track(VerificationSuccess(pane))
                    navigationManager.tryNavigateTo(Destination.LinkAccountPicker(referrer = pane))
                },
                onFailure = {
                    val nextPaneOnFailure = manifest.initialInstitution
                        ?.let { Pane.PARTNER_AUTH }
                        ?: Pane.INSTITUTION_PICKER
                    analyticsTracker.logError(
                        extraMessage = "Error confirming verification or marking link as verified",
                        error = it,
                        logger = logger,
                        pane = pane
                    )
                    analyticsTracker.track(VerificationError(pane, MarkLinkVerifiedError))
                    navigationManager.tryNavigateTo(nextPaneOnFailure.destination(referrer = pane))
                }
            )
        }
    }

    private suspend fun saveToNetworkAndLinkRequest(
        accounts: List<CachedPartnerAccount>?,
        consumerSessionClientSecret: String,
        isDataFlow: Boolean,
        pane: Pane
    ) {
        runCatching {
            saveAccountToLink.existing(
                consumerSessionClientSecret = consumerSessionClientSecret,
                selectedAccounts = accounts,
                shouldPollAccountNumbers = isDataFlow
            )
        }.onSuccess {
            analyticsTracker.track(VerificationSuccess(pane))
        }.onFailure {
            logger.error("Error saving account to Link", it)
            analyticsTracker.track(VerificationError(pane, VerificationError.Error.SaveToLinkError))
        }

        runCatching { markLinkVerified() }
            .onSuccess {
                // navigate to success regardless of save result.
                // the save use case takes care of updating the success screen message (that will be a failure in case
                // of something going wrong)
                navigationManager.tryNavigateTo(Success(referrer = pane))
            }.onFailure {
                // mimicking behavior from v2 where if this request fails we use default error handling
                // we don't do this in the query definition because the call below does not use the default
                handleError(
                    extraMessage = "Error marking link as verified",
                    error = it,
                    pane = pane,
                    displayErrorScreen = true
                )
            }
    }
}
