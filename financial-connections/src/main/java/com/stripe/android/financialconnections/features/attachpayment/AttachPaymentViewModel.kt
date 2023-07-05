package com.stripe.android.financialconnections.features.attachpayment

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.Error
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.PaneLoaded
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.PollAttachPaymentsSucceeded
import com.stripe.android.financialconnections.domain.GetCachedAccounts
import com.stripe.android.financialconnections.domain.GetCachedConsumerSession
import com.stripe.android.financialconnections.domain.GetManifest
import com.stripe.android.financialconnections.domain.PollAttachPaymentAccount
import com.stripe.android.financialconnections.domain.toNavigationCommand
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.model.LinkAccountSessionPaymentAccount
import com.stripe.android.financialconnections.model.PaymentAccountParams
import com.stripe.android.financialconnections.navigation.NavigationDirections
import com.stripe.android.financialconnections.navigation.NavigationManager
import com.stripe.android.financialconnections.navigation.NavigationState.NavigateToRoute
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
    private val getManifest: GetManifest,
    private val getCachedConsumerSession: GetCachedConsumerSession,
    private val logger: Logger
) : MavericksViewModel<AttachPaymentState>(initialState) {

    init {
        logErrors()
        suspend {
            val manifest = getManifest()
            AttachPaymentState.Payload(
                businessName = manifest.businessName,
                accountsCount = getCachedAccounts().size
            )
        }.execute { copy(payload = it) }
        suspend {
            val manifest = getManifest()
            val consumerSession = getCachedConsumerSession()
            val authSession = requireNotNull(manifest.activeAuthSession)
            val activeInstitution = requireNotNull(manifest.activeInstitution)
            val accounts = getCachedAccounts()
            require(accounts.size == 1)
            val id = accounts.first().linkedAccountId
            val (result, millis) = measureTimeMillis {
                pollAttachPaymentAccount(
                    allowManualEntry = manifest.allowManualEntry,
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
                logger.error("Error retrieving accounts to attach payment", it)
                eventTracker.track(Error(Pane.ATTACH_LINKED_PAYMENT_ACCOUNT, it))
            },
            onSuccess = {
                eventTracker.track(PaneLoaded(Pane.ATTACH_LINKED_PAYMENT_ACCOUNT))
            }
        )
        onAsync(
            AttachPaymentState::linkPaymentAccount,
            onSuccess = {
                saveToLinkWithStripeSucceeded.set(true)
            },
            onFail = {
                saveToLinkWithStripeSucceeded.set(false)
                eventTracker.track(Error(Pane.ATTACH_LINKED_PAYMENT_ACCOUNT, it))
                logger.error("Error Attaching payment account", it)
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
