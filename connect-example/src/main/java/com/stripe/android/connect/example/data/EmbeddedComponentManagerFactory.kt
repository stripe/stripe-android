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
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(PrivateBetaConnectSDK::class)
@Singleton
class EmbeddedComponentManagerFactory @Inject constructor(
    private val embeddedComponentService: EmbeddedComponentService,
    private val settingsService: SettingsService,
) {

    // this factory manages the EmbeddedComponentManager instance, since it needs to wait for
    // a publishable key to be received from the backend before building it.
    // In the future it may manage multiple instances if needed.
    private var embeddedComponentManager: EmbeddedComponentManager? = null

    private val loggingTag = this::class.java.name
    private val logger: Logger = Logger.getInstance(enableLogging = BuildConfig.DEBUG)
    private val ioScope: CoroutineScope by lazy { CoroutineScope(Dispatchers.IO) }

    /**
     * Init must be called in Application.onCreate().
     */
    fun init() {
        initializeEmbeddedComponentManagerInstance()
    }

    fun getEmbeddedComponentManager(): EmbeddedComponentManager {
        return embeddedComponentManager ?: throw IllegalStateException(
            "EmbeddedComponentManager not yet initialized. Publishable key must be populated in " +
                "EmbeddedComponentService first."
        )
    }

    private fun initializeEmbeddedComponentManagerInstance() {
        ioScope.launch {
            val publishableKey = embeddedComponentService.publishableKey.filterNotNull().firstOrNull() ?: return@launch

            embeddedComponentManager = EmbeddedComponentManager(
                configuration = EmbeddedComponentManager.Configuration(
                    publishableKey = publishableKey
                ),
                fetchClientSecretCallback = ::fetchClientSecret
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
