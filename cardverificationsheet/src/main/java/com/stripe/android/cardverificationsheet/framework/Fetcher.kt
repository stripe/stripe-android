package com.stripe.android.cardverificationsheet.framework

import android.util.Log
import androidx.annotation.VisibleForTesting
import com.stripe.android.cardverificationsheet.framework.api.downloadFileWithRetries
import com.stripe.android.cardverificationsheet.framework.time.days
import com.stripe.android.cardverificationsheet.framework.util.HashMismatchException
import com.stripe.android.cardverificationsheet.framework.util.calculateHash
import com.stripe.android.cardverificationsheet.framework.util.memoizeSuspend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.URL
import java.security.NoSuchAlgorithmException

/**
 * Fetched data metadata.
 */
sealed class FetchedModelMeta(open val modelVersion: String, open val hashAlgorithm: String)
internal data class FetchedModelFileMeta(
    override val modelVersion: String,
    override val hashAlgorithm: String,
    val modelFile: File?,
) : FetchedModelMeta(modelVersion, hashAlgorithm)

internal data class FetchedModelResourceMeta(
    override val modelVersion: String,
    override val hashAlgorithm: String,
    val hash: String,
    val assetFileName: String?,
) : FetchedModelMeta(modelVersion, hashAlgorithm)

/**
 * Fetched data information.
 */
sealed class FetchedData(
    open val modelClass: String,
    open val modelFrameworkVersion: Int,
    open val modelVersion: String,
    open val modelHash: String?,
    open val modelHashAlgorithm: String?,
) {
    companion object {
        fun fromFetchedModelMeta(
            modelClass: String,
            modelFrameworkVersion: Int,
            meta: FetchedModelMeta,
        ) = when (meta) {
            is FetchedModelFileMeta ->
                FetchedFile(
                    modelClass = modelClass,
                    modelFrameworkVersion = modelFrameworkVersion,
                    modelVersion = meta.modelVersion,
                    modelHash = meta.modelFile?.let {
                        runBlocking {
                            try {
                                calculateHash(it, meta.hashAlgorithm)
                            } catch (t: Throwable) {
                                null
                            }
                        }
                    },
                    modelHashAlgorithm = meta.hashAlgorithm,
                    file = meta.modelFile
                )
            is FetchedModelResourceMeta ->
                FetchedResource(
                    modelClass = modelClass,
                    modelFrameworkVersion = modelFrameworkVersion,
                    modelVersion = meta.modelVersion,
                    modelHash = meta.hash,
                    modelHashAlgorithm = meta.hashAlgorithm,
                    assetFileName = meta.assetFileName,
                )
        }
    }

    abstract val successfullyFetched: Boolean
}

data class FetchedResource(
    override val modelClass: String,
    override val modelFrameworkVersion: Int,
    override val modelVersion: String,
    override val modelHash: String?,
    override val modelHashAlgorithm: String?,
    val assetFileName: String?,
) : FetchedData(modelClass, modelFrameworkVersion, modelVersion, modelHash, modelHashAlgorithm) {
    override val successfullyFetched: Boolean = assetFileName != null
}

data class FetchedFile(
    override val modelClass: String,
    override val modelFrameworkVersion: Int,
    override val modelVersion: String,
    override val modelHash: String?,
    override val modelHashAlgorithm: String?,
    val file: File?,
) : FetchedData(modelClass, modelFrameworkVersion, modelVersion, modelHash, modelHashAlgorithm) {
    override val successfullyFetched: Boolean = modelHash != null
}

/**
 * An interface for getting data ready to be loaded into memory.
 */
interface Fetcher {
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
abstract class ResourceFetcher : Fetcher {
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
            assetFileName = assetFileName,
        )

    override suspend fun isCached(): Boolean = true

    override suspend fun clearCache() { /* no op */ }
}

/**
 * A [Fetcher] that downloads data from the web.
 */
