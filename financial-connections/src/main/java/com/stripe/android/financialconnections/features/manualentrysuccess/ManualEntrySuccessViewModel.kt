package com.stripe.android.financialconnections.features.manualentrysuccess

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.ClickDone
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.PaneLoaded
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.domain.GetManifest
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.features.success.SuccessState
import com.stripe.android.financialconnections.model.FinancialConnectionsSession
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.navigation.topappbar.TopAppBarHost
import com.stripe.android.financialconnections.presentation.FinancialConnectionsViewModel
import com.stripe.android.financialconnections.repository.SuccessContentRepository
import com.stripe.android.financialconnections.utils.parentViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class ManualEntrySuccessViewModel @Inject constructor(
    initialState: ManualEntrySuccessState,
    topAppBarHost: TopAppBarHost,
    private val getManifest: GetManifest,
    private val successContentRepository: SuccessContentRepository,
    private val eventTracker: FinancialConnectionsAnalyticsTracker,
    private val nativeAuthFlowCoordinator: NativeAuthFlowCoordinator,
) : FinancialConnectionsViewModel<ManualEntrySuccessState>(initialState, topAppBarHost) {

    init {
        suspend {
            val manifest = getManifest()
            SuccessState.Payload(
                businessName = manifest.businessName,
                customSuccessMessage = successContentRepository.get().customSuccessMessage,
                accountsCount = 1, // on manual entry just one account is connected,
                skipSuccessPane = false
            ).also {
                eventTracker.track(PaneLoaded(Pane.MANUAL_ENTRY_SUCCESS))
            }
        }.execute { copy(payload = it) }
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
            val parentViewModel = viewModelContext.parentViewModel()
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
    val payload: Async<SuccessState.Payload> = Uninitialized,
    val completeSession: Async<FinancialConnectionsSession> = Uninitialized
) : MavericksState
