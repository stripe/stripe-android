package com.stripe.android.financialconnections.example.data

import android.content.Context
import android.content.pm.PackageManager

/**
 * See [Configure the app](https://github.com/stripe/stripe-android/tree/master/example#configure-the-app)
 * for instructions on how to configure the example app before running it.
 */
class Settings(context: Context) {
    private val appContext = context.applicationContext
    private val backendMetadata = getMetadata(METADATA_BACKEND_URL_KEY)

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

    private companion object {
        /**
         * Note: only necessary if not configured via `gradle.properties`.
         *
         * Set to the base URL of your test backend. If you are using
         * [example-mobile-backend](https://github.com/stripe/example-mobile-backend),
         * the URL will be something like `https://stripe-example-mobile-backend.glitch.me/`.
         */
        private const val BASE_URL = "https://stripe-mobile-connections-example.glitch.me/"

        private const val METADATA_BACKEND_URL_KEY =
            "com.stripe.financialconnections.example.metadata.backend_url"
    }
}
