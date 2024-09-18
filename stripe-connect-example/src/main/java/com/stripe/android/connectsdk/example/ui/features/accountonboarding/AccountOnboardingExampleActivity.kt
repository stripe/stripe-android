package com.stripe.android.connectsdk.example.ui.features.accountonboarding

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import com.stripe.android.connectsdk.AccountOnboardingComponentFragment
import com.stripe.android.connectsdk.EmbeddedComponentManager
import com.stripe.android.connectsdk.EmbeddedComponentManager.Configuration
import com.stripe.android.connectsdk.PayoutComponentFragment
import com.stripe.android.connectsdk.PrivateBetaConnectSDK
import com.stripe.android.connectsdk.example.ConnectSdkExampleTheme
import com.stripe.android.connectsdk.example.MainContent
import com.stripe.android.connectsdk.example.R
import com.stripe.android.connectsdk.example.ui.common.LaunchEmbeddedComponentsScreen
import com.stripe.android.connectsdk.example.ui.features.accountonboarding.AccountOnboardingExampleViewModel.AccountOnboardingExampleState
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(PrivateBetaConnectSDK::class)
class AccountOnboardingExampleActivity : AppCompatActivity() {

    private val viewModel: AccountOnboardingExampleViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.stripe_account_onboarding_example_activity)

        findViewById<ComposeView>(R.id.composableContainer).setContent {
            CircularProgressIndicator()
        }

        lifecycleScope.launch {
            viewModel.state.collect { state ->
                val publishableKey = state.publishableKey
                if (publishableKey != null && state.accounts != null) {
                    val embeddedComponentManager = EmbeddedComponentManager.init(
                        activity = this@AccountOnboardingExampleActivity,
                        configuration = Configuration(publishableKey),
                        fetchClientSecret = viewModel::fetchClientSecret,
                    )
                    setupViews(embeddedComponentManager, state)
                }
            }
        }
    }

    private fun setupViews(embeddedComponentManager: EmbeddedComponentManager, state: AccountOnboardingExampleState) {
        // Implement Compose in the top half
        findViewById<ComposeView>(R.id.composableContainer).setContent {
            ConnectSdkExampleTheme {
                MainContent(title = "Account Onboarding Example") {
                    state.accounts ?: return@MainContent
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        LaunchEmbeddedComponentsScreen(
                            embeddedComponentName = "Account Onboarding",
                            selectedAccount = state.selectedAccount,
                            connectSDKAccounts = state.accounts,
                            onConnectSDKAccountSelected = viewModel::onAccountSelected,
                            onEmbeddedComponentLaunched = embeddedComponentManager::presentAccountOnboarding,
                        )
                    }
                }
            }
        }

        // Attach Fragment to the bottom half
        val fragment = AccountOnboardingComponentFragment()
        supportFragmentManager.commit {
            replace(R.id.fragmentContainer, fragment)
        }
        GlobalScope.launch {
            delay(2000)
            MainScope().launch {
                fragment.load(Configuration(state.publishableKey!!))
            }
        }
    }
}
