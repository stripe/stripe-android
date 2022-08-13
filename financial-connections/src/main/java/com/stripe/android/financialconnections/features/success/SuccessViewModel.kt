package com.stripe.android.financialconnections.features.success

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import com.stripe.android.financialconnections.domain.GetAuthorizationSessionAccounts
import com.stripe.android.financialconnections.domain.GetManifest
import com.stripe.android.financialconnections.model.PartnerAccountsList
import com.stripe.android.financialconnections.ui.FinancialConnectionsSheetNativeActivity
import javax.inject.Inject

internal class SuccessViewModel @Inject constructor(
    initialState: SuccessState,
    getAuthorizationSessionAccounts: GetAuthorizationSessionAccounts,
    getManifest: GetManifest
) : MavericksViewModel<SuccessState>(initialState) {

    init {
        suspend {
            val manifest = getManifest()
            getAuthorizationSessionAccounts(manifest.activeAuthSession!!.id)
        }.execute {
            copy(
                partnerAccountsList = it
            )
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
    val partnerAccountsList: Async<PartnerAccountsList> = Uninitialized
) : MavericksState
