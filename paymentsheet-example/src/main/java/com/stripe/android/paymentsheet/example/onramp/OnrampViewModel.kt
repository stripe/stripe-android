package com.stripe.android.paymentsheet.example.onramp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.stripe.android.core.utils.requireApplication
import com.stripe.android.crypto.onramp.OnrampCoordinator
import com.stripe.android.crypto.onramp.model.LinkUserInfo
import com.stripe.android.crypto.onramp.model.OnrampConfiguration
import com.stripe.android.crypto.onramp.model.OnrampLinkLookupResult
import com.stripe.android.crypto.onramp.model.OnrampRegisterUserResult
import com.stripe.android.crypto.onramp.model.OnrampVerificationResult
import com.stripe.android.paymentsheet.PaymentSheet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class OnrampViewModel(
    application: Application,
) : AndroidViewModel(application) {

    val onrampCoordinator = OnrampCoordinator.Builder().build(application)

    private val _uiState = MutableStateFlow<OnrampUiState>(OnrampUiState.Loading)
    val uiState: StateFlow<OnrampUiState> = _uiState.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private var currentEmail: String = ""

    init {
        val configuration = OnrampConfiguration(
            paymentSheetAppearance = PaymentSheet.Appearance()
        )
        viewModelScope.launch {
            onrampCoordinator.configure(configuration)
            // Set initial state to EmailInput after configuration
            _uiState.value = OnrampUiState.EmailInput
        }
    }

    fun checkIfLinkUser(email: String) = viewModelScope.launch {
        if (email.isBlank()) {
            _message.value = "Please enter an email address"
            return@launch
        }

        currentEmail = email.trim()
        _uiState.value = OnrampUiState.Loading
        val result = onrampCoordinator.isLinkUser(currentEmail)
        when (result) {
            is OnrampLinkLookupResult.Completed -> {
                if (result.isLinkUser) {
                    _message.value = "User exists in Link. Please authenticate:"
                    _uiState.value = OnrampUiState.Authentication(currentEmail)
                } else {
                    _message.value = "User does not exist in Link. Please register:"
                    _uiState.value = OnrampUiState.Registration(currentEmail)
                }
            }
            is OnrampLinkLookupResult.Failed -> {
                _message.value = "Lookup failed: ${result.error.message}"
                _uiState.value = OnrampUiState.EmailInput
            }
        }
    }

    fun onBackToEmailInput() {
        _uiState.value = OnrampUiState.EmailInput
    }

    fun clearMessage() {
        _message.value = null
    }

    fun onAuthenticationResult(result: OnrampVerificationResult) {
        when (result) {
            is OnrampVerificationResult.Completed -> {
                _message.value = "Authentication successful"
                _uiState.value = OnrampUiState.EmailInput
            }
            is OnrampVerificationResult.Cancelled -> {
                _message.value = "Authentication cancelled, please try again"
            }
            is OnrampVerificationResult.Failed -> {
                _message.value = "Authentication failed: ${result.error.message}"
                _uiState.value = OnrampUiState.EmailInput
            }
        }
    }

    fun registerNewLinkUser(userInfo: LinkUserInfo) {
        viewModelScope.launch {
            val result = onrampCoordinator.registerNewLinkUser(userInfo)
            when (result) {
                is OnrampRegisterUserResult.Completed -> {
                    _message.value = "Registration successful"
                    _uiState.value = OnrampUiState.EmailInput
                }
                is OnrampRegisterUserResult.Failed -> {
                    _message.value = "Registration failed: ${result.error.message}"
                    _uiState.value = OnrampUiState.EmailInput
                }
            }
        }
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(
            modelClass: Class<T>,
            extras: CreationExtras
        ): T {
            val application = extras.requireApplication()
            return OnrampViewModel(application) as T
        }
    }
}

internal sealed class OnrampUiState {
    object EmailInput : OnrampUiState()
    object Loading : OnrampUiState()
    data class Registration(val email: String) : OnrampUiState()
    data class Authentication(val email: String) : OnrampUiState()
}
