package com.stripe.android.paymentsheet.ui

import android.app.Application
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.link.ui.replaceHyperlinks
import com.stripe.android.lpmfoundations.paymentmethod.WalletType
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.R
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
    application: Application,
) : SignupToLinkToggleInteractor {

    val title = application.getString(R.string.stripe_link_signup_toggle_title)
    val description = application.getString(R.string.stripe_link_signup_toggle_description)
    val termsAndConditions = AnnotatedString
        .fromHtml(application.getString(R.string.stripe_link_sign_up_terms).replaceHyperlinks())

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
                title = title,
                description = description,
                termsAndConditions = termsAndConditions
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
