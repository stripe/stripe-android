package com.stripe.android.identity.networking

import java.io.File

/**
 * A class to fetch the model file for IDDetector either from local cache or from web.
 *
 * TODO(ccen): Merge it with [Fetcher] from stripecardscan
 */
internal interface IDDetectorFetcher {
    suspend fun fetchIDDetector(modelUrl: String): File
}
