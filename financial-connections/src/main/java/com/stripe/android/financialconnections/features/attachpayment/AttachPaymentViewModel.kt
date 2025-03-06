package com.stripe.android.financialconnections.features.attachpayment

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.PollAttachPaymentsSucceeded
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.analytics.logError
import com.stripe.android.financialconnections.di.FinancialConnectionsSheetNativeComponent
import com.stripe.android.financialconnections.domain.CachedPartnerAccount
import com.stripe.android.financialconnections.domain.GetCachedAccounts
import com.stripe.android.financialconnections.domain.GetOrFetchSync
import com.stripe.android.financialconnections.domain.IsNetworkingRelinkSession
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.domain.PollAttachPaymentAccount
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.model.LinkAccountSessionPaymentAccount
import com.stripe.android.financialconnections.model.PaymentAccountParams
import com.stripe.android.financialconnections.navigation.Destination.ManualEntry
import com.stripe.android.financialconnections.navigation.Destination.Reset
import com.stripe.android.financialconnections.navigation.destination
import com.stripe.android.financialconnections.navigation.topappbar.TopAppBarStateUpdate
import com.stripe.android.financialconnections.presentation.Async
import com.stripe.android.financialconnections.presentation.Async.Uninitialized
import com.stripe.android.financialconnections.presentation.FinancialConnectionsViewModel
import com.stripe.android.financialconnections.repository.SuccessContentRepository
import com.stripe.android.financialconnections.ui.TextResource.PluralId
import com.stripe.android.financialconnections.utils.error
import com.stripe.android.financialconnections.utils.measureTimeMillis
import com.stripe.android.uicore.navigation.NavigationManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class AttachPaymentViewModel @AssistedInject constructor(
    @Assisted initialState: AttachPaymentState,
    nativeAuthFlowCoordinator: NativeAuthFlowCoordinator,
    private val successContentRepository: SuccessContentRepository,
    private val pollAttachPaymentAccount: PollAttachPaymentAccount,
    private val eventTracker: FinancialConnectionsAnalyticsTracker,
    private val getCachedAccounts: GetCachedAccounts,
    private val navigationManager: NavigationManager,
    private val getOrFetchSync: GetOrFetchSync,
    private val logger: Logger,
    private val isNetworkingRelinkSession: IsNetworkingRelinkSession,
) : FinancialConnectionsViewModel<AttachPaymentState>(initialState, nativeAuthFlowCoordinator) {

    init {
        logErrors()
        suspend {
            val sync = getOrFetchSync()
            val manifest = requireNotNull(sync.manifest)
            val authSession = requireNotNull(manifest.activeAuthSession)
            val activeInstitution = requireNotNull(manifest.activeInstitution)
            val accounts = getCachedAccounts()
            val id = accounts.single().linkedAccountId
            val (result, millis) = measureTimeMillis {
                pollAttachPaymentAccount(
                    sync = sync,
                    activeInstitution = activeInstitution,
                    params = PaymentAccountParams.LinkedAccount(requireNotNull(id))
                )
            }
            if (result.networkingSuccessful == true) {
                setSuccessMessageIfNecessary(manifest, accounts)
            }
            eventTracker.track(
                PollAttachPaymentsSucceeded(
                    pane = PANE,
                    authSessionId = authSession.id,
                    duration = millis
                )
            )
            val nextPane = result.nextPane ?: Pane.SUCCESS
            navigationManager.tryNavigateTo(nextPane.destination(referrer = PANE))
            result
        }.execute { copy(linkPaymentAccount = it) }
    }

    override fun updateTopAppBar(state: AttachPaymentState): TopAppBarStateUpdate {
        return TopAppBarStateUpdate(
            pane = PANE,
            allowBackNavigation = false,
            error = state.linkPaymentAccount.error,
        )
    }

    private fun setSuccessMessageIfNecessary(
        manifest: FinancialConnectionsSessionManifest,
        accounts: List<CachedPartnerAccount>,
    ) {
        if (manifest.canSetCustomLinkSuccessMessage && !isNetworkingRelinkSession()) {
            successContentRepository.set(
                message = PluralId(
                    value = R.plurals.stripe_success_pane_desc_link_success,
                    count = accounts.size
                )
            )
        }
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

    @AssistedFactory
    interface Factory {
        fun create(initialState: AttachPaymentState): AttachPaymentViewModel
    }

    companion object {

        fun factory(parentComponent: FinancialConnectionsSheetNativeComponent): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    parentComponent.attachPaymentViewModelFactory.create(AttachPaymentState())
                }
            }

        private val PANE = Pane.ATTACH_LINKED_PAYMENT_ACCOUNT
    }
}

internal data class AttachPaymentState(
    val linkPaymentAccount: Async<LinkAccountSessionPaymentAccount> = Uninitialized
)

private val FinancialConnectionsSessionManifest.canSetCustomLinkSuccessMessage: Boolean
    get() = isNetworkingUserFlow == true && accountholderIsLinkConsumer == true
