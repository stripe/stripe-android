package com.stripe.android.financialconnections.features.partnerauth

import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.di.financialConnectionsSubComponentBuilderProvider
import com.stripe.android.financialconnections.domain.FetchFinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator.Message
import com.stripe.android.financialconnections.navigation.NavigationDirections
import javax.inject.Inject

internal class PartnerAuthViewModel @Inject constructor(
    private val nativeAuthFlowCoordinator: NativeAuthFlowCoordinator,
    private val fetchFinancialConnectionsSessionManifest: FetchFinancialConnectionsSessionManifest,
    private val logger: Logger
) : MavericksViewModel<PartnerAuthState>(PartnerAuthState()) {

    fun onUrlChanged(currentUrl: String?) {
        logger.debug("CURRENTURL $currentUrl")
        currentUrl?.let {
            if (it.contains(other = "connections-auth.stripe.com", ignoreCase = true)) {
                suspend {
                    val manifest = fetchFinancialConnectionsSessionManifest()
                    nativeAuthFlowCoordinator().emit(Message.UpdateManifest(manifest))
                    nativeAuthFlowCoordinator().emit(
                        Message.RequestNextStep(
                            currentStep = NavigationDirections.partnerAuth
                        )
                    )
                }.execute { this }
            }
        }
    }

    companion object : MavericksViewModelFactory<PartnerAuthViewModel, PartnerAuthState> {

        override fun create(
            viewModelContext: ViewModelContext,
            state: PartnerAuthState
        ): PartnerAuthViewModel {
            return viewModelContext.financialConnectionsSubComponentBuilderProvider
                .partnerAuthSubcomponentBuilder.get()
                .initialState(state)
                .build()
                .viewModel
        }
    }
}

data class PartnerAuthState(
    val test: String = ""
) : MavericksState