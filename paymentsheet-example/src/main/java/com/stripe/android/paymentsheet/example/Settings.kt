package com.stripe.android.paymentsheet.example

import android.content.Context
import android.content.pm.PackageManager

/**
 * See [Configure the app](https://github.com/stripe/stripe-android/tree/master/example#configure-the-app)
 * for instructions on how to configure the example app before running it.
 */
class Settings(context: Context) {
    private val appContext = context.applicationContext
    private val backendMetadata = getMetadata(METADATA_KEY_BACKEND_URL_KEY)
    private val publishableKeyMetadata = getMetadata(METADATA_KEY_PUBLISHABLE_KEY)

    val backendUrl: String
        get() {
            return backendMetadata ?: BASE_URL
        }

    val publishableKey: String
        get() {
            return publishableKeyMetadata ?: PUBLISHABLE_KEY
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
         * Note: only necessary if not configured via `gradle.properties`.
         *
         * Set to the base URL of your test backend. If you are using
         * [example-mobile-backend](https://github.com/stripe/example-mobile-backend),
         * the URL will be something like `https://hidden-beach-12345.herokuapp.com/`.
         */
        private const val BASE_URL = "put your base url here"

        /**
         * Note: only necessary if not configured via `gradle.properties`.
         *
         * Set to publishable key from https://dashboard.stripe.com/test/apikeys
         */
        private const val PUBLISHABLE_KEY = "pk_test_your_key_goes_here"

        private const val METADATA_KEY_BACKEND_URL_KEY =
            "com.stripe.android.paymentsheet.example.metadata.backend_url"
        private const val METADATA_KEY_PUBLISHABLE_KEY =
            "com.stripe.android.paymentsheet.example.metadata.publishable_key"
    }
}
