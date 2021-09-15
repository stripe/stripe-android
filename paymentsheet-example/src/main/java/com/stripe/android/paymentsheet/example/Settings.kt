package com.stripe.android.paymentsheet.example

import android.content.Context
import android.content.pm.PackageManager
import com.stripe.android.paymentsheet.example.service.CheckoutBackendApi

/**
 * Class that provides global app settings.
 */
class Settings(context: Context) {
    private val appContext = context.applicationContext
    private val backendMetadata = getMetadata(METADATA_KEY_BACKEND_URL_KEY)

    val backendUrl: String
        get() {
            return backendMetadata ?: BASE_URL
        }

    private fun getMetadata(key: String): String? {
        return appContext.packageManager
            .getApplicationInfo(appContext.packageName, PackageManager.GET_META_DATA)
            .metaData
            .getString(key)
            .takeIf { it?.isNotBlank() == true }
    }

    internal companion object {
        /**
         * The base URL of the test backend, implementing a `/checkout` endpoint as defined by
         * [CheckoutBackendApi].
         *
         * Note: only necessary if not configured via `gradle.properties`.
         */
        private const val BASE_URL =
            "https://quaint-ludicrous-skipjack.glitch.me/"//""https://stripe-mobile-payment-sheet-test-playground-v4.glitch.me/"

        private const val METADATA_KEY_BACKEND_URL_KEY =
            "com.stripe.android.paymentsheet.example.metadata.backend_url"
    }
}
