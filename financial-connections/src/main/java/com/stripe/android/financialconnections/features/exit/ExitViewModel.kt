package com.stripe.android.financialconnections.features.exit

import android.os.Bundle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.analytics.logError
import com.stripe.android.financialconnections.di.FinancialConnectionsSheetNativeComponent
import com.stripe.android.financialconnections.domain.GetOrFetchSync
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator.Message
import com.stripe.android.financialconnections.features.common.getBusinessName
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.model.LinkBrand
import com.stripe.android.financialconnections.navigation.Destination
import com.stripe.android.financialconnections.navigation.topappbar.TopAppBarStateUpdate
import com.stripe.android.financialconnections.presentation.Async
import com.stripe.android.financialconnections.presentation.Async.Uninitialized
import com.stripe.android.financialconnections.presentation.FinancialConnectionsViewModel
import com.stripe.android.financialconnections.ui.TextResource
import com.stripe.android.uicore.navigation.NavigationManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.launch

internal class ExitViewModel @AssistedInject constructor(
    @Assisted initialState: ExitState,
    nativeAuthFlowCoordinator: NativeAuthFlowCoordinator,
    private val getOrFetchSync: GetOrFetchSync,
    private val coordinator: NativeAuthFlowCoordinator,
    private val eventTracker: FinancialConnectionsAnalyticsTracker,
    private val navigationManager: NavigationManager,
    private val logger: Logger
) : FinancialConnectionsViewModel<ExitState>(initialState, nativeAuthFlowCoordinator) {

    init {
        logErrors()
        suspend {
            val manifest = kotlin.runCatching { getOrFetchSync().manifest }.getOrNull()
            val businessName = manifest?.getBusinessName()
            val isNetworkingSignupPane =
                manifest?.isNetworkingUserFlow == true && stateFlow.value.referrer == Pane.NETWORKING_LINK_SIGNUP_PANE
            // Safe default: manifest is null only on fetch failure, and Link is the original brand.
            val linkBrand = manifest?.linkBrand ?: LinkBrand.Link
            val description = when {
                isNetworkingSignupPane -> when (businessName) {
                    null -> if (linkBrand == LinkBrand.Link) {
                        TextResource.StringId(R.string.stripe_close_dialog_networking_desc_no_business)
                    } else {
                        TextResource.StringId(
                            value = R.string.stripe_close_dialog_networking_desc_no_business_with_brand,
                            args = listOf(linkBrand.brandName())
                        )
                    }
                    else -> if (linkBrand == LinkBrand.Link) {
                        TextResource.StringId(
                            value = R.string.stripe_close_dialog_networking_desc,
                            args = listOf(businessName)
                        )
                    } else {
                        TextResource.StringId(
                            value = R.string.stripe_close_dialog_networking_desc_with_brand,
                            args = listOf(businessName, linkBrand.brandName())
                        )
                    }
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

    override fun updateTopAppBar(state: ExitState): TopAppBarStateUpdate? {
        return null
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

    @AssistedFactory
    interface Factory {
        fun create(initialState: ExitState): ExitViewModel
    }

    companion object {

        fun factory(parentComponent: FinancialConnectionsSheetNativeComponent, arguments: Bundle?) = viewModelFactory {
            initializer {
                parentComponent.exitViewModelFactory.create(ExitState(arguments))
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
