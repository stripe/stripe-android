package com.stripe.android.crypto.onramp

import android.content.ContentResolver
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.core.net.toUri
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.core.injection.STRIPE_ACCOUNT_ID
import com.stripe.android.crypto.onramp.di.OnrampPresenterScope
import com.stripe.android.crypto.onramp.exception.PaymentFailedException
import com.stripe.android.crypto.onramp.model.OnrampCallbacks
import com.stripe.android.crypto.onramp.model.OnrampCheckoutResult
import com.stripe.android.crypto.onramp.model.OnrampIdentityVerificationResult
import com.stripe.android.crypto.onramp.model.OnrampStartVerificationResult
import com.stripe.android.crypto.onramp.model.OnrampVerificationResult
import com.stripe.android.crypto.onramp.model.PaymentMethodType
import com.stripe.android.identity.IdentityVerificationSheet
import com.stripe.android.link.LinkController
import com.stripe.android.link.NoLinkAccountFoundException
import com.stripe.android.model.PaymentIntent
import com.stripe.android.payments.paymentlauncher.PaymentLauncher
import com.stripe.android.payments.paymentlauncher.PaymentResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

@OnrampPresenterScope
internal class OnrampPresenterCoordinator @Inject constructor(
    private val interactor: OnrampInteractor,
    @Named(PUBLISHABLE_KEY) publishableKeyProvider: () -> String,
    @Named(STRIPE_ACCOUNT_ID) accountIdProvider: () -> String?,
    linkController: LinkController,
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

    private val paymentLauncher: PaymentLauncher = PaymentLauncher.create(
        activity = activity,
        publishableKey = publishableKeyProvider(),
        stripeAccountId = accountIdProvider(),
        callback = ::handlePaymentLauncherResult
    )

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

        // Observe checkout state changes and react accordingly
        lifecycleOwner.lifecycleScope.launch {
            interactor.checkoutState.collect { checkoutState ->
                handleCheckoutStateChange(checkoutState)
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
     * @param checkoutHandler An async closure that calls your backend to perform a checkout.
     *     Your backend should call Stripe's `/v1/crypto/onramp_sessions/:id/checkout` endpoint with the session ID.
     *     The closure should return the onramp session client secret on success, or throw an Error on failure.
     *     This closure may be called twice: once initially, and once more after handling any required authentication.
     */
    fun performCheckout(
        onrampSessionId: String,
        checkoutHandler: suspend () -> String
    ) {
        coroutineScope.launch {
            interactor.startCheckout(onrampSessionId, checkoutHandler)
        }
    }

    /**
     * Handles checkout state changes from the interactor.
     */
    private fun handleCheckoutStateChange(checkoutState: CheckoutState) {
        when (checkoutState.status) {
            CheckoutState.Status.Idle -> {
                // Nothing to do
            }
            CheckoutState.Status.Processing -> {
                // Nothing to do - let the interactor work
            }
            is CheckoutState.Status.RequiresNextAction -> {
                // Launch PaymentLauncher for next action
                handleNextAction(checkoutState.status.paymentIntent)
            }
            is CheckoutState.Status.Completed -> {
                // Checkout finished - notify callback
                onrampCallbacks.checkoutCallback.onResult(checkoutState.status.result)
            }
        }
    }

    /**
     * Handles the next action for a PaymentIntent using PaymentLauncher.
     * The result will be handled by the PaymentLauncher callback.
     */
    private fun handleNextAction(intent: PaymentIntent) {
        val clientSecret = intent.clientSecret
        if (clientSecret == null) {
            // No client secret - notify failure immediately
            onrampCallbacks.checkoutCallback.onResult(OnrampCheckoutResult.Failed(PaymentFailedException()))
            return
        }

        // Launch the next action - result will be handled by handlePaymentLauncherResult
        paymentLauncher.handleNextActionForPaymentIntent(clientSecret)
    }

    /**
     * Handles PaymentLauncher results and continues the checkout flow via the interactor.
     */
    private fun handlePaymentLauncherResult(paymentResult: PaymentResult) {
        when (paymentResult) {
            is PaymentResult.Completed -> {
                // Next action completed successfully, tell interactor to continue
                coroutineScope.launch {
                    interactor.continueCheckout()
                }
            }
            is PaymentResult.Canceled -> {
                // User canceled the next action
                onrampCallbacks.checkoutCallback.onResult(OnrampCheckoutResult.Canceled)
            }
            is PaymentResult.Failed -> {
                // Next action failed
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
