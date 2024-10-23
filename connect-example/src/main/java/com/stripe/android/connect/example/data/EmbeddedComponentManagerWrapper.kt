package com.stripe.android.connect.example.data

import android.app.Application
import com.stripe.android.connect.EmbeddedComponentManager
import com.stripe.android.connect.FetchClientSecretCallback.ClientSecretResultCallback
import com.stripe.android.connect.PrivateBetaConnectSDK
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

@OptIn(PrivateBetaConnectSDK::class)
class EmbeddedComponentManagerWrapper private constructor(private val application: Application) {

    private val ioScope: CoroutineScope by lazy { CoroutineScope(Dispatchers.IO) }

    private val embeddedComponentService: EmbeddedComponentService by lazy { EmbeddedComponentService.getInstance() }
    private val settingsService: SettingsService by lazy { SettingsService(application.baseContext) }

    init {
        reinitManagerOnPublishableKeyChange()
    }

    // re-init the manager anytime the publishable key changes
    private fun reinitManagerOnPublishableKeyChange() {
        ioScope.launch {
            settingsService.publishableKey
                .filterNotNull()
                .collect { publishableKey ->
                    EmbeddedComponentManager.init(
                        configuration = EmbeddedComponentManager.Configuration(
                            publishableKey = publishableKey,
                        ),
                        fetchClientSecret = ::fetchClientSecret,
                    )
                }
        }
    }

    /**
     * Helper wrapper around [fetchClientSecret] that fetches the client secret
     */
    @OptIn(PrivateBetaConnectSDK::class)
    private fun fetchClientSecret(clientSecretResultCallback: ClientSecretResultCallback) {
        val account: String = settingsService.getSelectedMerchant()
            ?: return clientSecretResultCallback.onResult(null)

        ioScope.launch {
            try {
                val clientSecret = embeddedComponentService.fetchClientSecret(account)
                clientSecretResultCallback.onResult(clientSecret)
            } catch (e: Exception) {
                clientSecretResultCallback.onResult(null)
            }
        }
    }

    companion object {
        private var instance: EmbeddedComponentManagerWrapper? = null

        /**
         * Initializes the wrapper. This must be done in Application.onCreate().
         */
        fun init(application: Application): EmbeddedComponentManagerWrapper {
            return EmbeddedComponentManagerWrapper(application).also {
                instance = it
            }
        }


        fun getInstance(): EmbeddedComponentManagerWrapper {
            return instance ?: throw IllegalStateException("EmbeddedComponentManagerWrapper is not initialized")
        }
    }
}