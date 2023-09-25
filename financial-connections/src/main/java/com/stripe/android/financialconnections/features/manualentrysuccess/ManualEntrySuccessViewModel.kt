package com.stripe.android.financialconnections.features.manualentrysuccess

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.ClickDone
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.Complete
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.PaneLoaded
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.domain.CompleteFinancialConnectionsSession
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator.Message.Finish
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityResult.Completed
import com.stripe.android.financialconnections.model.FinancialConnectionsSession
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.ui.FinancialConnectionsSheetNativeActivity
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
internal class ManualEntrySuccessViewModel @Inject constructor(
    initialState: ManualEntrySuccessState,
    private val completeFinancialConnectionsSession: CompleteFinancialConnectionsSession,
    private val eventTracker: FinancialConnectionsAnalyticsTracker,
    private val nativeAuthFlowCoordinator: NativeAuthFlowCoordinator,
    private val logger: Logger
) : MavericksViewModel<ManualEntrySuccessState>(initialState) {

    init {
        logErrors()
        viewModelScope.launch {
            eventTracker.track(PaneLoaded(Pane.MANUAL_ENTRY_SUCCESS))
        }
    }

    private fun logErrors() {
        onAsync(
            ManualEntrySuccessState::completeSession,
            onSuccess = {
                eventTracker.track(
                    Complete(
                        connectedAccounts = it.accounts.data.count(),
                        exceptionExtraMessage = null,
                        exception = null
                    )
                )
            },
            onFail = {
                eventTracker.track(
                    Complete(
                        connectedAccounts = null,
                        exceptionExtraMessage = null,
                        exception = it
                    )
                )
                logger.error("Error completing session", it)
            }
        )
    }

    fun onSubmit() {
        viewModelScope.launch {
            eventTracker.track(ClickDone(Pane.MANUAL_ENTRY_SUCCESS))
        }
        suspend {
            completeFinancialConnectionsSession().also {
                val result = Completed(
                    financialConnectionsSession = it,
                    token = it.parsedToken
                )
                nativeAuthFlowCoordinator().emit(Finish(result))
            }
        }.execute { copy(completeSession = it) }
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
