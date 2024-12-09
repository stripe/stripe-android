package com.stripe.android.connect.example.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stripe.android.connect.BuildConfig
import com.stripe.android.connect.example.data.EmbeddedComponentService
import com.stripe.android.connect.example.data.FieldOption
import com.stripe.android.connect.example.data.FutureRequirement
import com.stripe.android.connect.example.data.OnboardingSettings
import com.stripe.android.connect.example.data.PresentationSettings
import com.stripe.android.connect.example.data.SettingsService
import com.stripe.android.connect.example.data.SkipTermsOfService
import com.stripe.android.connect.example.ui.settings.SettingsViewModel.SettingsState.DemoMerchant
import com.stripe.android.core.Logger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val embeddedComponentService: EmbeddedComponentService,
    private val settingsService: SettingsService
) : ViewModel() {

    private val logger: Logger = Logger.getInstance(enableLogging = BuildConfig.DEBUG)
    private val loggingTag = this::class.java.simpleName

    private val _state = MutableStateFlow(SettingsState(serverUrl = embeddedComponentService.serverBaseUrl))
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        loadSettings()

        viewModelScope.launch {
            _state.map {
                // when any of the below values change,
                // enable the save button
                listOf(
                    it.presentationSettings,
                    it.onboardingSettings,
                    it.accounts,
                    it.selectedAccount,
                    it.serverUrl,
                )
            }.distinctUntilChanged()
                .collect {
                    _state.update { it.copy(saveEnabled = true) }
                }
        }
    }

    fun onAccountSelected(account: DemoMerchant) {
        _state.update { it.copy(selectedAccount = account) }
    }

    fun onOtherAccountInputChanged(otherAccountIdInput: String) {
        _state.update { state ->
            val updatedAccounts = state.accounts.map { acc ->
                when (acc) {
                    is DemoMerchant.Merchant -> acc
                    is DemoMerchant.Other -> acc.copy(merchantId = otherAccountIdInput)
                }
            }
            val otherAccount = updatedAccounts.filterIsInstance<DemoMerchant.Other>().firstOrNull()
            state.copy(
                selectedAccount = otherAccount,
                accounts = updatedAccounts,
                saveEnabled = true,
            )
        }
    }

    fun onOnboardingSettingsConfirmed(onboardingSettings: OnboardingSettings) {
        viewModelScope.launch {
            settingsService.setOnboardingSettings(onboardingSettings)
            logger.info("($loggingTag) Onboarding settings saved")

            _state.update { it.copy(onboardingSettings = onboardingSettings) }
        }
    }

    fun onServerUrlChanged(url: String) {
        _state.update { it.copy(serverUrl = url) }
    }

    fun onResetServerUrlClicked() {
        onServerUrlChanged(EmbeddedComponentService.DEFAULT_SERVER_BASE_URL)
    }

    fun saveSettings() {
        viewModelScope.launch {
            with(state.value) {
                settingsService.setSelectedServerBaseURL(serverUrl)
                embeddedComponentService.setBackendBaseUrl(serverUrl)

                val selectedAccountId = selectedAccount?.merchantId
                if (selectedAccountId != null) settingsService.setSelectedMerchant(selectedAccountId)
                settingsService.setPresentationSettings(presentationSettings)
            }
            logger.info("($loggingTag) Settings saved")

            _state.update { it.copy(saveEnabled = false) }
        }
    }

    // Private functions

    private fun loadSettings() {
        _state.update { state ->
            val accountsFromService = embeddedComponentService.accounts.value ?: emptyList()
            val selectedAccountId = settingsService.getSelectedMerchant() ?: ""
            val selectedAccountIsOther = accountsFromService.none { it.merchantId == selectedAccountId }

            val otherAccount = DemoMerchant.Other(if (selectedAccountIsOther) selectedAccountId else "")
            val mergedAccounts = accountsFromService.map {
                DemoMerchant.Merchant(
                    displayName = it.displayName,
                    merchantId = it.merchantId
                )
            } + otherAccount

            val selectedAccount = mergedAccounts.firstOrNull { it.merchantId == selectedAccountId } ?: otherAccount
            state.copy(
                serverUrl = embeddedComponentService.serverBaseUrl,
                selectedAccount = selectedAccount,
                accounts = mergedAccounts,
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
        val selectedAccount: DemoMerchant? = accounts.firstOrNull(),
        val onboardingSettings: OnboardingSettings = OnboardingSettings(),
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
                override val merchantId: String = "", // other merchant id is based on user input
            ) : DemoMerchant
        }
    }
}
