package com.stripe.android.financialconnections.features.success

import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import com.airbnb.mvrx.withState
import com.stripe.android.financialconnections.model.PartnerAccountsList
import com.stripe.android.financialconnections.ui.FinancialConnectionsSheetNativeActivity
import javax.inject.Inject

internal class SuccessViewModel @Inject constructor(
    initialState: SuccessState
) : MavericksViewModel<SuccessState>(initialState) {

    companion object : MavericksViewModelFactory<SuccessViewModel, SuccessState> {

        override fun initialState(viewModelContext: ViewModelContext): SuccessState {
            val parentViewModel =
                viewModelContext.activity<FinancialConnectionsSheetNativeActivity>().viewModel
            return withState(parentViewModel) {
                SuccessState(
                    partnerAccountsList = requireNotNull(it.partnerAccountsList)
                )
            }
        }

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
    val partnerAccountsList: PartnerAccountsList
) : MavericksState
