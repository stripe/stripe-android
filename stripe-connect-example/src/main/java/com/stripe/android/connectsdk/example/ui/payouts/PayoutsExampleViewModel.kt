package com.stripe.android.connectsdk.example.ui.payouts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stripe.android.connectsdk.FetchClientSecretCallback.ClientSecretResultCallback
import com.stripe.android.connectsdk.PrivateBetaConnectSDK
import com.stripe.android.connectsdk.example.networking.EmbeddedComponentService
import kotlinx.coroutines.launch

class PayoutsExampleViewModel(
    private val embeddedComponentService: EmbeddedComponentService = EmbeddedComponentService(),
) : ViewModel() {

    @OptIn(PrivateBetaConnectSDK::class)
    fun fetchClientSecret(resultCallback: ClientSecretResultCallback) {
        viewModelScope.launch {
            val clientSecret = embeddedComponentService.fetchClientSecret()
            if (clientSecret != null) {
                resultCallback.onResult(clientSecret)
            } else {
                resultCallback.onError()
            }
        }
    }
}
