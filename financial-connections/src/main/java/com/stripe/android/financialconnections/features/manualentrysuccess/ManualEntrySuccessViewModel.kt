package com.stripe.android.financialconnections.features.manualentrysuccess

import android.os.Bundle
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.ClickDone
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.PaneLoaded
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.domain.GetManifest
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.features.success.SuccessState
import com.stripe.android.financialconnections.model.FinancialConnectionsSession
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.navigation.Destination
import com.stripe.android.financialconnections.presentation.ScreenViewModel
import com.stripe.android.financialconnections.presentation.TopAppBarHost
import com.stripe.android.financialconnections.presentation.TopAppBarStateUpdate
import com.stripe.android.financialconnections.ui.FinancialConnectionsSheetNativeActivity
import com.stripe.android.financialconnections.ui.TextResource.StringId
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class ManualEntrySuccessViewModel @Inject constructor(
    initialState: ManualEntrySuccessState,
    topAppBarHost: TopAppBarHost,
    private val getManifest: GetManifest,
    private val eventTracker: FinancialConnectionsAnalyticsTracker,
    private val nativeAuthFlowCoordinator: NativeAuthFlowCoordinator,
) : ScreenViewModel<ManualEntrySuccessState>(initialState, topAppBarHost, Pane.MANUAL_ENTRY_SUCCESS) {

    init {
        suspend {
            val manifest = getManifest()
            SuccessState.Payload(
                businessName = manifest.businessName,
                customSuccessMessage = when (val last4 = initialState.last4) {
                    null -> StringId(R.string.stripe_success_pane_desc_microdeposits_no_account)
                    else -> StringId(R.string.stripe_success_pane_desc_microdeposits, listOf(last4))
                },
                accountsCount = 1, // on manual entry just one account is connected,
                skipSuccessPane = false
            ).also {
                eventTracker.track(PaneLoaded(Pane.MANUAL_ENTRY_SUCCESS))
            }
        }.execute { copy(payload = it) }
    }

    override fun updateTopAppBarState(state: ManualEntrySuccessState): TopAppBarStateUpdate {
        return TopAppBarStateUpdate(allowBackNavigation = false)
    }

    fun onSubmit() {
        viewModelScope.launch {
            setState { copy(completeSession = Loading()) }
            eventTracker.track(ClickDone(Pane.MANUAL_ENTRY_SUCCESS))
            nativeAuthFlowCoordinator().emit(NativeAuthFlowCoordinator.Message.Complete())
        }
    }

    companion object :
        MavericksViewModelFactory<ManualEntrySuccessViewModel, ManualEntrySuccessState> {

        override fun create(
            viewModelContext: ViewModelContext,
            state: ManualEntrySuccessState
        ): ManualEntrySuccessViewModel {
            val parentViewModel = viewModelContext.activity<FinancialConnectionsSheetNativeActivity>().viewModel
            return parentViewModel
                .activityRetainedComponent
                .manualEntrySuccessBuilder
                .initialState(state)
                .topAppBarHost(parentViewModel)
                .build()
                .viewModel
        }
    }
}

internal data class ManualEntrySuccessState(
    val last4: String?,
    val payload: Async<SuccessState.Payload>,
    val completeSession: Async<FinancialConnectionsSession>
) : MavericksState {

    @Suppress("unused") // used by mavericks to create initial state.
    constructor(args: Bundle?) : this(
        last4 = Destination.ManualEntrySuccess.last4(args),
        payload = Uninitialized,
        completeSession = Uninitialized
    )
}
