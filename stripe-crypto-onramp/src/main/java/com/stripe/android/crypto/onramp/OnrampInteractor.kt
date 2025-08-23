package com.stripe.android.crypto.onramp

import android.app.Application
import android.content.Context
import com.stripe.android.core.utils.flatMapCatching
import com.stripe.android.crypto.onramp.CheckoutState.Status
import com.stripe.android.crypto.onramp.exception.MissingConsumerSecretException
import com.stripe.android.crypto.onramp.exception.MissingPaymentMethodException
import com.stripe.android.crypto.onramp.exception.PaymentFailedException
import com.stripe.android.crypto.onramp.model.CryptoNetwork
import com.stripe.android.crypto.onramp.model.KycInfo
import com.stripe.android.crypto.onramp.model.LinkUserInfo
import com.stripe.android.crypto.onramp.model.OnrampCheckoutResult
import com.stripe.android.crypto.onramp.model.OnrampCollectPaymentResult
import com.stripe.android.crypto.onramp.model.OnrampConfiguration
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
import com.stripe.android.link.LinkController.AuthenticationResult
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

    private val _checkoutState = MutableStateFlow(CheckoutState())
    val checkoutState: StateFlow<CheckoutState> = _checkoutState.asStateFlow()

    suspend fun configure(configuration: OnrampConfiguration) {
        _state.value = _state.value.copy(configuration = configuration)

        // We are *not* calling `PaymentConfiguration.init()` here because we're relying on
        // `LinkController.configure()` to do it.
        linkController.configure(
            LinkController.Configuration.Builder(
                merchantDisplayName = configuration.merchantDisplayName,
                publishableKey = configuration.publishableKey,
            )
                .appearance(configuration.appearance)
                .build()
        )
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

        secret?.let {
            return cryptoApiRepository.collectKycData(kycInfo, secret)
                .fold(
                    onSuccess = { OnrampKYCResult.Completed },
                    onFailure = { OnrampKYCResult.Failed(it) }
                )
        } ?: run {
            return OnrampKYCResult.Failed(MissingConsumerSecretException())
        }
    }

    suspend fun startIdentityVerification(): OnrampStartVerificationResult {
        val secret = consumerSessionClientSecret()

        secret?.let {
            return cryptoApiRepository.startIdentityVerification(secret)
                .fold(
                    onSuccess = { OnrampStartVerificationResult.Completed(it) },
                    onFailure = { OnrampStartVerificationResult.Failed(it) }
                )
        } ?: run {
            return OnrampStartVerificationResult.Failed(MissingConsumerSecretException())
        }
    }

    suspend fun createCryptoPaymentToken(): OnrampCreateCryptoPaymentTokenResult {
        val secret = consumerSessionClientSecret()
            ?: return OnrampCreateCryptoPaymentTokenResult.Failed(MissingConsumerSecretException())
        return cryptoApiRepository.getPlatformSettings()
            .map { it.publishableKey }
            .mapCatching { apiKey ->
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
        result: AuthenticationResult
    ): OnrampVerificationResult = when (result) {
        is AuthenticationResult.Success -> {
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
        is AuthenticationResult.Failed -> OnrampVerificationResult.Failed(result.error)
        is AuthenticationResult.Canceled -> OnrampVerificationResult.Cancelled()
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
        // Start processing with session info
        _checkoutState.update {
            it.copy(
                onrampSessionId = onrampSessionId,
                checkoutHandler = checkoutHandler,
                status = Status.Processing
            )
        }

        // Perform the actual checkout
        performCheckoutInternal(onrampSessionId, checkoutHandler)
    }

    /**
     * Continues the checkout flow after PaymentLauncher completes a next action.
     * This should be called by the coordinator when PaymentLauncher finishes successfully.
     */
    suspend fun continueCheckout() {
        val currentCheckoutState = _checkoutState.value
        val sessionId = currentCheckoutState.onrampSessionId
        val sessionProvider = currentCheckoutState.checkoutHandler

        if (sessionId != null && sessionProvider != null) {
            // Continue processing with the existing session info
            _checkoutState.update {
                it.copy(status = Status.Processing)
            }
            performCheckoutInternal(sessionId, sessionProvider)
        } else {
            // No session to continue - this shouldn't happen
            _checkoutState.update {
                it.copy(
                    status = Status.Completed(OnrampCheckoutResult.Failed(PaymentFailedException()))
                )
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
        val platformApiKey = cryptoApiRepository.getPlatformSettings().getOrThrow().publishableKey

        val paymentIntent = retrievePaymentIntent(
            onrampSessionId = onrampSessionId,
            onrampSessionClientSecret = checkoutHandler(),
            platformApiKey = platformApiKey
        )

        // Check if the intent is already complete
        val checkoutResult = paymentIntent.toCheckoutResult()
        if (checkoutResult == null) {
            // Requires next action - trigger PaymentLauncher to handle next actions (UI)
            _checkoutState.update {
                it.copy(
                    onrampSessionId = onrampSessionId,
                    checkoutHandler = checkoutHandler,
                    status = Status.RequiresNextAction(paymentIntent)
                )
            }
        } else {
            // Checkout is complete - clear session info and emit result
            _checkoutState.update {
                it.copy(status = Status.Completed(checkoutResult))
            }
        }
    }.getOrElse { error ->
        // Error occurred - clear session info and emit failure
        _checkoutState.update {
            it.copy(status = Status.Completed(OnrampCheckoutResult.Failed(error)))
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
            StripeIntent.Status.Succeeded -> OnrampCheckoutResult.Completed
            StripeIntent.Status.RequiresPaymentMethod -> OnrampCheckoutResult.Failed(PaymentFailedException())
            StripeIntent.Status.RequiresAction -> null // More handling needed
            else -> OnrampCheckoutResult.Failed(PaymentFailedException())
        }
    }
}

internal data class OnrampState(
    val configuration: OnrampConfiguration? = null,
    val linkControllerState: LinkController.State? = null,
)
