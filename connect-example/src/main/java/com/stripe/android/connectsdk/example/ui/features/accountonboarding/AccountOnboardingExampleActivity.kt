package com.stripe.android.connectsdk.example.ui.features.accountonboarding

import android.os.Bundle
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
import androidx.fragment.app.FragmentActivity
import androidx.fragment.compose.AndroidFragment
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stripe.android.connectsdk.AccountOnboardingFragment
import com.stripe.android.connectsdk.PrivateBetaConnectSDK
import com.stripe.android.connectsdk.example.ConnectSdkExampleTheme
import com.stripe.android.connectsdk.example.MainContent
import com.stripe.android.connectsdk.example.R
import com.stripe.android.connectsdk.example.ui.common.LaunchEmbeddedComponentsScreen

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
                                LaunchEmbeddedComponentsScreen(
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
        AndroidFragment<AccountOnboardingFragment>()
    }
}
