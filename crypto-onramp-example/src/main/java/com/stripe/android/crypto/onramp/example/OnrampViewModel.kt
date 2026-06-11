package com.stripe.android.crypto.onramp.example

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.result.Result
import com.stripe.android.core.utils.requireApplication
import com.stripe.android.crypto.onramp.OnrampCoordinator
import com.stripe.android.crypto.onramp.example.model.AuthorizeEvent
import com.stripe.android.crypto.onramp.example.model.CheckoutEvent
import com.stripe.android.crypto.onramp.example.model.IdentifierInputEntry
import com.stripe.android.crypto.onramp.example.model.KEY_UI_STATE
import com.stripe.android.crypto.onramp.example.model.OnrampUiState
import com.stripe.android.crypto.onramp.example.model.OnrampUserData
import com.stripe.android.crypto.onramp.example.model.Screen
import com.stripe.android.crypto.onramp.example.network.LoginSignUpResponse
import com.stripe.android.crypto.onramp.example.network.SettlementSpeed
import com.stripe.android.crypto.onramp.example.network.TestBackendRepository
import com.stripe.android.crypto.onramp.example.store.OnrampUserDataStore
import com.stripe.android.crypto.onramp.model.CryptoNetwork
import com.stripe.android.crypto.onramp.model.KycInfo
import com.stripe.android.crypto.onramp.model.LinkUserInfo
import com.stripe.android.crypto.onramp.model.OnrampAttachKycInfoResult
import com.stripe.android.crypto.onramp.model.OnrampAuthorizeResult
import com.stripe.android.crypto.onramp.model.OnrampCallbacks
import com.stripe.android.crypto.onramp.model.OnrampCheckoutResult
import com.stripe.android.crypto.onramp.model.OnrampCollectPaymentMethodResult
import com.stripe.android.crypto.onramp.model.OnrampCreateCryptoPaymentTokenResult
import com.stripe.android.crypto.onramp.model.OnrampUserAttestationResult
import com.stripe.android.crypto.onramp.model.OnrampHasLinkAccountResult
import com.stripe.android.crypto.onramp.model.OnrampLogOutResult
import com.stripe.android.crypto.onramp.model.OnrampRegisterLinkUserResult
import com.stripe.android.crypto.onramp.model.OnrampRegisterWalletAddressResult
import com.stripe.android.crypto.onramp.model.OnrampRetrieveMissingIdentifiersResult
import com.stripe.android.crypto.onramp.model.OnrampSubmitIdentifiersResult
import com.stripe.android.crypto.onramp.model.OnrampTokenAuthenticationResult
import com.stripe.android.crypto.onramp.model.OnrampUpdatePhoneNumberResult
import com.stripe.android.crypto.onramp.model.OnrampVerifyIdentityResult
import com.stripe.android.crypto.onramp.model.OnrampVerifyKycInfoResult
import com.stripe.android.crypto.onramp.model.PaymentMethodDisplayData
import com.stripe.android.crypto.onramp.model.compliance.ComplianceIdentifier
import com.stripe.android.crypto.onramp.model.compliance.ComplianceIdentifierAlternativeGroup
import com.stripe.android.crypto.onramp.model.compliance.ComplianceIdentifierRequirement
import com.stripe.android.crypto.onramp.model.compliance.ComplianceIdentifierRequirements
import com.stripe.android.crypto.onramp.model.compliance.ComplianceIdentifierType
import com.stripe.android.crypto.onramp.model.compliance.SubmitIdentifiersResult
import com.stripe.android.link.utils.isLinkAuthorizationError
import com.stripe.android.paymentsheet.PaymentSheet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Suppress("TooManyFunctions", "LargeClass")
internal class OnrampViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    internal val callbacks = OnrampCallbacks()
        .verifyIdentityCallback(callback = ::onVerifyIdentityResult)
        .verifyKycCallback(callback = ::onVerifyKycResult)
        .checkoutCallback(callback = ::onCheckoutResult)
        .collectPaymentCallback(callback = ::onCollectPaymentResult)
        .authorizeCallback(callback = ::onAuthorizeResult)
        .onrampSessionClientSecretProvider(callback = ::checkoutWithBackend)
        .googlePayIsReadyCallback(callback = ::googlePayIsReady)
        .userAttestationCallback(callback = ::onUserAttestationResult)

    val onrampCoordinator: OnrampCoordinator =
        OnrampCoordinator.Builder().build(getApplication(), savedStateHandle, callbacks)

    private val testBackendRepository = TestBackendRepository()
    private val userDataStore = OnrampUserDataStore(getApplication<Application>().applicationContext)

    private val savedUiState: OnrampUiState?
        get() = savedStateHandle[KEY_UI_STATE]

    private val _uiState = MutableStateFlow(savedUiState ?: OnrampUiState())
    val uiState: StateFlow<OnrampUiState> = _uiState.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _checkoutEvent = MutableStateFlow<CheckoutEvent?>(null)
    val checkoutEvent: StateFlow<CheckoutEvent?> = _checkoutEvent.asStateFlow()

    private val _authorizeEvent = MutableStateFlow<AuthorizeEvent?>(null)
    val authorizeEvent: StateFlow<AuthorizeEvent?> = _authorizeEvent.asStateFlow()

    private val _updateAddressEvent = MutableStateFlow(false)
    val updateAddressEvent: StateFlow<Boolean> = _updateAddressEvent.asStateFlow()

    private val minPasswordLength = 8

    init {
        viewModelScope.launch {
            onrampCoordinator.configure(OnrampConfigurationFactory.create())

            if (savedUiState == null) {
                val savedUser = userDataStore.load()
                _uiState.update { currentState ->
                    savedUser?.let {
                        currentState.copy(
                            email = it.email,
                            authToken = it.authToken,
                            screen = Screen.SeamlessSignIn
                        )
                    } ?: currentState.copy(screen = Screen.LoginSignup)
                }
            }

            _uiState.collect { state ->
                savedStateHandle[KEY_UI_STATE] = state
            }
        }
    }

    fun registerUser(email: String, password: String) = authenticateUser(
        email = email,
        password = password,
        loadingMessage = "Registering...",
        successMessage = "Sign up successful!",
        failureLabel = "Sign up",
        request = { currentEmail, currentPassword ->
            testBackendRepository.signUp(currentEmail, currentPassword, false)
        }
    )

    fun loginUser(email: String, password: String) = authenticateUser(
        email = email,
        password = password,
        loadingMessage = "Logging in...",
        successMessage = "Log in successful!",
        failureLabel = "Log in",
        request = { currentEmail, currentPassword ->
            testBackendRepository.logIn(currentEmail, currentPassword, false)
        }
    )

    private fun authenticateUser(
        email: String,
        password: String,
        loadingMessage: String,
        successMessage: String,
        failureLabel: String,
        request: suspend (String, String) -> Result<LoginSignUpResponse, FuelError>,
    ) = viewModelScope.launch {
        val trimmedEmail = validateEmail(email) ?: return@launch
        if (!validatePassword(password)) return@launch

        _uiState.update {
            it.copy(
                screen = Screen.Loading,
                email = trimmedEmail,
                loadingMessage = loadingMessage
            )
        }

        when (val result = request(trimmedEmail, password)) {
            is Result.Success -> {
                val response = result.value
                if (response.success) {
                    _message.value = successMessage
                    _uiState.update { it.copy(authToken = response.token) }
                    checkIfLinkUser(trimmedEmail)
                } else {
                    resetToLoginSignup(message = "$failureLabel failed: Unknown Error")
                }
            }
            is Result.Failure -> {
                resetToLoginSignup(message = "$failureLabel failed: ${result.error.message}")
            }
        }
    }

    fun seamlessSignInContinue() = viewModelScope.launch {
        val authToken = _uiState.value.authToken
        if (authToken == null) {
            userDataStore.clear()
            resetToLoginSignup(message = "No auth token found, please log in again")
            return@launch
        }

        when (val result = testBackendRepository.createLinkAuthToken(authToken)) {
            is Result.Success -> {
                val authResult = onrampCoordinator.authenticateUserWithToken(
                    result.value.linkAuthTokenClientSecret
                )

                when (authResult) {
                    is OnrampTokenAuthenticationResult.Completed -> {
                        _message.value = "Seamless sign-in successful!"
                        _uiState.update {
                            it.copy(
                                screen = Screen.AuthenticatedOperations,
                                loadingMessage = null
                            )
                        }
                    }
                    is OnrampTokenAuthenticationResult.Failed -> {
                        userDataStore.clear()
                        resetToLoginSignup(
                            message = "Seamless sign-in failed: ${authResult.error.message}"
                        )
                    }
                }
            }
            is Result.Failure -> {
                userDataStore.clear()
                resetToLoginSignup(message = "Seamless sign-in failed: ${result.error.message}")
            }
        }
    }

    private fun checkIfLinkUser(email: String) = viewModelScope.launch {
        val trimmedEmail = validateEmail(email) ?: return@launch

        _uiState.update {
            it.copy(
                screen = Screen.Loading,
                email = trimmedEmail,
                loadingMessage = "Checking user..."
            )
        }

        when (val result = onrampCoordinator.hasLinkAccount(trimmedEmail)) {
            is OnrampHasLinkAccountResult.Completed -> {
                if (result.hasLinkAccount) {
                    _message.value = "User exists in Link. Please authenticate"
                    _uiState.update {
                        it.copy(
                            screen = Screen.Authentication,
                            loadingMessage = null
                        )
                    }
                } else {
                    _message.value = "User does not exist in Link. Please register"
                    _uiState.update {
                        it.copy(
                            screen = Screen.Registration,
                            loadingMessage = null
                        )
                    }
                }
            }
            is OnrampHasLinkAccountResult.Failed -> {
                resetToLoginSignup(message = "Lookup failed: ${result.error.message}")
            }
        }
    }

    fun onBackToLoginSignup() {
        val googlePayIsReady = _uiState.value.googlePayIsReady
        val savedUser = userDataStore.load()

        _uiState.value = savedUser?.let {
            OnrampUiState(
                email = it.email,
                authToken = it.authToken,
                screen = Screen.SeamlessSignIn,
                googlePayIsReady = googlePayIsReady
            )
        } ?: OnrampUiState(
            screen = Screen.LoginSignup,
            googlePayIsReady = googlePayIsReady
        )
    }

    fun clearMessage() {
        _message.value = null
    }

    fun onVerifyIdentityResult(result: OnrampVerifyIdentityResult) {
        when (result) {
            is OnrampVerifyIdentityResult.Completed -> {
                _message.value = "Identity Verification completed"
                _uiState.update {
                    it.copy(
                        screen = Screen.AuthenticatedOperations,
                        loadingMessage = null
                    )
                }
            }
            is OnrampVerifyIdentityResult.Cancelled -> {
                _message.value = "Identity Verification cancelled, please try again"
                _uiState.update { it.copy(loadingMessage = null) }
            }
            is OnrampVerifyIdentityResult.Failed -> {
                resetToLoginSignup(
                    message = "Identity Verification failed: ${result.error.message}"
                )
            }
        }
    }

    fun onUserAttestationResult(result: OnrampUserAttestationResult) {
        when (result) {
            is OnrampUserAttestationResult.Confirmed -> {
                _message.value = "User Attestation Confirmed"
            }
            is OnrampUserAttestationResult.Failed -> {
                _message.value = "User Attestation failed: ${result.error.message}"
            }
            is OnrampUserAttestationResult.Cancelled -> {
                _message.value = "User Attestation cancelled, please try again"
            }
        }
    }

    fun onVerifyKycResult(result: OnrampVerifyKycInfoResult) {
        when (result) {
            is OnrampVerifyKycInfoResult.Confirmed -> {
                _message.value = "KYC Verification Completed"
            }
            is OnrampVerifyKycInfoResult.UpdateAddress -> {
                _updateAddressEvent.value = true
                _message.value = "KYC Verification Requires Address Update"
            }
            is OnrampVerifyKycInfoResult.Cancelled -> {
                _message.value = "KYC Verification Cancelled"
            }
            is OnrampVerifyKycInfoResult.Failed -> {
                _message.value = "KYC Verification Failed: ${result.error.message}"
            }
        }
    }

    fun clearUpdateAddressEvent() {
        _updateAddressEvent.value = false
    }

    fun onCollectPaymentResult(result: OnrampCollectPaymentMethodResult) {
        when (result) {
            is OnrampCollectPaymentMethodResult.Completed -> {
                _message.value = "Payment selection completed"
                _uiState.update {
                    it.copy(
                        screen = Screen.AuthenticatedOperations,
                        selectedPaymentData = result.displayData,
                        kycFirstName = result.kycInfo?.firstName ?: it.kycFirstName,
                        kycLastName = result.kycInfo?.lastName ?: it.kycLastName,
                        kycBirthCountry = result.kycInfo?.birthCountry?.value ?: it.kycBirthCountry,
                        kycBirthCity = result.kycInfo?.birthCity ?: it.kycBirthCity,
                        kycNationalities = result.kycInfo?.nationalities
                            ?.takeIf { nationalities -> nationalities.isNotEmpty() }
                            ?.joinToString(", ") { nationality -> nationality.value }
                            ?: it.kycNationalities,
                        kycAddress = result.kycInfo?.address ?: it.kycAddress,
                        loadingMessage = null
                    )
                }
            }
            is OnrampCollectPaymentMethodResult.Cancelled -> {
                _message.value = "Payment selection cancelled, please try again"
                _uiState.update { it.copy(loadingMessage = null) }
            }
            is OnrampCollectPaymentMethodResult.Failed -> {
                handleError(result.error) {
                    _message.value = "Payment selection failed: ${result.error.message}"
                    _uiState.update {
                        it.copy(
                            screen = Screen.AuthenticatedOperations,
                            loadingMessage = null
                        )
                    }
                }
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

                _uiState.value.authToken?.let { authToken ->
                    viewModelScope.launch {
                        testBackendRepository.saveUser(
                            cryptoCustomerId = result.customerId,
                            tokenWithLAI = authToken
                        )
                    }
                }

                _uiState.update { currentState ->
                    currentState.copy(
                        screen = Screen.AuthenticatedOperations,
                        linkAuthIntentId = null,
                        consentedLinkAuthIntentIds =
                            currentState.consentedLinkAuthIntentIds +
                                listOfNotNull(currentState.linkAuthIntentId),
                        loadingMessage = null
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
                _uiState.update {
                    it.copy(
                        screen = Screen.AuthenticatedOperations,
                        loadingMessage = null
                    )
                }
            }
            is OnrampCheckoutResult.Canceled -> {
                _message.value = "Checkout was canceled by the user"
                _uiState.update {
                    it.copy(
                        screen = Screen.AuthenticatedOperations,
                        loadingMessage = null
                    )
                }
            }
            is OnrampCheckoutResult.Failed -> {
                _message.value = "Checkout failed: ${result.error.message}"
                _uiState.update {
                    it.copy(
                        screen = Screen.AuthenticatedOperations,
                        loadingMessage = null
                    )
                }
            }
        }
    }

    suspend fun checkoutWithBackend(sessionId: String): String {
        _uiState.update {
            it.copy(loadingMessage = "Calling test backend checkout...")
        }

        val authToken = _uiState.value.authToken
            ?: throw IllegalStateException("No authentication token available")

        return when (val result = testBackendRepository.checkout(sessionId, authToken)) {
            is Result.Success -> {
                _uiState.update { it.copy(onrampSession = result.value) }
                result.value.clientSecret
            }
            is Result.Failure -> {
                throw IllegalStateException("Backend checkout failed: ${result.error.message}")
            }
        }
    }

    fun registerNewLinkUser(userInfo: LinkUserInfo) {
        viewModelScope.launch {
            when (val result = onrampCoordinator.registerLinkUser(userInfo)) {
                is OnrampRegisterLinkUserResult.Completed -> {
                    _message.value = "Registration successful"
                    _uiState.update {
                        it.copy(
                            screen = Screen.Authentication,
                            email = userInfo.email,
                            loadingMessage = null
                        )
                    }
                }
                is OnrampRegisterLinkUserResult.Failed -> {
                    resetToLoginSignup(message = "Registration failed: ${result.error.message}")
                }
            }
        }
    }

    fun registerWalletAddress(walletAddress: String, network: CryptoNetwork) {
        viewModelScope.launch {
            val trimmedWalletAddress = walletAddress.trim()
            if (trimmedWalletAddress.isBlank()) {
                _message.value = "Please enter a wallet address"
                return@launch
            }

            _uiState.update {
                it.copy(
                    screen = Screen.Loading,
                    loadingMessage = "Registering wallet address..."
                )
            }

            when (val result = onrampCoordinator.registerWalletAddress(trimmedWalletAddress, network)) {
                is OnrampRegisterWalletAddressResult.Completed -> {
                    _message.value = "Wallet address registered successfully!"
                    _uiState.update {
                        it.copy(
                            screen = Screen.AuthenticatedOperations,
                            walletAddress = trimmedWalletAddress,
                            network = network,
                            loadingMessage = null
                        )
                    }
                }
                is OnrampRegisterWalletAddressResult.Failed -> handleError(result.error) {
                    _message.value = "Failed to register wallet address: ${result.error.message}"
                    _uiState.update {
                        it.copy(
                            screen = Screen.AuthenticatedOperations,
                            loadingMessage = null
                        )
                    }
                }
            }
        }
    }

    fun collectKycInfo(kycInfo: KycInfo) {
        _uiState.update {
            it.copy(
                screen = Screen.Loading,
                loadingMessage = "Collecting KYC info..."
            )
        }

        viewModelScope.launch {
            when (val result = onrampCoordinator.attachKycInfo(kycInfo)) {
                is OnrampAttachKycInfoResult.Completed -> {
                    _message.value = "KYC Collection successful"
                    _uiState.update {
                        it.copy(
                            screen = Screen.AuthenticatedOperations,
                            loadingMessage = null
                        )
                    }
                }
                is OnrampAttachKycInfoResult.Failed -> handleError(result.error) {
                    _message.value = "KYC Collection failed: ${result.error.message}"
                    _uiState.update {
                        it.copy(
                            screen = Screen.AuthenticatedOperations,
                            loadingMessage = null
                        )
                    }
                }
            }
        }
    }

    fun retrieveMissingIdentifiers() {
        _uiState.update {
            it.copy(
                screen = Screen.Loading,
                loadingMessage = "Retrieving missing identifiers..."
            )
        }

        viewModelScope.launch {
            when (val result = onrampCoordinator.retrieveMissingIdentifiers()) {
                is OnrampRetrieveMissingIdentifiersResult.Completed -> {
                    _message.value = "Missing identifiers retrieved"
                    _uiState.update {
                        it.copy(
                            screen = Screen.AuthenticatedOperations,
                            loadingMessage = null,
                            missingIdentifiersSummary =
                                formatIdentifierRequirements(result.requirements)
                        )
                    }
                }
                is OnrampRetrieveMissingIdentifiersResult.Failed -> handleError(result.error) {
                    _message.value = "Failed to retrieve missing identifiers: ${result.error.message}"
                    _uiState.update {
                        it.copy(
                            screen = Screen.AuthenticatedOperations,
                            loadingMessage = null
                        )
                    }
                }
            }
        }
    }

    fun submitIdentifiers() {
        val identifiers = buildIdentifiersRequest(_uiState.value) ?: return

        _uiState.update {
            it.copy(
                screen = Screen.Loading,
                loadingMessage = "Submitting identifiers..."
            )
        }

        viewModelScope.launch {
            when (val result = onrampCoordinator.submitIdentifiers(identifiers)) {
                is OnrampSubmitIdentifiersResult.Completed -> {
                    _message.value = "Identifiers submitted"
                    _uiState.update {
                        it.copy(
                            screen = Screen.AuthenticatedOperations,
                            loadingMessage = null,
                            submitIdentifiersSummary =
                                formatSubmitIdentifiersResult(result.result)
                        )
                    }
                }
                is OnrampSubmitIdentifiersResult.Failed -> handleError(result.error) {
                    _message.value = "Submit identifiers failed: ${result.error.message}"
                    _uiState.update {
                        it.copy(
                            screen = Screen.AuthenticatedOperations,
                            loadingMessage = null
                        )
                    }
                }
            }
        }
    }

    fun updatePhoneNumber(phoneNumber: String) {
        val trimmedPhoneNumber = phoneNumber.trim()
        if (trimmedPhoneNumber.isBlank()) {
            _message.value = "Please enter a phone number"
            return
        }

        viewModelScope.launch {
            val currentScreen = _uiState.value.screen
            _uiState.update {
                it.copy(
                    screen = Screen.Loading,
                    loadingMessage = "Updating phone number..."
                )
            }

            when (val result = onrampCoordinator.updatePhoneNumber(trimmedPhoneNumber)) {
                is OnrampUpdatePhoneNumberResult.Completed -> {
                    _message.value = "Phone number updated successfully!"
                    _uiState.update {
                        it.copy(
                            screen = currentScreen,
                            loadingMessage = null
                        )
                    }
                }
                is OnrampUpdatePhoneNumberResult.Failed -> handleError(result.error) {
                    _message.value = "Failed to update phone number: ${result.error.message}"
                    _uiState.update {
                        it.copy(
                            screen = currentScreen,
                            loadingMessage = null
                        )
                    }
                }
            }
        }
    }

    fun createCryptoPaymentToken() {
        _uiState.update {
            it.copy(
                screen = Screen.Loading,
                loadingMessage = "Creating crypto payment token..."
            )
        }

        viewModelScope.launch {
            when (val result = onrampCoordinator.createCryptoPaymentToken()) {
                is OnrampCreateCryptoPaymentTokenResult.Completed -> {
                    _message.value = "Created crypto payment token: ${result.cryptoPaymentToken}"
                    _uiState.update {
                        it.copy(
                            screen = Screen.AuthenticatedOperations,
                            cryptoPaymentToken = result.cryptoPaymentToken,
                            loadingMessage = null
                        )
                    }
                }
                is OnrampCreateCryptoPaymentTokenResult.Failed -> handleError(result.error) {
                    _message.value =
                        "Failed to create crypto payment token: ${result.error.message}"
                    _uiState.update {
                        it.copy(
                            screen = Screen.AuthenticatedOperations,
                            loadingMessage = null
                        )
                    }
                }
            }
        }
    }

    fun createSession() {
        val currentState = _uiState.value
        if (!validateOnrampSessionParams(currentState)) return

        val selectedPaymentType = currentState.selectedPaymentData?.type
        val settlementSpeed = when (selectedPaymentType) {
            PaymentMethodDisplayData.Type.BankAccount -> currentState.settlementSpeed
            PaymentMethodDisplayData.Type.Card,
            PaymentMethodDisplayData.Type.GooglePay,
            null -> SettlementSpeed.INSTANT
        }

        val paymentToken = requireNotNull(currentState.cryptoPaymentToken)
        val walletAddress = requireNotNull(currentState.walletAddress)
        val authToken = requireNotNull(currentState.authToken)
        val destinationNetwork = currentState.network?.value ?: DEFAULT_DESTINATION_NETWORK

        _uiState.update {
            it.copy(
                screen = Screen.Loading,
                loadingMessage = "Creating session..."
            )
        }

        viewModelScope.launch {
            when (
                val result = testBackendRepository.createOnrampSession(
                    paymentToken = paymentToken,
                    walletAddress = walletAddress,
                    authToken = authToken,
                    destinationNetwork = destinationNetwork,
                    settlementSpeed = settlementSpeed
                )
            ) {
                is Result.Success -> {
                    _message.value =
                        "Onramp session created successfully! Session ID: ${result.value.id}"
                    _uiState.update {
                        it.copy(
                            onrampSession = result.value,
                            screen = Screen.AuthenticatedOperations,
                            loadingMessage = null
                        )
                    }
                }
                is Result.Failure -> {
                    _message.value =
                        "Failed to create onramp session: ${result.error.message}"
                    _uiState.update {
                        it.copy(
                            screen = Screen.AuthenticatedOperations,
                            loadingMessage = null
                        )
                    }
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

        if (currentState.cryptoPaymentToken == null) {
            _message.value = "No crypto payment token available. Please create a payment token first."
            return
        }

        _uiState.update {
            it.copy(
                screen = Screen.Loading,
                loadingMessage = "Performing checkout..."
            )
        }

        _checkoutEvent.value = CheckoutEvent(sessionId = onrampSession.id)
    }

    fun updateSettlementSpeed(settlementSpeed: SettlementSpeed) {
        _uiState.update { it.copy(settlementSpeed = settlementSpeed) }
    }

    fun updateKycFirstName(value: String) {
        _uiState.update { it.copy(kycFirstName = value) }
    }

    fun updateKycLastName(value: String) {
        _uiState.update { it.copy(kycLastName = value) }
    }

    fun updateKycBirthCountry(value: String) {
        _uiState.update { it.copy(kycBirthCountry = value) }
    }

    fun updateKycBirthCity(value: String) {
        _uiState.update { it.copy(kycBirthCity = value) }
    }

    fun updateKycNationalities(value: String) {
        _uiState.update { it.copy(kycNationalities = value) }
    }

    fun updateKycAddress(address: PaymentSheet.Address) {
        _uiState.update { it.copy(kycAddress = address) }
    }

    fun updateIdentifierValue(index: Int, value: String) {
        _uiState.update { state ->
            state.copy(
                identifierInputs = state.identifierInputs.replaceAt(index) { entry ->
                    entry.copy(value = value)
                }
            )
        }
    }

    fun updateIdentifierType(index: Int, value: String) {
        _uiState.update { state ->
            state.copy(
                identifierInputs = state.identifierInputs.replaceAt(index) { entry ->
                    entry.copy(type = value)
                }
            )
        }
    }

    fun addIdentifierInput() {
        _uiState.update { state ->
            state.copy(identifierInputs = state.identifierInputs + IdentifierInputEntry())
        }
    }

    fun removeIdentifierInput(index: Int) {
        _uiState.update { state ->
            state.copy(identifierInputs = state.identifierInputs.removeEntryAt(index))
        }
    }

    fun clearCheckoutEvent() {
        _checkoutEvent.value = null
    }

    suspend fun createLinkAuthIntent(oauthScopes: String): String? {
        val authToken = _uiState.value.authToken
        if (authToken == null) {
            userDataStore.clear()
            resetToLoginSignup(message = "No auth token found, please log in again")
            return null
        }

        return when (val result = testBackendRepository.create(oauthScopes, authToken)) {
            is Result.Success -> {
                _uiState.update { it.copy(authToken = result.value.token) }
                _message.value = "Auth intent created successfully"
                userDataStore.save(
                    OnrampUserData(
                        email = _uiState.value.email,
                        authToken = result.value.token
                    )
                )
                result.value.authIntentId
            }
            is Result.Failure -> {
                _message.value = "Failed to create auth intent: ${result.error.message}"
                null
            }
        }
    }

    fun logOut() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    screen = Screen.Loading,
                    loadingMessage = "Logging out..."
                )
            }

            when (val result = onrampCoordinator.logOut()) {
                is OnrampLogOutResult.Completed -> {
                    _message.value = "Successfully logged out"
                    userDataStore.clear()
                    resetToLoginSignup()
                }
                is OnrampLogOutResult.Failed -> {
                    _message.value = "Logout failed: ${result.error.message}"
                    _uiState.update {
                        it.copy(
                            screen = Screen.AuthenticatedOperations,
                            loadingMessage = null
                        )
                    }
                }
            }
        }
    }

    fun clearAuthorizeEvent() {
        _authorizeEvent.value = null
    }

    private fun validateOnrampSessionParams(currentState: OnrampUiState): Boolean {
        val missingItems = mutableListOf<String>()

        if (currentState.walletAddress.isNullOrBlank()) {
            missingItems.add("wallet address registration")
        }
        if (currentState.selectedPaymentData == null) {
            missingItems.add("payment method selection")
        }
        if (currentState.cryptoPaymentToken.isNullOrBlank()) {
            missingItems.add("crypto payment token creation")
        }
        if (currentState.authToken.isNullOrBlank()) {
            missingItems.add("authentication token")
        }

        if (missingItems.isEmpty()) {
            return true
        }

        _message.value = when (missingItems.size) {
            1 -> "Please complete ${missingItems[0]} first"
            2 -> "Please complete ${missingItems[0]} and ${missingItems[1]} first"
            else -> {
                "Please complete the following steps first: ${missingItems.joinToString(", ")}"
            }
        }
        return false
    }

    private fun validateEmail(email: String): String? {
        if (email.isBlank()) {
            _message.value = "Please enter an email address"
            return null
        }

        return email.trim()
    }

    private fun validatePassword(password: String): Boolean {
        if (password.length >= minPasswordLength) {
            return true
        }

        _message.value = "Please enter a valid password (at least 8 characters)"
        return false
    }

    private fun handleError(error: Throwable, onNonAuthError: () -> Unit = {}) {
        if (!error.isLinkAuthorizationError()) {
            onNonAuthError()
            return
        }

        _message.value = "Session expired. Reauthorizing..."
        _uiState.update {
            it.copy(
                screen = Screen.Authentication,
                loadingMessage = null
            )
        }

        val linkAuthIntentId = _uiState.value.consentedLinkAuthIntentIds.firstOrNull()
        if (linkAuthIntentId != null) {
            _authorizeEvent.value = AuthorizeEvent(linkAuthIntentId)
        } else {
            _message.value = "Session expired. Please reauthenticate."
        }
    }

    private fun googlePayIsReady(isReady: Boolean) {
        _uiState.update { it.copy(googlePayIsReady = isReady) }
    }

    private fun buildIdentifiersRequest(state: OnrampUiState): List<ComplianceIdentifier>? {
        val identifiers = mutableListOf<ComplianceIdentifier>()

        state.identifierInputs.forEachIndexed { index, entry ->
            val (identifier, error) = buildIdentifier(
                label = "Identifier ${index + 1}",
                value = entry.value,
                type = entry.type
            )
            if (error != null) {
                _message.value = error
                return null
            }
            identifier?.let(identifiers::add)
        }

        if (identifiers.isEmpty()) {
            _message.value = "Enter at least one identifier"
            return null
        }

        return identifiers
    }

    private fun buildIdentifier(
        label: String,
        value: String,
        type: String
    ): Pair<ComplianceIdentifier?, String?> {
        val trimmedValue = value.trim()
        val trimmedType = type.trim()

        if (trimmedValue.isEmpty() && trimmedType.isEmpty()) {
            return null to null
        }

        if (trimmedType.isEmpty() || trimmedValue.isEmpty()) {
            return null to "$label requires both type and value"
        }

        return ComplianceIdentifier()
            .type(ComplianceIdentifierType.fromValue(trimmedType))
            .value(trimmedValue) to null
    }

    private fun formatIdentifierRequirements(
        requirements: ComplianceIdentifierRequirements
    ): String {
        return buildString {
            append("CARF TIN required: ${requirements.carfTinRequired}")
            append("\n")
            append("Identifiers: ")
            append(formatIdentifierRequirements(requirements.identifiers))
            append("\nAlternatives: ")
            append(formatAlternativeGroups(requirements.alternatives))
        }
    }

    private fun formatSubmitIdentifiersResult(result: SubmitIdentifiersResult): String {
        return buildString {
            append("Completed: ${result.completed}")
            append("\nCARF TIN required: ${result.carfTinRequired}")
            append("\nIdentifiers: ")
            append(formatIdentifierRequirements(result.identifiers))
            append("\nAlternatives: ")
            append(formatAlternativeGroups(result.alternatives))
            append("\nInvalid identifiers: ")
            append(result.invalidIdentifiers.map { it.value }.joinToStringOrNone())
        }
    }

    private fun formatIdentifierRequirements(
        identifierRequirements: List<ComplianceIdentifierRequirement>
    ): String {
        return identifierRequirements
            .takeIf { it.isNotEmpty() }
            ?.joinToString(separator = "\n") { requirement ->
                "${requirement.regulation.value}: ${requirement.type.value}"
            }
            ?: "None"
    }

    private fun formatAlternativeGroups(
        alternativeGroups: List<ComplianceIdentifierAlternativeGroup>
    ): String {
        return alternativeGroups
            .takeIf { it.isNotEmpty() }
            ?.joinToString(separator = "\n") { alternativeGroup ->
                "${alternativeGroup.originalMissingIdentifiers.joinToValueString()} -> " +
                    alternativeGroup.alternativeMissingIdentifiers.joinToValueString()
            }
            ?: "None"
    }

    private fun resetToLoginSignup(message: String? = null) {
        if (message != null) {
            _message.value = message
        }

        _uiState.update { currentState ->
            OnrampUiState(
                screen = Screen.LoginSignup,
                googlePayIsReady = currentState.googlePayIsReady
            )
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

private const val DEFAULT_DESTINATION_NETWORK = "ethereum"

private fun List<String>.joinToStringOrNone(): String {
    return takeIf { it.isNotEmpty() }?.joinToString(", ") ?: "None"
}

private fun List<ComplianceIdentifierType>.joinToValueString(): String {
    return map { it.value }.joinToStringOrNone()
}

private fun List<IdentifierInputEntry>.replaceAt(
    index: Int,
    transform: (IdentifierInputEntry) -> IdentifierInputEntry
): List<IdentifierInputEntry> {
    if (index !in indices) return this

    return mapIndexed { currentIndex, entry ->
        if (currentIndex == index) {
            transform(entry)
        } else {
            entry
        }
    }
}

private fun List<IdentifierInputEntry>.removeEntryAt(index: Int): List<IdentifierInputEntry> {
    if (index !in indices) return this
    return filterIndexed { currentIndex, _ -> currentIndex != index }
}
