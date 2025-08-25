package com.stripe.android.crypto.onramp

import android.app.Application
import android.content.Context
import com.stripe.android.core.utils.flatMapCatching
import com.stripe.android.crypto.onramp.CheckoutState.Status
import com.stripe.android.crypto.onramp.exception.MissingConsumerSecretException
import com.stripe.android.crypto.onramp.exception.MissingPaymentMethodException
import com.stripe.android.crypto.onramp.exception.MissingPlatformSettingsException
import com.stripe.android.crypto.onramp.exception.PaymentFailedException
import com.stripe.android.crypto.onramp.model.CryptoNetwork
import com.stripe.android.crypto.onramp.model.KycInfo
import com.stripe.android.crypto.onramp.model.LinkUserInfo
import com.stripe.android.crypto.onramp.model.OnrampAuthorizeResult
import com.stripe.android.crypto.onramp.model.OnrampCheckoutResult
import com.stripe.android.crypto.onramp.model.OnrampCollectPaymentResult
import com.stripe.android.crypto.onramp.model.OnrampConfiguration
import com.stripe.android.crypto.onramp.model.OnrampConfigurationResult
import com.stripe.android.crypto.onramp.model.OnrampCreateCryptoPaymentTokenResult
import com.stripe.android.crypto.onramp.model.OnrampIdentityVerificationResult
import com.stripe.android.crypto.onramp.model.OnrampKYCResult
import com.stripe.android.crypto.onramp.model.OnrampLinkLookupResult
import com.stripe.android.crypto.onramp.model.OnrampRegisterUserResult
import com.stripe.android.crypto.onramp.model.OnrampSetWalletAddressResult
import com.stripe.android.crypto.onramp.model.OnrampStartVerificationResult
import com.stripe.android.crypto.onramp.model.OnrampVerificationResult
import com.stripe.android.crypto.onramp.model.PaymentOptionDisplayData
import com.stripe.android.crypto.onramp.repositories.CryptoApiRepository
import com.stripe.android.identity.IdentityVerificationSheet
import com.stripe.android.link.LinkController
import com.stripe.android.link.LinkController.ConfigureResult
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.StripeIntent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Suppress("TooManyFunctions")
@Singleton
internal class OnrampInteractor @Inject constructor(
    private val application: Application,
    private val linkController: LinkController,
    private val cryptoApiRepository: CryptoApiRepository,
) {
    private val _state = MutableStateFlow(OnrampState())
    val state: StateFlow<OnrampState> = _state.asStateFlow()

    suspend fun configure(configuration: OnrampConfiguration): OnrampConfigurationResult {
        _state.value = _state.value.copy(configuration = configuration)

        // Fetch and cache platform publishable key
        val platformPublishableKey = getOrFetchPlatformKey()
        if (platformPublishableKey == null) {
            return OnrampConfigurationResult.Failed(MissingPlatformSettingsException())
        }

        // We are *not* calling `PaymentConfiguration.init()` here because we're relying on
        // `LinkController.configure()` to do it.
        val linkResult: ConfigureResult = linkController.configure(
            LinkController.Configuration.Builder(
                merchantDisplayName = configuration.merchantDisplayName,
                publishableKey = configuration.publishableKey,
            )
                .appearance(configuration.appearance)
                .build()
        )

        return when (linkResult) {
            is ConfigureResult.Success -> OnrampConfigurationResult.Completed(success = true)
            is ConfigureResult.Failed -> OnrampConfigurationResult.Failed(linkResult.error)
        }
    }

    suspend fun lookupLinkUser(email: String): OnrampLinkLookupResult {
        return when (val result = linkController.lookupConsumer(email)) {
            is LinkController.LookupConsumerResult.Success -> OnrampLinkLookupResult.Completed(
                isLinkUser = result.isConsumer
            )
            is LinkController.LookupConsumerResult.Failed -> OnrampLinkLookupResult.Failed(
                error = result.error
            )
        }
    }

    suspend fun registerNewLinkUser(info: LinkUserInfo): OnrampRegisterUserResult {
        val result = linkController.registerConsumer(
            email = info.email,
            phone = info.phone,
            country = info.country,
            name = info.fullName,
        )

        return when (result) {
            is LinkController.RegisterConsumerResult.Success -> {
                val secret = consumerSessionClientSecret()
                secret?.let {
                    val permissionsResult = cryptoApiRepository
                        .grantPartnerMerchantPermissions(it)

                    permissionsResult.fold(
                        onSuccess = { result ->
                            OnrampRegisterUserResult.Completed(result.id)
                        },
                        onFailure = { error ->
                            OnrampRegisterUserResult.Failed(error)
                        }
                    )
                } ?: run {
                    return OnrampRegisterUserResult.Failed(MissingConsumerSecretException())
                }
            }
            is LinkController.RegisterConsumerResult.Failed -> {
                OnrampRegisterUserResult.Failed(
                    error = result.error
                )
            }
        }
    }

    suspend fun registerWalletAddress(
        walletAddress: String,
        network: CryptoNetwork
    ): OnrampSetWalletAddressResult {
        val secret = consumerSessionClientSecret()
        return if (secret != null) {
            val result = cryptoApiRepository.setWalletAddress(walletAddress, network, secret)
            result.fold(
                onSuccess = {
                    OnrampSetWalletAddressResult.Completed()
                },
                onFailure = { error ->
                    OnrampSetWalletAddressResult.Failed(error)
                }
            )
        } else {
            OnrampSetWalletAddressResult.Failed(MissingConsumerSecretException())
        }
    }

    suspend fun collectKycInfo(kycInfo: KycInfo): OnrampKYCResult {
        val secret = consumerSessionClientSecret()
            ?: return OnrampKYCResult.Failed(MissingConsumerSecretException())

        return cryptoApiRepository.collectKycData(kycInfo, secret)
            .fold(
                onSuccess = { OnrampKYCResult.Completed },
                onFailure = { OnrampKYCResult.Failed(it) }
            )
    }

    suspend fun startIdentityVerification(): OnrampStartVerificationResult {
        val secret = consumerSessionClientSecret()
            ?: return OnrampStartVerificationResult.Failed(MissingConsumerSecretException())

        return cryptoApiRepository.startIdentityVerification(secret)
            .fold(
                onSuccess = { OnrampStartVerificationResult.Completed(it) },
                onFailure = { OnrampStartVerificationResult.Failed(it) }
            )
    }

    suspend fun createCryptoPaymentToken(): OnrampCreateCryptoPaymentTokenResult {
        val secret = consumerSessionClientSecret()
            ?: return OnrampCreateCryptoPaymentTokenResult.Failed(MissingConsumerSecretException())

        // Get or fetch platform publishable key
        val platformPublishableKey = getOrFetchPlatformKey()
            ?: return OnrampCreateCryptoPaymentTokenResult.Failed(MissingPlatformSettingsException())

        return runCatching {
            val apiKey = platformPublishableKey
            when (val result = linkController.createPaymentMethodForOnramp(apiKey = apiKey)) {
                is LinkController.CreatePaymentMethodResult.Success -> {
                    result.paymentMethod
                }
                is LinkController.CreatePaymentMethodResult.Failed -> {
                    throw result.error
                }
            }
        }
            .flatMapCatching { paymentMethod ->
                cryptoApiRepository.createPaymentToken(
                    consumerSessionClientSecret = secret,
                    paymentMethod = paymentMethod.id!!,
                )
            }
            .map { cryptoPayment -> cryptoPayment.id }
            .fold(
                onSuccess = { OnrampCreateCryptoPaymentTokenResult.Completed(it) },
                onFailure = { OnrampCreateCryptoPaymentTokenResult.Failed(it) }
            )
    }

    suspend fun handleAuthenticationResult(
        result: LinkController.AuthenticationResult
    ): OnrampVerificationResult = when (result) {
        is LinkController.AuthenticationResult.Success -> {
            val secret = consumerSessionClientSecret()
            secret?.let {
                val permissionsResult = cryptoApiRepository
                    .grantPartnerMerchantPermissions(it)
                permissionsResult.fold(
                    onSuccess = { result ->
                        OnrampVerificationResult.Completed(result.id)
                    },
                    onFailure = { error ->
                        OnrampVerificationResult.Failed(error)
                    }
                )
            } ?: OnrampVerificationResult.Failed(
                MissingConsumerSecretException()
            )
        }
        is LinkController.AuthenticationResult.Failed -> OnrampVerificationResult.Failed(result.error)
        is LinkController.AuthenticationResult.Canceled -> OnrampVerificationResult.Cancelled()
    }

    suspend fun handleAuthorizeResult(
        result: LinkController.AuthorizeResult
    ): OnrampAuthorizeResult = when (result) {
        is LinkController.AuthorizeResult.Consented -> {
            val secret = consumerSessionClientSecret()
            secret?.let {
                val permissionsResult = cryptoApiRepository
                    .grantPartnerMerchantPermissions(it)
                permissionsResult.fold(
                    onSuccess = { result ->
                        OnrampAuthorizeResult.Consented(result.id)
                    },
                    onFailure = { error ->
                        OnrampAuthorizeResult.Failed(error)
                    }
                )
            } ?: OnrampAuthorizeResult.Failed(
                MissingConsumerSecretException()
            )
        }
        is LinkController.AuthorizeResult.Denied ->
            OnrampAuthorizeResult.Denied()
        is LinkController.AuthorizeResult.Canceled ->
            OnrampAuthorizeResult.Canceled()
        is LinkController.AuthorizeResult.Failed ->
            OnrampAuthorizeResult.Failed(result.error)
    }

    fun handleIdentityVerificationResult(
        result: IdentityVerificationSheet.VerificationFlowResult
    ): OnrampIdentityVerificationResult = when (result) {
        is IdentityVerificationSheet.VerificationFlowResult.Completed -> {
            OnrampIdentityVerificationResult.Completed()
        }
        is IdentityVerificationSheet.VerificationFlowResult.Failed ->
            OnrampIdentityVerificationResult.Failed(result.throwable)
        is IdentityVerificationSheet.VerificationFlowResult.Canceled ->
            OnrampIdentityVerificationResult.Cancelled()
    }

    fun handleSelectPaymentResult(
        result: LinkController.PresentPaymentMethodsResult,
        context: Context,
    ): OnrampCollectPaymentResult = when (result) {
        is LinkController.PresentPaymentMethodsResult.Success -> {
            linkController.state(context).value.selectedPaymentMethodPreview?.let {
                OnrampCollectPaymentResult.Completed(
                    displayData = PaymentOptionDisplayData(
                        icon = it.icon,
                        label = it.label,
                        sublabel = it.sublabel
                    )
                )
            } ?: run {
                OnrampCollectPaymentResult.Failed(MissingPaymentMethodException())
            }
        }
        is LinkController.PresentPaymentMethodsResult.Failed ->
            OnrampCollectPaymentResult.Failed(result.error)
        is LinkController.PresentPaymentMethodsResult.Canceled ->
            OnrampCollectPaymentResult.Cancelled()
    }

    private fun consumerSessionClientSecret(): String? =
        _state.value.linkControllerState?.internalLinkAccount?.consumerSessionClientSecret
            ?: linkController.state(application).value.internalLinkAccount?.consumerSessionClientSecret

    fun onLinkControllerState(linkState: LinkController.State) {
        _state.value = _state.value.copy(linkControllerState = linkState)
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

        performCheckoutInternal(
            onrampSessionId = onrampSessionId,
            checkoutHandler = checkoutHandler
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
                    checkoutHandler = status.checkoutHandler
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
    private suspend fun performCheckoutInternal(
        onrampSessionId: String,
        checkoutHandler: suspend () -> String
    ) = runCatching {
        // Get or fetch platform publishable key
        val platformApiKey = getOrFetchPlatformKey()
        if (platformApiKey == null) {
            _state.update {
                it.copy(
                    checkoutState = CheckoutState(
                        status = Status.Completed(
                            OnrampCheckoutResult.Failed(MissingPlatformSettingsException())
                        )
                    )
                )
            }
            return
        }
        val paymentIntent = retrievePaymentIntent(
            onrampSessionId = onrampSessionId,
            onrampSessionClientSecret = checkoutHandler(),
            platformApiKey = platformApiKey
        )

        // Check if the intent is already complete
        val checkoutResult = paymentIntent.toCheckoutResult()
        if (checkoutResult == null) {
            // Requires next action - trigger PaymentLauncher to handle next actions (UI)
            _state.update {
                it.copy(
                    checkoutState = CheckoutState(
                        status = Status.RequiresNextAction(
                            platformKey = platformApiKey,
                            onrampSessionId = onrampSessionId,
                            checkoutHandler = checkoutHandler,
                            paymentIntent = paymentIntent
                        )
                    )
                )
            }
        } else {
            // Checkout is complete - emit result
            _state.update {
                it.copy(
                    checkoutState = CheckoutState(
                        status = Status.Completed(checkoutResult)
                    )
                )
            }
        }
    }.getOrElse { error ->
        // Error occurred - emit failure
        _state.update {
            it.copy(
                checkoutState = CheckoutState(
                    status = Status.Completed(OnrampCheckoutResult.Failed(error))
                )
            )
        }
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
    ): PaymentIntent {
        // Get the onramp session to extract the payment_intent_client_secret
        val onrampSession = cryptoApiRepository.getOnrampSession(
            sessionId = onrampSessionId,
            sessionClientSecret = onrampSessionClientSecret
        ).getOrThrow()

        // Retrieve and return the PaymentIntent using the special publishable key
        return cryptoApiRepository.retrievePaymentIntent(
            clientSecret = onrampSession.paymentIntentClientSecret,
            publishableKey = platformApiKey
        ).getOrThrow()
    }

    /**
     * Maps a PaymentIntent status to a CheckoutResult, or returns null if more handling is needed.
     */
    private fun PaymentIntent.toCheckoutResult(): OnrampCheckoutResult? {
        return when (status) {
            StripeIntent.Status.Succeeded -> OnrampCheckoutResult.Completed()
            StripeIntent.Status.RequiresPaymentMethod -> OnrampCheckoutResult.Failed(PaymentFailedException())
            StripeIntent.Status.RequiresAction -> null // More handling needed
            else -> OnrampCheckoutResult.Failed(PaymentFailedException())
        }
    }

    /**
     * Gets the platform publishable key from state, or fetches it if not available.
     * Returns null if fetch fails or key is null.
     */
    private suspend fun getOrFetchPlatformKey(): String? {
        _state.value.platformPublishableKey?.let { return it }

        // Fetch platform settings if not available
        val platformSettingsResult = cryptoApiRepository.getPlatformSettings()
        if (platformSettingsResult.isFailure) {
            return null
        }

        val platformPublishableKey = platformSettingsResult.getOrNull()?.publishableKey
        if (platformPublishableKey != null) {
            _state.update {
                it.copy(platformPublishableKey = platformPublishableKey)
            }
        }
        return platformPublishableKey
    }
}

internal data class OnrampState(
    val configuration: OnrampConfiguration? = null,
    val linkControllerState: LinkController.State? = null,
    val checkoutState: CheckoutState? = null,
    val platformPublishableKey: String? = null,
)
