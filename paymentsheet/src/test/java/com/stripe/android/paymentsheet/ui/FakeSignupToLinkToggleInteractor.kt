package com.stripe.android.paymentsheet.ui

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal class FakeSignupToLinkToggleInteractor : SignupToLinkToggleInteractor {

    private val _state = MutableStateFlow(
        SignupToLinkToggleInteractor.State(
            shouldDisplay = true,
            title = "Save my info for faster checkout with Link",
            subtitle = "Pay faster everywhere Link is accepted.",
            isChecked = false,
        )
    )

    override val state: StateFlow<SignupToLinkToggleInteractor.State> = _state

    private var _signupToLinkValue = false

    override fun handleToggleChange(checked: Boolean) {
        _signupToLinkValue = checked
        _state.value = _state.value.copy(isChecked = checked)
    }

    override fun getSignupToLinkValue(): Boolean {
        return _signupToLinkValue
    }

    fun setSignupToLinkValue(value: Boolean) {
        _signupToLinkValue = value
        _state.value = _state.value.copy(isChecked = value)
    }
}
