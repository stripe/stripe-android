package com.stripe.android.financialconnections.presentation

import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.di.DaggerFinancialConnectionsSheetNativeComponent
import com.stripe.android.financialconnections.di.FinancialConnectionsSubcomponentBuilderProvider
import com.stripe.android.financialconnections.domain.FlowCoordinatorMessage
import com.stripe.android.financialconnections.domain.GoNext
import com.stripe.android.financialconnections.domain.ObserveFlowUpdates
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetNativeActivityArgs
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.FinancialConnectionsAuthorizationSession
import com.stripe.android.financialconnections.navigation.NavigationManager
import com.stripe.android.financialconnections.presentation.FinancialConnectionsSheetNativeViewEffect.OpenAuthFlowWithUrl
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class FinancialConnectionsSheetNativeViewModel @Inject constructor(
    val navigationManager: NavigationManager,
    val subcomponentBuilderProvider: FinancialConnectionsSubcomponentBuilderProvider,
    val goNext: GoNext,
    val logger: Logger,
    observeFlowUpdates: ObserveFlowUpdates,
    initialState: FinancialConnectionsSheetNativeState
) : MavericksViewModel<FinancialConnectionsSheetNativeState>(initialState) {

    init {
        viewModelScope.launch {
            observeFlowUpdates().collectLatest { message ->
                when (message) {
                    is FlowCoordinatorMessage.RequestNextStep -> withState {
                        goNext(
                            currentPane = message.currentStep,
                            manifest = it.manifest,
                            authorizationSession = it.authorizationSession
                        )
                    }
                    is FlowCoordinatorMessage.UpdateAuthorizationSession -> setState {
                        copy(authorizationSession = message.authorizationSession)
                    }
                    is FlowCoordinatorMessage.UpdateManifest -> setState {
                        copy(manifest = message.manifest)
                    }
                    FlowCoordinatorMessage.OpenWebAuthFlow -> setState {
                        copy(viewEffect = OpenAuthFlowWithUrl(manifest.hostedAuthUrl))
                    }
                }
            }
        }
    }

    fun onViewEffectLaunched() {
        setState { copy(viewEffect = null) }
    }

    companion object :
        MavericksViewModelFactory<FinancialConnectionsSheetNativeViewModel, FinancialConnectionsSheetNativeState> {

        override fun create(
            viewModelContext: ViewModelContext,
            state: FinancialConnectionsSheetNativeState
        ): FinancialConnectionsSheetNativeViewModel {
            return DaggerFinancialConnectionsSheetNativeComponent
                .builder()
                .application(viewModelContext.app())
                .configuration(state.configuration)
                .initialState(state)
                .build()
                .viewModel
        }
    }
}

/**
 * Constructor used by Mavericks to build the initial state.
 */
internal data class FinancialConnectionsSheetNativeState(
    val manifest: FinancialConnectionsSessionManifest,
    val authorizationSession: FinancialConnectionsAuthorizationSession?,
    val configuration: FinancialConnectionsSheet.Configuration,
    val viewEffect: FinancialConnectionsSheetNativeViewEffect?
) : MavericksState {

    @Suppress("Unused")
    /**
     * Used by Mavericks to build initial state based on args.
     */
    constructor(args: FinancialConnectionsSheetNativeActivityArgs) : this(
        manifest = args.manifest,
        configuration = args.configuration,
        authorizationSession = null,
        viewEffect = null
    )
}

internal sealed interface FinancialConnectionsSheetNativeViewEffect {
    /**
     * Open the Web AuthFlow.
     */
    data class OpenAuthFlowWithUrl(
        val url: String
    ) : FinancialConnectionsSheetNativeViewEffect
}
