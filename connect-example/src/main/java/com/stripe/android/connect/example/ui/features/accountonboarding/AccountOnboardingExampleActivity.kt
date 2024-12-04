package com.stripe.android.connect.example.ui.features.accountonboarding

import android.content.Context
import android.view.View
import com.stripe.android.connect.PrivateBetaConnectSDK
import com.stripe.android.connect.example.R
import com.stripe.android.connect.example.ui.common.BasicComponentExampleActivity
import dagger.hilt.android.AndroidEntryPoint

@OptIn(PrivateBetaConnectSDK::class)
@AndroidEntryPoint
class AccountOnboardingExampleActivity : BasicComponentExampleActivity() {
    override val titleRes: Int = R.string.account_onboarding

    override fun createComponentView(context: Context): View {
        return embeddedComponentManager.createAccountOnboardingView(context)
    }
}
