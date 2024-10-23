package com.stripe.android.connect.example.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.kittinunf.fuel.core.FuelError
import com.stripe.android.connect.BuildConfig
import com.stripe.android.connect.EmbeddedComponentManager
import com.stripe.android.connect.FetchClientSecretCallback.ClientSecretResultCallback
import com.stripe.android.connect.PrivateBetaConnectSDK
import com.stripe.android.connect.example.data.EmbeddedComponentService
import com.stripe.android.connect.example.data.FieldOption
import com.stripe.android.connect.example.data.FutureRequirement
import com.stripe.android.connect.example.data.Merchant
import com.stripe.android.connect.example.data.SettingsService
import com.stripe.android.connect.example.data.OnboardingSettings
import com.stripe.android.connect.example.data.PresentationSettings
import com.stripe.android.connect.example.data.SkipTermsOfService
import com.stripe.android.core.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val logger: Logger = Logger.getInstance(enableLogging = BuildConfig.DEBUG),
    private val settingsService: SettingsService = SettingsService.getInstance(),
) : ViewModel() {

    private val _state = MutableStateFlow(
        SettingsState(serverUrl = settingsService.getSelectedServerBaseURL())
    )
    val state = _state.asStateFlow()

    init {
        loadAppSettings()
    }

    fun onAccountSelected(accountId: String) {
        _state.update { it.copy(selectedAccountId = accountId) }
    }

    fun onServerUrlChanged(url: String) {
        _state.update { it.copy(serverUrl = url) }
    }

    fun onOnboardingSettingsChanged(settings: OnboardingSettings) {
        _state.update { it.copy(onboardingSettings = settings) }
    }

    fun onPresentationSettingsChanged(settings: PresentationSettings) {
        _state.update { it.copy(presentationSettings = settings) }
    }

    fun saveSettings() {
        viewModelScope.launch {
            with(state.value) {
                settingsService.setSelectedServerBaseURL(serverUrl)
                if (selectedAccountId != null) settingsService.setSelectedMerchant(selectedAccountId)
                settingsService.setOnboardingSettings(onboardingSettings)
                settingsService.setPresentationSettings(presentationSettings)
            }
            logger.info("Settings saved")
        }
    }

    private fun loadAppSettings() {
        _state.update {
            it.copy(
                serverUrl = settingsService.getSelectedServerBaseURL(),
                selectedAccountId = settingsService.getSelectedMerchant(),
                onboardingSettings = settingsService.getOnboardingSettings(),
                presentationSettings = settingsService.getPresentationSettings()
            )
        }
    }

    data class SettingsState(
        val serverUrl: String,
        val selectedAccountId: String? = null,
        val accounts: List<Merchant>? = null,
        val onboardingSettings: OnboardingSettings = OnboardingSettings(
            fullTermsOfServiceString = null,
            recipientTermsOfServiceString = null,
            privacyPolicyString = null,
            skipTermsOfService = SkipTermsOfService.DEFAULT,
            fieldOption = FieldOption.DEFAULT,
            futureRequirement = FutureRequirement.DEFAULT
        ),
        val presentationSettings: PresentationSettings = PresentationSettings(
            presentationStyleIsPush = true,
            embedInTabBar = false,
            embedInNavBar = true
        )
    )
}
