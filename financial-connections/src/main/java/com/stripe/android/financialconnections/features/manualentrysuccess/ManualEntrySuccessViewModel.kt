package com.stripe.android.financialconnections.features.manualentrysuccess

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.ClickDone
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.PaneLoaded
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.di.FinancialConnectionsSheetNativeComponent
import com.stripe.android.financialconnections.domain.GetOrFetchSync
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
import com.stripe.android.financialconnections.ui.TextResource
import com.stripe.android.financialconnections.utils.error
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.launch

internal class ManualEntrySuccessViewModel @AssistedInject constructor(
    @Assisted initialState: ManualEntrySuccessState,
    private val getOrFetchSync: GetOrFetchSync,
    private val successContentRepository: SuccessContentRepository,
    private val eventTracker: FinancialConnectionsAnalyticsTracker,
    private val nativeAuthFlowCoordinator: NativeAuthFlowCoordinator,
) : FinancialConnectionsViewModel<ManualEntrySuccessState>(initialState, nativeAuthFlowCoordinator) {

    init {
        suspend {
            val manifest = getOrFetchSync().manifest
            val successContent = successContentRepository.get()
            val title = successContent?.heading ?: TextResource.StringId(R.string.stripe_success_pane_title)
            val content = successContent?.message ?: TextResource.PluralId(
                singular = R.string.stripe_success_pane_desc_singular,
                plural = R.string.stripe_success_pane_desc_plural,
                // on manual entry just one account is connected.
                count = 1,
            )
            SuccessState.Payload(
                businessName = manifest.businessName,
                title = title,
                content = content,
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
            error = state.payload.error,
            canCloseWithoutConfirmation = true,
        )
    }

    fun onSubmit() {
        viewModelScope.launch {
            setState { copy(completeSession = Loading()) }
            eventTracker.track(ClickDone(Pane.MANUAL_ENTRY_SUCCESS))
            nativeAuthFlowCoordinator().emit(NativeAuthFlowCoordinator.Message.Complete())
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(initialState: ManualEntrySuccessState): ManualEntrySuccessViewModel
    }

    companion object {

        fun factory(parentComponent: FinancialConnectionsSheetNativeComponent): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    parentComponent.manualEntrySuccessViewModelFactory.create(ManualEntrySuccessState())
                }
            }
    }
}

internal data class ManualEntrySuccessState(
    val payload: Async<SuccessState.Payload> = Uninitialized,
    val completeSession: Async<FinancialConnectionsSession> = Uninitialized
)
