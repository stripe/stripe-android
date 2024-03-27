package com.stripe.android.financialconnections.features.networkinglinkloginwarmup

import android.os.Bundle
import androidx.lifecycle.ViewModelProvider.Factory
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.Click
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.PaneLoaded
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.di.FinancialConnectionsSheetNativeComponent
import com.stripe.android.financialconnections.domain.DisableNetworking
import com.stripe.android.financialconnections.domain.GetManifest
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
import com.stripe.android.financialconnections.presentation.FinancialConnectionsViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class NetworkingLinkLoginWarmupViewModel @Inject constructor(
    initialState: NetworkingLinkLoginWarmupState,
    nativeAuthFlowCoordinator: NativeAuthFlowCoordinator,
    private val eventTracker: FinancialConnectionsAnalyticsTracker,
    private val handleError: HandleError,
    private val getManifest: GetManifest,
    private val disableNetworking: DisableNetworking,
    private val navigationManager: NavigationManager
) : FinancialConnectionsViewModel<NetworkingLinkLoginWarmupState>(initialState, nativeAuthFlowCoordinator) {

    init {
        logErrors()
        suspend {
            val manifest = getManifest()
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

    fun onSkipClicked() {
        suspend {
            eventTracker.track(Click("click.skip_sign_in", PANE))
            disableNetworking().also {
                val popUpToBehavior = determinePopUpToBehaviorForSkip()
                navigationManager.tryNavigateTo(
                    route = it.nextPane.destination(referrer = PANE),
                    popUpTo = popUpToBehavior,
                )
            }
        }.execute { copy(disableNetworkingAsync = it) }
    }

    private suspend fun determinePopUpToBehaviorForSkip(): PopUpToBehavior {
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

    companion object {

        internal val PANE = Pane.NETWORKING_LINK_LOGIN_WARMUP

        fun factory(parentComponent: FinancialConnectionsSheetNativeComponent, arguments: Bundle?): Factory =
            viewModelFactory {
                initializer {
                    parentComponent
                        .networkingLinkLoginWarmupSubcomponent
                        .initialState(NetworkingLinkLoginWarmupState(arguments))
                        .build()
                        .viewModel
                }
            }
    }
}

internal data class NetworkingLinkLoginWarmupState(
    val referrer: Pane? = null,
    val payload: Async<Payload> = Uninitialized,
    val disableNetworkingAsync: Async<FinancialConnectionsSessionManifest> = Uninitialized,
) {

    constructor(args: Bundle?) : this(
        referrer = Destination.referrer(args),
        payload = Uninitialized,
        disableNetworkingAsync = Uninitialized,
    )

    data class Payload(
        val merchantName: String?,
        val email: String
    )
}
