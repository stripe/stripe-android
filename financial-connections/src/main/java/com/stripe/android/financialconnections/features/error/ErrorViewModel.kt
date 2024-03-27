package com.stripe.android.financialconnections.features.error

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.analytics.logError
import com.stripe.android.financialconnections.di.FinancialConnectionsSheetNativeComponent
import com.stripe.android.financialconnections.domain.GetManifest
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator.Message
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.navigation.Destination
import com.stripe.android.financialconnections.navigation.NavigationManager
import com.stripe.android.financialconnections.navigation.PopUpToBehavior
import com.stripe.android.financialconnections.navigation.topappbar.TopAppBarStateUpdate
import com.stripe.android.financialconnections.presentation.Async
import com.stripe.android.financialconnections.presentation.Async.Uninitialized
import com.stripe.android.financialconnections.presentation.FinancialConnectionsViewModel
import com.stripe.android.financialconnections.repository.FinancialConnectionsErrorRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class ErrorViewModel @Inject constructor(
    initialState: ErrorState,
    nativeAuthFlowCoordinator: NativeAuthFlowCoordinator,
    private val coordinator: NativeAuthFlowCoordinator,
    private val getManifest: GetManifest,
    private val errorRepository: FinancialConnectionsErrorRepository,
    private val eventTracker: FinancialConnectionsAnalyticsTracker,
    private val navigationManager: NavigationManager,
    private val logger: Logger
) : FinancialConnectionsViewModel<ErrorState>(initialState, nativeAuthFlowCoordinator) {

    init {
        logErrors()
        suspend {
            // Clear the partner web auth state if it exists, so that if the user lands back in the partner_auth
            // pane after an error, they will be able to start over.
            coordinator().emit(Message.ClearPartnerWebAuth)
            ErrorState.Payload(
                error = requireNotNull(errorRepository.get()),
                disableLinkMoreAccounts = getManifest().disableLinkMoreAccounts,
                allowManualEntry = getManifest().allowManualEntry
            )
        }.execute { copy(payload = it) }
    }

    override fun updateTopAppBar(state: ErrorState): TopAppBarStateUpdate {
        return TopAppBarStateUpdate(
            pane = PANE,
            allowBackNavigation = false,
        )
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
        // NOTE: we do not clear error when going to manual entry
        // this allows us to enable the user to go back to this pane.
        // we may still attach `terminal_error` in the /complete call,
        // but we do that today in v2 and we will still successfully complete
        // the session.
        navigationManager.tryNavigateTo(
            route = Destination.ManualEntry(referrer = PANE),
        )
    }

    private fun reset() {
        navigationManager.tryNavigateTo(
            route = Destination.Reset(referrer = PANE),
            popUpTo = PopUpToBehavior.Current(inclusive = true),
        )
    }

    suspend fun close(error: Throwable) {
        coordinator().emit(Message.CloseWithError(error))
    }

    fun onSelectAnotherBank() = viewModelScope.launch {
        kotlin.runCatching {
            val payload = requireNotNull(stateFlow.value.payload())
            if (payload.disableLinkMoreAccounts) {
                close(payload.error)
            } else {
                reset()
            }
        }.onFailure {
            close(it)
        }
    }

    override fun onCleared() {
        errorRepository.clear()
        super.onCleared()
    }

    companion object {

        fun factory(parentComponent: FinancialConnectionsSheetNativeComponent): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    parentComponent
                        .errorSubcomponent
                        .create(ErrorState())
                        .viewModel
                }
            }

        internal val PANE = Pane.UNEXPECTED_ERROR
    }
}

internal data class ErrorState(
    val payload: Async<Payload> = Uninitialized
) {
    data class Payload(
        val error: Throwable,
        val disableLinkMoreAccounts: Boolean,
        val allowManualEntry: Boolean
    )
}
