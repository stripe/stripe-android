package com.stripe.android.paymentsheet.example.onramp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stripe.android.crypto.onramp.model.LinkUserInfo
import com.stripe.android.crypto.onramp.model.OnrampConfigurationResult
import com.stripe.android.crypto.onramp.model.OnrampLinkLookupResult
import com.stripe.android.crypto.onramp.model.OnrampRegisterUserResult
import com.stripe.android.crypto.onramp.model.OnrampVerificationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class OnrampViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<OnrampUiState>(OnrampUiState.Loading)
    val uiState: StateFlow<OnrampUiState> = _uiState.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private var currentEmail: String = ""

    fun checkIfLinkUser(email: String, onCheckUser: suspend (String) -> Unit) {
        if (email.isBlank()) {
            _message.value = "Please enter an email address"
            return
        }

        currentEmail = email.trim()
        _uiState.value = OnrampUiState.Loading
        
        viewModelScope.launch {
            try {
                onCheckUser(currentEmail)
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                _message.value = "Lookup failed: ${e.message}"
                _uiState.value = OnrampUiState.EmailInput
            }
        }
    }

    fun registerNewUser(
        email: String,
        phone: String,
        country: String,
        fullName: String?,
        onRegister: suspend (LinkUserInfo) -> Unit
    ) {
        if (email.isBlank() || phone.isBlank() || country.isBlank()) {
            _message.value = "Please fill in all required fields"
            return
        }

        _uiState.value = OnrampUiState.Loading
        val userInfo = LinkUserInfo(
            email = email.trim(),
            phone = phone.trim(),
            country = country.trim(),
            fullName = fullName?.trim()?.takeIf { it.isNotEmpty() }
        )

        viewModelScope.launch {
            try {
                onRegister(userInfo)
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                _message.value = "Registration failed: ${e.message}"
                _uiState.value = OnrampUiState.Registration(email)
            }
        }
    }

    fun authenticateUser(email: String, onAuthenticate: suspend (String) -> Unit) {
        if (email.isBlank()) {
            _message.value = "Email is required for authentication"
            return
        }

        _uiState.value = OnrampUiState.Loading
        
        viewModelScope.launch {
            try {
                onAuthenticate(email.trim())
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                _message.value = "Authentication failed: ${e.message}"
                _uiState.value = OnrampUiState.Authentication(email)
            }
        }
    }

    fun onBackToEmailInput() {
        _uiState.value = OnrampUiState.EmailInput
    }

    fun clearMessage() {
        _message.value = null
    }

    // Handle configuration result
    fun onConfigurationResult(result: OnrampConfigurationResult) {
        when (result) {
            is OnrampConfigurationResult.Completed -> {
                if (result.success) {
                    _message.value = "Configuration successful"
                    _uiState.value = OnrampUiState.EmailInput
                } else {
                    _message.value = "Configuration failed"
                    _uiState.value = OnrampUiState.EmailInput // Still allow user to try
                }
            }
            is OnrampConfigurationResult.Failed -> {
                _message.value = "Configuration failed: ${result.error.message}"
                _uiState.value = OnrampUiState.EmailInput // Still allow user to try
            }
        }
    }

    // Handle lookup result
    fun onLookupResult(result: OnrampLinkLookupResult) {
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

    fun onAuthenticationResult(result: OnrampVerificationResult) {
        when (result) {
            is OnrampVerificationResult.Completed -> {
                _message.value = "Authentication successful - Customer ID: ${result.customerId}"
                _uiState.value = OnrampUiState.EmailInput
            }
            is OnrampVerificationResult.Cancelled -> {
                _message.value = "Authentication cancelled, please try again"
                _uiState.value = OnrampUiState.EmailInput
            }
            is OnrampVerificationResult.Failed -> {
                _message.value = "Authentication failed: ${result.error.message}"
                _uiState.value = OnrampUiState.EmailInput
            }
        }
    }

    fun onRegisterUserResult(result: OnrampRegisterUserResult) {
        when (result) {
            is OnrampRegisterUserResult.Completed -> {
                _message.value = "Registration successful - Customer ID: ${result.customerId}"
                _uiState.value = OnrampUiState.EmailInput
            }
            is OnrampRegisterUserResult.Failed -> {
                _message.value = "Registration failed: ${result.error.message}"
                _uiState.value = OnrampUiState.EmailInput
            }
        }
    }

    // Update registration state with email
    fun showRegistrationForEmail(email: String) {
        _uiState.value = OnrampUiState.Registration(email)
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return OnrampViewModel() as T
        }
    }
}

internal sealed class OnrampUiState {
    object EmailInput : OnrampUiState()
    object Loading : OnrampUiState()
    data class Registration(val email: String) : OnrampUiState()
    data class Authentication(val email: String) : OnrampUiState()
}
