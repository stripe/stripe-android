package com.stripe.android.crypto.onramp

import android.app.Application
import android.content.Context
import com.stripe.android.core.utils.flatMapCatching
import com.stripe.android.crypto.onramp.CheckoutState.Status
import com.stripe.android.crypto.onramp.analytics.OnrampAnalyticsEvent
import com.stripe.android.crypto.onramp.analytics.OnrampAnalyticsService
import com.stripe.android.crypto.onramp.exception.MissingConsumerSecretException
import com.stripe.android.crypto.onramp.exception.MissingCryptoCustomerException
import com.stripe.android.crypto.onramp.exception.MissingPaymentMethodException
import com.stripe.android.crypto.onramp.exception.PaymentFailedException
import com.stripe.android.crypto.onramp.model.CryptoNetwork
import com.stripe.android.crypto.onramp.model.KycInfo
import com.stripe.android.crypto.onramp.model.LinkUserInfo
import com.stripe.android.crypto.onramp.model.OnrampAttachKycInfoResult
import com.stripe.android.crypto.onramp.model.OnrampAuthenticateResult
import com.stripe.android.crypto.onramp.model.OnrampAuthorizeResult
import com.stripe.android.crypto.onramp.model.OnrampCheckoutResult
import com.stripe.android.crypto.onramp.model.OnrampCollectPaymentMethodResult
import com.stripe.android.crypto.onramp.model.OnrampConfiguration
import com.stripe.android.crypto.onramp.model.OnrampConfigurationResult
import com.stripe.android.crypto.onramp.model.OnrampCreateCryptoPaymentTokenResult
import com.stripe.android.crypto.onramp.model.OnrampHasLinkAccountResult
import com.stripe.android.crypto.onramp.model.OnrampLogOutResult
import com.stripe.android.crypto.onramp.model.OnrampRegisterLinkUserResult
import com.stripe.android.crypto.onramp.model.OnrampRegisterWalletAddressResult
import com.stripe.android.crypto.onramp.model.OnrampStartVerificationResult
import com.stripe.android.crypto.onramp.model.OnrampUpdatePhoneNumberResult
import com.stripe.android.crypto.onramp.model.OnrampVerifyIdentityResult
import com.stripe.android.crypto.onramp.model.PaymentMethodDisplayData
import com.stripe.android.crypto.onramp.model.PaymentMethodType
import com.stripe.android.crypto.onramp.repositories.CryptoApiRepository
import com.stripe.android.identity.IdentityVerificationSheet
import com.stripe.android.link.LinkController
import com.stripe.android.link.LinkController.ConfigureResult
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.StripeIntent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Suppress("LargeClass", "TooManyFunctions")
@Singleton
internal class OnrampInteractor @Inject constructor(
    private val application: Application,
    private val linkController: LinkController,
    private val cryptoApiRepository: CryptoApiRepository,
    private val analyticsServiceFactory: OnrampAnalyticsService.Factory,
) {
    private val _state = MutableStateFlow(OnrampState())
    val state: StateFlow<OnrampState> = _state.asStateFlow()

    private var analyticsService: OnrampAnalyticsService? = null

    suspend fun configure(configuration: OnrampConfiguration): OnrampConfigurationResult {
        _state.value = OnrampState(
            configuration = configuration,
            cryptoCustomerId = configuration.cryptoCustomerId,
        )

        // We are *not* calling `PaymentConfiguration.init()` here because we're relying on
        // `LinkController.configure()` to do it.
        val linkResult: ConfigureResult = linkController.configure(
            LinkController.Configuration.Builder(
                merchantDisplayName = configuration.merchantDisplayName,
                publishableKey = configuration.publishableKey,
            )
                .allowLogOut(false)
                .allowUserEmailEdits(false)
                .appearance(configuration.appearance)
                .build()
        )

        return when (linkResult) {
            is ConfigureResult.Success -> OnrampConfigurationResult.Completed(success = true)
            is ConfigureResult.Failed -> {
                track(
                    OnrampAnalyticsEvent.ErrorOccurred(
                        operation = OnrampAnalyticsEvent.ErrorOccurred.Operation.Configure,
                        error = linkResult.error,
                    )
                )
                OnrampConfigurationResult.Failed(linkResult.error)
            }
        }
    }

    suspend fun hasLinkAccount(email: String): OnrampHasLinkAccountResult {
        return when (val result = linkController.lookupConsumer(email)) {
            is LinkController.LookupConsumerResult.Success -> {
                track(
                    OnrampAnalyticsEvent.LinkAccountLookupCompleted(
                        hasLinkAccount = result.isConsumer
                    )
                )
                OnrampHasLinkAccountResult.Completed(
                    hasLinkAccount = result.isConsumer
                )
            }
            is LinkController.LookupConsumerResult.Failed -> {
                track(
                    OnrampAnalyticsEvent.ErrorOccurred(
                        operation = OnrampAnalyticsEvent.ErrorOccurred.Operation.HasLinkAccount,
                        error = result.error,
                    )
                )
                OnrampHasLinkAccountResult.Failed(
                    error = result.error
                )
            }
        }
    }

    suspend fun registerLinkUser(info: LinkUserInfo): OnrampRegisterLinkUserResult {
        val result = linkController.registerConsumer(
            email = info.email,
            phone = info.phone,
            country = info.country,
            name = info.fullName,
        )

        return when (result) {
            is LinkController.RegisterConsumerResult.Success -> {
                val secret = consumerSessionClientSecret()
                if (secret != null) {
                    cryptoApiRepository.createCryptoCustomer(secret).fold(
                        onSuccess = { customerResponse ->
                            _state.update { it.copy(cryptoCustomerId = customerResponse.id) }
                            track(OnrampAnalyticsEvent.LinkRegistrationCompleted)
                            OnrampRegisterLinkUserResult.Completed(customerResponse.id)
                        },
                        onFailure = { error ->
                            track(
                                OnrampAnalyticsEvent.ErrorOccurred(
                                    operation = OnrampAnalyticsEvent.ErrorOccurred.Operation.RegisterLinkUser,
                                    error = error,
                                )
                            )
                            OnrampRegisterLinkUserResult.Failed(error)
                        }
                    )
                } else {
                    val error = MissingConsumerSecretException()
                    track(
                        OnrampAnalyticsEvent.ErrorOccurred(
                            operation = OnrampAnalyticsEvent.ErrorOccurred.Operation.RegisterLinkUser,
                            error = error,
                        )
                    )
                    OnrampRegisterLinkUserResult.Failed(error)
                }
            }
            is LinkController.RegisterConsumerResult.Failed -> {
                track(
                    OnrampAnalyticsEvent.ErrorOccurred(
                        operation = OnrampAnalyticsEvent.ErrorOccurred.Operation.RegisterLinkUser,
                        error = result.error,
                    )
                )
                OnrampRegisterLinkUserResult.Failed(
                    error = result.error
                )
            }
        }
    }

    suspend fun updatePhoneNumber(phoneNumber: String): OnrampUpdatePhoneNumberResult {
        return when (val result = linkController.updatePhoneNumber(phoneNumber)) {
            is LinkController.UpdatePhoneNumberResult.Success -> {
                track(OnrampAnalyticsEvent.LinkPhoneNumberUpdated)
                OnrampUpdatePhoneNumberResult.Completed
            }
            is LinkController.UpdatePhoneNumberResult.Failed -> {
                track(
                    OnrampAnalyticsEvent.ErrorOccurred(
                        operation = OnrampAnalyticsEvent.ErrorOccurred.Operation.UpdatePhoneNumber,
                        error = result.error,
                    )
                )
                OnrampUpdatePhoneNumberResult.Failed(
                    error = result.error
                )
            }
        }
    }

    suspend fun registerWalletAddress(
        walletAddress: String,
        network: CryptoNetwork
    ): OnrampRegisterWalletAddressResult {
        val secret = consumerSessionClientSecret()
        return if (secret != null) {
            val result = cryptoApiRepository.setWalletAddress(walletAddress, network, secret)
            result.fold(
                onSuccess = {
                    track(OnrampAnalyticsEvent.WalletRegistered(network))
                    OnrampRegisterWalletAddressResult.Completed()
                },
                onFailure = { error ->
                    track(
                        OnrampAnalyticsEvent.ErrorOccurred(
                            operation = OnrampAnalyticsEvent.ErrorOccurred.Operation.RegisterWalletAddress,
                            error = error,
                        )
                    )
                    OnrampRegisterWalletAddressResult.Failed(error)
                }
            )
        } else {
            val error = MissingConsumerSecretException()
            track(
                OnrampAnalyticsEvent.ErrorOccurred(
                    operation = OnrampAnalyticsEvent.ErrorOccurred.Operation.RegisterWalletAddress,
                    error = error,
                )
            )
            OnrampRegisterWalletAddressResult.Failed(error)
        }
    }

    suspend fun attachKycInfo(kycInfo: KycInfo): OnrampAttachKycInfoResult {
        val secret = consumerSessionClientSecret()
        if (secret == null) {
            val error = MissingConsumerSecretException()
            track(
                OnrampAnalyticsEvent.ErrorOccurred(
                    operation = OnrampAnalyticsEvent.ErrorOccurred.Operation.AttachKycInfo,
                    error = error,
                )
            )
            return OnrampAttachKycInfoResult.Failed(error)
        }

        return cryptoApiRepository.collectKycData(kycInfo, secret)
            .fold(
                onSuccess = {
                    track(OnrampAnalyticsEvent.KycInfoSubmitted)
                    OnrampAttachKycInfoResult.Completed
                },
                onFailure = { error ->
                    track(
                        OnrampAnalyticsEvent.ErrorOccurred(
                            operation = OnrampAnalyticsEvent.ErrorOccurred.Operation.AttachKycInfo,
                            error = error,
                        )
                    )
                    OnrampAttachKycInfoResult.Failed(error)
                }
            )
    }

    suspend fun startIdentityVerification(): OnrampStartVerificationResult {
        val secret = consumerSessionClientSecret()
        if (secret == null) {
            val error = MissingConsumerSecretException()
            track(
                OnrampAnalyticsEvent.ErrorOccurred(
                    operation = OnrampAnalyticsEvent.ErrorOccurred.Operation.VerifyIdentity,
                    error = error,
                )
            )
            return OnrampStartVerificationResult.Failed(error)
        }

        return cryptoApiRepository.startIdentityVerification(secret)
            .fold(
                onSuccess = { result ->
                    track(OnrampAnalyticsEvent.IdentityVerificationStarted)
                    OnrampStartVerificationResult.Completed(result)
                },
                onFailure = { error ->
                    track(
                        OnrampAnalyticsEvent.ErrorOccurred(
                            operation = OnrampAnalyticsEvent.ErrorOccurred.Operation.VerifyIdentity,
                            error = error,
                        )
                    )
                    OnrampStartVerificationResult.Failed(error)
                }
            )
    }

    @Suppress("LongMethod")
    suspend fun createCryptoPaymentToken(): OnrampCreateCryptoPaymentTokenResult {
        val secret = consumerSessionClientSecret()
        if (secret == null) {
            val error = MissingConsumerSecretException()
            track(
                OnrampAnalyticsEvent.ErrorOccurred(
                    operation = OnrampAnalyticsEvent.ErrorOccurred.Operation.CreateCryptoPaymentToken,
                    error = error,
                )
            )
            return OnrampCreateCryptoPaymentTokenResult.Failed(error)
        }

        // Get the platform publishable key
        return getOrFetchPlatformKey()
            // Create a PaymentMethod
            .mapCatching { platformPublishableKey ->
                when (val result = linkController.createPaymentMethodForOnramp(apiKey = platformPublishableKey)) {
                    is LinkController.CreatePaymentMethodResult.Success -> {
                        result.paymentMethod
                    }
                    is LinkController.CreatePaymentMethodResult.Failed -> {
                        throw result.error
                    }
                }
            }
            // Create a crypto payment token
            .flatMapCatching { paymentMethod ->
                cryptoApiRepository.createPaymentToken(
                    consumerSessionClientSecret = secret,
                    paymentMethod = paymentMethod.id!!,
                )
            }
            .fold(
                onSuccess = { cryptoPaymentToken ->
                    track(
                        OnrampAnalyticsEvent.CryptoPaymentTokenCreated(
                            paymentMethodType = _state.value.collectingPaymentMethodType
                        )
                    )
                    OnrampCreateCryptoPaymentTokenResult.Completed(cryptoPaymentToken.id)
                },
                onFailure = { error ->
                    track(
                        OnrampAnalyticsEvent.ErrorOccurred(
                            operation = OnrampAnalyticsEvent.ErrorOccurred.Operation.CreateCryptoPaymentToken,
                            error = error,
                        )
                    )
                    OnrampCreateCryptoPaymentTokenResult.Failed(error)
                }
            )
    }

    suspend fun logOut(): OnrampLogOutResult {
        return when (val result = linkController.logOut()) {
            is LinkController.LogOutResult.Success -> {
                track(OnrampAnalyticsEvent.LinkLogout)
                OnrampLogOutResult.Completed
            }
            is LinkController.LogOutResult.Failed -> {
                track(
                    OnrampAnalyticsEvent.ErrorOccurred(
                        operation = OnrampAnalyticsEvent.ErrorOccurred.Operation.LogOut,
                        error = result.error,
                    )
                )
                OnrampLogOutResult.Failed(result.error)
            }
        }
    }

    suspend fun handleAuthenticationResult(
        result: LinkController.AuthenticationResult
    ): OnrampAuthenticateResult = when (result) {
        is LinkController.AuthenticationResult.Success -> {
            val secret = consumerSessionClientSecret()
            if (secret != null) {
                cryptoApiRepository.createCryptoCustomer(secret).fold(
                    onSuccess = { customerResponse ->
                        _state.update { it.copy(cryptoCustomerId = customerResponse.id) }
                        track(OnrampAnalyticsEvent.LinkUserAuthenticationCompleted)
                        OnrampAuthenticateResult.Completed(customerResponse.id)
                    },
                    onFailure = { error ->
                        track(
                            OnrampAnalyticsEvent.ErrorOccurred(
                                operation = OnrampAnalyticsEvent.ErrorOccurred.Operation.AuthenticateUser,
                                error = error,
                            )
                        )
                        OnrampAuthenticateResult.Failed(error)
                    }
                )
            } else {
                val error = MissingConsumerSecretException()
                track(
                    OnrampAnalyticsEvent.ErrorOccurred(
                        operation = OnrampAnalyticsEvent.ErrorOccurred.Operation.AuthenticateUser,
                        error = error,
                    )
                )
                OnrampAuthenticateResult.Failed(error)
            }
        }
        is LinkController.AuthenticationResult.Failed -> {
            track(
                OnrampAnalyticsEvent.ErrorOccurred(
                    operation = OnrampAnalyticsEvent.ErrorOccurred.Operation.AuthenticateUser,
                    error = result.error,
                )
            )
            OnrampAuthenticateResult.Failed(result.error)
        }
        is LinkController.AuthenticationResult.Canceled -> OnrampAuthenticateResult.Cancelled()
    }

    suspend fun handleAuthorizeResult(
        result: LinkController.AuthorizeResult
    ): OnrampAuthorizeResult = when (result) {
        is LinkController.AuthorizeResult.Consented -> {
            val secret = consumerSessionClientSecret()
            if (secret != null) {
                cryptoApiRepository.createCryptoCustomer(secret).fold(
                    onSuccess = { customerResponse ->
                        _state.update { it.copy(cryptoCustomerId = customerResponse.id) }
                        track(OnrampAnalyticsEvent.LinkAuthorizationCompleted(consented = true))
                        OnrampAuthorizeResult.Consented(customerResponse.id)
                    },
                    onFailure = { error ->
                        track(
                            OnrampAnalyticsEvent.ErrorOccurred(
                                operation = OnrampAnalyticsEvent.ErrorOccurred.Operation.Authorize,
                                error = error,
                            )
                        )
                        OnrampAuthorizeResult.Failed(error)
                    }
                )
            } else {
                val error = MissingConsumerSecretException()
                track(
                    OnrampAnalyticsEvent.ErrorOccurred(
                        operation = OnrampAnalyticsEvent.ErrorOccurred.Operation.Authorize,
                        error = error,
                    )
                )
                OnrampAuthorizeResult.Failed(error)
            }
        }
        is LinkController.AuthorizeResult.Denied -> {
            track(OnrampAnalyticsEvent.LinkAuthorizationCompleted(consented = false))
            OnrampAuthorizeResult.Denied()
        }
        is LinkController.AuthorizeResult.Canceled ->
            OnrampAuthorizeResult.Canceled()
        is LinkController.AuthorizeResult.Failed -> {
            track(
                OnrampAnalyticsEvent.ErrorOccurred(
                    operation = OnrampAnalyticsEvent.ErrorOccurred.Operation.Authorize,
                    error = result.error,
                )
            )
            OnrampAuthorizeResult.Failed(result.error)
        }
    }

    fun handleIdentityVerificationResult(
        result: IdentityVerificationSheet.VerificationFlowResult
    ): OnrampVerifyIdentityResult = when (result) {
        is IdentityVerificationSheet.VerificationFlowResult.Completed -> {
            track(OnrampAnalyticsEvent.IdentityVerificationCompleted)
            OnrampVerifyIdentityResult.Completed()
        }
        is IdentityVerificationSheet.VerificationFlowResult.Failed -> {
            track(
                OnrampAnalyticsEvent.ErrorOccurred(
                    operation = OnrampAnalyticsEvent.ErrorOccurred.Operation.VerifyIdentity,
                    error = result.throwable,
                )
            )
            OnrampVerifyIdentityResult.Failed(result.throwable)
        }
        is IdentityVerificationSheet.VerificationFlowResult.Canceled ->
            OnrampVerifyIdentityResult.Cancelled()
    }

    fun handlePresentPaymentMethodsResult(
        result: LinkController.PresentPaymentMethodsResult,
        context: Context,
    ): OnrampCollectPaymentMethodResult = when (result) {
        is LinkController.PresentPaymentMethodsResult.Success -> {
            track(
                OnrampAnalyticsEvent.CollectPaymentMethodCompleted(
                    paymentMethodType = _state.value.collectingPaymentMethodType
                )
            )
            linkController.state(context).value.selectedPaymentMethodPreview?.let {
                OnrampCollectPaymentMethodResult.Completed(
                    displayData = PaymentMethodDisplayData(
                        iconRes = it.iconRes,
                        label = it.label,
                        sublabel = it.sublabel
                    )
                )
            } ?: run {
                OnrampCollectPaymentMethodResult.Failed(MissingPaymentMethodException())
            }
        }
        is LinkController.PresentPaymentMethodsResult.Failed -> {
            track(
                OnrampAnalyticsEvent.ErrorOccurred(
                    operation = OnrampAnalyticsEvent.ErrorOccurred.Operation.CollectPaymentMethod,
                    error = result.error,
                )
            )
            OnrampCollectPaymentMethodResult.Failed(result.error)
        }
        is LinkController.PresentPaymentMethodsResult.Canceled ->
            OnrampCollectPaymentMethodResult.Cancelled()
    }

    private fun consumerSessionClientSecret(): String? =
        _state.value.linkControllerState?.internalLinkAccount?.consumerSessionClientSecret
            ?: linkController.state(application).value.internalLinkAccount?.consumerSessionClientSecret

    fun onLinkControllerState(linkState: LinkController.State) {
        if (analyticsService?.elementsSessionId != linkState.elementsSessionId) {
            analyticsService = linkState.elementsSessionId
                ?.let { analyticsServiceFactory.create(it) }
            track(OnrampAnalyticsEvent.SessionCreated)
        }
        _state.value = _state.value.copy(linkControllerState = linkState)
    }

    fun onAuthorize() {
        track(OnrampAnalyticsEvent.LinkAuthorizationStarted)
    }

    fun onAuthenticateUser() {
        track(OnrampAnalyticsEvent.LinkUserAuthenticationStarted)
    }

    fun onCollectPaymentMethod(type: PaymentMethodType) {
        _state.update { it.copy(collectingPaymentMethodType = type) }
        track(
            OnrampAnalyticsEvent.CollectPaymentMethodStarted(type)
        )
    }

    fun onHandleNextActionError(error: Throwable) {
        track(
            OnrampAnalyticsEvent.ErrorOccurred(
                operation = OnrampAnalyticsEvent.ErrorOccurred.Operation.PerformCheckout,
                error = error,
            )
        )
    }

    /**
     * Starts the checkout flow for a crypto onramp session. The checkout state will be emitted
     * through the state StateFlow. The coordinator should observe the checkoutState and react accordingly.
     *
     * @param onrampSessionId The onramp session identifier.
     * @param checkoutHandler An async closure that calls your backend to perform a checkout.
     */
    suspend fun startCheckout(
        onrampSessionId: String,
        checkoutHandler: suspend () -> String
    ) {
        if (_state.value.checkoutState?.status?.inProgress == true) {
            // Checkout is already in progress - ignore duplicate calls
            return
        }
        _state.update {
            it.copy(
                checkoutState = CheckoutState(
                    status = Status.Processing(
                        onrampSessionId = onrampSessionId,
                        checkoutHandler = checkoutHandler
                    )
                )
            )
        }
        track(
            OnrampAnalyticsEvent.CheckoutStarted(
                onrampSessionId = onrampSessionId,
                paymentMethodType = _state.value.collectingPaymentMethodType
            )
        )
        performCheckoutInternal(
            onrampSessionId = onrampSessionId,
            checkoutHandler = checkoutHandler,
            isContinuation = false,
        )
    }

    /**
     * Continues the checkout flow after PaymentLauncher completes a next action.
     * This should be called by the coordinator when PaymentLauncher finishes successfully.
     */
    suspend fun continueCheckout() {
        val currentCheckoutState = _state.value.checkoutState
        when (val status = currentCheckoutState?.status) {
            is Status.RequiresNextAction -> {
                // Continue processing with the existing session info
                _state.update {
                    it.copy(
                        checkoutState = CheckoutState(
                            status = Status.Processing(
                                onrampSessionId = status.onrampSessionId,
                                checkoutHandler = status.checkoutHandler
                            )
                        )
                    )
                }
                performCheckoutInternal(
                    onrampSessionId = status.onrampSessionId,
                    checkoutHandler = status.checkoutHandler,
                    isContinuation = true,
                )
            }
            else -> {
                // No valid session to continue - this shouldn't happen
                _state.update {
                    it.copy(
                        checkoutState = CheckoutState(
                            status = Status.Completed(OnrampCheckoutResult.Failed(PaymentFailedException()))
                        )
                    )
                }
            }
        }
    }

    /**
     * Internal method that performs the actual checkout logic and updates the state accordingly.
     */
    @Suppress("LongMethod")
    private suspend fun performCheckoutInternal(
        onrampSessionId: String,
        checkoutHandler: suspend () -> String,
        isContinuation: Boolean,
    ) {
        val checkoutStatus = getOrFetchPlatformKey()
            .flatMapCatching { platformApiKey ->
                retrievePaymentIntent(
                    onrampSessionId = onrampSessionId,
                    onrampSessionClientSecret = checkoutHandler(),
                    platformApiKey = platformApiKey
                ).map { platformApiKey to it }
            }
            .fold(
                onSuccess = { (platformApiKey, paymentIntent) ->
                    // If there is a checkout result, we're done. Otherwise, signal that next action is needed,
                    // which is handled by PaymentLauncher in the presenter.
                    paymentIntent.toCheckoutResult()
                        ?.let { checkoutResult ->
                            track(
                                OnrampAnalyticsEvent.CheckoutCompleted(
                                    onrampSessionId = onrampSessionId,
                                    paymentMethodType = _state.value.collectingPaymentMethodType,
                                    requiredAction = isContinuation
                                )
                            )
                            Status.Completed(checkoutResult)
                        }
                        ?: Status.RequiresNextAction(
                            platformKey = platformApiKey,
                            onrampSessionId = onrampSessionId,
                            checkoutHandler = checkoutHandler,
                            paymentIntent = paymentIntent
                        )
                },
                onFailure = { error ->
                    track(
                        OnrampAnalyticsEvent.ErrorOccurred(
                            operation = OnrampAnalyticsEvent.ErrorOccurred.Operation.PerformCheckout,
                            error = error,
                        )
                    )
                    Status.Completed(OnrampCheckoutResult.Failed(error))
                }
            )
        _state.update { it.copy(checkoutState = CheckoutState(checkoutStatus)) }
    }

    /**
     * Performs checkout and retrieves the resulting PaymentIntent.
     *
     * @param onrampSessionId The onramp session identifier.
     * @param onrampSessionClientSecret The onramp session client secret.
     * @param platformApiKey The platform API key.
     * @return The PaymentIntent after checkout.
     */
    private suspend fun retrievePaymentIntent(
        onrampSessionId: String,
        onrampSessionClientSecret: String,
        platformApiKey: String
    ): Result<PaymentIntent> {
        // Get the onramp session to extract the payment_intent_client_secret
        return cryptoApiRepository.getOnrampSession(
            sessionId = onrampSessionId,
            sessionClientSecret = onrampSessionClientSecret
        )
            // Retrieve and return the PaymentIntent using the special publishable key
            .flatMapCatching { onrampSession ->
                cryptoApiRepository.retrievePaymentIntent(
                    clientSecret = onrampSession.paymentIntentClientSecret,
                    publishableKey = platformApiKey
                )
            }
    }

    /**
     * Maps a PaymentIntent status to a CheckoutResult, or returns null if more handling is needed.
     */
    private fun PaymentIntent.toCheckoutResult(): OnrampCheckoutResult? {
        return when (status) {
            StripeIntent.Status.Succeeded, StripeIntent.Status.RequiresCapture -> {
                OnrampCheckoutResult.Completed()
            }
            StripeIntent.Status.Processing -> {
                if (paymentMethod?.type == PaymentMethod.Type.USBankAccount) {
                    OnrampCheckoutResult.Completed()
                } else {
                    OnrampCheckoutResult.Failed(PaymentFailedException())
                }
            }
            StripeIntent.Status.RequiresPaymentMethod -> OnrampCheckoutResult.Failed(PaymentFailedException())
            StripeIntent.Status.RequiresAction -> null // More handling needed
            else -> OnrampCheckoutResult.Failed(PaymentFailedException())
        }
    }

    /**
     * Gets the platform publishable key from state, or fetches it if not available.
     * Returns null if fetch fails or key is null.
     */
    private suspend fun getOrFetchPlatformKey(): Result<String> {
        val cryptoCustomerId = _state.value.cryptoCustomerId
        val cachedKey = _state.value.platformKeyCache

        // Check if we have a valid cached key for the current customer
        if (cachedKey != null && cachedKey.cryptoCustomerId == cryptoCustomerId) {
            return Result.success(cachedKey.publishableKey)
        }

        if (cryptoCustomerId == null) {
            return Result.failure(MissingCryptoCustomerException())
        }

        // Fetch platform settings if not available or customer changed
        return cryptoApiRepository.getPlatformSettings(
            cryptoCustomerId = cryptoCustomerId,
            countryHint = null
        )
            .map { it.publishableKey }
            .onSuccess { platformPublishableKey ->
                // Cache the key with the current consumer session
                _state.update {
                    it.copy(
                        platformKeyCache = PlatformKeyCache(
                            publishableKey = platformPublishableKey,
                            cryptoCustomerId = cryptoCustomerId
                        )
                    )
                }
            }
    }

    private fun track(event: OnrampAnalyticsEvent) {
        analyticsService?.track(event)
    }
}

internal data class OnrampState(
    val configuration: OnrampConfiguration? = null,
    val linkControllerState: LinkController.State? = null,
    val cryptoCustomerId: String? = null,
    val collectingPaymentMethodType: PaymentMethodType? = null,
    val checkoutState: CheckoutState? = null,
    val platformKeyCache: PlatformKeyCache? = null,
)

/**
 * Caches the platform publishable key along with the consumer session it was fetched for.
 */
internal data class PlatformKeyCache(
    val publishableKey: String,
    val cryptoCustomerId: String?
)
