package com.stripe.android.connect.example.ui.settings

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.stripe.android.connect.example.R
import com.stripe.android.connect.example.ui.common.BackIconButton
import com.stripe.android.connect.example.ui.common.ConnectExampleScaffold

@Composable
fun AccountOnboardingSettingsView(
    onBack: () -> Unit,
) {
    ConnectExampleScaffold(
        title = stringResource(R.string.onboarding_settings),
        navigationIcon = { BackIconButton(onBack) }
    ) {
        Text("TODO")
    }
}
