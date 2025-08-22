package com.stripe.android.crypto.onramp

import android.content.ContentResolver
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.core.net.toUri
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.stripe.android.core.exception.APIException
import com.stripe.android.crypto.onramp.di.OnrampPresenterScope
import com.stripe.android.crypto.onramp.exception.PaymentFailedException
import com.stripe.android.crypto.onramp.model.OnrampCallbacks
import com.stripe.android.crypto.onramp.model.OnrampCheckoutResult
import com.stripe.android.crypto.onramp.model.OnrampIdentityVerificationResult
import com.stripe.android.crypto.onramp.model.OnrampStartVerificationResult
import com.stripe.android.crypto.onramp.model.OnrampVerificationResult
import com.stripe.android.crypto.onramp.model.PaymentMethodType
import com.stripe.android.crypto.onramp.repositories.CryptoApiRepository
import com.stripe.android.identity.IdentityVerificationSheet
import com.stripe.android.link.LinkController
import com.stripe.android.link.NoLinkAccountFoundException
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.payments.paymentlauncher.PaymentLauncher
import com.stripe.android.payments.paymentlauncher.PaymentResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.launch
import javax.inject.Inject

@OnrampPresenterScope
internal class OnrampPresenterCoordinator @Inject constructor(
    private val linkController: LinkController,
    private val interactor: OnrampInteractor,
    private val cryptoApiRepository: CryptoApiRepository,
    lifecycleOwner: LifecycleOwner,
    private val activity: ComponentActivity,
    private val onrampCallbacks: OnrampCallbacks,
    private val coroutineScope: CoroutineScope,
) {
    private val linkControllerState = linkController.state(activity)

    private val linkPresenter = linkController.createPresenter(
        activity = activity,
        presentPaymentMethodsCallback = ::handleSelectPaymentResult,
        authenticationCallback = ::handleAuthenticationResult,
        authorizeCallback = {}
    )

    private var identityVerificationSheet: IdentityVerificationSheet? = null

    private var currentCheckoutParams: CheckoutParams? = null

    private val paymentLauncher: PaymentLauncher by lazy {
        PaymentLauncher.create(
            activity = activity,
            publishableKey = interactor.state.value.configuration?.publishableKey ?: "",
            stripeAccountId = interactor.state.value.configuration?.stripeAccountId,
            callback = ::handlePaymentLauncherResult
        )
    }

    private val currentLinkAccount: LinkController.LinkAccount?
        get() = interactor.state.value.linkControllerState?.internalLinkAccount

    init {
        // Observe Link controller state
        lifecycleOwner.lifecycleScope.launch {
            linkControllerState.collect { state ->
                interactor.onLinkControllerState(state)
            }
        }

        // Update the identity verification sheet when the merchant logo URL changes.
        lifecycleOwner.lifecycleScope.launch {
            linkControllerState.distinctUntilChangedBy { it.merchantLogoUrl }.collect { state ->
                identityVerificationSheet = createIdentityVerificationSheet(state.merchantLogoUrl)
            }
        }
    }

    fun presentForVerification() {
        val email = currentLinkAccount?.email
        if (email == null) {
            onrampCallbacks.authenticationCallback.onResult(
                OnrampVerificationResult.Failed(NoLinkAccountFoundException())
            )
            return
        }
        linkPresenter.authenticateExistingConsumer(email)
    }

    fun promptForIdentityVerification() {
        coroutineScope.launch {
            when (val verification = interactor.startIdentityVerification()) {
                is OnrampStartVerificationResult.Completed -> {
                    verification.response.ephemeralKey?.let {
                        identityVerificationSheet?.present(
                            verificationSessionId = verification.response.id,
                            ephemeralKeySecret = verification.response.ephemeralKey
                        )
                    } ?: run {
                        onrampCallbacks.identityVerificationCallback.onResult(
                            OnrampIdentityVerificationResult.Failed(APIException(message = "No ephemeral key found."))
                        )
                    }
                }
                is OnrampStartVerificationResult.Failed -> {
                    onrampCallbacks.identityVerificationCallback.onResult(
                        OnrampIdentityVerificationResult.Failed(verification.error)
                    )
                }
            }
        }
    }

    fun collectPaymentMethod(type: PaymentMethodType) {
        linkPresenter.presentPaymentMethods(
            email = clientEmail(),
            paymentMethodType = type.toLinkType()
        )
    }

    /**
     * Performs the checkout flow for a crypto onramp session, handling any required authentication steps.
     *
     * @param onrampSessionId The onramp session identifier.
     * @param onrampSessionClientSecretProvider An async closure that calls your backend to perform a checkout.
     *     Your backend should call Stripe's `/v1/crypto/onramp_sessions/:id/checkout` endpoint with the session ID.
     *     The closure should return the onramp session client secret on success, or throw an Error on failure.
     *     This closure may be called twice: once initially, and once more after handling any required authentication.
     */
    fun performCheckout(
        onrampSessionId: String,
        onrampSessionClientSecretProvider: suspend () -> String
    ) {
        coroutineScope.launch {
            runCatching {
                val platformApiKey = cryptoApiRepository.getPlatformSettings().getOrThrow().publishableKey

                // Store checkout parameters for potential recursive calls
                currentCheckoutParams = CheckoutParams(
                    platformApiKey = platformApiKey,
                    onrampSessionId = onrampSessionId,
                    onrampSessionClientSecretProvider = onrampSessionClientSecretProvider
                )

                performCheckoutRecursively()
            }.onFailure {
                currentCheckoutParams = null // Clear parameters on error
                onrampCallbacks.checkoutCallback.onResult(OnrampCheckoutResult.Failed(it))
            }
        }
    }

    /**
     * Recursively performs the checkout flow, handling next actions as needed.
     */
    private suspend fun performCheckoutRecursively() {
        val params = currentCheckoutParams
            ?: return onrampCallbacks.checkoutCallback.onResult(OnrampCheckoutResult.Failed(PaymentFailedException()))

        runCatching {
            // Perform checkout and get PaymentIntent
            val paymentIntent = performCheckoutAndRetrievePaymentIntent(
                onrampSessionId = params.onrampSessionId,
                onrampSessionClientSecretProvider = params.onrampSessionClientSecretProvider,
                platformApiKey = params.platformApiKey
            )

            // Check if the intent is already complete
            mapIntentToCheckoutResult(paymentIntent)?.let { result ->
                currentCheckoutParams = null // Clear parameters
                onrampCallbacks.checkoutCallback.onResult(result)
                return
            }

            // Handle any required next action (e.g., 3DS authentication)
            // The PaymentLauncher callback will handle the recursion
            handleNextAction(paymentIntent)
        }.onFailure {
            currentCheckoutParams = null
            onrampCallbacks.checkoutCallback.onResult(OnrampCheckoutResult.Failed(it))
        }
    }

    /**
     * Performs checkout and retrieves the resulting PaymentIntent.
     *
     * @param onrampSessionId The onramp session identifier.
     * @param onrampSessionClientSecretProvider A suspend function that calls your backend to perform a checkout.
     * @return The PaymentIntent after checkout.
     */
    private suspend fun performCheckoutAndRetrievePaymentIntent(
        onrampSessionId: String,
        onrampSessionClientSecretProvider: suspend () -> String,
        platformApiKey: String
    ): PaymentIntent {
        // Call the backend to perform checkout
        val onrampSessionClientSecret = onrampSessionClientSecretProvider()

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
    private fun mapIntentToCheckoutResult(intent: PaymentIntent): OnrampCheckoutResult? {
        return when (intent.status) {
            StripeIntent.Status.Succeeded -> OnrampCheckoutResult.Completed
            StripeIntent.Status.RequiresPaymentMethod -> OnrampCheckoutResult.Failed(PaymentFailedException())
            StripeIntent.Status.RequiresAction -> null // More handling needed
            else -> OnrampCheckoutResult.Failed(PaymentFailedException())
        }
    }

    /**
     * Handles the next action for a PaymentIntent using PaymentLauncher.
     * The result will be handled by the PaymentLauncher callback.
     */
    private fun handleNextAction(intent: PaymentIntent) {
        val clientSecret = intent.clientSecret
        if (clientSecret == null) {
            currentCheckoutParams = null // Clear parameters
            onrampCallbacks.checkoutCallback.onResult(OnrampCheckoutResult.Failed(PaymentFailedException()))
            return
        }

        // Launch the next action - result will be handled by handlePaymentLauncherResult
        paymentLauncher.handleNextActionForPaymentIntent(clientSecret)
    }

    /**
     * Handles PaymentLauncher results and continues the checkout flow recursively if needed.
     */
    private fun handlePaymentLauncherResult(paymentResult: PaymentResult) {
        when (paymentResult) {
            is PaymentResult.Completed -> {
                // Next action completed successfully, recursively call checkout to complete payment
                coroutineScope.launch {
                    performCheckoutRecursively()
                }
            }
            is PaymentResult.Canceled -> {
                // User canceled the next action
                currentCheckoutParams = null // Clear parameters
                onrampCallbacks.checkoutCallback.onResult(OnrampCheckoutResult.Canceled)
            }
            is PaymentResult.Failed -> {
                // Next action failed
                currentCheckoutParams = null // Clear parameters
                onrampCallbacks.checkoutCallback.onResult(OnrampCheckoutResult.Failed(paymentResult.throwable))
            }
        }
    }

    private fun clientEmail(): String? =
        interactor.state.value.linkControllerState?.internalLinkAccount?.email

    private fun handleAuthenticationResult(result: LinkController.AuthenticationResult) {
        coroutineScope.launch {
            onrampCallbacks.authenticationCallback.onResult(
                interactor.handleAuthenticationResult(result)
            )
        }
    }

    private fun handleIdentityVerificationResult(result: IdentityVerificationSheet.VerificationFlowResult) {
        coroutineScope.launch {
            onrampCallbacks.identityVerificationCallback.onResult(
                interactor.handleIdentityVerificationResult(result)
            )
        }
    }

    private fun handleSelectPaymentResult(result: LinkController.PresentPaymentMethodsResult) {
        coroutineScope.launch {
            onrampCallbacks.selectPaymentCallback.onResult(
                interactor.handleSelectPaymentResult(result, activity)
            )
        }
    }

    private fun createIdentityVerificationSheet(merchantLogoUrl: String?): IdentityVerificationSheet {
        val fallbackMerchantLogoUri: Uri = Uri.Builder()
            .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
            .authority(activity.packageName)
            .appendPath("drawable")
            .appendPath("stripe_ic_business")
            .build()

        val logoUri = merchantLogoUrl?.toUri() ?: fallbackMerchantLogoUri

        return IdentityVerificationSheet.onrampCreate(
            from = activity,
            configuration = IdentityVerificationSheet.Configuration(brandLogo = logoUri),
            identityVerificationCallback = ::handleIdentityVerificationResult
        )
    }
}

private fun PaymentMethodType.toLinkType(): LinkController.PaymentMethodType =
    when (this) {
        PaymentMethodType.Card -> LinkController.PaymentMethodType.Card
        PaymentMethodType.BankAccount -> LinkController.PaymentMethodType.BankAccount
    }

private data class CheckoutParams(
    val platformApiKey: String,
    val onrampSessionId: String,
    val onrampSessionClientSecretProvider: suspend () -> String
)
