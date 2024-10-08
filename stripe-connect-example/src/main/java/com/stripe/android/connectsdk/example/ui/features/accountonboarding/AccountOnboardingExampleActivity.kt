package com.stripe.android.connectsdk.example.ui.features.accountonboarding

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stripe.android.connectsdk.EmbeddedComponentManager
import com.stripe.android.connectsdk.EmbeddedComponentManager.Configuration
import com.stripe.android.connectsdk.PrivateBetaConnectSDK
import com.stripe.android.connectsdk.example.ConnectSdkExampleTheme
import com.stripe.android.connectsdk.example.MainContent
import com.stripe.android.connectsdk.example.ui.common.LaunchEmbeddedComponentsScreen

class AccountOnboardingExampleActivity : ComponentActivity() {

    @OptIn(PrivateBetaConnectSDK::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ConnectSdkExampleTheme {
                val viewModel: AccountOnboardingExampleViewModel = viewModel()
                val accountOnboardingExampleState by viewModel.state.collectAsState()
                val sdkPublishableKey = accountOnboardingExampleState.publishableKey
                val accounts = accountOnboardingExampleState.accounts

                MainContent(title = "Account Onboarding Example") {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (sdkPublishableKey != null && accounts != null) {
                            val embeddedComponentManager = remember(sdkPublishableKey) {
                                EmbeddedComponentManager(
                                    activity = this@AccountOnboardingExampleActivity,
                                    configuration = Configuration(sdkPublishableKey),
                                    fetchClientSecret = viewModel::fetchClientSecret,
                                )
                            }

                            LaunchEmbeddedComponentsScreen(
                                embeddedComponentName = "Account Onboarding",
                                selectedAccount = accountOnboardingExampleState.selectedAccount,
                                connectSDKAccounts = accounts,
                                onConnectSDKAccountSelected = viewModel::onAccountSelected,
                                onEmbeddedComponentLaunched = embeddedComponentManager::presentAccountOnboarding,
                            )
                        } else {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }
}
