package com.stripe.android.crypto.onramp

import android.content.ContentResolver
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.core.net.toUri
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.stripe.android.core.exception.APIException
import com.stripe.android.crypto.onramp.di.OnrampPresenterScope
import com.stripe.android.crypto.onramp.model.OnrampCallbacks
import com.stripe.android.crypto.onramp.model.OnrampIdentityVerificationResult
import com.stripe.android.crypto.onramp.model.OnrampStartVerificationResult
import com.stripe.android.crypto.onramp.model.OnrampVerificationResult
import com.stripe.android.crypto.onramp.model.PaymentMethodType
import com.stripe.android.identity.IdentityVerificationSheet
import com.stripe.android.link.LinkController
import com.stripe.android.link.NoLinkAccountFoundException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.launch
import javax.inject.Inject

@OnrampPresenterScope
internal class OnrampPresenterCoordinator @Inject constructor(
    private val linkController: LinkController,
    private val interactor: OnrampInteractor,
    lifecycleOwner: LifecycleOwner,
    private val activity: ComponentActivity,
    private val onrampCallbacks: OnrampCallbacks,
    private val coroutineScope: CoroutineScope,
) {
    private val linkControllerState = linkController.state(activity)

    private val linkPresenter = linkController.createPresenter(
        activity = activity,
        presentPaymentMethodsCallback = ::handleSelectPaymentResult,
        authenticationCallback = ::handleAuthenticationResult
    )

    private var identityVerificationSheet: IdentityVerificationSheet? = null

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
