package com.stripe.android.connect.example.ui.features.accountonboarding

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.FragmentActivity
import androidx.fragment.compose.AndroidFragment
import com.stripe.android.connect.AccountOnboardingFragment
import com.stripe.android.connect.PrivateBetaConnectSDK
import com.stripe.android.connect.example.ConnectSdkExampleTheme
import com.stripe.android.connect.example.MainContent
import com.stripe.android.connect.example.R
import com.stripe.android.connect.example.ui.common.BackIconButton

class AccountOnboardingExampleActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ConnectSdkExampleTheme {
                MainContent(
                    title = stringResource(R.string.account_onboarding),
                    navigationIcon = {
                        BackIconButton(onClick = this@AccountOnboardingExampleActivity::finish)
                    }
                ) {
                    AccountOnboardingComponentWrapper(onDismiss = this@AccountOnboardingExampleActivity::finish)
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
