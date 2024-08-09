package com.stripe.android.financialconnections.features.success

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.ClickDone
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.PaneLoaded
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.di.FinancialConnectionsSheetNativeComponent
import com.stripe.android.financialconnections.domain.GetCachedAccounts
import com.stripe.android.financialconnections.domain.GetOrFetchSync
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator.Message.Complete
import com.stripe.android.financialconnections.features.common.useContinueWithMerchantText
import com.stripe.android.financialconnections.model.FinancialConnectionsSession
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.navigation.topappbar.TopAppBarStateUpdate
import com.stripe.android.financialconnections.presentation.Async
import com.stripe.android.financialconnections.presentation.Async.Loading
import com.stripe.android.financialconnections.presentation.Async.Uninitialized
import com.stripe.android.financialconnections.presentation.FinancialConnectionsViewModel
import com.stripe.android.financialconnections.repository.SuccessContentRepository
import com.stripe.android.financialconnections.utils.error
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.launch

internal class SuccessViewModel @AssistedInject constructor(
    @Assisted initialState: SuccessState,
    getCachedAccounts: GetCachedAccounts,
    getOrFetchSync: GetOrFetchSync,
    private val successContentRepository: SuccessContentRepository,
    private val eventTracker: FinancialConnectionsAnalyticsTracker,
    private val logger: Logger,
    private val nativeAuthFlowCoordinator: NativeAuthFlowCoordinator
) : FinancialConnectionsViewModel<SuccessState>(initialState, nativeAuthFlowCoordinator) {

    init {
        observeAsyncs()
        suspend {
            val manifest = getOrFetchSync().manifest
            val accounts = getCachedAccounts()
            SuccessState.Payload(
                skipSuccessPane = manifest.skipSuccessPane ?: false,
                accountsCount = accounts.size,
                customSuccessContent = successContentRepository.get(),
                // We just want to use the business name in the CTA if the feature is enabled in the manifest.
                businessName = manifest.businessName?.takeIf { manifest.useContinueWithMerchantText() },
            )
        }.execute {
            copy(payload = it)
        }
    }

    override fun updateTopAppBar(state: SuccessState): TopAppBarStateUpdate {
        return TopAppBarStateUpdate(
            pane = PANE,
            allowBackNavigation = false,
            error = state.payload.error,
        )
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

    @AssistedFactory
    interface Factory {
        fun create(initialState: SuccessState): SuccessViewModel
    }

    companion object {

        fun factory(parentComponent: FinancialConnectionsSheetNativeComponent): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    parentComponent.successViewModelFactory.create(SuccessState())
                }
            }

        private val PANE = Pane.SUCCESS
    }
}

internal data class SuccessState(
    val payload: Async<Payload> = Uninitialized,
    val completeSession: Async<FinancialConnectionsSession> = Uninitialized
) {

    data class Payload(
        val businessName: String?,
        val customSuccessContent: SuccessContentRepository.State?,
        val accountsCount: Int,
        val skipSuccessPane: Boolean
    )
}
