package com.stripe.android.paymentsheet.ui

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.lifecycle.viewModelScope
import com.stripe.android.common.model.CommonConfiguration
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.flowcontroller.FlowControllerViewModel
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.uicore.utils.combineAsStateFlow
import kotlinx.coroutines.CoroutineScope
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
    private val arguments: StateFlow<Arguments?>,
    private val linkAccountHolder: LinkAccountHolder,
    private val coroutineScope: CoroutineScope,
) : SignupToLinkToggleInteractor {

    private val _isChecked = MutableStateFlow(false)

    override val state: StateFlow<SignupToLinkToggleInteractor.State> = combineAsStateFlow(
        arguments,
        _isChecked,
        linkAccountHolder.linkAccountInfo
    ) { arguments, isChecked, linkAccountInfo ->
        val shouldDisplay = arguments?.run {
            paymentMethodMetadata.linkState != null &&
                linkAccountInfo.account == null
        } ?: false

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

    data class Arguments(
        val paymentMethodMetadata: PaymentMethodMetadata,
        val configuration: CommonConfiguration,
        val appearance: PaymentSheet.Appearance,
        val initializationMode: PaymentElementLoader.InitializationMode,
    )

    companion object {
        fun create(
            flowControllerViewModel: FlowControllerViewModel,
        ): SignupToLinkToggleInteractor {
            return DefaultSignupToLinkToggleInteractor(
                arguments = combineAsStateFlow(
                    flowControllerViewModel.stateFlow,
                    flowControllerViewModel.configureRequest,
                ) { flowControllerState, configureRequest ->
                    if (flowControllerState != null && configureRequest != null) {
                        Arguments(
                            configuration = flowControllerState.paymentSheetState.config,
                            paymentMethodMetadata = flowControllerState.paymentSheetState.paymentMethodMetadata,
                            appearance = configureRequest.configuration.appearance,
                            initializationMode = configureRequest.initializationMode,
                        )
                    } else {
                        null
                    }
                },
                linkAccountHolder = flowControllerViewModel.flowControllerStateComponent.linkAccountHolder,
                coroutineScope = flowControllerViewModel.viewModelScope,
            )
        }
    }
}
