package com.stripe.android.test.e2e

import android.content.Context
import android.content.pm.PackageManager

/**
 * See [Configure the app](https://github.com/stripe/stripe-android/tree/master/example#configure-the-app)
 * for instructions on how to configure the example app before running it.
 */
internal data class Settings(
    val backendUrl: String
) {
    constructor(context: Context) : this(
        getMetadata(context, METADATA_KEY_BACKEND_URL_KEY)
    )

    companion object {
        const val PUBLISHABLE_KEY = "pk_test_ErsyMEOTudSjQR8hh0VrQr5X008sBXGOu6"

        /**
         * Return the manifest metadata value for the given key.
         */
        private fun getMetadata(
            context: Context,
            key: String
        ): String {
            return context.packageManager
                .getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
                .metaData
                .getString(key)
                .takeIf { it?.isNotBlank() == true }
                .orEmpty()
        }

        private const val METADATA_KEY_BACKEND_URL_KEY =
            "com.stripe.android.test.e2e.metadata.backend_url"
    }
}
