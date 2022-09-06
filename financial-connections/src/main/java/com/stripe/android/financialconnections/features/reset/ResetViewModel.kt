package com.stripe.android.financialconnections.features.reset

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.domain.GoNext
import com.stripe.android.financialconnections.domain.LinkMoreAccounts
import com.stripe.android.financialconnections.ui.FinancialConnectionsSheetNativeActivity
import javax.inject.Inject

internal class ResetViewModel @Inject constructor(
    initialState: ResetState,
    private val linkMoreAccounts: LinkMoreAccounts,
    private val goNext: GoNext,
    private val logger: Logger
) : MavericksViewModel<ResetState>(initialState) {

    init {
        logErrors()
        suspend {
            val updatedManifest = linkMoreAccounts()
            goNext(updatedManifest.nextPane)
            Unit
        }.execute { copy(payload = it) }
    }

    private fun logErrors() {
        onAsync(ResetState::payload, onFail = {
            logger.error("Error linking more accounts", it)
        })
    }

    companion object : MavericksViewModelFactory<ResetViewModel, ResetState> {

        override fun create(
            viewModelContext: ViewModelContext,
            state: ResetState
        ): ResetViewModel {
            return viewModelContext.activity<FinancialConnectionsSheetNativeActivity>()
                .viewModel
                .activityRetainedComponent
                .resetSubcomponent
                .initialState(state)
                .build()
                .viewModel
        }
    }
}

internal data class ResetState(
    val payload: Async<Unit> = Uninitialized,
) : MavericksState
