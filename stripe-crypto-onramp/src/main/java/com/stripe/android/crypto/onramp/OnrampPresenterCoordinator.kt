package com.stripe.android.crypto.onramp

import androidx.activity.ComponentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.stripe.android.crypto.onramp.di.OnrampPresenterScope
import com.stripe.android.crypto.onramp.model.OnrampCallbacks
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

    private fun handleAuthenticationResult(result: LinkController.AuthenticationResult) {
        coroutineScope.launch {
            onrampCallbacks.authenticationCallback.onResult(
                interactor.handleAuthenticationResult(result)
            )
        }
    }
}
