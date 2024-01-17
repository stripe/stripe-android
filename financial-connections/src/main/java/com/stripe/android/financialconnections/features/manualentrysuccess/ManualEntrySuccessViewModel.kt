package com.stripe.android.financialconnections.features.manualentrysuccess

import android.os.Bundle
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
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
import com.stripe.android.financialconnections.ui.FinancialConnectionsSheetNativeActivity
import com.stripe.android.financialconnections.ui.TextResource
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
internal class ManualEntrySuccessViewModel @Inject constructor(
    initialState: ManualEntrySuccessState,
    private val getManifest: GetManifest,
    private val eventTracker: FinancialConnectionsAnalyticsTracker,
    private val nativeAuthFlowCoordinator: NativeAuthFlowCoordinator,
) : MavericksViewModel<ManualEntrySuccessState>(initialState) {

    init {
        suspend {
            val manifest = getManifest()
            val last4 = Destination.ManualEntrySuccess.last4(initialState.args)
            SuccessState.Payload(
                businessName = manifest.businessName,
                customSuccessMessage = TextResource.StringId(
                    value = R.string.stripe_success_pane_desc_microdeposits,
                    args = listOf(last4 ?: "")
                ),
                accountsCount = 1, // on manual entry just one account is connected,
                skipSuccessPane = manifest.skipSuccessPane ?: false
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
    val args: Bundle?,
    val payload: Async<SuccessState.Payload>,
    val completeSession: Async<FinancialConnectionsSession>
) : MavericksState {

    @Suppress("unused") // used by mavericks to create initial state.
    constructor(args: Bundle?) : this(
        args = args,
        payload = Uninitialized,
        completeSession = Uninitialized
    )
}
