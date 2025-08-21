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
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

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

    // Current continuation for PaymentLauncher operations
    private var paymentLauncherContinuation: Continuation<OnrampCheckoutResult>? = null

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
                // First, attempt to check out and get the PaymentIntent
                val paymentIntent = performCheckoutAndRetrievePaymentIntent(
                    platformApiKey = platformApiKey,
                    onrampSessionId = onrampSessionId,
                    onrampSessionClientSecretProvider = onrampSessionClientSecretProvider
                )

                // Check if the intent is already complete
                mapIntentToCheckoutResult(paymentIntent)?.let { result ->
                    onrampCallbacks.checkoutCallback.onResult(result)
                    return@launch
                }

                // Handle any required next action (e.g., 3DS authentication)
                when (val handledIntentResult = handleNextAction(paymentIntent)) {
                    is OnrampCheckoutResult.Completed -> {
                        if (paymentIntent.status == StripeIntent.Status.Succeeded ||
                            paymentIntent.status == StripeIntent.Status.RequiresCapture
                        ) {
                            // After successful next_action handling, attempt checkout again to complete the payment
                            val finalPaymentIntent = performCheckoutAndRetrievePaymentIntent(
                                platformApiKey = platformApiKey,
                                onrampSessionId = onrampSessionId,
                                onrampSessionClientSecretProvider = onrampSessionClientSecretProvider,
                            )

                            // Map the final PaymentIntent status to a checkout result
                            mapIntentToCheckoutResult(finalPaymentIntent)?.let { checkoutResult ->
                                onrampCallbacks.checkoutCallback.onResult(checkoutResult)
                            } ?: run {
                                onrampCallbacks.checkoutCallback
                                    .onResult(OnrampCheckoutResult.Failed(PaymentFailedException()))
                            }
                        } else {
                            onrampCallbacks.checkoutCallback
                                .onResult(OnrampCheckoutResult.Failed(PaymentFailedException()))
                        }
                    }
                    OnrampCheckoutResult.Canceled -> {
                        onrampCallbacks.checkoutCallback.onResult(handledIntentResult)
                    }
                    is OnrampCheckoutResult.Failed -> {
                        onrampCallbacks.checkoutCallback
                            .onResult(handledIntentResult)
                    }
                }
            }.onFailure {
                onrampCallbacks.checkoutCallback.onResult(OnrampCheckoutResult.Failed(it))
            }
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
     */
    private suspend fun handleNextAction(intent: PaymentIntent): OnrampCheckoutResult {
        val clientSecret = intent.clientSecret
            ?: return OnrampCheckoutResult.Failed(PaymentFailedException())

        return suspendCoroutine { continuation ->
            // Store the continuation so the callback can resume it
            paymentLauncherContinuation = continuation
            paymentLauncher.handleNextActionForPaymentIntent(clientSecret)
        }
    }

    /**
     * Handles PaymentLauncher results and resumes the appropriate continuation.
     */
    private fun handlePaymentLauncherResult(paymentResult: PaymentResult) {
        val continuation = paymentLauncherContinuation
        if (continuation != null) {
            paymentLauncherContinuation = null // Clear the continuation
            val onrampCheckoutResult = when (paymentResult) {
                is PaymentResult.Completed -> OnrampCheckoutResult.Completed
                is PaymentResult.Canceled -> OnrampCheckoutResult.Canceled
                is PaymentResult.Failed -> OnrampCheckoutResult.Failed(paymentResult.throwable)
            }
            continuation.resume(onrampCheckoutResult)
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
