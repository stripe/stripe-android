package com.stripe.android.connect.example.ui.features.accountonboarding

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
import com.stripe.android.connect.AccountOnboardingFragment
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
                AccountOnboardingComponentWrapper(onDismiss = this@AccountOnboardingExampleActivity::finish)
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
