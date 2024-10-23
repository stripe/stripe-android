package com.stripe.android.connect.example.ui.features.payouts

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PayoutsExampleViewModel : ViewModel() {

    private val _state = MutableStateFlow(PayoutsExampleState)
    val state: StateFlow<PayoutsExampleState> = _state.asStateFlow()

    // public methods

    // private methods


    // state

    object PayoutsExampleState
}
