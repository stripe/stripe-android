package com.stripe.android.cardverificationsheet.framework

import android.content.Context
import android.util.Log
import com.stripe.android.cardverificationsheet.framework.ml.trackModelLoaded
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

/**
 * An interface for loading data into a byte buffer.
 */
internal class Loader(private val context: Context) {

    /**
     * Load previously fetched data into memory.
     */
    suspend fun loadData(fetchedData: FetchedData): ByteBuffer? = when (fetchedData) {
        is FetchedResource -> loadResourceData(fetchedData)
        is FetchedFile -> loadFileData(fetchedData)
    }

    /**
     * Create a [ByteBuffer] object from an android resource.
     */
    private suspend fun loadResourceData(fetchedData: FetchedResource): ByteBuffer? {
        val stat = Stats.trackPersistentRepeatingTask("resource_loader:${fetchedData.modelClass}")

        if (fetchedData.assetFileName == null) {
            trackModelLoaded(
                fetchedData.modelClass,
                fetchedData.modelVersion,
                fetchedData.modelFrameworkVersion,
                false,
            )
            stat.trackResult("failure:${fetchedData.modelClass}")
            return null
        }

        return try {
            val loadedData = readAssetToByteBuffer(context, fetchedData.assetFileName)
            stat.trackResult("success")
            trackModelLoaded(
                fetchedData.modelClass,
                fetchedData.modelVersion,
                fetchedData.modelFrameworkVersion,
                true,
            )
            loadedData
        } catch (t: Throwable) {
            Log.e(Config.logTag, "Failed to load resource", t)
            trackModelLoaded(
                fetchedData.modelClass,
                fetchedData.modelVersion,
                fetchedData.modelFrameworkVersion,
                false,
            )
            null
        }
    }

    /**
     * Create a [ByteBuffer] object from a [File].
     */
    private suspend fun loadFileData(fetchedData: FetchedFile): ByteBuffer? {
        val stat = Stats.trackPersistentRepeatingTask("web_loader:${fetchedData.modelClass}")

        if (fetchedData.file == null) {
            trackModelLoaded(
                fetchedData.modelClass,
                fetchedData.modelVersion,
                fetchedData.modelFrameworkVersion,
                false,
            )
            stat.trackResult("failure:${fetchedData.modelClass}")
            return null
        }

        return try {
            val loadedData = readFileToByteBuffer(fetchedData.file)
            stat.trackResult("success")
            trackModelLoaded(
                fetchedData.modelClass,
                fetchedData.modelVersion,
                fetchedData.modelFrameworkVersion,
                true,
            )
            loadedData
        } catch (t: Throwable) {
            stat.trackResult("failure:${fetchedData.modelClass}")
            trackModelLoaded(
                fetchedData.modelClass,
                fetchedData.modelVersion,
                fetchedData.modelFrameworkVersion,
                true,
            )
            null
        }
    }
}

/**
 * Read a [File] into a [ByteBuffer].
 */
private suspend fun readFileToByteBuffer(file: File) = withContext(Dispatchers.IO) {
    FileInputStream(file).use { readFileToByteBuffer(it, 0, file.length()) }
}

/**
 * Read a raw resource into a [ByteBuffer].
 */
private suspend fun readAssetToByteBuffer(context: Context, assetFileName: String) =
    withContext(Dispatchers.IO) {
        context.assets.openFd(assetFileName).use { fileDescriptor ->
            FileInputStream(fileDescriptor.fileDescriptor).use { input ->
                readFileToByteBuffer(
                    input,
                    fileDescriptor.startOffset,
                    fileDescriptor.declaredLength,
                )
            }
        }
    }

/**
 * Read a [fileInputStream] into a [ByteBuffer].
 */
@Throws(IOException::class)
private fun readFileToByteBuffer(
    fileInputStream: FileInputStream,
    startOffset: Long,
    declaredLength: Long
): ByteBuffer = fileInputStream.channel.map(
    FileChannel.MapMode.READ_ONLY,
    startOffset,
    declaredLength
)
