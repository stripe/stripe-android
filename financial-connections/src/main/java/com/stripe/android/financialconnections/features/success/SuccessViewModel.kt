package com.stripe.android.financialconnections.features.success

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.ClickDone
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.PaneLoaded
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.domain.GetCachedAccounts
import com.stripe.android.financialconnections.domain.GetManifest
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator.Message.Complete
import com.stripe.android.financialconnections.features.common.useContinueWithMerchantText
import com.stripe.android.financialconnections.model.FinancialConnectionsSession
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.presentation.ScreenViewModel
import com.stripe.android.financialconnections.presentation.TopAppBarHost
import com.stripe.android.financialconnections.repository.SaveToLinkWithStripeSucceededRepository
import com.stripe.android.financialconnections.ui.FinancialConnectionsSheetNativeActivity
import com.stripe.android.financialconnections.ui.TextResource
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class SuccessViewModel @Inject constructor(
    initialState: SuccessState,
    topAppBarHost: TopAppBarHost,
    getCachedAccounts: GetCachedAccounts,
    getManifest: GetManifest,
    private val saveToLinkWithStripeSucceeded: SaveToLinkWithStripeSucceededRepository,
    private val eventTracker: FinancialConnectionsAnalyticsTracker,
    private val logger: Logger,
    private val nativeAuthFlowCoordinator: NativeAuthFlowCoordinator
) : ScreenViewModel<SuccessState>(initialState, topAppBarHost, Pane.SUCCESS) {

    init {
        observeAsyncs()
        suspend {
            val manifest = getManifest()
            val accounts = getCachedAccounts()
            val saveToLinkWithStripeSucceeded: Boolean? = saveToLinkWithStripeSucceeded.get()
            SuccessState.Payload(
                skipSuccessPane = manifest.skipSuccessPane ?: false,
                accountsCount = accounts.size,
                customSuccessMessage = saveToLinkWithStripeSucceeded?.let { buildCustomMessage(it, accounts.size) },
                // We just want to use the business name in the CTA if the feature is enabled in the manifest.
                businessName = manifest.businessName?.takeIf { manifest.useContinueWithMerchantText() },
            )
        }.execute {
            copy(payload = it)
        }
    }

    // TODO: Should this be solved via NavController?
    override fun allowsBackNavigation(state: SuccessState): Boolean {
        return false
    }

    override fun hidesStripeLogo(state: SuccessState, originalValue: Boolean): Boolean {
        return originalValue
    }

    private fun buildCustomMessage(
        saveToLinkWithStripeSucceeded: Boolean,
        accountsCount: Int
    ): TextResource = when (saveToLinkWithStripeSucceeded) {
        true -> TextResource.PluralId(R.plurals.stripe_success_pane_desc_link_success, accountsCount)
        false -> TextResource.PluralId(R.plurals.stripe_success_pane_desc_link_error, accountsCount)
    }

    private fun observeAsyncs() {
        onAsync(
            SuccessState::payload,
            onFail = {
                logger.error("Error retrieving payload", it)
            },
            onSuccess = {
                if (it.skipSuccessPane.not()) {
                    eventTracker.track(PaneLoaded(PANE))
                } else {
                    completeSession()
                }
            }
        )
    }

    fun onDoneClick() = viewModelScope.launch {
        eventTracker.track(ClickDone(PANE))
        setState { copy(completeSession = Loading()) }
        completeSession()
    }

    private suspend fun completeSession() {
        nativeAuthFlowCoordinator().emit(Complete())
    }

    companion object : MavericksViewModelFactory<SuccessViewModel, SuccessState> {

        override fun create(
            viewModelContext: ViewModelContext,
            state: SuccessState
        ): SuccessViewModel {
            val parentViewModel = viewModelContext.activity<FinancialConnectionsSheetNativeActivity>().viewModel
            return parentViewModel
                .activityRetainedComponent
                .successSubcomponent
                .initialState(state)
                .topAppBarHost(parentViewModel)
                .build()
                .viewModel
        }

        private val PANE = Pane.SUCCESS
    }
}

internal data class SuccessState(
    val payload: Async<Payload> = Uninitialized,
    val completeSession: Async<FinancialConnectionsSession> = Uninitialized
) : MavericksState {

    data class Payload(
        val businessName: String?,
        val customSuccessMessage: TextResource?,
        val accountsCount: Int,
        val skipSuccessPane: Boolean
    )
}
