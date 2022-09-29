package com.stripe.android.financialconnections.features.manualentrysuccess

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.domain.CompleteFinancialConnectionsSession
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator.Message.Finish
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityResult.Completed
import com.stripe.android.financialconnections.model.FinancialConnectionsSession
import com.stripe.android.financialconnections.ui.FinancialConnectionsSheetNativeActivity
import javax.inject.Inject

@Suppress("LongParameterList")
internal class ManualEntrySuccessViewModel @Inject constructor(
    initialState: ManualEntrySuccessState,
    private val completeFinancialConnectionsSession: CompleteFinancialConnectionsSession,
    private val nativeAuthFlowCoordinator: NativeAuthFlowCoordinator,
    private val logger: Logger
) : MavericksViewModel<ManualEntrySuccessState>(initialState) {

    init {
        logErrors()
    }

    private fun logErrors() {
        onAsync(ManualEntrySuccessState::completeSession, onFail = {
            logger.error("Error completing session", it)
        })
    }

    fun onSubmit() {
        suspend {
            completeFinancialConnectionsSession().also {
                val result = Completed(
                    financialConnectionsSession = it,
                    token = it.parsedToken
                )
                nativeAuthFlowCoordinator().emit(Finish(result))
            }
        }.execute { copy(completeSession = it) }
    }

    companion object :
        MavericksViewModelFactory<ManualEntrySuccessViewModel, ManualEntrySuccessState> {

        override fun create(
            viewModelContext: ViewModelContext,
            state: ManualEntrySuccessState
        ): ManualEntrySuccessViewModel {
            return viewModelContext.activity<FinancialConnectionsSheetNativeActivity>()
                .viewModel
                .activityRetainedComponent
                .manualEntrySuccessBuilder
                .initialState(state)
                .build()
                .viewModel
        }
    }
}

internal data class ManualEntrySuccessState(
    val completeSession: Async<FinancialConnectionsSession> = Uninitialized
) : MavericksState
