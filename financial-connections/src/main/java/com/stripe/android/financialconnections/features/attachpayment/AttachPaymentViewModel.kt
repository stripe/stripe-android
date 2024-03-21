package com.stripe.android.financialconnections.features.attachpayment

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.PollAttachPaymentsSucceeded
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.analytics.logError
import com.stripe.android.financialconnections.domain.GetCachedAccounts
import com.stripe.android.financialconnections.domain.GetCachedConsumerSession
import com.stripe.android.financialconnections.domain.GetOrFetchSync
import com.stripe.android.financialconnections.domain.PollAttachPaymentAccount
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.model.LinkAccountSessionPaymentAccount
import com.stripe.android.financialconnections.model.PaymentAccountParams
import com.stripe.android.financialconnections.navigation.Destination.ManualEntry
import com.stripe.android.financialconnections.navigation.Destination.Reset
import com.stripe.android.financialconnections.navigation.NavigationManager
import com.stripe.android.financialconnections.navigation.destination
import com.stripe.android.financialconnections.navigation.topappbar.TopAppBarHost
import com.stripe.android.financialconnections.presentation.FinancialConnectionsViewModel
import com.stripe.android.financialconnections.repository.SuccessContentRepository
import com.stripe.android.financialconnections.ui.TextResource.PluralId
import com.stripe.android.financialconnections.utils.measureTimeMillis
import com.stripe.android.financialconnections.utils.parentViewModel
import javax.inject.Inject

internal class AttachPaymentViewModel @Inject constructor(
    initialState: AttachPaymentState,
    topAppBarHost: TopAppBarHost,
    private val successContentRepository: SuccessContentRepository,
    private val pollAttachPaymentAccount: PollAttachPaymentAccount,
    private val eventTracker: FinancialConnectionsAnalyticsTracker,
    private val getCachedAccounts: GetCachedAccounts,
    private val navigationManager: NavigationManager,
    private val getOrFetchSync: GetOrFetchSync,
    private val getCachedConsumerSession: GetCachedConsumerSession,
    private val logger: Logger
) : FinancialConnectionsViewModel<AttachPaymentState>(initialState, topAppBarHost) {

    init {
        logErrors()
        suspend {
            val sync = getOrFetchSync()
            val manifest = requireNotNull(sync.manifest)
            val consumerSession = getCachedConsumerSession()
            val authSession = requireNotNull(manifest.activeAuthSession)
            val activeInstitution = requireNotNull(manifest.activeInstitution)
            val accounts = getCachedAccounts()
            require(accounts.size == 1)
            val id = accounts.first().linkedAccountId
            val (result, millis) = measureTimeMillis {
                pollAttachPaymentAccount(
                    sync = sync,
                    activeInstitution = activeInstitution,
                    consumerSessionClientSecret = consumerSession?.clientSecret,
                    params = PaymentAccountParams.LinkedAccount(requireNotNull(id))
                )
            }
            if (manifest.isNetworkingUserFlow == true && manifest.accountholderIsLinkConsumer == true) {
                result.networkingSuccessful?.let {
                    successContentRepository.update {
                        copy(
                            customSuccessMessage = PluralId(
                                value = R.plurals.stripe_success_pane_desc_link_success,
                                count = accounts.size
                            )
                        )
                    }
                }
            }
            eventTracker.track(
                PollAttachPaymentsSucceeded(
                    authSessionId = authSession.id,
                    duration = millis
                )
            )
            val nextPane = result.nextPane ?: Pane.SUCCESS
            navigationManager.tryNavigateTo(nextPane.destination(referrer = PANE))
            result
        }.execute { copy(linkPaymentAccount = it) }
    }

    private fun logErrors() {
        onAsync(
            AttachPaymentState::linkPaymentAccount,
            onFail = {
                eventTracker.logError(
                    logger = logger,
                    pane = PANE,
                    extraMessage = "Error Attaching payment account",
                    error = it
                )
            }
        )
    }

    fun onEnterDetailsManually() =
        navigationManager.tryNavigateTo(ManualEntry(referrer = PANE))

    fun onSelectAnotherBank() =
        navigationManager.tryNavigateTo(Reset(referrer = PANE))

    companion object : MavericksViewModelFactory<AttachPaymentViewModel, AttachPaymentState> {

        override fun create(
            viewModelContext: ViewModelContext,
            state: AttachPaymentState
        ): AttachPaymentViewModel {
            val parentViewModel = viewModelContext.parentViewModel()
            return parentViewModel
                .activityRetainedComponent
                .attachPaymentSubcomponent
                .initialState(state)
                .topAppBarHost(parentViewModel)
                .build()
                .viewModel
        }

        private val PANE = Pane.ATTACH_LINKED_PAYMENT_ACCOUNT
    }
}

internal data class AttachPaymentState(
    val linkPaymentAccount: Async<LinkAccountSessionPaymentAccount> = Uninitialized
) : MavericksState
