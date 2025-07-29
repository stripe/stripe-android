package com.stripe.android.crypto.onramp

import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.stripe.android.crypto.onramp.model.LinkUserInfo
import com.stripe.android.crypto.onramp.viewmodels.OnrampCoordinatorViewModel
import com.stripe.android.link.LinkController
import com.stripe.android.model.ConsumerSignUpConsentAction
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * A controller responsible for interacting with, and handling responses from, a [LinkController].
 */
internal class OnrampLinkController @Inject constructor(
    private val activity: ComponentActivity,
    private val viewModel: OnrampCoordinatorViewModel,
    private val lifecycleOwner: LifecycleOwner
) {
    private val linkController: LinkController by lazy {
        LinkController.create(
            activity = activity,
            presentPaymentMethodsCallback = { /* No-op for now */ },
            lookupConsumerCallback = viewModel::handleConsumerLookupResult,
            createPaymentMethodCallback = { /* No-op for now */ },
            authenticationCallback = viewModel::handleAuthenticationResult,
            registerConsumerCallback = viewModel::handleRegisterNewUserResult,
        )
    }

    init {
        check(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.INITIALIZED))

        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.configurationFlow
                        .collect(::configureLinkController)
                }

                launch {
                    linkController.state
                        .collect(viewModel::onLinkControllerState)
                }
            }
        }
    }

    private suspend fun configureLinkController(configuration: LinkController.Configuration) {
        val result = linkController.configure(configuration)

        viewModel.onLinkControllerConfigureResult(result)
    }

    fun isLinkUser(email: String) {
        linkController.lookupConsumer(email)
    }

    fun authenticateExistingUser(email: String) {
        linkController.authenticateExistingConsumer(email)
    }

    fun registerNewUser(info: LinkUserInfo) {
        linkController.registerConsumer(
            email = info.email,
            phone = info.phone,
            country = info.country,
            name = info.fullName,
            consentAction = ConsumerSignUpConsentAction.Implied
        )
    }
}
