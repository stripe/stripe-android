package com.stripe.android.financialconnections.features.networkinglinkloginwarmup

import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.Click
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.PaneLoaded
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.di.FinancialConnectionsSheetNativeComponent
import com.stripe.android.financialconnections.domain.DisableNetworking
import com.stripe.android.financialconnections.domain.GetOrFetchSync
import com.stripe.android.financialconnections.domain.HandleError
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.features.common.getBusinessName
import com.stripe.android.financialconnections.features.common.getRedactedEmail
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.navigation.Destination
import com.stripe.android.financialconnections.navigation.NavigationManager
import com.stripe.android.financialconnections.navigation.PopUpToBehavior
import com.stripe.android.financialconnections.navigation.destination
import com.stripe.android.financialconnections.navigation.topappbar.TopAppBarStateUpdate
import com.stripe.android.financialconnections.presentation.Async
import com.stripe.android.financialconnections.presentation.Async.Uninitialized
import com.stripe.android.financialconnections.presentation.FinancialConnectionsSheetNativeState
import com.stripe.android.financialconnections.presentation.FinancialConnectionsViewModel
import com.stripe.android.financialconnections.repository.NextPaneOnDisableNetworkingRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.launch

internal class NetworkingLinkLoginWarmupViewModel @AssistedInject constructor(
    @Assisted initialState: NetworkingLinkLoginWarmupState,
    nativeAuthFlowCoordinator: NativeAuthFlowCoordinator,
    private val eventTracker: FinancialConnectionsAnalyticsTracker,
    private val handleError: HandleError,
    private val getOrFetchSync: GetOrFetchSync,
    private val disableNetworking: DisableNetworking,
    private val nextPaneOnDisableNetworkingRepository: NextPaneOnDisableNetworkingRepository,
    private val navigationManager: NavigationManager
) : FinancialConnectionsViewModel<NetworkingLinkLoginWarmupState>(initialState, nativeAuthFlowCoordinator) {

    init {
        logErrors()
        suspend {
            val manifest = getOrFetchSync().manifest
            eventTracker.track(PaneLoaded(PANE))
            NetworkingLinkLoginWarmupState.Payload(
                merchantName = manifest.getBusinessName(),
                email = requireNotNull(manifest.getRedactedEmail())
            )
        }.execute { copy(payload = it) }
    }

    override fun updateTopAppBar(state: NetworkingLinkLoginWarmupState): TopAppBarStateUpdate? {
        return null
    }

    private fun logErrors() {
        onAsync(
            NetworkingLinkLoginWarmupState::payload,
            onFail = { error ->
                handleError(
                    extraMessage = "Error fetching payload",
                    error = error,
                    pane = PANE,
                    displayErrorScreen = true
                )
            },
        )
        onAsync(
            NetworkingLinkLoginWarmupState::disableNetworkingAsync,
            onFail = { error ->
                handleError(
                    extraMessage = "Error disabling networking",
                    error = error,
                    displayErrorScreen = true,
                    pane = PANE
                )
            },
        )
    }

    fun onContinueClick() = viewModelScope.launch {
        eventTracker.track(Click("click.continue", PANE))
        navigationManager.tryNavigateTo(Destination.NetworkingLinkVerification(referrer = PANE))
    }

    fun onSecondaryButtonClicked() {
        if (stateFlow.value.isInstantDebits) {
            // In Instant Debits, the consumer can't skip networking. We simply close the
            // sheet and await the consumer's next action.
            navigationManager.tryNavigateBack()
        } else {
            skipNetworking()
        }
    }

    override fun onCleared() {
        super.onCleared()
        nextPaneOnDisableNetworkingRepository.clear()
    }

    private fun skipNetworking() {
        suspend {
            eventTracker.track(Click("click.skip_sign_in", PANE))
            disableNetworking(
                clientSuggestedNextPaneOnDisableNetworking = nextPaneOnDisableNetworkingRepository.get()?.nextPane
            ).also {
                val popUpToBehavior = determinePopUpToBehaviorForSkip()
                navigationManager.tryNavigateTo(
                    route = it.nextPane.destination(referrer = PANE),
                    popUpTo = popUpToBehavior,
                )
            }
        }.execute { copy(disableNetworkingAsync = it) }
    }

    private fun determinePopUpToBehaviorForSkip(): PopUpToBehavior {
        // Skipping disables networking, which means we don't want the user to navigate back to
        // the warm-up pane. Since the warmup pane is displayed as a bottom sheet, we need to
        // pop up all the way to the pane that opened it.
        val referrer = stateFlow.value.referrer

        return if (referrer != null) {
            PopUpToBehavior.Route(
                route = referrer.destination.fullRoute,
                inclusive = true,
            )
        } else {
            // Let's give it our best shot even though we don't know the referrer
            PopUpToBehavior.Current(inclusive = true)
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(initialState: NetworkingLinkLoginWarmupState): NetworkingLinkLoginWarmupViewModel
    }

    companion object {

        internal val PANE = Pane.NETWORKING_LINK_LOGIN_WARMUP

        fun factory(
            parentComponent: FinancialConnectionsSheetNativeComponent,
            arguments: Bundle?
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val parentState = parentComponent.viewModel.stateFlow.value
                parentComponent.networkingLinkLoginWarmupViewModelFactory.create(
                    NetworkingLinkLoginWarmupState(arguments, parentState)
                )
            }
        }
    }
}

internal data class NetworkingLinkLoginWarmupState(
    val referrer: Pane? = null,
    val payload: Async<Payload> = Uninitialized,
    val disableNetworkingAsync: Async<FinancialConnectionsSessionManifest> = Uninitialized,
    val isInstantDebits: Boolean = false,
) {

    val secondaryButtonLabel: Int
        get() = if (isInstantDebits) {
            R.string.stripe_networking_link_login_warmup_cta_cancel
        } else {
            R.string.stripe_networking_link_login_warmup_cta_skip
        }

    constructor(
        args: Bundle?,
        state: FinancialConnectionsSheetNativeState,
    ) : this(
        referrer = Destination.referrer(args),
        payload = Uninitialized,
        disableNetworkingAsync = Uninitialized,
        isInstantDebits = state.isLinkWithStripe,
    )

    data class Payload(
        val merchantName: String?,
        val email: String
    )
}
