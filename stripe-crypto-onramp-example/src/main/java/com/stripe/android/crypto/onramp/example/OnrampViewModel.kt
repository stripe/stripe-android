package com.stripe.android.crypto.onramp.example

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.github.kittinunf.result.Result
import com.stripe.android.core.utils.requireApplication
import com.stripe.android.crypto.onramp.OnrampCoordinator
import com.stripe.android.crypto.onramp.example.network.OnrampSessionResponse
import com.stripe.android.crypto.onramp.example.network.TestBackendRepository
import com.stripe.android.crypto.onramp.model.CryptoNetwork
import com.stripe.android.crypto.onramp.model.KycInfo
import com.stripe.android.crypto.onramp.model.LinkUserInfo
import com.stripe.android.crypto.onramp.model.OnrampAuthorizeResult
import com.stripe.android.crypto.onramp.model.OnrampCollectPaymentResult
import com.stripe.android.crypto.onramp.model.OnrampConfiguration
import com.stripe.android.crypto.onramp.model.OnrampCreateCryptoPaymentTokenResult
import com.stripe.android.crypto.onramp.model.OnrampIdentityVerificationResult
import com.stripe.android.crypto.onramp.model.OnrampKYCResult
import com.stripe.android.crypto.onramp.model.OnrampLinkLookupResult
import com.stripe.android.crypto.onramp.model.OnrampRegisterUserResult
import com.stripe.android.crypto.onramp.model.OnrampSetWalletAddressResult
import com.stripe.android.crypto.onramp.model.OnrampVerificationResult
import com.stripe.android.crypto.onramp.model.PaymentOptionDisplayData
import com.stripe.android.link.LinkAppearance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Suppress("TooManyFunctions")
internal class OnrampViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    val onrampCoordinator: OnrampCoordinator =
        OnrampCoordinator
            .Builder()
            .build(application, savedStateHandle)

    private val testBackendRepository = TestBackendRepository()

    private val _uiState = MutableStateFlow(OnrampUiState())
    val uiState: StateFlow<OnrampUiState> = _uiState.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        viewModelScope.launch {
            @Suppress("MagicNumber", "MaxLineLength")
            val configuration = OnrampConfiguration(
                merchantDisplayName = "Onramp Example",
                publishableKey = "pk_test_51K9W3OHMaDsveWq0oLP0ZjldetyfHIqyJcz27k2BpMGHxu9v9Cei2tofzoHncPyk3A49jMkFEgTOBQyAMTUffRLa00xzzARtZO",
                appearance = LinkAppearance(
                    lightColors = LinkAppearance.Colors(
                        primary = Color.Blue,
                        contentOnPrimary = Color.White,
                        borderSelected = Color.Red
                    ),
                    darkColors = LinkAppearance.Colors(
                        primary = Color(0xFF9886E6),
                        contentOnPrimary = Color.White,
                        borderSelected = Color.White
                    ),
                    style = LinkAppearance.Style.AUTOMATIC,
                    primaryButton = LinkAppearance.PrimaryButton()
                )
            )

            onrampCoordinator.configure(configuration = configuration)
            // Set initial state to EmailInput after configuration
            _uiState.update { it.copy(screen = Screen.EmailInput) }
        }
    }

    fun checkIfLinkUser(email: String) = viewModelScope.launch {
        if (email.isBlank()) {
            _message.value = "Please enter an email address"
            return@launch
        }

        val currentEmail = email.trim()
        _uiState.update { it.copy(screen = Screen.Loading, email = currentEmail) }

        val result = onrampCoordinator.lookupLinkUser(currentEmail)
        when (result) {
            is OnrampLinkLookupResult.Completed -> {
                if (result.isLinkUser) {
                    _message.value = "User exists in Link. Please authenticate"
                    _uiState.update { it.copy(screen = Screen.Authentication) }
                } else {
                    _message.value = "User does not exist in Link. Please register"
                    _uiState.update { it.copy(screen = Screen.Registration) }
                }
            }
            is OnrampLinkLookupResult.Failed -> {
                _message.value = "Lookup failed: ${result.error.message}"
                _uiState.update { it.copy(screen = Screen.EmailInput) }
            }
        }
    }

    fun onBackToEmailInput() {
        _uiState.update { OnrampUiState() }
    }

    fun clearMessage() {
        _message.value = null
    }

    fun onAuthenticationResult(result: OnrampVerificationResult) {
        when (result) {
            is OnrampVerificationResult.Completed -> {
                _message.value = "Authentication successful! You can now perform authenticated operations."
                _uiState.update {
                    it.copy(
                        screen = Screen.AuthenticatedOperations,
                        customerId = result.customerId
                    )
                }
            }
            is OnrampVerificationResult.Cancelled -> {
                _message.value = "Authentication cancelled, please try again"
            }
            is OnrampVerificationResult.Failed -> {
                _message.value = "Authentication failed: ${result.error.message}"
                _uiState.update { it.copy(screen = Screen.EmailInput) }
            }
        }
    }

    fun onIdentityVerificationResult(result: OnrampIdentityVerificationResult) {
        when (result) {
            is OnrampIdentityVerificationResult.Completed -> {
                _message.value = "Identity Verification completed"
                _uiState.update { it.copy(screen = Screen.AuthenticatedOperations) }
            }
            is OnrampIdentityVerificationResult.Cancelled -> {
                _message.value = "Identity Verification cancelled, please try again"
            }
            is OnrampIdentityVerificationResult.Failed -> {
                _message.value = "Identity Verification failed: ${result.error.message}"
                _uiState.update { it.copy(screen = Screen.EmailInput) }
            }
        }
    }

    fun onSelectPaymentResult(result: OnrampCollectPaymentResult) {
        when (result) {
            is OnrampCollectPaymentResult.Completed -> {
                _message.value = "Payment selection completed"
                _uiState.update {
                    it.copy(
                        screen = Screen.AuthenticatedOperations,
                        selectedPaymentData = result.displayData,
                    )
                }
            }
            is OnrampCollectPaymentResult.Cancelled -> {
                _message.value = "Payment selection cancelled, please try again"
            }
            is OnrampCollectPaymentResult.Failed -> {
                _message.value = "Payment selection failed: ${result.error.message}"
                _uiState.update { it.copy(screen = Screen.AuthenticatedOperations) }
            }
        }
    }

    fun onAuthorizeResult(result: OnrampAuthorizeResult) {
        when (result) {
            is OnrampAuthorizeResult.Consented -> {
                _message.value = "Authorization successful! User consented to scopes."
                _uiState.update {
                    it.copy(
                        screen = Screen.AuthenticatedOperations,
                        customerId = result.customerId
                    )
                }
            }
            is OnrampAuthorizeResult.Denied -> {
                _message.value = "Authorization denied by user."
            }
            is OnrampAuthorizeResult.Canceled -> {
                _message.value = "Authorization cancelled by user."
            }
            is OnrampAuthorizeResult.Failed -> {
                _message.value = "Authorization failed: ${result.error.message}"
            }
        }
    }

    fun registerNewLinkUser(userInfo: LinkUserInfo) {
        viewModelScope.launch {
            val result = onrampCoordinator.registerLinkUser(userInfo)
            when (result) {
                is OnrampRegisterUserResult.Completed -> {
                    _message.value = "Registration successful"
                    _uiState.update { it.copy(screen = Screen.EmailInput) }
                }
                is OnrampRegisterUserResult.Failed -> {
                    _message.value = "Registration failed: ${result.error.message}"
                    _uiState.update { it.copy(screen = Screen.EmailInput) }
                }
            }
        }
    }

    fun registerWalletAddress(walletAddress: String, network: CryptoNetwork) {
        viewModelScope.launch {
            if (walletAddress.isBlank()) {
                _message.value = "Please enter a wallet address"
                return@launch
            }

            _uiState.update { it.copy(screen = Screen.Loading) }
            val result = onrampCoordinator.registerWalletAddress(walletAddress.trim(), network)
            when (result) {
                is OnrampSetWalletAddressResult.Completed -> {
                    _message.value = "Wallet address registered successfully!"
                    _uiState.update {
                        it.copy(
                            screen = Screen.AuthenticatedOperations,
                            walletAddress = walletAddress.trim(),
                            network = network
                        )
                    }
                }
                is OnrampSetWalletAddressResult.Failed -> {
                    _message.value = "Failed to register wallet address: ${result.error.message}"
                    _uiState.update { it.copy(screen = Screen.AuthenticatedOperations) }
                }
            }
        }
    }

    fun collectKycInfo(kycInfo: KycInfo) {
        _uiState.update { it.copy(screen = Screen.Loading) }

        viewModelScope.launch {
            val result = onrampCoordinator.collectKycInfo(kycInfo)

            when (result) {
                is OnrampKYCResult.Completed -> {
                    _message.value = "KYC Collection successful"
                    _uiState.update { it.copy(screen = Screen.EmailInput) }
                }
                is OnrampKYCResult.Failed -> {
                    _message.value = "KYC Collection failed: ${result.error.message}"
                    _uiState.update { it.copy(screen = Screen.AuthenticatedOperations) }
                }
            }
        }
    }

    fun createCryptoPaymentToken() {
        _uiState.update { it.copy(screen = Screen.Loading) }

        viewModelScope.launch {
            val result = onrampCoordinator.createCryptoPaymentToken()

            when (result) {
                is OnrampCreateCryptoPaymentTokenResult.Completed -> {
                    _message.value = "Created crypto payment token: ${result.cryptoPaymentToken}"
                    _uiState.update {
                        it.copy(
                            screen = Screen.AuthenticatedOperations,
                            cryptoPaymentToken = result.cryptoPaymentToken,
                        )
                    }
                }
                is OnrampCreateCryptoPaymentTokenResult.Failed -> {
                    _message.value = "Failed to create crypto payment token: ${result.error.message}"
                    _uiState.update { it.copy(screen = Screen.AuthenticatedOperations) }
                }
            }
        }
    }

    fun createOnrampSession() {
        val currentState = _uiState.value
        val paymentToken = currentState.cryptoPaymentToken
        val walletAddress = currentState.walletAddress
        val customerId = currentState.customerId
        val network = currentState.network
        val authToken = currentState.authToken

        // Check what's missing and provide helpful guidance
        val validParams = validateOnrampSessionParams(
            customerId = customerId,
            walletAddress = walletAddress,
            currentState = currentState,
            paymentToken = paymentToken,
            authToken = authToken
        )
        if (validParams.not()) return

        _uiState.update { it.copy(screen = Screen.Loading) }

        viewModelScope.launch {
            val destinationNetwork = network?.value ?: "ethereum"

            val result = testBackendRepository.createOnrampSession(
                paymentToken = paymentToken!!,
                walletAddress = walletAddress!!,
                cryptoCustomerId = customerId!!,
                authToken = authToken!!,
                destinationNetwork = destinationNetwork
            )

            when (result) {
                is Result.Success -> {
                    val response = result.value
                    _message.value = "Onramp session created successfully! Session ID: ${response.id}"
                    _uiState.update {
                        it.copy(
                            screen = Screen.AuthenticatedOperations,
                            onrampSession = response
                        )
                    }
                }
                is Result.Failure -> {
                    _message.value = "Failed to create onramp session: ${result.error.message}"
                    _uiState.update { it.copy(screen = Screen.AuthenticatedOperations) }
                }
            }
        }
    }

    private fun validateOnrampSessionParams(
        customerId: String?,
        walletAddress: String?,
        currentState: OnrampUiState,
        paymentToken: String?,
        authToken: String?
    ): Boolean {
        val missingItems = mutableListOf<String>()
        if (customerId.isNullOrBlank()) missingItems.add("customer authentication")
        if (walletAddress.isNullOrBlank()) missingItems.add("wallet address registration")
        if (currentState.selectedPaymentData == null) missingItems.add("payment method selection")
        if (paymentToken.isNullOrBlank()) missingItems.add("crypto payment token creation")
        if (authToken.isNullOrBlank()) missingItems.add("authentication token")
        if (missingItems.isNotEmpty()) {
            val message = when (missingItems.size) {
                1 -> "Please complete ${missingItems[0]} first"
                2 -> "Please complete ${missingItems[0]} and ${missingItems[1]} first"
                else -> "Please complete the following steps first: ${missingItems.joinToString(", ")}"
            }
            _message.value = message
            return false
        }
        return true
    }

    suspend fun createLinkAuthIntent(oauthScopes: String): String? {
        val currentState = _uiState.value
        val email = currentState.email.takeIf { currentState.screen == Screen.Authentication }
            ?: return null

        return createAuthIntentForUser(
            email = email,
            oauthScopes = oauthScopes
        )
    }

    private suspend fun createAuthIntentForUser(email: String, oauthScopes: String): String? {
        val result = testBackendRepository.createAuthIntent(email = email, oauthScopes = oauthScopes)
        when (result) {
            is Result.Success -> {
                val response = result.value
                _uiState.update { it.copy(authToken = response.token) }
                _message.value = "Auth intent created successfully"
                return response.data.id
            }
            is Result.Failure -> {
                _message.value = "Failed to create auth intent: ${result.error.message}"
                return null
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
            return OnrampViewModel(application, extras.createSavedStateHandle()) as T
        }
    }
}

data class OnrampUiState(
    val screen: Screen = Screen.Loading,
    val email: String = "",
    val customerId: String? = null,
    val selectedPaymentData: PaymentOptionDisplayData? = null,
    val cryptoPaymentToken: String? = null,
    val walletAddress: String? = null,
    val network: CryptoNetwork? = null,
    val authToken: String? = null,
    val onrampSession: OnrampSessionResponse? = null,
)

enum class Screen {
    EmailInput,
    Loading,
    Registration,
    Authentication,
    AuthenticatedOperations,
}
