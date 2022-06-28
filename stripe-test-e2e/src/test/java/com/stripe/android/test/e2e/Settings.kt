package com.stripe.android.test.e2e

import android.content.Context
import android.content.pm.PackageManager

/**
 * See [Configure the app](https://github.com/stripe/stripe-android/tree/master/example#configure-the-app)
 * for instructions on how to configure the example app before running it.
 */
internal data class Settings(
    val backendUrl: String,
    val publishableKey: String
) {
    constructor(context: Context) : this(
        getMetadata(context, METADATA_KEY_BACKEND_URL_KEY),
        getMetadata(context, METADATA_KEY_PUBLISHABLE_KEY)
    )

    private companion object {
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
        private const val METADATA_KEY_PUBLISHABLE_KEY =
            "com.stripe.android.test.e2e.metadata.publishable_key"
    }
}
