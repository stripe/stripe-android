package com.stripe.android.crypto.onramp

import android.content.ContentResolver
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.stripe.android.EphemeralKey
import com.stripe.android.crypto.onramp.di.OnrampPresenterScope
import com.stripe.android.crypto.onramp.model.OnrampCallbacks
import com.stripe.android.crypto.onramp.model.OnrampStartVerificationResult
import com.stripe.android.crypto.onramp.model.StartIdentityVerificationResponse
import com.stripe.android.identity.IdentityVerificationSheet
import com.stripe.android.link.LinkController
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

    private val sheet = IdentityVerificationSheet.create(
        activity,
        configuration = IdentityVerificationSheet.Configuration(
            brandLogo = Uri.Builder()
                .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                .authority("test")
                .appendPath("1")
                .build()),
        identityVerificationCallback = { result -> },
    )

    init {
        // Observe Link controller state
        lifecycleOwner.lifecycleScope.launch {
            linkController.state(activity).collect { state ->
                interactor.onLinkControllerState(state)
            }
        }
    }

    fun authenticateExistingLinkUser(email: String) {
        linkPresenter.authenticateExistingConsumer(email)
    }

    fun promptForIdentityVerification() {
        coroutineScope.launch {
            val verification = interactor.startIdentityVerification()

            when(verification) {
                is OnrampStartVerificationResult.Completed -> {
                    verification.response.ephemeralKey?.let {
                        sheet.present(
                            verificationSessionId = verification.response.id,
                            ephemeralKeySecret = verification.response.ephemeralKey
                        )
                    } ?: run {

                    }
                }
                is OnrampStartVerificationResult.Failed -> {

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
}
