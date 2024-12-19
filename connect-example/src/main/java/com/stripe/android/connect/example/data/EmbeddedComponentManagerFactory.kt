package com.stripe.android.connect.example.data

import com.github.kittinunf.fuel.core.FuelError
import com.stripe.android.connect.EmbeddedComponentManager
import com.stripe.android.connect.FetchClientSecretCallback.ClientSecretResultCallback
import com.stripe.android.connect.PrivateBetaConnectSDK
import com.stripe.android.connect.appearance.fonts.CustomFontSource
import com.stripe.android.core.BuildConfig
import com.stripe.android.core.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
    private val loggingTag = this::class.java.simpleName
    private val logger: Logger = Logger.getInstance(enableLogging = BuildConfig.DEBUG)
    private val ioScope: CoroutineScope by lazy { CoroutineScope(Dispatchers.IO) }

    /**
     * Creates an instance of [EmbeddedComponentManager].
     * Returns null if it cannot be created at this time.
     */
    fun createEmbeddedComponentManager(): EmbeddedComponentManager? {
        val publishableKey = embeddedComponentService.publishableKey.value
            ?: return null

        return EmbeddedComponentManager(
            configuration = EmbeddedComponentManager.Configuration(
                publishableKey = publishableKey,
            ),
            fetchClientSecretCallback = ::fetchClientSecret,
            customFonts = listOf(
                CustomFontSource(
                    "fonts/doto.ttf",
                    "doto",
                    weight = 1000,
                )
            )
        )
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
