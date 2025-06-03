package com.stripe.android.paymentsheet.example.playground.wallets

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * ViewModel for the WalletsButton demo screen.
 * Note: We don't actually use this class since we're using the existing PaymentSheetPlaygroundViewModel.
 * This is just a placeholder to satisfy imports in our UI class.
 */
class WalletsButtonViewModel(
    application: Application,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WalletsButtonUiState())
    val uiState: StateFlow<WalletsButtonUiState> = _uiState

    class Factory(
        private val application: Application,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return WalletsButtonViewModel(
                application = application
            ) as T
        }
    }
}

data class WalletsButtonUiState(
    val checkoutState: CheckoutState = CheckoutState.Loading,
    val paymentSheetResult: Any? = null
) {
    sealed class CheckoutState {
        object Loading : CheckoutState()
        object Success : CheckoutState()
        data class Error(val message: String) : CheckoutState()
    }
}