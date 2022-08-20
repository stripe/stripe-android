package com.stripe.android.financialconnections.features.success

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import com.stripe.android.financialconnections.domain.GetAuthorizationSessionAccounts
import com.stripe.android.financialconnections.domain.GetManifest
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.features.common.AccessibleDataCalloutModel
import com.stripe.android.financialconnections.features.consent.FinancialConnectionsUrlResolver
import com.stripe.android.financialconnections.model.PartnerAccountsList
import com.stripe.android.financialconnections.ui.FinancialConnectionsSheetNativeActivity
import javax.inject.Inject

internal class SuccessViewModel @Inject constructor(
    initialState: SuccessState,
    getAuthorizationSessionAccounts: GetAuthorizationSessionAccounts,
    getManifest: GetManifest,
    val nativeAuthFlowCoordinator: NativeAuthFlowCoordinator
) : MavericksViewModel<SuccessState>(initialState) {

    init {
        suspend {
            val manifest = getManifest()
            SuccessState.Payload(
                accessibleData = AccessibleDataCalloutModel.fromManifest(manifest),
                accounts = getAuthorizationSessionAccounts(manifest.activeAuthSession!!.id),
                disconnectUrl = FinancialConnectionsUrlResolver.getDisconnectUrl(manifest)
            )
        }.execute {
            copy(payload = it)
        }
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
    }
}

internal data class SuccessState(
    val payload: Async<Payload> = Uninitialized
) : MavericksState {
    data class Payload(
        val accessibleData: AccessibleDataCalloutModel,
        val accounts: PartnerAccountsList,
        val disconnectUrl: String
    )
}
