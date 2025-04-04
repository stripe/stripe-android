package com.stripe.android.connect.example.data

import com.github.kittinunf.fuel.core.FuelError
import com.stripe.android.connect.EmbeddedComponentManager
import com.stripe.android.connect.PrivateBetaConnectSDK
import com.stripe.android.connect.appearance.fonts.CustomFontSource
import com.stripe.android.core.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(PrivateBetaConnectSDK::class)
@Singleton
class EmbeddedComponentManagerFactory @Inject constructor(
    private val embeddedComponentService: EmbeddedComponentService,
    private val settingsService: SettingsService,
    private val logger: Logger,
) {
    private val loggingTag = this::class.java.simpleName

    /**
     * Creates an instance of [EmbeddedComponentManager].
     * Returns null if it cannot be created at this time.
     */
    fun createEmbeddedComponentManager(): EmbeddedComponentManager? {
        val publishableKey = embeddedComponentService.publishableKey.value
            ?: return null

        return EmbeddedComponentManager(
            publishableKey = publishableKey,
            fetchClientSecret = ::fetchClientSecret,
            customFonts = listOf(
                CustomFontSource(
                    assetsFilePath = "fonts/doto.ttf",
                    name = "doto",
                    weight = 1000,
                )
            )
        )
    }

    /**
     * Helper wrapper around [fetchClientSecret] that fetches the client secret
     */
    private suspend fun fetchClientSecret(): String? = withContext(Dispatchers.IO) {
        val account: String = settingsService.getSelectedMerchant()
            ?: return@withContext null

        try {
            embeddedComponentService.fetchClientSecret(account)
        } catch (e: FuelError) {
            logger.error("($loggingTag) Failed to fetch client secret", e)
            null
        } catch (e: IOException) {
            logger.error("($loggingTag) Failed to fetch client secret", e)
            null
        }
    }
}
