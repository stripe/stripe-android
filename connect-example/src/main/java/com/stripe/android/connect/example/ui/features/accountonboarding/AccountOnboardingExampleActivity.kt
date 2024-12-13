package com.stripe.android.connect.example.ui.features.accountonboarding

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.activity.viewModels
import com.stripe.android.connect.EmbeddedComponentManager
import com.stripe.android.connect.PrivateBetaConnectSDK
import com.stripe.android.connect.example.R
import com.stripe.android.connect.example.databinding.ViewAccountOnboardingExampleBinding
import com.stripe.android.connect.example.ui.common.BasicExampleComponentActivity
import com.stripe.android.connect.example.ui.settings.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint

@OptIn(PrivateBetaConnectSDK::class)
@AndroidEntryPoint
class AccountOnboardingExampleActivity : BasicExampleComponentActivity() {
    override val titleRes: Int = R.string.account_onboarding

    private val settingsViewModel by viewModels<SettingsViewModel>()

    override fun createComponentView(context: Context, embeddedComponentManager: EmbeddedComponentManager): View {
        val settings = settingsViewModel.state.value
        val onboardingSettings = settings.onboardingSettings
        val props = onboardingSettings.toProps()
        return if (settings.presentationSettings.useXmlViews) {
            ViewAccountOnboardingExampleBinding.inflate(LayoutInflater.from(context)).root
                .apply {
                    initialize(
                        embeddedComponentManager = embeddedComponentManager,
                        listener = null,
                        props = props,
                    )
                }
        } else {
            embeddedComponentManager.createAccountOnboardingView(
                context = context,
                props = props
            )
        }
    }
}
