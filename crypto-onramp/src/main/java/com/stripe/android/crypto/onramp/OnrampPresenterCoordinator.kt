package com.stripe.android.crypto.onramp

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.utils.StatusBarCompat
import com.stripe.android.crypto.onramp.di.OnrampPresenterScope
import com.stripe.android.crypto.onramp.exception.PaymentFailedException
import com.stripe.android.crypto.onramp.model.OnrampCallbacks
import com.stripe.android.crypto.onramp.model.OnrampCheckoutResult
import com.stripe.android.crypto.onramp.model.OnrampStartVerificationResult
import com.stripe.android.crypto.onramp.model.OnrampVerifyIdentityResult
import com.stripe.android.crypto.onramp.model.OnrampVerifyKycInfoResult
import com.stripe.android.crypto.onramp.model.PaymentMethodType
import com.stripe.android.crypto.onramp.ui.VerifyKycActivityArgs
import com.stripe.android.crypto.onramp.ui.VerifyKycActivityResult
import com.stripe.android.crypto.onramp.ui.VerifyKycInfoActivityContract
import com.stripe.android.identity.IdentityVerificationSheet
import com.stripe.android.link.LinkController
import com.stripe.android.model.PaymentIntent
import com.stripe.android.payments.paymentlauncher.InternalPaymentResult
import com.stripe.android.payments.paymentlauncher.PaymentLauncherFactory
import com.stripe.android.paymentsheet.PaymentSheet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@OnrampPresenterScope
internal class OnrampPresenterCoordinator @Inject constructor(
    private val interactor: OnrampInteractor,
    linkController: LinkController,
    lifecycleOwner: LifecycleOwner,
    private val activity: ComponentActivity,
    onrampCallbacks: OnrampCallbacks,
    private val coroutineScope: CoroutineScope,
) {
    private val onrampCallbacksState = onrampCallbacks.build()
    private val linkControllerState = linkController.state(activity)

    private val linkPresenter = linkController.createPresenter(
        activity = activity,
        presentPaymentMethodsCallback = ::handlePresentPaymentResult,
        authenticationCallback = { /* No-op: Authentication is not used for Onramp */ },
        authorizeCallback = ::handleAuthorizeResult
    )

    private var identityVerificationSheet: IdentityVerificationSheet? = null

    private val paymentLauncherFactory: PaymentLauncherFactory = PaymentLauncherFactory(
        activityResultRegistryOwner = activity,
        lifecycleOwner = lifecycleOwner,
        statusBarColor = StatusBarCompat.color(activity),
        callback = ::handlePaymentLauncherResult
    )

    private val verifyKycResultLauncher: ActivityResultLauncher<VerifyKycActivityArgs> =
        activity.activityResultRegistry.register(
            key = "OnrampPresenterCoordinator_VerifyKycResultLauncher",
            contract = VerifyKycInfoActivityContract(),
            callback = ::handleVerifyKycResult
        )

    init {
        // Observe Link controller state
        lifecycleOwner.lifecycleScope.launch {
            linkControllerState.collect { state ->
                interactor.onLinkControllerState(state)
            }
        }

        identityVerificationSheet = createIdentityVerificationSheet()

        val tag = "OnrampCheckoutObs"
        Log.d(tag, "Collecting interactor=${System.identityHashCode(interactor)} " +
            "stateFlow=${System.identityHashCode(interactor.state)}")

        // Observe checkout state changes and react accordingly
        lifecycleOwner.lifecycleScope.launch {
            interactor.state
                .onEach { s ->
                    Log.d(tag, "EMIT interactor=${System.identityHashCode(interactor)} " +
                        "checkoutState=${s.checkoutState}")
                }
                .distinctUntilChangedBy { it.checkoutState }
                .collect { s ->
                    Log.d(tag, "DISTINCT checkoutState=${s.checkoutState}")
                    s.checkoutState?.let(::handleCheckoutStateChange)
                }
        }

        lifecycleOwner.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    verifyKycResultLauncher.unregister()
                }
            }
        )
    }

    fun verifyIdentity() {
        coroutineScope.launch {
            when (val verification = interactor.startIdentityVerification()) {
                is OnrampStartVerificationResult.Completed -> {
                    verification.response.ephemeralKey?.let {
                        identityVerificationSheet?.present(
                            verificationSessionId = verification.response.id,
                            ephemeralKeySecret = verification.response.ephemeralKey
                        )
                    } ?: run {
                        onrampCallbacksState.verifyIdentityCallback.onResult(
                            OnrampVerifyIdentityResult.Failed(APIException(message = "No ephemeral key found."))
                        )
                    }
                }
                is OnrampStartVerificationResult.Failed -> {
                    onrampCallbacksState.verifyIdentityCallback.onResult(
                        OnrampVerifyIdentityResult.Failed(verification.error)
                    )
                }
            }
        }
    }

    fun verifyKycInfo(updatedAddress: PaymentSheet.Address? = null) {
        coroutineScope.launch {
            when (val verification = interactor.startKycVerification(updatedAddress)) {
                is OnrampStartKycVerificationResult.Completed -> {
                    verifyKycResultLauncher.launch(
                        VerifyKycActivityArgs(verification.response, verification.appearance)
                    )
                }
                is OnrampStartKycVerificationResult.Failed -> {
                    onrampCallbacksState.verifyKycCallback.onResult(
                        OnrampVerifyKycInfoResult.Failed(verification.error)
                    )
                }
            }
        }
    }

    fun collectPaymentMethod(type: PaymentMethodType) {
        interactor.onCollectPaymentMethod(type)
        linkPresenter.presentPaymentMethodsForOnramp(
            email = clientEmail(),
            paymentMethodType = type.toLinkType()
        )
    }

    fun authorize(linkAuthIntentId: String) {
        interactor.onAuthorize()
        linkPresenter.authorize(linkAuthIntentId)
    }

    /**
     * Performs the checkout flow for a crypto onramp session, handling any required authentication steps.
     *
     * @param onrampSessionId The onramp session identifier.
     */
    fun performCheckout(
        onrampSessionId: String
    ) {
        coroutineScope.launch {
            interactor.startCheckout(onrampSessionId)
        }
    }

    /**
     * Handles checkout state changes from the interactor.
     */
    private fun handleCheckoutStateChange(checkoutState: CheckoutState) {
        when (val status = checkoutState.status) {
            is CheckoutState.Status.Processing -> {
                // Nothing to do - let the interactor work
            }
            is CheckoutState.Status.RequiresNextAction -> {
                // Launch PaymentLauncher for next action
                handleNextAction(status.paymentIntent, status.platformKey)
            }
            is CheckoutState.Status.Completed -> {
                // Checkout finished - notify callback
                onrampCallbacksState.checkoutCallback.onResult(status.result)
            }
        }
    }

    /**
     * Handles the next action for a PaymentIntent using PaymentLauncher.
     * The result will be handled by the PaymentLauncher callback.
     */
    private fun handleNextAction(intent: PaymentIntent, platformKey: String) {
        val clientSecret = intent.clientSecret
        if (clientSecret == null) {
            // No client secret - notify failure immediately
            val error = PaymentFailedException()
            interactor.onHandleNextActionError(error)
            onrampCallbacksState.checkoutCallback.onResult(OnrampCheckoutResult.Failed(error))
            return
        }

        // Launch the next action - result will be handled by handlePaymentLauncherResult
        paymentLauncherFactory.create(publishableKey = platformKey).handleNextActionForPaymentIntent(clientSecret)
    }

    /**
     * Handles PaymentLauncher results and continues the checkout flow via the interactor.
     */
    private fun handlePaymentLauncherResult(paymentResult: InternalPaymentResult) {
        when (paymentResult) {
            is InternalPaymentResult.Completed -> {
                // Next action completed successfully, tell interactor to continue
                coroutineScope.launch { interactor.continueCheckout() }
            }
            is InternalPaymentResult.Canceled -> {
                // User canceled the next action
                onrampCallbacksState.checkoutCallback.onResult(
                    OnrampCheckoutResult.Canceled()
                )
            }
            is InternalPaymentResult.Failed -> {
                // Next action failed
                onrampCallbacksState.checkoutCallback.onResult(
                    OnrampCheckoutResult.Failed(paymentResult.throwable)
                )
            }
        }
    }

    private fun handleVerifyKycResult(result: VerifyKycActivityResult) {
        coroutineScope.launch {
            onrampCallbacksState.verifyKycCallback.onResult(
                interactor.handleVerifyKycResult(result)
            )
        }
    }

    private fun clientEmail(): String? =
        interactor.state.value.linkControllerState?.internalLinkAccount?.email

    private fun handleAuthorizeResult(result: LinkController.AuthorizeResult) {
        coroutineScope.launch {
            onrampCallbacksState.authorizeCallback.onResult(
                interactor.handleAuthorizeResult(result)
            )
        }
    }

    private fun handleIdentityVerificationResult(result: IdentityVerificationSheet.VerificationFlowResult) {
        coroutineScope.launch {
            onrampCallbacksState.verifyIdentityCallback.onResult(
                interactor.handleIdentityVerificationResult(result)
            )
        }
    }

    private fun handlePresentPaymentResult(result: LinkController.PresentPaymentMethodsResult) {
        coroutineScope.launch {
            onrampCallbacksState.collectPaymentCallback.onResult(
                interactor.handlePresentPaymentMethodsResult(result, activity)
            )
        }
    }

    private fun createIdentityVerificationSheet(): IdentityVerificationSheet {
        check(com.stripe.android.paymentsheet.R.drawable.stripe_ic_paymentsheet_link_arrow != 0)
        val linkLogoUri: Uri = Uri.Builder()
            .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
            .authority(activity.packageName)
            .appendPath("drawable")
            .appendPath("stripe_ic_paymentsheet_link_arrow")
            .build()

        return IdentityVerificationSheet.onrampCreate(
            from = activity,
            configuration = IdentityVerificationSheet.Configuration(brandLogo = linkLogoUri),
            identityVerificationCallback = ::handleIdentityVerificationResult
        )
    }
}

private fun PaymentMethodType.toLinkType(): LinkController.PaymentMethodType? =
    when (this) {
        PaymentMethodType.Card -> LinkController.PaymentMethodType.Card
        PaymentMethodType.BankAccount -> LinkController.PaymentMethodType.BankAccount
        PaymentMethodType.CardAndBankAccount -> null
    }
