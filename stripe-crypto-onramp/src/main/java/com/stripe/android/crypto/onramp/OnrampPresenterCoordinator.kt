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
import com.stripe.android.identity.IdentityVerificationSheet
import com.stripe.android.link.LinkController
import com.stripe.android.link.NoLinkAccountFoundException
import kotlinx.coroutines.CoroutineScope
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

    private val linkPresenter = linkController.createPresenter(
        activity = activity,
        presentPaymentMethodsCallback = { result ->
            // Handle payment methods result if needed
        },
        authenticationCallback = ::handleAuthenticationResult
    )

    private val fallbackMerchantLogoUri: Uri = Uri.Builder()
        .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
        .authority(activity.packageName)
        .appendPath("drawable")
        .appendPath("stripe_ic_business")
        .build()

    private val sheet = IdentityVerificationSheet.create(
        from = activity,
        configuration = IdentityVerificationSheet.Configuration(
            brandLogo = linkController.state(activity).value.merchantLogoUrl?.toUri() ?: fallbackMerchantLogoUri,
        ),
        identityVerificationCallback = ::handleIdentityVerificationResult,
    )

    private val currentLinkAccount: LinkController.LinkAccount?
        get() = interactor.state.value.linkControllerState?.internalLinkAccount

    init {
        // Observe Link controller state
        lifecycleOwner.lifecycleScope.launch {
            linkController.state(activity).collect { state ->
                interactor.onLinkControllerState(state)
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
                        sheet.present(
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
}
