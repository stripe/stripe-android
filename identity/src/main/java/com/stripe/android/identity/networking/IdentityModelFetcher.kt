package com.stripe.android.identity.networking

import java.io.File

/**
 * A class to fetch the model file for Identity either from local cache or from web.
 *
 * TODO(ccen): Merge it with [Fetcher] from stripecardscan
 */
internal interface IdentityModelFetcher {
    suspend fun fetchIdentityModel(modelUrl: String): File
}
