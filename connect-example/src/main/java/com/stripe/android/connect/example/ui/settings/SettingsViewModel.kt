package com.stripe.android.connect.example.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
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
    application: Application,
    private val logger: Logger = Logger.getInstance(enableLogging = BuildConfig.DEBUG),
    private val settingsService: SettingsService = SettingsService(application.baseContext),
    private val embeddedComponentService: EmbeddedComponentService = EmbeddedComponentService(
        exampleBackendBaseUrl = settingsService.getSelectedServerBaseURL()
    ),
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(SettingsState(settingsService.getSelectedServerBaseURL()))
    val state = _state.asStateFlow()

    init {
        getAccounts()
        loadAppSettings()
    }

    fun onAccountSelected(account: Merchant) {
        _state.update { it.copy(selectedAccount = account) }
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
                selectedAccount?.let { settingsService.setSelectedMerchant(it) }
                settingsService.setOnboardingSettings(onboardingSettings)
                settingsService.setPresentationSettings(presentationSettings)
            }
            logger.info("Settings saved")
        }
    }

    @OptIn(PrivateBetaConnectSDK::class)
    private fun fetchClientSecret(resultCallback: ClientSecretResultCallback) {
        val account = state.value.selectedAccount ?: return resultCallback.onResult(null)
        viewModelScope.launch {
            try {
                val clientSecret = embeddedComponentService.fetchClientSecret(account.merchantId)
                resultCallback.onResult(clientSecret)
            } catch (e: FuelError) {
                resultCallback.onResult(null)
                logger.error("Error fetching client secret: $e")
            }
        }
    }

    @OptIn(PrivateBetaConnectSDK::class)
    private fun getAccounts() {
        viewModelScope.launch {
            try {
                val response = embeddedComponentService.getAccounts()
                _state.update {
                    it.copy(accounts = response.availableMerchants)
                }
                EmbeddedComponentManager.init(
                    configuration = EmbeddedComponentManager.Configuration(
                        publishableKey = response.publishableKey
                    ),
                    fetchClientSecret = this@SettingsViewModel::fetchClientSecret
                )
            } catch (e: FuelError) {
                logger.error("Error getting accounts: $e")
            }
        }
    }

    private fun loadAppSettings() {
        _state.update {
            it.copy(
                serverUrl = settingsService.getSelectedServerBaseURL(),
                selectedAccount = settingsService.getSelectedMerchant()?.let { merchantId ->
                    Merchant(merchantId, "")
                },
                onboardingSettings = settingsService.getOnboardingSettings(),
                presentationSettings = settingsService.getPresentationSettings()
            )
        }
    }

    data class SettingsState(
        val serverUrl: String,
        val selectedAccount: Merchant? = null,
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
