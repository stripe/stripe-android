package com.stripe.android.paymentsheet.ui

import androidx.compose.ui.text.buildAnnotatedString
import com.stripe.android.paymentsheet.PaymentSheet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal class FakeSignupToLinkToggleInteractor : SignupToLinkToggleInteractor {

    private val _state = MutableStateFlow<PaymentSheet.LinkSignupOptInState>(
        PaymentSheet.LinkSignupOptInState.Visible(
            title = "Save my info for faster checkout with Link",
            description = "Pay faster everywhere Link is accepted.",
            termsAndConditions = buildAnnotatedString {
                append(
                    """
                    By saving my payment information to Link, I agree to Link's 
                    <terms>Terms</terms> and <privacy>Privacy Policy</privacy>.
                    """.trimIndent()
                )
            }
        )
    )

    override val state: StateFlow<PaymentSheet.LinkSignupOptInState> = _state

    private val _toggleValue = MutableStateFlow(false)
    override val toggleValue: MutableStateFlow<Boolean> = _toggleValue

    private var _signupToLinkValue = false

    override fun handleToggleChange(checked: Boolean) {
        _signupToLinkValue = checked
        _toggleValue.value = checked
    }

    override fun getSignupToLinkValue(): Boolean {
        return _signupToLinkValue
    }

    fun setSignupToLinkValue(value: Boolean) {
        _signupToLinkValue = value
        _toggleValue.value = value
    }

    fun setState(newState: PaymentSheet.LinkSignupOptInState) {
        _state.value = newState
    }
}
