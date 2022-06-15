package com.stripe.android.financialconnections.presentation

import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import com.stripe.android.financialconnections.di.financialConnectionsSubComponentBuilderProvider
import com.stripe.android.financialconnections.domain.AcceptConsent
import com.stripe.android.financialconnections.domain.FlowCoordinator
import com.stripe.android.financialconnections.domain.FlowCoordinatorMessage.RequestNextStep
import com.stripe.android.financialconnections.domain.FlowCoordinatorMessage.UpdateManifest
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.navigation.NavigationDirections
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class ConsentViewModel @Inject constructor(
    initialState: ConsentState,
    private val acceptConsent: AcceptConsent,
    private val flowCoordinator: FlowCoordinator,
) : MavericksViewModel<ConsentState>(initialState) {

    fun onContinueClick() {
        viewModelScope.launch {
            val manifest: FinancialConnectionsSessionManifest = acceptConsent()
            with(flowCoordinator()) {
                emit(UpdateManifest(manifest))
                emit(RequestNextStep(currentStep = NavigationDirections.consent))
            }
        }
    }

    fun onManifestChanged(manifest: FinancialConnectionsSessionManifest) {
        setState {
            copy(
                title = manifest.businessName + " works with Stripe to link your accounts"
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
    val title: String = ""
) : MavericksState
