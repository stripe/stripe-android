package com.stripe.android.crypto.onramp

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.utils.StatusBarCompat
import com.stripe.android.crypto.onramp.di.OnrampPresenterScope
import com.stripe.android.crypto.onramp.exception.PaymentFailedException
import com.stripe.android.crypto.onramp.model.OnrampAuthenticateResult
import com.stripe.android.crypto.onramp.model.OnrampCallbacks
import com.stripe.android.crypto.onramp.model.OnrampCheckoutResult
import com.stripe.android.crypto.onramp.model.OnrampStartVerificationResult
import com.stripe.android.crypto.onramp.model.OnrampVerifyIdentityResult
import com.stripe.android.crypto.onramp.model.PaymentMethodType
import com.stripe.android.crypto.onramp.ui.KYCRefreshScreen
import com.stripe.android.identity.IdentityVerificationSheet
import com.stripe.android.link.LinkAppearance
import com.stripe.android.link.LinkController
import com.stripe.android.link.NoLinkAccountFoundException
import com.stripe.android.model.PaymentIntent
import com.stripe.android.payments.paymentlauncher.InternalPaymentResult
import com.stripe.android.payments.paymentlauncher.PaymentLauncherFactory
import com.stripe.android.paymentsheet.PaymentSheet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.launch
import javax.inject.Inject

@OnrampPresenterScope
internal class OnrampPresenterCoordinator @Inject constructor(
    private val interactor: OnrampInteractor,
    linkController: LinkController,
    lifecycleOwner: LifecycleOwner,
    private val activity: ComponentActivity,
    private val onrampCallbacks: OnrampCallbacks,
    private val coroutineScope: CoroutineScope,
) {
    private val linkControllerState = linkController.state(activity)

    private val linkPresenter = linkController.createPresenter(
        activity = activity,
        presentPaymentMethodsCallback = ::handlePresentPaymentResult,
        authenticationCallback = ::handleAuthenticationResult,
        authorizeCallback = ::handleAuthorizeResult
    )

    private var identityVerificationSheet: IdentityVerificationSheet? = null

    private val paymentLauncherFactory: PaymentLauncherFactory = PaymentLauncherFactory(
        activityResultRegistryOwner = activity,
        lifecycleOwner = lifecycleOwner,
        statusBarColor = StatusBarCompat.color(activity),
        callback = ::handlePaymentLauncherResult
    )

    private val currentLinkAccount: LinkController.LinkAccount?
        get() = interactor.state.value.linkControllerState?.internalLinkAccount

    private val onrampActivityResultLauncher: ActivityResultLauncher<OnrampActivityContractArgs>

    init {
        onrampActivityResultLauncher = activity.registerForActivityResult(OnrampActivityContract()) { result ->
            // Handle the result (OnrampActivityContractResult)
            // e.g. onrampCallbacks.verifyKycCallback.onResult(result)
        }

        // Observe Link controller state
        lifecycleOwner.lifecycleScope.launch {
            linkControllerState.collect { state ->
                interactor.onLinkControllerState(state)
            }
        }

        identityVerificationSheet = createIdentityVerificationSheet()

        // Observe checkout state changes and react accordingly
        lifecycleOwner.lifecycleScope.launch {
            interactor.state
                .distinctUntilChangedBy { it.checkoutState }
                .collect { it.checkoutState?.let(::handleCheckoutStateChange) }
        }
    }

    fun authenticateUser() {
        val email = currentLinkAccount?.email
        if (email == null) {
            onrampCallbacks.authenticateUserCallback.onResult(
                OnrampAuthenticateResult.Failed(NoLinkAccountFoundException())
            )
            return
        }
        interactor.onAuthenticateUser()
        linkPresenter.authenticateExistingConsumer(email)
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
                        onrampCallbacks.verifyIdentityCallback.onResult(
                            OnrampVerifyIdentityResult.Failed(APIException(message = "No ephemeral key found."))
                        )
                    }
                }
                is OnrampStartVerificationResult.Failed -> {
                    onrampCallbacks.verifyIdentityCallback.onResult(
                        OnrampVerifyIdentityResult.Failed(verification.error)
                    )
                }
            }
        }
    }

    fun verifyKycInfo(updatedAddress: PaymentSheet.Address? = null) {
        onrampActivityResultLauncher.launch(OnrampActivityContractArgs(updatedAddress, appearance()))
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
                onrampCallbacks.checkoutCallback.onResult(status.result)
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
            onrampCallbacks.checkoutCallback.onResult(OnrampCheckoutResult.Failed(error))
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
                onrampCallbacks.checkoutCallback.onResult(
                    OnrampCheckoutResult.Canceled()
                )
            }
            is InternalPaymentResult.Failed -> {
                // Next action failed
                onrampCallbacks.checkoutCallback.onResult(
                    OnrampCheckoutResult.Failed(paymentResult.throwable)
                )
            }
        }
    }

    private fun appearance(): LinkAppearance? =
        interactor.state.value.configuration?.appearance

    private fun clientEmail(): String? =
        interactor.state.value.linkControllerState?.internalLinkAccount?.email

    private fun handleAuthenticationResult(result: LinkController.AuthenticationResult) {
        coroutineScope.launch {
            onrampCallbacks.authenticateUserCallback.onResult(
                interactor.handleAuthenticationResult(result)
            )
        }
    }

    private fun handleAuthorizeResult(result: LinkController.AuthorizeResult) {
        coroutineScope.launch {
            onrampCallbacks.authorizeCallback.onResult(
                interactor.handleAuthorizeResult(result)
            )
        }
    }

    private fun handleIdentityVerificationResult(result: IdentityVerificationSheet.VerificationFlowResult) {
        coroutineScope.launch {
            onrampCallbacks.verifyIdentityCallback.onResult(
                interactor.handleIdentityVerificationResult(result)
            )
        }
    }

    private fun handlePresentPaymentResult(result: LinkController.PresentPaymentMethodsResult) {
        coroutineScope.launch {
            onrampCallbacks.collectPaymentCallback.onResult(
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

private fun PaymentMethodType.toLinkType(): LinkController.PaymentMethodType =
    when (this) {
        PaymentMethodType.Card -> LinkController.PaymentMethodType.Card
        PaymentMethodType.BankAccount -> LinkController.PaymentMethodType.BankAccount
    }

class KycFullScreenActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val address = intent.getParcelableExtra<PaymentSheet.Address>("updatedAddress")
        val linkAppearance = intent.getParcelableExtra<LinkAppearance>("linkAppearance")
        enableEdgeToEdge()

        setContent {
            KYCRefreshScreen(
                linkAppearance,
                address,
                onClose = { finish() },
                onEdit = { },
                onConfirm = { }
            )
        }
    }
}
data class OnrampActivityContractArgs(
    val updatedAddress: PaymentSheet.Address?,
    val linkAppearance: LinkAppearance?
)

data class OnrampActivityContractResult(
    val success: Boolean
)

class OnrampActivityContract : ActivityResultContract<OnrampActivityContractArgs, OnrampActivityContractResult>() {
    override fun createIntent(context: Context, input: OnrampActivityContractArgs): Intent {
        return Intent(context, KycFullScreenActivity::class.java).apply {
            putExtra("updatedAddress", input.updatedAddress)
            putExtra("linkAppearance", input.linkAppearance)
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): OnrampActivityContractResult {
        val success = resultCode == Activity.RESULT_OK
        return OnrampActivityContractResult(success)
    }
}
