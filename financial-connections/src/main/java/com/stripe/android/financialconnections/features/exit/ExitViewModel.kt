package com.stripe.android.financialconnections.features.exit

import android.os.Bundle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.analytics.logError
import com.stripe.android.financialconnections.core.Async
import com.stripe.android.financialconnections.core.Async.Uninitialized
import com.stripe.android.financialconnections.core.FinancialConnectionsViewModel
import com.stripe.android.financialconnections.di.FinancialConnectionsSheetNativeComponent
import com.stripe.android.financialconnections.domain.GetManifest
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator.Message
import com.stripe.android.financialconnections.features.common.getBusinessName
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.navigation.Destination
import com.stripe.android.financialconnections.navigation.NavigationManager
import com.stripe.android.financialconnections.ui.TextResource
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class ExitViewModel @Inject constructor(
    initialState: ExitState,
    private val getManifest: GetManifest,
    private val coordinator: NativeAuthFlowCoordinator,
    private val eventTracker: FinancialConnectionsAnalyticsTracker,
    private val navigationManager: NavigationManager,
    private val logger: Logger
) : FinancialConnectionsViewModel<ExitState>(initialState) {

    init {
        logErrors()
        suspend {
            val manifest = kotlin.runCatching { getManifest() }.getOrNull()
            val businessName = manifest?.getBusinessName()
            val isNetworkingSignupPane =
                manifest?.isNetworkingUserFlow == true && stateFlow.value.referrer == Pane.NETWORKING_LINK_SIGNUP_PANE
            val description = when {
                isNetworkingSignupPane -> when (businessName) {
                    null -> TextResource.StringId(R.string.stripe_close_dialog_networking_desc_no_business)
                    else -> TextResource.StringId(
                        value = R.string.stripe_close_dialog_networking_desc,
                        args = listOf(businessName)
                    )
                }

                else -> when (businessName) {
                    null -> TextResource.StringId(R.string.stripe_exit_modal_desc_no_business)
                    else -> TextResource.StringId(
                        value = R.string.stripe_exit_modal_desc,
                        args = listOf(businessName)
                    )
                }
            }
            ExitState.Payload(
                description = description,
            )
        }.execute { copy(payload = it) }
    }

    fun onCloseConfirm() = viewModelScope.launch {
        setState { copy(closing = true) }
        coordinator().emit(Message.Complete(cause = null))
    }

    fun onCloseDismiss() {
        navigationManager.tryNavigateBack()
    }

    private fun logErrors() {
        onAsync(
            ExitState::payload,
            onFail = { error ->
                eventTracker.logError(
                    extraMessage = "Error loading payload",
                    error = error,
                    logger = logger,
                    pane = PANE
                )
            },
        )
    }

    companion object {

        fun factory(parentComponent: FinancialConnectionsSheetNativeComponent, arguments: Bundle?) = viewModelFactory {
            initializer {
                parentComponent
                    .exitSubcomponent
                    .create(ExitState(arguments))
                    .viewModel
            }
        }

        internal val PANE = Pane.EXIT
    }
}

internal data class ExitState(
    val referrer: Pane?,
    val payload: Async<Payload>,
    val closing: Boolean
) {
    data class Payload(
        val description: TextResource,
    )

    constructor(args: Bundle?) : this(
        referrer = Destination.referrer(args),
        payload = Uninitialized,
        closing = false
    )
}
