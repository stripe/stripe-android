package com.stripe.android.connectsdk.example.ui.features.payouts

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

@OptIn(PrivateBetaConnectSDK::class)
class PayoutsExampleActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ConnectSdkExampleTheme {
                val viewModel: PayoutsExampleViewModel = viewModel()
                val payoutsExampleState by viewModel.state.collectAsState()
                val sdkPublishableKey = payoutsExampleState.publishableKey
                val accounts = payoutsExampleState.accounts

                MainContent(title = "Payouts Example") {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (sdkPublishableKey != null && accounts != null) {
                            val embeddedComponentManager = remember(sdkPublishableKey) {
                                EmbeddedComponentManager(
                                    activity = this@PayoutsExampleActivity,
                                    configuration = Configuration(sdkPublishableKey),
                                    fetchClientSecret = viewModel::fetchClientSecret,
                                )
                            }

                            LaunchEmbeddedComponentsScreen(
                                embeddedComponentName = "Payouts",
                                selectedAccount = payoutsExampleState.selectedAccount,
                                connectSDKAccounts = accounts,
                                onConnectSDKAccountSelected = viewModel::onAccountSelected,
                                onEmbeddedComponentLaunched = embeddedComponentManager::presentPayouts,
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
