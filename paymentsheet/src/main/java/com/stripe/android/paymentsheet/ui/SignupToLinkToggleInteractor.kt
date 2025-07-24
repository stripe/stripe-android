package com.stripe.android.paymentsheet.ui

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.paymentsheet.flowcontroller.DefaultFlowController
import com.stripe.android.paymentsheet.flowcontroller.FlowControllerViewModel
import com.stripe.android.uicore.utils.combineAsStateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal interface SignupToLinkToggleInteractor {
    val state: StateFlow<State>

    @Immutable
    @Stable
    data class State(
        val shouldDisplay: Boolean,
        val title: String,
        val subtitle: String,
        val isChecked: Boolean,
    )

    fun handleToggleChange(checked: Boolean)
    fun getSignupToLinkValue(): Boolean
}

internal class DefaultSignupToLinkToggleInteractor(
    private val flowControllerState: StateFlow<DefaultFlowController.State?>,
    private val linkAccountHolder: LinkAccountHolder,
) : SignupToLinkToggleInteractor {

    private val _isChecked = MutableStateFlow(false)

    override val state: StateFlow<SignupToLinkToggleInteractor.State> = combineAsStateFlow(
        flowControllerState,
        _isChecked,
        linkAccountHolder.linkAccountInfo
    ) { state, isChecked, linkAccountInfo ->
        val metadata = state?.paymentSheetState?.paymentMethodMetadata
        val shouldDisplay = metadata?.linkState != null && linkAccountInfo.account == null

        SignupToLinkToggleInteractor.State(
            shouldDisplay = shouldDisplay,
            title = "Save my info for faster checkout with Link",
            subtitle = "Pay faster everywhere Link is accepted.",
            isChecked = isChecked,
        )
    }

    override fun handleToggleChange(checked: Boolean) {
        _isChecked.value = checked
    }

    override fun getSignupToLinkValue(): Boolean {
        return _isChecked.value
    }

    companion object {
        fun create(
            flowControllerViewModel: FlowControllerViewModel,
        ): SignupToLinkToggleInteractor {
            return DefaultSignupToLinkToggleInteractor(
                flowControllerState = flowControllerViewModel.stateFlow,
                linkAccountHolder = flowControllerViewModel.flowControllerStateComponent.linkAccountHolder,
            )
        }
    }
}
