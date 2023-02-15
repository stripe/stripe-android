package com.stripe.android.financialconnections.features.linkaccountpicker

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
import com.stripe.android.financialconnections.domain.GetConsumerSession
import com.stripe.android.financialconnections.domain.GetManifest
import com.stripe.android.financialconnections.domain.PollNetworkedAccounts
import com.stripe.android.financialconnections.features.common.AccessibleDataCalloutModel
import com.stripe.android.financialconnections.features.consent.ConsentTextBuilder
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.model.PartnerAccount
import com.stripe.android.financialconnections.ui.FinancialConnectionsSheetNativeActivity
import javax.inject.Inject

internal class LinkAccountPickerViewModel @Inject constructor(
    initialState: LinkAccountPickerState,
    private val eventTracker: FinancialConnectionsAnalyticsTracker,
    private val getConsumerSession: GetConsumerSession,
    private val pollNetworkedAccounts: PollNetworkedAccounts,
    private val getManifest: GetManifest,
    private val logger: Logger
) : MavericksViewModel<LinkAccountPickerState>(initialState) {

    init {
        logErrors()
        suspend {
            val manifest = getManifest()
            val accessibleData = AccessibleDataCalloutModel.fromManifest(manifest)
            val consumerSessionClientSecret = getConsumerSession().clientSecret
            val accounts = pollNetworkedAccounts(consumerSessionClientSecret)
                .data
                .sortedBy { it.allowSelection.not() }
            eventTracker.track(PaneLoaded(Pane.LINK_ACCOUNT_PICKER))
            LinkAccountPickerState.Payload(
                businessName = ConsentTextBuilder.getBusinessName(manifest) ?: "",
                accounts = accounts,
                accessibleData = accessibleData
            )
        }.execute { copy(payload = it) }
    }

    private fun logErrors() {
        onAsync(
            LinkAccountPickerState::payload,
            onFail = { error ->
                logger.error("Error fetching payload", error)
                eventTracker.track(Error(Pane.LINK_ACCOUNT_PICKER, error))
            },
        )
    }

    fun onLearnMoreAboutDataAccessClick() {
        TODO("Not yet implemented")
    }

    fun onNewBankAccountClick() {
        TODO("Not yet implemented")
    }

    fun onSelectAccountClick() {
        TODO("Not yet implemented")
    }

    companion object :
        MavericksViewModelFactory<LinkAccountPickerViewModel, LinkAccountPickerState> {

        override fun create(
            viewModelContext: ViewModelContext,
            state: LinkAccountPickerState
        ): LinkAccountPickerViewModel {
            return viewModelContext.activity<FinancialConnectionsSheetNativeActivity>()
                .viewModel
                .activityRetainedComponent
                .linkAccountPickerSubcomponent
                .initialState(state)
                .build()
                .viewModel
        }
    }
}

internal data class LinkAccountPickerState(
    val payload: Async<Payload> = Uninitialized,
) : MavericksState {

    data class Payload(
        val accounts: List<PartnerAccount>,
        val accessibleData: AccessibleDataCalloutModel,
        val businessName: String
    )
}
