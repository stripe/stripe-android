package com.stripe.android.connect.example.data

import com.github.kittinunf.fuel.core.FuelError
import com.stripe.android.connect.EmbeddedComponentManager
import com.stripe.android.connect.FetchClientSecretCallback.ClientSecretResultCallback
import com.stripe.android.connect.PrivateBetaConnectSDK
import com.stripe.android.core.BuildConfig
import com.stripe.android.core.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import java.io.IOException

@OptIn(PrivateBetaConnectSDK::class)
class EmbeddedComponentManagerWrapper private constructor() {

    private val loggingTag = this::class.java.name
    private val logger: Logger = Logger.getInstance(enableLogging = BuildConfig.DEBUG)
    private val ioScope: CoroutineScope by lazy { CoroutineScope(Dispatchers.IO) }

    private val embeddedComponentService: EmbeddedComponentService by lazy { EmbeddedComponentService.getInstance() }
    private val settingsService: SettingsService by lazy { SettingsService.getInstance() }

    init {
        reinitManagerOnPublishableKeyChange()
    }

    // re-init the manager anytime the publishable key changes
    private fun reinitManagerOnPublishableKeyChange() {
        ioScope.launch {
            embeddedComponentService.publishableKey
                .filterNotNull()
                .distinctUntilChanged()
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
            } catch (e: FuelError) {
                logger.error("($loggingTag) Failed to fetch client secret", e)
                clientSecretResultCallback.onResult(null)
            } catch (e: IOException) {
                logger.error("($loggingTag) Failed to fetch client secret", e)
                clientSecretResultCallback.onResult(null)
            }
        }
    }

    companion object {
        private var instance: EmbeddedComponentManagerWrapper? = null

        /**
         * Initializes the wrapper. This must be done in Application.onCreate().
         */
        fun init(): EmbeddedComponentManagerWrapper {
            return EmbeddedComponentManagerWrapper().also {
                instance = it
            }
        }

        fun getInstance(): EmbeddedComponentManagerWrapper {
            return instance ?: throw IllegalStateException("EmbeddedComponentManagerWrapper is not initialized")
        }
    }
}
