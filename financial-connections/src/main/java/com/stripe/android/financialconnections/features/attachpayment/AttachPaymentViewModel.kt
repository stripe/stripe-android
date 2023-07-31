package com.stripe.android.financialconnections.features.attachpayment

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.PaneLoaded
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.PollAttachPaymentsSucceeded
import com.stripe.android.financialconnections.analytics.logError
import com.stripe.android.financialconnections.domain.GetCachedAccounts
import com.stripe.android.financialconnections.domain.GetCachedConsumerSession
import com.stripe.android.financialconnections.domain.GetOrFetchSync
import com.stripe.android.financialconnections.domain.PollAttachPaymentAccount
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.model.LinkAccountSessionPaymentAccount
import com.stripe.android.financialconnections.model.PaymentAccountParams
import com.stripe.android.financialconnections.navigation.NavigationDirections
import com.stripe.android.financialconnections.navigation.NavigationManager
import com.stripe.android.financialconnections.navigation.NavigationState.NavigateToRoute
import com.stripe.android.financialconnections.navigation.toNavigationCommand
import com.stripe.android.financialconnections.repository.SaveToLinkWithStripeSucceededRepository
import com.stripe.android.financialconnections.ui.FinancialConnectionsSheetNativeActivity
import com.stripe.android.financialconnections.utils.measureTimeMillis
import javax.inject.Inject

@Suppress("LongParameterList")
internal class AttachPaymentViewModel @Inject constructor(
    initialState: AttachPaymentState,
    private val saveToLinkWithStripeSucceeded: SaveToLinkWithStripeSucceededRepository,
    private val pollAttachPaymentAccount: PollAttachPaymentAccount,
    private val eventTracker: FinancialConnectionsAnalyticsTracker,
    private val getCachedAccounts: GetCachedAccounts,
    private val navigationManager: NavigationManager,
    private val getOrFetchSync: GetOrFetchSync,
    private val getCachedConsumerSession: GetCachedConsumerSession,
    private val logger: Logger
) : MavericksViewModel<AttachPaymentState>(initialState) {

    init {
        logErrors()
        suspend {
            val sync = getOrFetchSync()
            val manifest = requireNotNull(sync.manifest)
            AttachPaymentState.Payload(
                businessName = manifest.businessName,
                accountsCount = getCachedAccounts().size
            )
        }.execute { copy(payload = it) }
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
                ).also {
                    val nextPane = it.nextPane ?: Pane.SUCCESS
                    navigationManager.navigate(NavigateToRoute(nextPane.toNavigationCommand()))
                }
            }
            eventTracker.track(PollAttachPaymentsSucceeded(authSession.id, millis))
            result
        }.execute { copy(linkPaymentAccount = it) }
    }

    private fun logErrors() {
        onAsync(
            AttachPaymentState::payload,
            onFail = {
                eventTracker.logError(
                    logger = logger,
                    pane = Pane.ATTACH_LINKED_PAYMENT_ACCOUNT,
                    extraMessage = "Error retrieving accounts to attach payment",
                    error = it
                )
            },
            onSuccess = {
                eventTracker.track(PaneLoaded(Pane.ATTACH_LINKED_PAYMENT_ACCOUNT))
            }
        )
        onAsync(
            AttachPaymentState::linkPaymentAccount,
            onSuccess = {
                runCatching { saveToLinkWithStripeSucceeded.set(true) }
            },
            onFail = {
                runCatching { saveToLinkWithStripeSucceeded.set(false) }
                eventTracker.logError(
                    logger = logger,
                    pane = Pane.ATTACH_LINKED_PAYMENT_ACCOUNT,
                    extraMessage = "Error Attaching payment account",
                    error = it
                )
            }
        )
    }

    fun onEnterDetailsManually() =
        navigationManager.navigate(NavigateToRoute(NavigationDirections.manualEntry))

    fun onSelectAnotherBank() =
        navigationManager.navigate(NavigateToRoute(NavigationDirections.reset))

    companion object : MavericksViewModelFactory<AttachPaymentViewModel, AttachPaymentState> {

        override fun create(
            viewModelContext: ViewModelContext,
            state: AttachPaymentState
        ): AttachPaymentViewModel {
            return viewModelContext.activity<FinancialConnectionsSheetNativeActivity>()
                .viewModel
                .activityRetainedComponent
                .attachPaymentSubcomponent
                .initialState(state)
                .build()
                .viewModel
        }
    }
}

internal data class AttachPaymentState(
    val payload: Async<Payload> = Uninitialized,
    val linkPaymentAccount: Async<LinkAccountSessionPaymentAccount> = Uninitialized
) : MavericksState {
    data class Payload(
        val accountsCount: Int,
        val businessName: String?
    )
}
