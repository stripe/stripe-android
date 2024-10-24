package com.stripe.android.connect.example.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stripe.android.connect.BuildConfig
import com.stripe.android.connect.example.data.EmbeddedComponentService
import com.stripe.android.connect.example.data.FieldOption
import com.stripe.android.connect.example.data.FutureRequirement
import com.stripe.android.connect.example.data.SettingsService
import com.stripe.android.connect.example.data.OnboardingSettings
import com.stripe.android.connect.example.data.PresentationSettings
import com.stripe.android.connect.example.data.SkipTermsOfService
import com.stripe.android.connect.example.ui.settings.SettingsViewModel.SettingsState.DemoMerchant
import com.stripe.android.core.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val logger: Logger = Logger.getInstance(enableLogging = BuildConfig.DEBUG),
    private val embeddedComponentService: EmbeddedComponentService = EmbeddedComponentService.getInstance(),
    private val settingsService: SettingsService = SettingsService.getInstance(),
) : ViewModel() {

    private val _state = MutableStateFlow(
        SettingsState(serverUrl = settingsService.getSelectedServerBaseURL())
    )
    val state = _state.asStateFlow()

    init {
        loadAppSettings()

        viewModelScope.launch {
            _state.map {
                // when any of the below values change,
                // enable the save button
                listOf(
                    it.presentationSettings,
                    it.onboardingSettings,
                    it.accounts,
                    it.selectedAccountId,
                    it.serverUrl,
                )
            }.distinctUntilChanged()
            .collect {
                _state.update { it.copy(saveEnabled = true) }
            }
        }
    }

    fun onAccountSelected(accountId: String) {
        _state.update { it.copy(selectedAccountId = accountId) }
    }

    fun onOtherAccountInputChanged(otherAccountIdInput: String) {
        _state.update { state ->
            state.copy(
                selectedAccountId = otherAccountIdInput,
                accounts = state.accounts.map { acc ->
                    when (acc) {
                        is DemoMerchant.Merchant -> acc
                        is DemoMerchant.Other -> acc.copy(merchantId = otherAccountIdInput)
                    }
                },
                saveEnabled = true,
            )
        }
    }

    fun onServerUrlChanged(url: String) {
        _state.update { it.copy(serverUrl = url) }
    }

    fun onResetServerUrlClicked() {
        onServerUrlChanged(EmbeddedComponentService.DEFAULT_SERVER_BASE_URL)
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
                embeddedComponentService.setBackendBaseUrl(serverUrl)
                if (selectedAccountId != null) settingsService.setSelectedMerchant(selectedAccountId)
                settingsService.setOnboardingSettings(onboardingSettings)
                settingsService.setPresentationSettings(presentationSettings)
            }
            logger.info("Settings saved")

            _state.update { it.copy(saveEnabled = false) }
        }
    }

    // Private functions

    private fun loadAppSettings() {
        _state.update {
            it.copy(
                serverUrl = settingsService.getSelectedServerBaseURL(),
                selectedAccountId = settingsService.getSelectedMerchant(),
                onboardingSettings = settingsService.getOnboardingSettings(),
                presentationSettings = settingsService.getPresentationSettings(),
                saveEnabled = false,
            )
        }
    }

    // State

    data class SettingsState(
        val serverUrl: String,
        val saveEnabled: Boolean = false,
        val accounts: List<DemoMerchant> = listOf(DemoMerchant.Other()),
        val selectedAccountId: String? = accounts.firstOrNull()?.merchantId,
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
    ) {
        val serverUrlResetEnabled: Boolean
            get() = serverUrl != EmbeddedComponentService.DEFAULT_SERVER_BASE_URL

        sealed interface DemoMerchant {
            val merchantId: String?

            data class Merchant(
                val displayName: String,
                override val merchantId: String,
            ) : DemoMerchant

            data class Other(
                override val merchantId: String? = null,
            ) : DemoMerchant
        }
    }
}
