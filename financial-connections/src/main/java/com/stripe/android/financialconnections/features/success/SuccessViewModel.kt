package com.stripe.android.financialconnections.features.success

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import com.stripe.android.core.Logger
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
import com.stripe.android.financialconnections.ui.FinancialConnectionsSheetNativeActivity
import com.stripe.android.financialconnections.ui.TextResource
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
internal class SuccessViewModel @Inject constructor(
    initialState: SuccessState,
    getCachedAccounts: GetCachedAccounts,
    getManifest: GetManifest,
    private val eventTracker: FinancialConnectionsAnalyticsTracker,
    private val logger: Logger,
    private val nativeAuthFlowCoordinator: NativeAuthFlowCoordinator
) : MavericksViewModel<SuccessState>(initialState) {

    init {
        observeAsyncs()
        suspend {
            val manifest = getManifest()
            val accounts = getCachedAccounts()
            SuccessState.Payload(
                skipSuccessPane = manifest.skipSuccessPane ?: false,
                accountsCount = accounts.size,
                customSuccessMessage = null,
                // We just want to use the business name in the CTA if the feature is enabled in the manifest.
                businessName = manifest.businessName?.takeIf { manifest.useContinueWithMerchantText() },
            )
        }.execute {
            copy(payload = it)
        }
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
            return viewModelContext.activity<FinancialConnectionsSheetNativeActivity>()
                .viewModel
                .activityRetainedComponent
                .successSubcomponent
                .initialState(state)
                .build()
                .viewModel
        }

        private val PANE = Pane.SUCCESS
    }
}

internal data class SuccessState(
    // Just enabled on Compose Previews: allows to preview the post-animation state.
    val overrideAnimationForPreview: Boolean = false,
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
