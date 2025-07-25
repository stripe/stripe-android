package com.stripe.android.paymentsheet.ui

import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.lpmfoundations.paymentmethod.WalletType
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.flowcontroller.DefaultFlowController
import com.stripe.android.uicore.utils.combineAsStateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal interface SignupToLinkToggleInteractor {
    val state: StateFlow<PaymentSheet.LinkSignupOptInState>
    val toggleValue: MutableStateFlow<Boolean>

    fun handleToggleChange(checked: Boolean)
    fun getSignupToLinkValue(): Boolean
}

internal class DefaultSignupToLinkToggleInteractor(
    flowControllerState: StateFlow<DefaultFlowController.State?>,
    linkAccountHolder: LinkAccountHolder,
    private val stringProvider: SignupToLinkToggleStringProvider,
) : SignupToLinkToggleInteractor {

    private val _isChecked = MutableStateFlow(false)

    override val toggleValue: MutableStateFlow<Boolean> = _isChecked

    override val state: StateFlow<PaymentSheet.LinkSignupOptInState> = combineAsStateFlow(
        flowControllerState,
        _isChecked,
        linkAccountHolder.linkAccountInfo
    ) { state, isChecked, linkAccountInfo ->
        val paymentMethodMetadata = state?.paymentSheetState?.paymentMethodMetadata
        val linkState = paymentMethodMetadata?.linkState

        // We don't recognize the Link account
        val hasNoExistingAccount = linkAccountInfo.account == null
        // Shop Pay is available in the available wallets list
        val shopPayAvailable = paymentMethodMetadata?.availableWallets?.contains(WalletType.ShopPay) == true

        val shouldDisplay = linkState != null && hasNoExistingAccount && shopPayAvailable.not()

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

    override fun handleToggleChange(checked: Boolean) {
        toggleValue.value = checked
    }

    override fun getSignupToLinkValue(): Boolean {
        return toggleValue.value
    }
}
