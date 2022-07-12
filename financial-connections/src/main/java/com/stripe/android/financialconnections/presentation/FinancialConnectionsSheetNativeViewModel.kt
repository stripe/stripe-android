package com.stripe.android.financialconnections.presentation

import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.di.DaggerFinancialConnectionsSheetNativeComponent
import com.stripe.android.financialconnections.di.FinancialConnectionsSheetNativeComponent
import com.stripe.android.financialconnections.domain.GetManifest
import com.stripe.android.financialconnections.domain.GoNext
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator.Message
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetNativeActivityArgs
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.FinancialConnectionsAuthorizationSession
import com.stripe.android.financialconnections.presentation.FinancialConnectionsSheetNativeViewEffect.OpenAuthFlowWithUrl
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
internal class FinancialConnectionsSheetNativeViewModel @Inject constructor(
    /**
     * Exposes parent dagger component (activity viewModel scoped so that it survives config changes)
     * No other dependencies should be exposed from the viewModel
     */
    val activityRetainedComponent: FinancialConnectionsSheetNativeComponent,
    private val goNext: GoNext,
    private val nativeAuthFlowCoordinator: NativeAuthFlowCoordinator,
    private val getManifest: GetManifest,
    initialState: FinancialConnectionsSheetNativeState
) : MavericksViewModel<FinancialConnectionsSheetNativeState>(initialState) {

    init {
        viewModelScope.launch {
            nativeAuthFlowCoordinator().collect { message ->
                when (message) {
                    is Message.RequestNextStep -> goNext(
                        currentPane = message.currentStep,
                        manifest = getManifest(),
                        authorizationSession = awaitState().authorizationSession
                    )
                    is Message.UpdateAuthorizationSession -> setState {
                        copy(authorizationSession = message.authorizationSession)
                    }
                    Message.OpenWebAuthFlow -> {
                        val manifest = getManifest()
                        setState {
                            copy(viewEffect = OpenAuthFlowWithUrl(manifest.hostedAuthUrl))
                        }
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

internal data class FinancialConnectionsSheetNativeState(
    val authorizationSession: FinancialConnectionsAuthorizationSession?,
    val configuration: FinancialConnectionsSheet.Configuration,
    val viewEffect: FinancialConnectionsSheetNativeViewEffect?
) : MavericksState {

    /**
     * Used by Mavericks to build initial state based on args.
     */
    @Suppress("Unused")
    constructor(args: FinancialConnectionsSheetNativeActivityArgs) : this(
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