abstract class WebFetcher : Fetcher {
    protected data class DownloadDetails(
        val url: URL,
        val hash: String,
        val hashAlgorithm: String,
        val modelVersion: String,
    )

    /**
     * Keep track of any exceptions that occurred when fetching data  after the specified number of
     * retries. This is used to prevent the fetcher from repeatedly trying to fetch the data from
     * multiple threads after the number of retries has been reached.
     */
    private var fetchException: Throwable? = null

    override suspend fun fetchData(forImmediateUse: Boolean, isOptional: Boolean): FetchedData {
        val stat = Stats.trackPersistentRepeatingTask("web_fetcher_$modelClass")
        val cachedData = FetchedData.fromFetchedModelMeta(
            modelClass,
            modelFrameworkVersion,
            tryFetchLatestCachedData(),
        )

        // attempt to fetch the data from local cache if it's needed immediately or downloading is
        // not allowed
        if (forImmediateUse) {
            tryFetchLatestCachedData().run {
                val data =
                    FetchedData.fromFetchedModelMeta(modelClass, modelFrameworkVersion, this)
                if (data.successfullyFetched) {
                    Log.d(
                        Config.logTag,
                        "Fetcher: $modelClass is needed immediately and cached version " +
                            "${data.modelVersion} is available.",
                    )
                    stat.trackResult("success")
                    return@fetchData data
                }
            }
        }

        // get details for downloading the data. If download details cannot be retrieved, use the
        // latest cached version
        val downloadDetails =
            fetchDownloadDetails(cachedData.modelHash, cachedData.modelHashAlgorithm) ?: run {
                Log.d(
                    Config.logTag,
                    "Fetcher: using cached version ${cachedData.modelVersion} for $modelClass",
                )
                stat.trackResult("no_download_details")
                return@fetchData cachedData
            }

        // if no cache is available, this is needed immediately, and this is optional, return a
        // download failure
        if (forImmediateUse && isOptional) {
            Log.d(
                Config.logTag,
                "Fetcher: optional $modelClass needed for immediate use, but no cache available.",
            )
            stat.trackResult("optional_model_not_downloaded")
            return FetchedData.fromFetchedModelMeta(
                modelClass = modelClass,
                modelFrameworkVersion = modelFrameworkVersion,
                meta = FetchedModelFileMeta(
                    modelVersion = downloadDetails.modelVersion,
                    hashAlgorithm = downloadDetails.hashAlgorithm,
                    modelFile = null,
                ),
            )
        }

        return try {
            // check the local cache for a matching model
            tryFetchMatchingCachedFile(downloadDetails.hash, downloadDetails.hashAlgorithm).run {
                val data =
                    FetchedData.fromFetchedModelMeta(modelClass, modelFrameworkVersion, this)
                if (data.successfullyFetched) {
                    Log.d(
                        Config.logTag,
                        "Fetcher: $modelClass already has latest version downloaded.",
                    )
                    stat.trackResult("success_cached")
                    return@fetchData data
                }
            }

            downloadData(downloadDetails).also {
                if (it.successfullyFetched) {
                    Log.d(
                        Config.logTag,
                        "Fetcher: $modelClass successfully downloaded.",
                    )
                    stat.trackResult("success_downloaded")
                } else {
                    Log.d(
                        Config.logTag,
                        "Fetcher: $modelClass failed to download from $downloadDetails.",
                    )
                    stat.trackResult("download_failed")
                }
            }
        } catch (t: Throwable) {
            fetchException = t
            if (cachedData.successfullyFetched) {
                Log.w(
                    Config.logTag,
                    "Fetcher: Failed to download model $modelClass, loaded from local cache",
                    t,
                )
                stat.trackResult("success_download_failed_but_cached")
            } else {
                Log.e(
                    Config.logTag,
                    "Fetcher: Failed to download model $modelClass, no local cache available",
                    t,
                )
                stat.trackResult(t::class.java.simpleName)
            }
            cachedData
        }
    }

