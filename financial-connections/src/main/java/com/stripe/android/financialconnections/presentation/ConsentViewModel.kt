package com.stripe.android.financialconnections.presentation

import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.di.financialConnectionsSubComponentBuilderProvider
import com.stripe.android.financialconnections.domain.AcceptConsent
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator.Message.RequestNextStep
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator.Message.UpdateManifest
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.navigation.NavigationDirections
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class ConsentViewModel @Inject constructor(
    initialState: ConsentState,
    private val acceptConsent: AcceptConsent,
    private val nativeAuthFlowCoordinator: NativeAuthFlowCoordinator,
    private val logger: Logger
) : MavericksViewModel<ConsentState>(initialState) {

    fun onContinueClick() {
        viewModelScope.launch {
            val manifest: FinancialConnectionsSessionManifest = acceptConsent()
            with(nativeAuthFlowCoordinator()) {
                emit(UpdateManifest(manifest))
                emit(RequestNextStep(currentStep = NavigationDirections.consent))
            }
        }
    }

    fun onClickableTextClick(tag: String) {
        when(tag) {
            "terms" -> logger.debug("Terms clicked")
            "privacy" -> logger.debug("Privacy Policy clicked")
            "more" -> logger.debug("Learn more clicked")
        }
    }

    fun onManifestChanged(manifest: FinancialConnectionsSessionManifest) {
        setState {
            copy(
                title = manifest.businessName + " works with Stripe to link your accounts",
                content = listOf(
                    "Random very long text that takes more than one line on the screen.",
                    "Random very long text that takes more than one line on the screen.",
                    "Random very long text that takes more than one line on the screen.",
                    "Random very long text that takes more than one line on the screen.",
                )
            )
        }
    }

    companion object : MavericksViewModelFactory<ConsentViewModel, ConsentState> {

        override fun create(
            viewModelContext: ViewModelContext,
            state: ConsentState
        ): ConsentViewModel {
            return viewModelContext.financialConnectionsSubComponentBuilderProvider
                .consentSubComponentBuilder.get()
                .initialState(state)
                .build()
                .viewModel
        }
    }
}

internal data class ConsentState(
    val title: String = "",
    val content: List<String> = emptyList()
) : MavericksState
