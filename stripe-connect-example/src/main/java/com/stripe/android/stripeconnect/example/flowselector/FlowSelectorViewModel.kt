package com.stripe.android.stripeconnect.example.flowselector

import androidx.lifecycle.ViewModel
import com.stripe.android.stripeconnect.Appearance
import com.stripe.android.stripeconnect.StripeConnectComponent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FlowSelectorViewModel: ViewModel() {
    private val _uiState = MutableStateFlow(FlowSelectorState())
    val uiState: StateFlow<FlowSelectorState> = _uiState.asStateFlow()

    fun onSelectedFlowChanged(newFlow: StripeConnectComponent?) {
        _uiState.value = _uiState.value.copy(selectedFlow = newFlow)
    }

    fun onSelectedAccountChanged(newAccount: StripeAccount?) {
        _uiState.value = _uiState.value.copy(selectedAccount = newAccount)
    }

    fun onSelectedAppearanceChanged(newAppearance: Pair<Appearance?, String>?) {
        _uiState.value = _uiState.value.copy(selectedAppearance = newAppearance)
    }
}
