package com.stripe.android.crypto.onramp.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.crypto.onramp.OnrampCoordinator
import com.stripe.android.crypto.onramp.example.ui.OnrampApp
import kotlinx.coroutines.launch

internal class OnrampActivity : ComponentActivity() {

    private lateinit var onrampPresenter: OnrampCoordinator.Presenter

    private val viewModel: OnrampViewModel by viewModels {
        OnrampViewModel.Factory()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        FeatureFlags.nativeLinkEnabled.setEnabled(true)
        onrampPresenter = viewModel.onrampCoordinator.createPresenter(this)

        observeViewModelEvents()

        setContent {
            OnrampApp(
                viewModel = viewModel,
                onAuthenticateUser = ::authenticateUser,
                onCollectPayment = onrampPresenter::collectPaymentMethod,
                onStartVerification = onrampPresenter::verifyIdentity,
                onShowCrsCarfDeclaration = onrampPresenter::presentCrsCarfDeclaration,
                onSubmitAddress = { address ->
                    onrampPresenter.verifyKycInfo(address)
                },
                onVerifyKyc = { onrampPresenter.verifyKycInfo() }
            )
        }
    }

    private fun observeViewModelEvents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.checkoutEvent.collect { event ->
                        if (event == null) return@collect

                        onrampPresenter.performCheckout(onrampSessionId = event.sessionId)
                        viewModel.clearCheckoutEvent()
                    }
                }

                launch {
                    viewModel.authorizeEvent.collect { event ->
                        if (event == null) return@collect

                        onrampPresenter.authorize(event.linkAuthIntentId)
                        viewModel.clearAuthorizeEvent()
                    }
                }
            }
        }
    }

    private fun authenticateUser(oauthScopes: String) {
        lifecycleScope.launch {
            val linkAuthIntentId = viewModel.createLinkAuthIntent(oauthScopes) ?: return@launch
            viewModel.onAuthorize(linkAuthIntentId)
            onrampPresenter.authorize(linkAuthIntentId)
        }
    }
}
