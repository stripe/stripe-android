package com.stripe.android.connectsdk.example.ui.features.payouts

import androidx.lifecycle.ViewModel
import com.github.kittinunf.fuel.core.FuelError
import com.stripe.android.connectsdk.FetchClientSecretCallback.ClientSecretResultCallback
import com.stripe.android.connectsdk.PrivateBetaConnectSDK
import com.stripe.android.connectsdk.example.networking.EmbeddedComponentService
import com.stripe.android.connectsdk.example.networking.Merchant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

class PayoutsExampleViewModel(
    private val embeddedComponentService: EmbeddedComponentService = EmbeddedComponentService(),
    private val networkingScope: CoroutineScope = CoroutineScope(Dispatchers.IO),
): ViewModel() {

    private val timber get() = Timber.tag("PayoutsExampleViewModel")

    private val _state = MutableStateFlow(PayoutsExampleState())
    val state: StateFlow<PayoutsExampleState> = _state.asStateFlow()

    init {
        getAccounts()
    }

    @OptIn(PrivateBetaConnectSDK::class)
    fun fetchClientSecret(resultCallback: ClientSecretResultCallback) {
        val account = _state.value.selectedAccount ?: return
        networkingScope.launch {
            try {
                val clientSecret = embeddedComponentService.fetchClientSecret(account.merchantId)
                resultCallback.onResult(clientSecret)
            } catch (e: FuelError) {
                resultCallback.onError()
                timber.e("Error fetching client secret: $e")
            }
        }
    }

    fun onAccountSelected(account: Merchant) {
        _state.update {
            it.copy(selectedAccount = account)
        }
    }

    private fun getAccounts() {
        networkingScope.launch {
            try {
                val response = embeddedComponentService.getAccounts()
                _state.update {
                    it.copy(
                        publishableKey = response.publishableKey,
                        accounts = response.availableMerchants,
                    )
                }
            } catch (e: FuelError) {
                timber.e("Error getting accounts: $e")
            }
        }
    }

    data class PayoutsExampleState(
        val selectedAccount: Merchant? = null,
        val accounts: List<Merchant>? = null,
        val publishableKey: String? = null,
    )
}