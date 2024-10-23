package com.stripe.android.connect.example.ui.features.accountonboarding

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AccountOnboardingExampleViewModel : ViewModel() {

    private val _state = MutableStateFlow(AccountOnboardingExampleState)
    val state: StateFlow<AccountOnboardingExampleState> = _state.asStateFlow()

    // public methods

    // private methods

    // state

    object AccountOnboardingExampleState
}
