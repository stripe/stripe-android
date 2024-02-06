package com.stripe.android.financialconnections.features.error

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.analytics.logError
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator.Message
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.navigation.Destination
import com.stripe.android.financialconnections.navigation.NavigationManager
import com.stripe.android.financialconnections.repository.FinancialConnectionsErrorRepository
import com.stripe.android.financialconnections.ui.FinancialConnectionsSheetNativeActivity
import javax.inject.Inject

internal class ErrorViewModel @Inject constructor(
    initialState: ErrorState,
    private val coordinator: NativeAuthFlowCoordinator,
    private val errorRepository: FinancialConnectionsErrorRepository,
    private val eventTracker: FinancialConnectionsAnalyticsTracker,
    private val navigationManager: NavigationManager,
    private val logger: Logger
) : MavericksViewModel<ErrorState>(initialState) {

    init {
        logErrors()
        suspend {
            // Clear the partner web auth state if it exists, so that if the user lands back in the partner_auth
            // pane after an error, they will be able to start over.
            coordinator().emit(Message.ClearPartnerWebAuth)
            ErrorState.Payload(
                error = requireNotNull(errorRepository.get())
            )
        }.execute { copy(payload = it) }
    }

    private fun logErrors() {
        onAsync(
            ErrorState::payload,
            onFail = { error ->
                eventTracker.logError(
                    extraMessage = "Error linking more accounts",
                    error = error,
                    logger = logger,
                    pane = PANE
                )
            },
        )
    }

    fun onManualEntryClick() {
        errorRepository.clear()
        navigationManager.tryNavigateTo(
            route = Destination.ManualEntry(referrer = PANE),
            popUpToCurrent = true,
            inclusive = true
        )
    }

    fun onSelectBankClick() {
        errorRepository.clear()
        navigationManager.tryNavigateTo(
            Destination.InstitutionPicker(referrer = PANE),
            popUpToCurrent = true,
            inclusive = true
        )
    }

    companion object : MavericksViewModelFactory<ErrorViewModel, ErrorState> {

        override fun create(
            viewModelContext: ViewModelContext,
            state: ErrorState
        ): ErrorViewModel {
            return viewModelContext.activity<FinancialConnectionsSheetNativeActivity>()
                .viewModel
                .activityRetainedComponent
                .errorSubcomponent
                .initialState(state)
                .build()
                .viewModel
        }

        internal val PANE = Pane.UNEXPECTED_ERROR
    }
}

internal data class ErrorState(
    val payload: Async<Payload> = Uninitialized
) : MavericksState {
    data class Payload(
        val error: Throwable
    )
}
