package com.stripe.android.crypto.onramp

import android.content.ContentResolver
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.DefaultCardFundingFilter
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.utils.StatusBarCompat
import com.stripe.android.crypto.onramp.di.OnrampPresenterScope
import com.stripe.android.crypto.onramp.exception.PaymentFailedException
import com.stripe.android.crypto.onramp.exception.SamsungPayException.Reason
import com.stripe.android.crypto.onramp.model.OnrampCallbacks
import com.stripe.android.crypto.onramp.model.OnrampCollectPaymentMethodResult
import com.stripe.android.crypto.onramp.model.OnrampStartVerificationResult
import com.stripe.android.crypto.onramp.model.OnrampUserAttestationResult
import com.stripe.android.crypto.onramp.model.OnrampVerifyIdentityResult
import com.stripe.android.crypto.onramp.model.OnrampVerifyKycInfoResult
import com.stripe.android.crypto.onramp.model.PaymentMethodSelection
import com.stripe.android.crypto.onramp.model.PaymentMethodType
import com.stripe.android.crypto.onramp.model.SamsungPayAvailabilityResult
import com.stripe.android.crypto.onramp.samsungpay.SamsungPayException
import com.stripe.android.crypto.onramp.samsungpay.SamsungPayLauncher
import com.stripe.android.crypto.onramp.samsungpay.SamsungPayPresentation
import com.stripe.android.crypto.onramp.samsungpay.SamsungPayResult
import com.stripe.android.crypto.onramp.samsungpay.SamsungPayStatus
import com.stripe.android.crypto.onramp.ui.UserAttestationActivityArgs
import com.stripe.android.crypto.onramp.ui.UserAttestationActivityContract
import com.stripe.android.crypto.onramp.ui.UserAttestationActivityResult
import com.stripe.android.crypto.onramp.ui.VerifyKycActivityArgs
import com.stripe.android.crypto.onramp.ui.VerifyKycActivityResult
import com.stripe.android.crypto.onramp.ui.VerifyKycInfoActivityContract
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncher
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncherContractV2
import com.stripe.android.identity.IdentityVerificationSheet
import com.stripe.android.link.LinkController
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
    private val coroutineScope: CoroutineScope,
    private val onrampCallbackIdentifier: String,
    private val samsungPayLauncherFactory: SamsungPayLauncher.Factory,
) {
    private val onrampCallbacksState: OnrampCallbacks.State
        get() = OnrampCallbackReferences[onrampCallbackIdentifier]
            ?: error("OnrampCallbackReferences not registered for key: $onrampCallbackIdentifier")
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

    private val googlePayActivityResultLauncher: ActivityResultLauncher<GooglePayPaymentMethodLauncherContractV2.Args> =
        activity.activityResultRegistry.register(
            "OnrampPresenterCoordinator_GooglePayResultLauncher",
            GooglePayPaymentMethodLauncherContractV2(),
            ::handleGooglePayPaymentSelection
        )

    private val googlePayPaymentMethodLauncher: GooglePayPaymentMethodLauncher? = googlePayConfig()?.let {
        GooglePayPaymentMethodLauncher(
            context = activity,
            lifecycleScope = activity.lifecycleScope,
            activityResultLauncher = googlePayActivityResultLauncher,
            config = it,
            readyCallback = ::handleGooglePayIsReady,
            cardBrandFilter = DefaultCardBrandFilter,
            cardFundingFilter = DefaultCardFundingFilter
        )
    }

    private var samsungPayLauncher: SamsungPayLauncher? = null

    private val verifyKycResultLauncher: ActivityResultLauncher<VerifyKycActivityArgs> =
        activity.activityResultRegistry.register(
            key = "OnrampPresenterCoordinator_VerifyKycResultLauncher($onrampCallbackIdentifier)",
            contract = VerifyKycInfoActivityContract(),
            callback = ::handleVerifyKycResult
        )

    private val userAttestationResultLauncher: ActivityResultLauncher<UserAttestationActivityArgs> =
        activity.activityResultRegistry.register(
            key = "OnrampPresenterCoordinator_UserAttestationResultLauncher($onrampCallbackIdentifier)",
            contract = UserAttestationActivityContract(),
            callback = ::handleUserAttestationResult
        )

    init {
        // Observe Link controller state
        lifecycleOwner.lifecycleScope.launch {
            linkControllerState.collect { state ->
                interactor.onLinkControllerState(state)
                if (state.elementsSessionId != null && samsungPayLauncher == null) {
                    initializeSamsungPay()
                }
            }
        }

        identityVerificationSheet = createIdentityVerificationSheet()

        // Observe checkout state changes and react accordingly
        lifecycleOwner.lifecycleScope.launch {
            interactor.state
                .distinctUntilChangedBy { it.checkoutState }
                .collect { it.checkoutState?.let(::handleCheckoutStateChange) }
        }

        lifecycleOwner.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    googlePayActivityResultLauncher.unregister()
                    samsungPayLauncher?.destroy()
                    verifyKycResultLauncher.unregister()
                    userAttestationResultLauncher.unregister()

                    if (activity.isFinishing) {
                        OnrampCallbackReferences.remove(onrampCallbackIdentifier)
                    }
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

    fun presentUserAttestation() {
        coroutineScope.launch {
            when (val result = interactor.startUserAttestation()) {
                is OnrampStartUserAttestationResult.Completed -> {
                    userAttestationResultLauncher.launch(
                        UserAttestationActivityArgs(result.attestation, result.appearance)
                    )
                }
                is OnrampStartUserAttestationResult.Failed -> {
                    onrampCallbacksState.userAttestationCallback?.onResult(
                        OnrampUserAttestationResult.Failed(result.error)
                    )
                }
            }
        }
    }

    fun collectPaymentMethod(selection: PaymentMethodSelection) {
        interactor.onCollectPaymentMethod(selection.type)

        when (selection) {
            is PaymentMethodSelection.Card,
            is PaymentMethodSelection.BankAccount,
            is PaymentMethodSelection.CardAndBankAccount -> {
                linkPresenter.presentPaymentMethodsForOnramp(
                    email = clientEmail(),
                    paymentMethodTypes = selection.type.toLinkType()
                )
            }
            is PaymentMethodSelection.GooglePay -> {
                coroutineScope.launch {
                    interactor.getOrFetchPlatformKey().fold(
                        onSuccess = {
                            googlePayPaymentMethodLauncher?.present(
                                currencyCode = selection.currencyCode,
                                amount = selection.amount,
                                clientAttributionMetadata = null,
                                transactionId = selection.transactionId,
                                label = selection.label,
                                publishableKey = it
                            )
                        },
                        onFailure = { error ->
                            onrampCallbacksState.collectPaymentCallback.onResult(
                                OnrampCollectPaymentMethodResult.Failed(error)
                            )
                        }
                    )
                }
            }
            is PaymentMethodSelection.SamsungPay -> {
                presentSamsungPay(selection)
            }
        }
    }

    private fun presentSamsungPay(selection: PaymentMethodSelection.SamsungPay) {
        coroutineScope.launch {
            val launcher = samsungPayLauncher
            if (launcher == null) {
                handleSamsungPayPaymentSelection(
                    SamsungPayResult.Failed(
                        SamsungPayException(
                            message = "Samsung Pay is not configured for this onramp coordinator.",
                            cause = null,
                            errorCode = null,
                            reason = Reason.NotConfigured,
                        ),
                    ),
                    platformPublishableKey = null,
                )
                return@launch
            }

            interactor.getOrFetchPlatformKey().fold(
                onSuccess = { platformPublishableKey ->
                    launcher.present(
                        presentation = SamsungPayPresentation(
                            currencyCode = selection.currencyCode,
                            amount = selection.amount,
                            orderNumber = selection.orderNumber,
                        ),
                        callback = { result ->
                            handleSamsungPayPaymentSelection(result, platformPublishableKey)
                        },
                    )
                },
                onFailure = { error ->
                    onrampCallbacksState.collectPaymentCallback.onResult(
                        interactor.handleSamsungPayPlatformKeyFailure(error),
                    )
                },
            )
        }
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
                // Launch PaymentLauncher for next action, unless it has already been launched
                if (interactor.markNextActionLaunched(status)) {
                    handleNextAction(status.paymentIntent, status.platformKey)
                }
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
                interactor.onHandleNextActionCanceled()
            }
            is InternalPaymentResult.Failed -> {
                // Next action failed
                interactor.onHandleNextActionError(paymentResult.throwable)
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

    private fun handleUserAttestationResult(result: UserAttestationActivityResult) {
        coroutineScope.launch {
            val attestationResult = interactor.handleUserAttestationResult(result)

            onrampCallbacksState.userAttestationCallback?.onResult(attestationResult)
        }
    }

    private fun googlePayConfig(): GooglePayPaymentMethodLauncher.Config? =
        interactor.state.value.configurationState?.googlePayConfig

    private fun initializeSamsungPay() {
        val onrampConfiguration = interactor.state.value.configurationState ?: return
        val configuration = onrampConfiguration.samsungPayConfig ?: return
        samsungPayLauncher = samsungPayLauncherFactory.create(
            context = activity.applicationContext,
            configuration = configuration,
            merchantDisplayName = onrampConfiguration.merchantDisplayName,
        ).also { launcher ->
            launcher.getStatus(::handleSamsungPayStatus)
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

    private fun handleGooglePayPaymentSelection(result: GooglePayPaymentMethodLauncher.Result) {
        coroutineScope.launch {
            onrampCallbacksState.collectPaymentCallback.onResult(
                interactor.handleGooglePayPaymentResult(result)
            )
        }
    }

    private fun handleSamsungPayPaymentSelection(
        result: SamsungPayResult,
        platformPublishableKey: String?,
    ) {
        coroutineScope.launch {
            onrampCallbacksState.collectPaymentCallback.onResult(
                interactor.handleSamsungPayPaymentResult(result, platformPublishableKey),
            )
        }
    }

    private fun handleGooglePayIsReady(isReady: Boolean) {
        coroutineScope.launch {
            onrampCallbacksState.googlePayIsReadyCallback?.let { it(isReady) }
        }
    }

    private fun handleSamsungPayStatus(status: SamsungPayStatus) {
        coroutineScope.launch {
            onrampCallbacksState.samsungPayIsReadyCallback?.let { callback ->
                val availability = interactor.handleSamsungPayAvailability(status)
                callback(
                    availability is SamsungPayAvailabilityResult.Available,
                    availability,
                )
            }
        }
    }
}

private fun PaymentMethodType.toLinkType(): List<LinkController.PaymentMethodType>? =
    when (this) {
        PaymentMethodType.Card -> listOf(LinkController.PaymentMethodType.Card)
        PaymentMethodType.BankAccount -> listOf(LinkController.PaymentMethodType.BankAccount)
        PaymentMethodType.CardAndBankAccount -> null
        PaymentMethodType.GooglePay -> error("Google Pay is not supported in LinkController")
        PaymentMethodType.SamsungPay -> error("Samsung Pay is not supported in LinkController")
    }