    override suspend fun isCached(): Boolean = when (val meta = tryFetchLatestCachedData()) {
        is FetchedModelFileMeta -> meta.modelFile != null
        is FetchedModelResourceMeta -> true
    }

    /**
     * Get information about what version of the model to download.
     */
    private val fetchDownloadDetails =
        memoizeSuspend(3.days) { cachedHash: String?, cachedHashAlgorithm: String? ->
            getDownloadDetails(cachedHash, cachedHashAlgorithm)
        }

    /**
     * Download the data using memoization so that data is only downloaded once.
     */
    private val downloadData = memoizeSuspend { downloadDetails: DownloadDetails ->
        val downloadOutputFile = getDownloadOutputFile(downloadDetails.modelVersion)

        // if a previous exception was encountered, attempt to fetch cached data
        fetchException?.run {
            Log.d(
                Config.logTag,
                "Fetcher: Previous exception encountered for $modelClass, rethrowing",
            )
            throw this
        }

        try {
            downloadAndVerify(
                url = downloadDetails.url,
                outputFile = downloadOutputFile,
                hash = downloadDetails.hash,
                hashAlgorithm = downloadDetails.hashAlgorithm,
            )

            Log.d(
                Config.logTag,
                "Fetcher: $modelClass downloaded version ${downloadDetails.modelVersion}",
            )
            return@memoizeSuspend FetchedFile(
                modelClass = modelClass,
                modelFrameworkVersion = modelFrameworkVersion,
                modelVersion = downloadDetails.modelVersion,
                modelHash = downloadDetails.hash,
                modelHashAlgorithm = downloadDetails.hashAlgorithm,
                file = downloadOutputFile,
            )
        } finally {
            cleanUpPostDownload(downloadOutputFile)
        }
    }

    /**
     * Attempt to load the data from the local cache.
     */
    protected abstract suspend fun tryFetchLatestCachedData(): FetchedModelMeta

    /**
     * Attempt to load a cached data given the required [hash] and [hashAlgorithm].
     */
    protected abstract suspend fun tryFetchMatchingCachedFile(
        hash: String,
        hashAlgorithm: String,
    ): FetchedModelMeta

    /**
     * Get [DownloadDetails] for the data that will be downloaded.
     *
     * @param cachedModelHash: the hash of the cached model, or null if nothing is cached
     * @param cachedModelHashAlgorithm: the hash algorithm used to calculate the hash
     */
    protected abstract suspend fun getDownloadDetails(
        cachedModelHash: String?,
        cachedModelHashAlgorithm: String?,
    ): DownloadDetails?

    /**
     * Get the file where the data should be downloaded.
     */
    protected abstract suspend fun getDownloadOutputFile(modelVersion: String): File

    /**
     * After download, clean up.
     */
    protected abstract suspend fun cleanUpPostDownload(downloadedFile: File)
}

/**
 * Download a file from a given [url] and ensure that it matches the expected [hash].
 */
@Throws(IOException::class, NoSuchAlgorithmException::class, HashMismatchException::class)
private suspend fun downloadAndVerify(
    url: URL,
    outputFile: File,
    hash: String,
    hashAlgorithm: String
) {
    downloadFile(url, outputFile)
    val calculatedHash = calculateHash(outputFile, hashAlgorithm)

    if (hash != calculatedHash) {
        withContext(Dispatchers.IO) { outputFile.delete() }
        throw HashMismatchException(hashAlgorithm, hash, calculatedHash)
    }
}

/**
 * Download a file from the provided [url] into the provided [outputFile].
 */
@Throws(IOException::class, FileAlreadyExistsException::class, NoSuchFileException::class)
private suspend fun downloadFile(
    url: URL,
    outputFile: File,
) = withContext(Dispatchers.IO) {
    if (outputFile.exists()) {
        outputFile.delete()
    }
    downloadFileWithRetries(url, outputFile)
}
