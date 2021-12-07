package com.stripe.android.stripecardscan.example

import android.content.Context
import android.content.pm.PackageManager

class Settings(context: Context) {
    private val appContext = context.applicationContext
    private val backendMetadata = getMetadata(METADATA_KEY_BACKEND_URL_KEY)
    private val publishableKeyMetadata = getMetadata(METADATA_KEY_PUBLISHABLE_KEY)

    val backendUrl: String
        get() {
            return (backendMetadata ?: BASE_URL).trimEnd('/')
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

    private companion object {
        /**
         * Note: only necessary if not configured via `gradle.properties`.
         *
         * Set to the base URL of your test backend. If you are using
         * [example-mobile-backend](https://glitch.com/edit/#!/stripe-card-scan-civ-example-app/),
         * the URL will be something like `https://resonant-thread-rock.glitch.me/`.
         */
        private const val BASE_URL = "https://put your base url here"

        /**
         * Note: only necessary if not configured via `gradle.properties`.
         *
         * Set to publishable key from https://dashboard.stripe.com/test/apikeys
         */
        private const val PUBLISHABLE_KEY = "pk_test_your_key_goes_here"

        private const val METADATA_KEY_BACKEND_URL_KEY =
            "com.stripe.android.stripecardscan.example.metadata.backend_url"
        private const val METADATA_KEY_PUBLISHABLE_KEY =
            "com.stripe.android.stripecardscan.example.metadata.publishable_key"
    }
}
