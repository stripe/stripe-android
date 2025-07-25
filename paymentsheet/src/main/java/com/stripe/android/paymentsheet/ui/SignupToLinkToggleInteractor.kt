package com.stripe.android.paymentsheet.ui

import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.flowcontroller.DefaultFlowController
import com.stripe.android.uicore.utils.combineAsStateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal interface SignupToLinkToggleInteractor {
    val state: StateFlow<PaymentSheet.LinkSignupOptInState>
    val toggleValue: MutableStateFlow<Boolean>

    fun getSignupToLinkValue(): Boolean
}

internal class DefaultSignupToLinkToggleInteractor(
    flowControllerState: StateFlow<DefaultFlowController.State?>,
    linkAccountHolder: LinkAccountHolder,
    private val stringProvider: SignupToLinkToggleStringProvider,
) : SignupToLinkToggleInteractor {

    private val _isChecked = MutableStateFlow(false)
    private var hasInitialized = false

    override val toggleValue: MutableStateFlow<Boolean> = _isChecked

    override val state: StateFlow<PaymentSheet.LinkSignupOptInState> = combineAsStateFlow(
        flowControllerState,
        _isChecked,
        linkAccountHolder.linkAccountInfo
    ) { state, isChecked, linkAccountInfo ->
        val paymentMethodMetadata = state?.paymentSheetState?.paymentMethodMetadata
        val linkConfiguration = paymentMethodMetadata?.linkState?.configuration
        val hasLinkSPMs = state?.hasLinkPMs() == true

        setToggleInitialValue(linkConfiguration)

        val shouldDisplay = listOf(
            // Link is enabled
            linkConfiguration != null,
            // Already has Link SPMs
            hasLinkSPMs.not(),
            // We don't recognize the Link account
            linkAccountInfo.account == null,
            // Feature flag to enable new user signup API
            linkConfiguration?.enableNewUserSignupAPI == true
        ).all { it }

        if (shouldDisplay) {
            PaymentSheet.LinkSignupOptInState.Visible(
                title = stringProvider.title,
                description = stringProvider.description,
                termsAndConditions = stringProvider.termsAndConditions
            )
        } else {
            PaymentSheet.LinkSignupOptInState.Hidden
        }
    }

    private fun setToggleInitialValue(linkConfiguration: LinkConfiguration?) {
        if (!hasInitialized && linkConfiguration != null) {
            _isChecked.value = linkConfiguration.newUserSignupInitialValue
            hasInitialized = true
        }
    }

    private fun DefaultFlowController.State.hasLinkPMs(): Boolean? =
        paymentSheetState.customer?.paymentMethods
            ?.any { it.isLinkPaymentMethod || it.isLinkPassthroughMode }

    override fun getSignupToLinkValue(): Boolean {
        return toggleValue.value
    }
}
