package com.stripe.android.stripecardscan.framework

import androidx.annotation.VisibleForTesting
import java.io.File

/**
 * Fetched data information.
 */
internal sealed class FetchedData(
    open val modelClass: String,
    open val modelFrameworkVersion: Int,
    open val modelVersion: String,
    open val modelHash: String?,
    open val modelHashAlgorithm: String?
) {
    abstract val successfullyFetched: Boolean
}

internal data class FetchedResource(
    override val modelClass: String,
    override val modelFrameworkVersion: Int,
    override val modelVersion: String,
    override val modelHash: String?,
    override val modelHashAlgorithm: String?,
    val assetFileName: String?
) : FetchedData(modelClass, modelFrameworkVersion, modelVersion, modelHash, modelHashAlgorithm) {
    override val successfullyFetched: Boolean = assetFileName != null
}

internal data class FetchedFile(
    override val modelClass: String,
    override val modelFrameworkVersion: Int,
    override val modelVersion: String,
    override val modelHash: String?,
    override val modelHashAlgorithm: String?,
    val file: File?
) : FetchedData(modelClass, modelFrameworkVersion, modelVersion, modelHash, modelHashAlgorithm) {
    override val successfullyFetched: Boolean = modelHash != null
}

/**
 * An interface for getting data ready to be loaded into memory.
 */
internal interface Fetcher {
    val modelClass: String
    val modelFrameworkVersion: Int

    /**
     * Prepare data to be loaded into memory. If the fetched data is to be used immediately, the
     * fetcher will prioritize fetching from the cache over getting the latest version.
     *
     * @param forImmediateUse: if there is a cached version of the model, return that immediately
     * instead of downloading a new model
     */
    suspend fun fetchData(forImmediateUse: Boolean, isOptional: Boolean): FetchedData

    suspend fun isCached(): Boolean

    /**
     * Clear the cache for this loader. This will force new downloads.
     */
    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    suspend fun clearCache()
}

/**
 * A [Fetcher] that gets data from android resources.
 */
internal abstract class ResourceFetcher : Fetcher {
    protected abstract val modelVersion: String
    protected abstract val hash: String
    protected abstract val hashAlgorithm: String
    protected abstract val assetFileName: String

    override suspend fun fetchData(forImmediateUse: Boolean, isOptional: Boolean): FetchedResource =
        FetchedResource(
            modelClass = modelClass,
            modelFrameworkVersion = modelFrameworkVersion,
            modelVersion = modelVersion,
            modelHash = hash,
            modelHashAlgorithm = hashAlgorithm,
            assetFileName = assetFileName
        )

    override suspend fun isCached(): Boolean = true

    override suspend fun clearCache() { /* no op */ }
}
