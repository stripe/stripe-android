package com.stripe.android.crypto.onramp

import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.stripe.android.crypto.onramp.model.LinkUserInfo
import com.stripe.android.crypto.onramp.viewmodels.OnrampCoordinatorViewModel
import com.stripe.android.link.LinkController
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
    private val linkControllerPresenter: LinkController.Presenter by lazy {
        viewModel.linkController.createPresenter(
            activity = activity,
            presentPaymentMethodsCallback = { /* No-op for now */ },
            authenticationCallback = viewModel::handleAuthenticationResult,
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
                    viewModel.linkController.state(activity)
                        .collect(viewModel::onLinkControllerState)
                }
            }
        }
    }

    private suspend fun configureLinkController(configuration: LinkController.Configuration) {
        val result = viewModel.linkController.configure(configuration)

        viewModel.onLinkControllerConfigureResult(result)
    }

    fun isLinkUser(email: String) {
        viewModel.onIsLinkUser(email)
    }

    fun authenticateExistingUser(email: String) {
        linkControllerPresenter.authenticateExistingConsumer(email)
    }

    fun registerNewUser(info: LinkUserInfo) {
        viewModel.onRegisterNewUser(info)
    }
}
