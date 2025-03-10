package com.stripe.android.connect.example.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stripe.android.connect.BuildConfig
import com.stripe.android.connect.example.core.Async
import com.stripe.android.connect.example.core.Uninitialized
import com.stripe.android.connect.example.data.EmbeddedComponentService
import com.stripe.android.connect.example.data.Merchant
import com.stripe.android.connect.example.data.OnboardingSettings
import com.stripe.android.connect.example.data.PresentationSettings
import com.stripe.android.connect.example.data.SettingsService
import com.stripe.android.connect.example.ui.settings.SettingsViewModel.SettingsState.DemoMerchant
import com.stripe.android.core.Logger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
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
        loadOneTimeSettings()
        observeAccountsFromService()
        observeSettingsChanges()
    }

    fun onAccountSelected(account: DemoMerchant) {
        _state.update { it.copy(selectedAccountId = account.merchantId) }
    }

    fun onOtherAccountInputChanged(otherAccountIdInput: String) {
        _state.update { state ->
            val wasOtherPreviouslySelected = state.selectedAccountIsOther
            state.copy(
                selectedAccountId = if (wasOtherPreviouslySelected) otherAccountIdInput else state.selectedAccountId,
                otherAccountInput = otherAccountIdInput,
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

    fun onPresentationSettingsConfirmed(presentationSettings: PresentationSettings) {
        viewModelScope.launch {
            settingsService.setPresentationSettings(presentationSettings)
            logger.info("($loggingTag) Presentation settings saved")

            _state.update { it.copy(presentationSettings = presentationSettings) }
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

    private fun loadOneTimeSettings() {
        _state.update { state ->
            state.copy(
                serverUrl = embeddedComponentService.serverBaseUrl,
                onboardingSettings = settingsService.getOnboardingSettings(),
                presentationSettings = settingsService.getPresentationSettings(),
            )
        }
    }

    private fun observeAccountsFromService() {
        viewModelScope.launch {
            embeddedComponentService.accounts.collectLatest { async ->
                _state.update { state ->
                    val accountsFromService = async()
                    val firstMerchantId = accountsFromService?.firstOrNull()?.merchantId
                    val selectedAccountId = settingsService.getSelectedMerchant()
                    // Persist selected merchant if it's not already set.
                    if (selectedAccountId == null && firstMerchantId != null) {
                        settingsService.setSelectedMerchant(firstMerchantId)
                    }
                    // Determine what the "other account" text input value should be.
                    // It can be set to the selected account but we don't want to override the user's input.
                    val otherAccountInput =
                        if (state.otherAccountInput == null &&
                            accountsFromService?.none { it.merchantId == selectedAccountId } == true
                        ) {
                            selectedAccountId
                        } else {
                            state.otherAccountInput
                        }
                    state.copy(
                        otherAccountInput = otherAccountInput,
                        accountsFromServiceAsync = async,
                        selectedAccountId = selectedAccountId,
                    )
                }
            }
        }
    }

    private fun observeSettingsChanges() {
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

    // State

    data class SettingsState(
        val serverUrl: String,
        val saveEnabled: Boolean = false,
        val accountsFromServiceAsync: Async<List<Merchant>> = Uninitialized,
        val selectedAccountId: String? = null,
        val otherAccountInput: String? = "",
        val onboardingSettings: OnboardingSettings = OnboardingSettings(),
        val presentationSettings: PresentationSettings = PresentationSettings()
    ) {
        val serverUrlResetEnabled: Boolean
            get() = serverUrl != EmbeddedComponentService.DEFAULT_SERVER_BASE_URL

        val selectedAccountIsOther: Boolean =
            accountsFromServiceAsync()?.none { it.merchantId == selectedAccountId } == true

        val accounts: List<DemoMerchant> = run {
            val accountsFromService = accountsFromServiceAsync() ?: emptyList()
            val otherAccount = DemoMerchant.Other(otherAccountInput ?: "")
            buildList {
                accountsFromService.forEach {
                    add(
                        DemoMerchant.Merchant(
                            displayName = it.displayName,
                            merchantId = it.merchantId
                        )
                    )
                }
                add(otherAccount)
            }
        }

        val selectedAccount: DemoMerchant?
            get() = accounts.firstOrNull { it.merchantId == selectedAccountId }

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
