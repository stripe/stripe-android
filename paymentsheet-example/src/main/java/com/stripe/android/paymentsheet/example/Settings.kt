package com.stripe.android.paymentsheet.example

import android.content.Context
import android.content.pm.PackageManager

/**
 * Class that provides global app settings.
 */
class Settings(context: Context) {
    private val appContext = context.applicationContext
    private val backendMetadata = getMetadata(METADATA_KEY_BACKEND_URL_KEY)
    private val googlePlacesMetadata = getMetadata(METADATA_KEY_GOOGLE_PLACES_API_KEY)

    val playgroundBackendUrl: String
        get() {
            return backendMetadata ?: BASE_URL
        }

    val googlePlacesApiKey: String?
        get() {
            return googlePlacesMetadata
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
         * The base URL of the test backend, implementing a `/checkout` endpoint.
         *
         * Note: only necessary if not configured via `gradle.properties`.
         */
        private const val BASE_URL = "https://stp-mobile-playground-backend-v7.stripedemos.com/"

        private const val METADATA_KEY_BACKEND_URL_KEY =
            "com.stripe.android.paymentsheet.example.metadata.backend_url"
        private const val METADATA_KEY_GOOGLE_PLACES_API_KEY =
            "com.stripe.android.paymentsheet.example.metadata.google_places_api_key"
    }
}
