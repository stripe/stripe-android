package com.stripe.android.crypto.onramp

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultRegistryOwner
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

internal class OnrampController @Inject constructor(
    private val viewModel: OnrampCoordinatorViewModel,
    private val lifecycleOwner: LifecycleOwner,
    private val activityResultRegistryOwner: ActivityResultRegistryOwner
) {
    private val linkController: LinkController by lazy {
        // Resolve the hosting activity, fail fast if incorrect type
        val activity: ComponentActivity =
            (activityResultRegistryOwner as? ComponentActivity)
                ?: throw IllegalStateException(
                    "Expected a ComponentActivity, got ${activityResultRegistryOwner::class}"
                )

        LinkController.create(
            activity = activity,
            presentPaymentMethodsCallback = { /* No-op for now */ },
            lookupConsumerCallback = viewModel::handleConsumerLookupResult,
            createPaymentMethodCallback = { /* No-op for now */ },
            authenticationCallback = ::handleAuthenticationResult,
            registerConsumerCallback = ::handleRegisterNewUserResult,
        )
    }

    private val sessionClientSecret: String?
        get() = linkController.state.value.internalLinkAccount?.consumerSessionClientSecret

    init {
        check(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.INITIALIZED))

        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.configurationFlow
                        .collect(::configureLinkController)
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

    private fun handleAuthenticationResult(result: LinkController.AuthenticationResult) {
        viewModel.handleAuthenticationResult(result, sessionClientSecret)
    }

    private fun handleRegisterNewUserResult(result: LinkController.RegisterConsumerResult) {
        viewModel.handleRegisterNewUserResult(result, sessionClientSecret)
    }
}
