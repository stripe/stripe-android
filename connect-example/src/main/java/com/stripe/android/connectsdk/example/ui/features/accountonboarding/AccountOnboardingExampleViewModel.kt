package com.stripe.android.connectsdk.example.ui.features.accountonboarding

import androidx.lifecycle.ViewModel
import com.github.kittinunf.fuel.core.FuelError
import com.stripe.android.connectsdk.EmbeddedComponentManager
import com.stripe.android.connectsdk.FetchClientSecretCallback.ClientSecretResultCallback
import com.stripe.android.connectsdk.PrivateBetaConnectSDK
import com.stripe.android.connectsdk.example.BuildConfig
import com.stripe.android.connectsdk.example.networking.EmbeddedComponentService
import com.stripe.android.connectsdk.example.networking.Merchant
import com.stripe.android.core.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AccountOnboardingExampleViewModel(
    private val embeddedComponentService: EmbeddedComponentService = EmbeddedComponentService(),
    private val networkingScope: CoroutineScope = CoroutineScope(Dispatchers.IO),
    private val logger: Logger = Logger.getInstance(enableLogging = BuildConfig.DEBUG),
) : ViewModel() {

    private val _state = MutableStateFlow(AccountOnboardingExampleState())
    val state: StateFlow<AccountOnboardingExampleState> = _state.asStateFlow()

    init {
        getAccounts()
    }

    // public methods

    fun onAccountSelected(account: Merchant) {
        _state.update {
            it.copy(selectedAccount = account)
        }
    }

    // private methods

    @OptIn(PrivateBetaConnectSDK::class)
    private fun fetchClientSecret(resultCallback: ClientSecretResultCallback) {
        val account = _state.value.selectedAccount ?: return resultCallback.onResult(null)
        networkingScope.launch {
            try {
                val clientSecret = embeddedComponentService.fetchClientSecret(account.merchantId)
                resultCallback.onResult(clientSecret)
            } catch (e: FuelError) {
                resultCallback.onResult(null)
                logger.error("(AccountOnboardingExampleViewModel) Error fetching client secret: $e")
            }
        }
    }

    @OptIn(PrivateBetaConnectSDK::class)
    private fun getAccounts() {
        networkingScope.launch {
            try {
                val response = embeddedComponentService.getAccounts()
                _state.update {
                    it.copy(
                        accounts = response.availableMerchants,
                    )
                }
                EmbeddedComponentManager.init(
                    configuration = EmbeddedComponentManager.Configuration(
                        publishableKey = response.publishableKey
                    ),
                    fetchClientSecret = this@AccountOnboardingExampleViewModel::fetchClientSecret
                )
            } catch (e: FuelError) {
                logger.error("(AccountOnboardingExampleViewModel) Error getting accounts: $e")
            }
        }
    }

    // state

    data class AccountOnboardingExampleState(
        val selectedAccount: Merchant? = null,
        val accounts: List<Merchant>? = null,
    )
}
