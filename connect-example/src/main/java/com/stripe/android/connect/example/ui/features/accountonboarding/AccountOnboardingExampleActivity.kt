package com.stripe.android.connect.example.ui.features.accountonboarding

import android.os.Bundle
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stripe.android.connect.AccountOnboardingFragmentListener
import com.stripe.android.connect.AccountOnboardingView
import com.stripe.android.connect.PrivateBetaConnectSDK
import com.stripe.android.connect.example.ConnectSdkExampleTheme
import com.stripe.android.connect.example.MainContent
import com.stripe.android.connect.example.R
import com.stripe.android.connect.example.ui.common.EmbeddedComponentsLauncherScreen

class AccountOnboardingExampleActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ConnectSdkExampleTheme {
                val viewModel: AccountOnboardingExampleViewModel = viewModel()
                val accountOnboardingExampleState by viewModel.state.collectAsState()
                val accounts = accountOnboardingExampleState.accounts

                MainContent(title = stringResource(R.string.account_onboarding)) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (accounts != null) {
                            var isAccountOnboardingVisible by remember { mutableStateOf(false) }

                            if (isAccountOnboardingVisible) {
                                AccountOnboardingComponentWrapper(onDismiss = { isAccountOnboardingVisible = false })
                            } else {
                                EmbeddedComponentsLauncherScreen(
                                    embeddedComponentName = stringResource(R.string.account_onboarding),
                                    selectedAccount = accountOnboardingExampleState.selectedAccount,
                                    connectSDKAccounts = accounts,
                                    onConnectSDKAccountSelected = viewModel::onAccountSelected,
                                    onEmbeddedComponentLaunched = { isAccountOnboardingVisible = true },
                                )
                            }
                        } else {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }

    @OptIn(PrivateBetaConnectSDK::class)
    @Composable
    private fun AccountOnboardingComponentWrapper(onDismiss: () -> Unit) {
        BackHandler(onBack = onDismiss)
        AndroidView(factory = { context ->
            AccountOnboardingView(context).apply {
                registerListener(object : AccountOnboardingFragmentListener {
                    override fun onLoadFailure(error: Throwable) {
                        // Handle error
                    }

                    override fun onExit() {
                        onDismiss()
                    }
                })
            }
        })
    }
}
