package com.stripe.android.financialconnections.features.manualentrysuccess

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.ClickDone
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.PaneLoaded
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.model.FinancialConnectionsSession
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.ui.FinancialConnectionsSheetNativeActivity
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
internal class ManualEntrySuccessViewModel @Inject constructor(
    initialState: ManualEntrySuccessState,
    private val eventTracker: FinancialConnectionsAnalyticsTracker,
    private val nativeAuthFlowCoordinator: NativeAuthFlowCoordinator,
) : MavericksViewModel<ManualEntrySuccessState>(initialState) {

    init {
        viewModelScope.launch {
            eventTracker.track(PaneLoaded(Pane.MANUAL_ENTRY_SUCCESS))
        }
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
            return viewModelContext.activity<FinancialConnectionsSheetNativeActivity>()
                .viewModel
                .activityRetainedComponent
                .manualEntrySuccessBuilder
                .initialState(state)
                .build()
                .viewModel
        }
    }
}

internal data class ManualEntrySuccessState(
    val completeSession: Async<FinancialConnectionsSession> = Uninitialized
) : MavericksState
