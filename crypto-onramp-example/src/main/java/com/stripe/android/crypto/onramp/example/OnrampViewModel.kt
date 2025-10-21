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
import com.stripe.android.crypto.onramp.model.OnrampAttachKycInfoResult
import com.stripe.android.crypto.onramp.model.OnrampAuthenticateResult
import com.stripe.android.crypto.onramp.model.OnrampAuthorizeResult
import com.stripe.android.crypto.onramp.model.OnrampCheckoutResult
import com.stripe.android.crypto.onramp.model.OnrampCollectPaymentMethodResult
import com.stripe.android.crypto.onramp.model.OnrampConfiguration
import com.stripe.android.crypto.onramp.model.OnrampCreateCryptoPaymentTokenResult
import com.stripe.android.crypto.onramp.model.OnrampHasLinkAccountResult
import com.stripe.android.crypto.onramp.model.OnrampLogOutResult
import com.stripe.android.crypto.onramp.model.OnrampRegisterLinkUserResult
import com.stripe.android.crypto.onramp.model.OnrampRegisterWalletAddressResult
import com.stripe.android.crypto.onramp.model.OnrampUpdatePhoneNumberResult
import com.stripe.android.crypto.onramp.model.OnrampVerifyIdentityResult
import com.stripe.android.crypto.onramp.model.PaymentMethodDisplayData
import com.stripe.android.link.LinkAppearance
import com.stripe.android.link.utils.isLinkAuthorizationError
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

    private val _authorizeEvent = MutableStateFlow<AuthorizeEvent?>(null)
    val authorizeEvent: StateFlow<AuthorizeEvent?> = _authorizeEvent.asStateFlow()

    private fun handleError(error: Throwable, onNonAuthError: () -> Unit = {}) {
        if (error.isLinkAuthorizationError()) {
            _message.value = "Session expired. Reauthorizing..."
            _uiState.update { it.copy(screen = Screen.Authentication) }

            // Use the existing consented link auth intent ID to retrigger authorization
            val currentState = _uiState.value
            val linkAuthIntentId = currentState.consentedLinkAuthIntentIds.firstOrNull()

            if (linkAuthIntentId != null) {
                _authorizeEvent.value = AuthorizeEvent(linkAuthIntentId)
            } else {
                _message.value = "Session expired. Please reauthenticate."
            }
        } else {
            onNonAuthError()
        }
    }

    fun clearAuthorizeEvent() {
        _authorizeEvent.value = null
    }

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
            // Set initial state to LoginSignup after configuration
            _uiState.update { it.copy(screen = Screen.LoginSignup) }
        }
    }

    fun registerUser(email: String, password: String) = viewModelScope.launch {
        if (email.isBlank()) {
            _message.value = "Please enter an email address"
            return@launch
        }

        if (password.isBlank()) {
            _message.value = "Please enter an valid password"
            return@launch
        }

        val currentEmail = email.trim()
        _uiState.update { it.copy(screen = Screen.Loading, email = currentEmail, loadingMessage = "Registering...") }

        val result = testBackendRepository.signUp(currentEmail, password, false)
        when (result) {
            is Result.Success -> {
                val response = result.value
                if (response.success) {
                    _message.value = "Sign up successful!"

                    _uiState.update { it.copy(authToken = response.token) }
                    checkIfLinkUser(currentEmail)
                } else {
                    _message.value = "Sign up failed: Unknown Error"
                    _uiState.update { it.copy(screen = Screen.LoginSignup, loadingMessage = null) }
                }
            }
            is Result.Failure -> {
                _message.value = "Sign up failed: ${result.error.message}"
                _uiState.update { it.copy(screen = Screen.LoginSignup, loadingMessage = null) }
            }
        }
    }

    fun loginUser(email: String, password: String) = viewModelScope.launch {
        if (email.isBlank()) {
            _message.value = "Please enter an email address"
            return@launch
        }

        if (password.isBlank()) {
            _message.value = "Please enter an valid password"
            return@launch
        }

        val currentEmail = email.trim()
        _uiState.update { it.copy(screen = Screen.Loading, email = currentEmail, loadingMessage = "Logging in...") }

        val result = testBackendRepository.logIn(currentEmail, password, false)
        when (result) {
            is Result.Success -> {
                val response = result.value
                if (response.success) {
                    _message.value = "Log in successful!"

                    _uiState.update { it.copy(authToken = response.token) }
                    checkIfLinkUser(currentEmail)
                } else {
                    _message.value = "Log in failed: Unknown Error"
                    _uiState.update { it.copy(screen = Screen.LoginSignup, loadingMessage = null) }
                }
            }
            is Result.Failure -> {
                _message.value = "Log in failed: ${result.error.message}"
                _uiState.update { it.copy(screen = Screen.LoginSignup, loadingMessage = null) }
            }
        }
    }

    private fun checkIfLinkUser(email: String) = viewModelScope.launch {
        if (email.isBlank()) {
            _message.value = "Please enter an email address"
            return@launch
        }

        val currentEmail = email.trim()
        _uiState.update { it.copy(screen = Screen.Loading, email = currentEmail, loadingMessage = "Checking user...") }

        val result = onrampCoordinator.hasLinkAccount(currentEmail)
        when (result) {
            is OnrampHasLinkAccountResult.Completed -> {
                if (result.hasLinkAccount) {
                    _message.value = "User exists in Link. Please authenticate"
                    _uiState.update { it.copy(screen = Screen.Authentication) }
                } else {
                    _message.value = "User does not exist in Link. Please register"
                    _uiState.update { it.copy(screen = Screen.Registration) }
                }
            }
            is OnrampHasLinkAccountResult.Failed -> {
                _message.value = "Lookup failed: ${result.error.message}"
                _uiState.update { it.copy(screen = Screen.LoginSignup) }
            }
        }
    }

    fun onBackToLoginSignup() {
        _uiState.update { OnrampUiState(screen = Screen.LoginSignup) }
    }

    fun clearMessage() {
        _message.value = null
    }

    fun onAuthenticateUserResult(result: OnrampAuthenticateResult) {
        when (result) {
            is OnrampAuthenticateResult.Completed -> {
                _message.value = "Authentication successful! You can now perform authenticated operations."
                _uiState.update {
                    it.copy(
                        screen = Screen.AuthenticatedOperations
                    )
                }
            }
            is OnrampAuthenticateResult.Cancelled -> {
                _message.value = "Authentication cancelled, please try again"
            }
            is OnrampAuthenticateResult.Failed -> {
                _message.value = "Authentication failed: ${result.error.message}"
                _uiState.update { it.copy(screen = Screen.LoginSignup) }
            }
        }
    }

    fun onVerifyIdentityResult(result: OnrampVerifyIdentityResult) {
        when (result) {
            is OnrampVerifyIdentityResult.Completed -> {
                _message.value = "Identity Verification completed"
                _uiState.update { it.copy(screen = Screen.AuthenticatedOperations) }
            }
            is OnrampVerifyIdentityResult.Cancelled -> {
                _message.value = "Identity Verification cancelled, please try again"
            }
            is OnrampVerifyIdentityResult.Failed -> {
                _message.value = "Identity Verification failed: ${result.error.message}"
                _uiState.update { it.copy(screen = Screen.LoginSignup) }
            }
        }
    }

    fun onCollectPaymentResult(result: OnrampCollectPaymentMethodResult) {
        when (result) {
            is OnrampCollectPaymentMethodResult.Completed -> {
                _message.value = "Payment selection completed"
                _uiState.update {
                    it.copy(
                        screen = Screen.AuthenticatedOperations,
                        selectedPaymentData = result.displayData,
                    )
                }
            }
            is OnrampCollectPaymentMethodResult.Cancelled -> {
                _message.value = "Payment selection cancelled, please try again"
            }
            is OnrampCollectPaymentMethodResult.Failed -> {
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
                is OnrampRegisterLinkUserResult.Completed -> {
                    _message.value = "Registration successful"
                    _uiState.update {
                        it.copy(
                            screen = Screen.Authentication,
                            email = userInfo.email
                        )
                    }
                }
                is OnrampRegisterLinkUserResult.Failed -> {
                    _message.value = "Registration failed: ${result.error.message}"
                    _uiState.update { it.copy(screen = Screen.LoginSignup) }
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
                is OnrampRegisterWalletAddressResult.Completed -> {
                    _message.value = "Wallet address registered successfully!"
                    _uiState.update {
                        it.copy(
                            screen = Screen.AuthenticatedOperations,
                            walletAddress = walletAddress.trim(),
                            network = network
                        )
                    }
                }
                is OnrampRegisterWalletAddressResult.Failed -> handleError(result.error) {
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
                is OnrampAttachKycInfoResult.Completed -> {
                    _message.value = "KYC Collection successful"
                    _uiState.update { it.copy(screen = Screen.AuthenticatedOperations) }
                }
                is OnrampAttachKycInfoResult.Failed -> handleError(result.error) {
                    _message.value = "KYC Collection failed: ${result.error.message}"
                    _uiState.update { it.copy(screen = Screen.AuthenticatedOperations) }
                }
            }
        }
    }

    fun updatePhoneNumber(phoneNumber: String) {
        if (phoneNumber.isBlank()) {
            _message.value = "Please enter a phone number"
            return
        }

        viewModelScope.launch {
            val currentScreen = _uiState.value.screen
            _uiState.update { it.copy(screen = Screen.Loading, loadingMessage = "Updating phone number...") }

            val result = onrampCoordinator.updatePhoneNumber(phoneNumber.trim())
            when (result) {
                is OnrampUpdatePhoneNumberResult.Completed -> {
                    _message.value = "Phone number updated successfully!"
                    _uiState.update { it.copy(screen = currentScreen) }
                }
                is OnrampUpdatePhoneNumberResult.Failed -> handleError(result.error) {
                    _message.value = "Failed to update phone number: ${result.error.message}"
                    _uiState.update { it.copy(screen = currentScreen) }
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
                is OnrampCreateCryptoPaymentTokenResult.Failed -> handleError(result.error) {
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
        val network = currentState.network
        val authToken = currentState.authToken

        // Check what's missing and provide helpful guidance
        val validParams = validateOnrampSessionParams(
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
        walletAddress: String?,
        currentState: OnrampUiState,
        paymentToken: String?,
        authToken: String?
    ): Boolean {
        val missingItems = mutableListOf<String>()
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
        return createAuthIntentForUser(
            oauthScopes = oauthScopes
        )
    }

    private suspend fun createAuthIntentForUser(oauthScopes: String): String? {
        val tokenWithoutLAI = _uiState.value.authToken
        if (tokenWithoutLAI == null) {
            _message.value = "No auth token found, please log in again"
            _uiState.update { OnrampUiState(screen = Screen.LoginSignup) }
            return null
        }

        val result = testBackendRepository.create(
            oauthScopes,
            tokenWithoutLAI = tokenWithoutLAI
        )

        when (result) {
            is Result.Success -> {
                val response = result.value
                _uiState.update { it.copy(authToken = response.token) }
                _message.value = "Auth intent created successfully"
                return response.authIntentId
            }
            is Result.Failure -> {
                _message.value = "Failed to create auth intent: ${result.error.message}"
                return null
            }
        }
    }

    fun logOut() {
        viewModelScope.launch {
            _uiState.update { it.copy(screen = Screen.Loading, loadingMessage = "Logging out...") }

            val result = onrampCoordinator.logOut()
            when (result) {
                is OnrampLogOutResult.Completed -> {
                    _message.value = "Successfully logged out"
                    _uiState.update { OnrampUiState(screen = Screen.LoginSignup) }
                }
                is OnrampLogOutResult.Failed -> {
                    _message.value = "Logout failed: ${result.error.message}"
                    _uiState.update { it.copy(screen = Screen.AuthenticatedOperations, loadingMessage = null) }
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
            return OnrampViewModel(application, extras.createSavedStateHandle()) as T
        }
    }
}

data class OnrampUiState(
    val screen: Screen = Screen.Loading,
    val email: String = "",
    val linkAuthIntentId: String? = null,
    val consentedLinkAuthIntentIds: List<String> = emptyList(),
    val selectedPaymentData: PaymentMethodDisplayData? = null,
    val cryptoPaymentToken: String? = null,
    val walletAddress: String? = null,
    val network: CryptoNetwork? = null,
    val authToken: String? = null,
    val onrampSession: OnrampSessionResponse? = null,
    val loadingMessage: String? = null,
)

enum class Screen {
    LoginSignup,
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

data class AuthorizeEvent(val linkAuthIntentId: String)
