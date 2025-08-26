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
import com.stripe.android.crypto.onramp.model.OnrampCheckoutResult
import com.stripe.android.crypto.onramp.model.OnrampCollectPaymentResult
import com.stripe.android.crypto.onramp.model.OnrampConfiguration
import com.stripe.android.crypto.onramp.model.OnrampCreateCryptoPaymentTokenResult
import com.stripe.android.crypto.onramp.model.OnrampIdentityVerificationResult
import com.stripe.android.crypto.onramp.model.OnrampAttachKycResult
import com.stripe.android.crypto.onramp.model.OnrampLinkLookupResult
import com.stripe.android.crypto.onramp.model.OnrampRegisterUserResult
import com.stripe.android.crypto.onramp.model.OnrampSetWalletAddressResult
import com.stripe.android.crypto.onramp.model.OnrampAuthenticationResult
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

    private val _checkoutEvent = MutableStateFlow<CheckoutEvent?>(null)
    val checkoutEvent: StateFlow<CheckoutEvent?> = _checkoutEvent.asStateFlow()

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
                        contentOnPrimary = Color(0xFF222222),
                        borderSelected = Color.White
                    ),
                    style = LinkAppearance.Style.ALWAYS_DARK,
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
        _uiState.update { it.copy(screen = Screen.Loading, email = currentEmail, loadingMessage = "Checking user...") }

        val result = onrampCoordinator.hasLinkAccount(currentEmail)
        when (result) {
            is OnrampLinkLookupResult.Completed -> {
                if (result.hasLinkAccount) {
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
        _uiState.update { OnrampUiState(screen = Screen.EmailInput) }
    }

    fun clearMessage() {
        _message.value = null
    }

    fun onAuthenticationResult(result: OnrampAuthenticationResult) {
        when (result) {
            is OnrampAuthenticationResult.Completed -> {
                _message.value = "Authentication successful! You can now perform authenticated operations."
                _uiState.update {
                    it.copy(
                        screen = Screen.AuthenticatedOperations,
                        customerId = result.customerId
                    )
                }
            }
            is OnrampAuthenticationResult.Cancelled -> {
                _message.value = "Authentication cancelled, please try again"
            }
            is OnrampAuthenticationResult.Failed -> {
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

    fun onAuthorize(linkAuthIntentId: String) {
        _uiState.update { it.copy(linkAuthIntentId = linkAuthIntentId) }
    }

    fun onAuthorizeResult(result: OnrampAuthorizeResult) {
        when (result) {
            is OnrampAuthorizeResult.Consented -> {
                _message.value = "Authorization successful! User consented to scopes."
                _uiState.update {
                    it.copy(
                        screen = Screen.AuthenticatedOperations,
                        customerId = result.customerId,
                        linkAuthIntentId = null,
                        consentedLinkAuthIntentIds = it.consentedLinkAuthIntentIds + it.linkAuthIntentId!!
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

    fun onCheckoutResult(result: OnrampCheckoutResult) {
        when (result) {
            is OnrampCheckoutResult.Completed -> {
                _message.value = "Checkout completed successfully!"
                _uiState.update { it.copy(screen = Screen.AuthenticatedOperations, loadingMessage = null) }
                // The session will be automatically updated with the latest status from the backend
            }
            is OnrampCheckoutResult.Canceled -> {
                _message.value = "Checkout was canceled by the user"
                _uiState.update { it.copy(screen = Screen.AuthenticatedOperations, loadingMessage = null) }
            }
            is OnrampCheckoutResult.Failed -> {
                _message.value = "Checkout failed: ${result.error.message}"
                _uiState.update { it.copy(screen = Screen.AuthenticatedOperations, loadingMessage = null) }
            }
        }
    }

    suspend fun checkoutWithBackend(sessionId: String): String {
        _uiState.update { it.copy(loadingMessage = "Calling test backend checkout...") }

        val currentState = _uiState.value
        val authToken = currentState.authToken
            ?: throw IllegalStateException("No authentication token available")

        val result = testBackendRepository.checkout(
            cosId = sessionId,
            authToken = authToken
        )

        return when (result) {
            is Result.Success -> {
                val checkoutResponse = result.value
                // Update the UI state with the updated session response
                _uiState.update {
                    it.copy(onrampSession = checkoutResponse)
                }
                checkoutResponse.clientSecret
            }
            is Result.Failure -> {
                throw IllegalStateException("Backend checkout failed: ${result.error.message}")
            }
        }
    }

    fun registerNewLinkUser(userInfo: LinkUserInfo) {
        viewModelScope.launch {
            val result = onrampCoordinator.registerLinkUser(userInfo)
            when (result) {
                is OnrampRegisterUserResult.Completed -> {
                    _message.value = "Registration successful"
                    _uiState.update {
                        it.copy(
                            screen = Screen.Authentication,
                            email = userInfo.email
                        )
                    }
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

            _uiState.update { it.copy(screen = Screen.Loading, loadingMessage = "Registering wallet address...") }
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
        _uiState.update { it.copy(screen = Screen.Loading, loadingMessage = "Collecting KYC info...") }

        viewModelScope.launch {
            val result = onrampCoordinator.attachKycInfo(kycInfo)

            when (result) {
                is OnrampAttachKycResult.Completed -> {
                    _message.value = "KYC Collection successful"
                    _uiState.update { it.copy(screen = Screen.AuthenticatedOperations) }
                }
                is OnrampAttachKycResult.Failed -> {
                    _message.value = "KYC Collection failed: ${result.error.message}"
                    _uiState.update { it.copy(screen = Screen.AuthenticatedOperations) }
                }
            }
        }
    }

    fun createCryptoPaymentToken() {
        _uiState.update { it.copy(screen = Screen.Loading, loadingMessage = "Creating crypto payment token...") }

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

    fun createSession() {
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

        _uiState.update { it.copy(screen = Screen.Loading, loadingMessage = "Creating session...") }

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
                            onrampSession = response,
                            screen = Screen.AuthenticatedOperations,
                            loadingMessage = null
                        )
                    }
                }
                is Result.Failure -> {
                    _message.value = "Failed to create onramp session: ${result.error.message}"
                    _uiState.update { it.copy(screen = Screen.AuthenticatedOperations, loadingMessage = null) }
                }
            }
        }
    }

    fun performCheckout() {
        val currentState = _uiState.value
        val onrampSession = currentState.onrampSession
            ?: run {
                _message.value = "No onramp session available. Please create a session first."
                return
            }

        val cryptoPaymentToken = currentState.cryptoPaymentToken
            ?: run {
                _message.value = "No crypto payment token available. Please create a payment token first."
                return
            }

        _uiState.update { it.copy(screen = Screen.Loading, loadingMessage = "Performing checkout...") }

        _checkoutEvent.value = CheckoutEvent(
            cryptoPaymentToken = cryptoPaymentToken,
            sessionId = onrampSession.id,
            sessionClientSecret = onrampSession.clientSecret
        )
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

    fun clearCheckoutEvent() {
        _checkoutEvent.value = null
    }

    suspend fun createLinkAuthIntent(oauthScopes: String): String? {
        val currentState = _uiState.value
        val email = currentState.email
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
    val linkAuthIntentId: String? = null,
    val consentedLinkAuthIntentIds: List<String> = emptyList(),
    val customerId: String? = null,
    val selectedPaymentData: PaymentOptionDisplayData? = null,
    val cryptoPaymentToken: String? = null,
    val walletAddress: String? = null,
    val network: CryptoNetwork? = null,
    val authToken: String? = null,
    val onrampSession: OnrampSessionResponse? = null,
    val loadingMessage: String? = null,
)

enum class Screen {
    EmailInput,
    Loading,
    Registration,
    Authentication,
    AuthenticatedOperations,
}

data class CheckoutEvent(
    val cryptoPaymentToken: String,
    val sessionId: String,
    val sessionClientSecret: String
)
