package com.stripe.android.connect.example.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stripe.android.connect.example.R
import com.stripe.android.connect.example.data.FieldOption
import com.stripe.android.connect.example.data.FutureRequirement
import com.stripe.android.connect.example.data.OnboardingSettings
import com.stripe.android.connect.example.data.RequirementsMode
import com.stripe.android.connect.example.data.SkipTermsOfService
import com.stripe.android.connect.example.ui.common.BackIconButton
import com.stripe.android.connect.example.ui.common.ConnectExampleScaffold
import com.stripe.android.connect.example.ui.common.ConnectSdkExampleTheme

@Composable
fun AccountOnboardingSettingsView(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val onboardingSettings = state.onboardingSettings
    AccountOnboardingSettingsView(
        onboardingSettings = onboardingSettings,
        onBack = onBack,
        onSave = { viewModel.onOnboardingSettingsConfirmed(it) },
    )
}

@Suppress("LongMethod")
@Composable
private fun AccountOnboardingSettingsView(
    onboardingSettings: OnboardingSettings,
    onBack: () -> Unit,
    onSave: (OnboardingSettings) -> Unit,
) {
    var fullTermsOfService by rememberSaveable {
        mutableStateOf(onboardingSettings.fullTermsOfServiceString ?: "")
    }
    var recipientTermsOfService by rememberSaveable {
        mutableStateOf(onboardingSettings.recipientTermsOfServiceString ?: "")
    }
    var privacyPolicy by rememberSaveable {
        mutableStateOf(onboardingSettings.privacyPolicyString ?: "")
    }
    var skipTermsOfService by rememberSaveable {
        mutableStateOf(onboardingSettings.skipTermsOfService)
    }
    var fieldOption by rememberSaveable {
        mutableStateOf(onboardingSettings.fieldOption)
    }
    var futureRequirement by rememberSaveable {
        mutableStateOf(onboardingSettings.futureRequirement)
    }
    var requirementsMode by rememberSaveable {
        mutableStateOf(onboardingSettings.requirementsMode)
    }
    var requirementsText by rememberSaveable {
        mutableStateOf(onboardingSettings.requirementsText ?: "")
    }
    ConnectExampleScaffold(
        title = stringResource(R.string.onboarding_settings),
        navigationIcon = { BackIconButton(onBack) },
        actions = {
            IconButton(
                onClick = {
                    onSave(
                        OnboardingSettings(
                            fullTermsOfServiceString = fullTermsOfService.trim().takeIf { it.isNotEmpty() },
                            recipientTermsOfServiceString = recipientTermsOfService.trim().takeIf { it.isNotEmpty() },
                            privacyPolicyString = privacyPolicy.trim().takeIf { it.isNotEmpty() },
                            skipTermsOfService = skipTermsOfService,
                            fieldOption = fieldOption,
                            futureRequirement = futureRequirement,
                            requirementsMode = requirementsMode,
                            requirementsText = requirementsText.trim().takeIf { it.isNotEmpty() },
                        )
                    )
                    onBack()
                },
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_material_check),
                    contentDescription = stringResource(R.string.save)
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.requiredHeight(16.dp))
            SettingsTextField(
                label = stringResource(R.string.full_terms_of_service),
                placeholder = stringResource(R.string.server_url_placeholder),
                value = fullTermsOfService,
                onValueChange = { fullTermsOfService = it },
            )
            Spacer(Modifier.requiredHeight(8.dp))
            SettingsTextField(
                label = stringResource(R.string.recipient_terms_of_service),
                placeholder = stringResource(R.string.server_url_placeholder),
                value = recipientTermsOfService,
                onValueChange = { recipientTermsOfService = it },
            )
            Spacer(Modifier.requiredHeight(8.dp))
            SettingsTextField(
                label = stringResource(R.string.privacy_policy),
                placeholder = stringResource(R.string.server_url_placeholder),
                value = privacyPolicy,
                onValueChange = { privacyPolicy = it },
            )
            Spacer(Modifier.requiredHeight(8.dp))
            SettingsDropdownField(
                label = "Skip terms of service",
                options = SkipTermsOfService.entries.toList(),
                selectedOption = skipTermsOfService,
                onSelectOption = { skipTermsOfService = it }
            )
            Spacer(Modifier.requiredHeight(8.dp))
            SettingsDropdownField(
                label = "Field option",
                options = FieldOption.entries.toList(),
                selectedOption = fieldOption,
                onSelectOption = { fieldOption = it }
            )
            Spacer(Modifier.requiredHeight(8.dp))
            SettingsDropdownField(
                label = "Future requirement",
                options = FutureRequirement.entries.toList(),
                selectedOption = futureRequirement,
                onSelectOption = { futureRequirement = it }
            )
            Spacer(Modifier.requiredHeight(8.dp))
            SettingsDropdownField(
                label = "Requirements mode",
                options = RequirementsMode.entries.toList(),
                selectedOption = requirementsMode,
                onSelectOption = { requirementsMode = it }
            )
            if (requirementsMode != RequirementsMode.DEFAULT) {
                Spacer(Modifier.requiredHeight(8.dp))
                SettingsTextField(
                    label = when (requirementsMode) {
                        RequirementsMode.ONLY -> "Requirements to include (comma-separated)"
                        RequirementsMode.EXCLUDE -> "Requirements to exclude (comma-separated)"
                        RequirementsMode.DEFAULT -> ""
                    },
                    placeholder = "e.g., external_account, business_profile.url",
                    value = requirementsText,
                    onValueChange = { requirementsText = it },
                )
            }
        }
    }
}

@Preview
@Composable
private fun AccountOnboardingSettingsViewPreview() {
    ConnectSdkExampleTheme {
        AccountOnboardingSettingsView(
            onboardingSettings = OnboardingSettings(),
            onBack = {},
            onSave = {}
        )
    }
}
