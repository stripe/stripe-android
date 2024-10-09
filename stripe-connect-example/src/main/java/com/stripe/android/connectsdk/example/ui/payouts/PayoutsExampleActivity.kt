package com.stripe.android.connectsdk.example.ui.payouts

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stripe.android.connectsdk.EmbeddedComponentManager
import com.stripe.android.connectsdk.PrivateBetaConnectSDK
import com.stripe.android.connectsdk.example.ConnectSdkExampleTheme
import com.stripe.android.connectsdk.example.MainContent

@OptIn(PrivateBetaConnectSDK::class)
class PayoutsExampleActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ConnectSdkExampleTheme {
                val viewModel: PayoutsExampleViewModel = viewModel()
                val embeddedComponentManager = remember {
                    EmbeddedComponentManager(
                        activity = this@PayoutsExampleActivity,
                        // TODO MXMOBILE-2511 - pass publishable key from backend to SDK
                        configuration = EmbeddedComponentManager.Configuration(""),
                        fetchClientSecret = viewModel::fetchClientSecret,
                    )
                }

                MainContent(title = "Payouts Example") {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Button(
                            onClick = embeddedComponentManager::presentPayouts,
                        ) {
                            Text("Launch payouts")
                        }
                    }
                }
            }
        }
    }
}
