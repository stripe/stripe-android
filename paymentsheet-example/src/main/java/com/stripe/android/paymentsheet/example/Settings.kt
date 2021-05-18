package com.stripe.android.paymentsheet.example

import android.content.Context
import android.content.pm.PackageManager

/**
 * See [Configure the app](https://github.com/stripe/stripe-android/tree/master/paymentsheet-example#configure-the-app)
 * for instructions on how to configure the PaymentSheet example app before running it.
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
         * The base URL of your test backend, implementing a `/checkout` endpoint akin to our
         * [integration guide](https://stripe.com/docs/payments/accept-a-payment?platform=android#add-server-endpoint).
         *
         * Note: only necessary if not configured via `gradle.properties`.
         */
        private const val BASE_URL = "https://stripe-mobile-payment-sheet.glitch.me/"

        private const val METADATA_KEY_BACKEND_URL_KEY =
            "com.stripe.android.paymentsheet.example.metadata.backend_url"
    }
}
