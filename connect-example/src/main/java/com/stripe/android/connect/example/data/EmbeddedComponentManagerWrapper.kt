package com.stripe.android.connect.example.data

import com.github.kittinunf.fuel.core.FuelError
import com.stripe.android.connect.EmbeddedComponentManager
import com.stripe.android.connect.FetchClientSecretCallback.ClientSecretResultCallback
import com.stripe.android.connect.PrivateBetaConnectSDK
import com.stripe.android.core.BuildConfig
import com.stripe.android.core.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.io.IOException

@OptIn(PrivateBetaConnectSDK::class)
class EmbeddedComponentManagerWrapper(
    private val embeddedComponentService: EmbeddedComponentService,
    private val settingsService: SettingsService,
) {

    private val loggingTag = this::class.java.name
    private val logger: Logger = Logger.getInstance(enableLogging = BuildConfig.DEBUG)
    private val ioScope: CoroutineScope by lazy { CoroutineScope(Dispatchers.IO) }

    /**
     * Init must be called in Application.onCreate().
     */
    fun init() {
        reinitManagerOnPublishableKeyChange()
    }

    // re-init the manager anytime the publishable key changes
    private fun reinitManagerOnPublishableKeyChange() {
        ioScope.launch {
            val publishableKey = embeddedComponentService.publishableKey.filterNotNull().firstOrNull() ?: return@launch

            EmbeddedComponentManager.init(
                configuration = EmbeddedComponentManager.Configuration(
                    publishableKey = publishableKey,
                ),
                fetchClientSecret = ::fetchClientSecret,
            )
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
}
