package com.stripe.android.financialconnections.features.manualentrysuccess

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.ClickDone
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.PaneLoaded
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.di.FinancialConnectionsSheetNativeComponent
import com.stripe.android.financialconnections.domain.GetManifest
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.features.success.SuccessState
import com.stripe.android.financialconnections.model.FinancialConnectionsSession
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.navigation.topappbar.TopAppBarStateUpdate
import com.stripe.android.financialconnections.presentation.Async
import com.stripe.android.financialconnections.presentation.Async.Loading
import com.stripe.android.financialconnections.presentation.Async.Uninitialized
import com.stripe.android.financialconnections.presentation.FinancialConnectionsViewModel
import com.stripe.android.financialconnections.repository.SuccessContentRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class ManualEntrySuccessViewModel @Inject constructor(
    initialState: ManualEntrySuccessState,
    private val getManifest: GetManifest,
    private val successContentRepository: SuccessContentRepository,
    private val eventTracker: FinancialConnectionsAnalyticsTracker,
    private val nativeAuthFlowCoordinator: NativeAuthFlowCoordinator,
) : FinancialConnectionsViewModel<ManualEntrySuccessState>(initialState, nativeAuthFlowCoordinator) {

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

    override fun updateTopAppBar(state: ManualEntrySuccessState): TopAppBarStateUpdate {
        return TopAppBarStateUpdate(
            pane = Pane.MANUAL_ENTRY_SUCCESS,
            allowBackNavigation = false,
        )
    }

    fun onSubmit() {
        viewModelScope.launch {
            setState { copy(completeSession = Loading()) }
            eventTracker.track(ClickDone(Pane.MANUAL_ENTRY_SUCCESS))
            nativeAuthFlowCoordinator().emit(NativeAuthFlowCoordinator.Message.Complete())
        }
    }

    companion object {

        fun factory(parentComponent: FinancialConnectionsSheetNativeComponent): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    parentComponent
                        .manualEntrySuccessBuilder
                        .initialState(ManualEntrySuccessState())
                        .build()
                        .viewModel
                }
            }
    }
}

internal data class ManualEntrySuccessState(
    val payload: Async<SuccessState.Payload> = Uninitialized,
    val completeSession: Async<FinancialConnectionsSession> = Uninitialized
)
