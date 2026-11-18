package com.stripe.android.identity.utils

import android.graphics.Bitmap
import android.net.Uri
import app.cash.turbine.Turbine
import com.stripe.android.identity.ml.BoundingBox
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

internal class FakeIdentityIO(
    private val imageFile: File,
    private val croppedBitmap: Bitmap,
    private val placeholderFile: File,
) : IdentityIO {
    val calls = Turbine<Call>()
    var preparationFailure: Exception? = null
    var cropFailure: Exception? = null
    var placeholderFailure: Exception? = null
    var preparationGate: CountDownLatch? = null
    var filesByUri: Map<Uri, File> = emptyMap()

    override fun resizeUriAndCreateFileToUpload(
        originalUri: Uri,
        verificationId: String,
        isFullFrame: Boolean,
        side: String?,
        maxDimension: Int,
        compressionQuality: Float
    ): File {
        return preparedFile(filesByUri[originalUri] ?: imageFile, Call(Operation.READ_URI, uri = originalUri))
    }

    override fun resizeBitmapAndCreateFileToUpload(
        bitmap: Bitmap,
        verificationId: String,
        fileName: String,
        maxDimension: Int,
        compressionQuality: Float
    ): File {
        return preparedFile(imageFile, Call(Operation.RESIZE_BITMAP, bitmap = bitmap))
    }

    override fun cropAndPadBitmap(original: Bitmap, boundingBox: BoundingBox, paddingSize: Float): Bitmap {
        val failure = cropFailure
        calls.add(Call(Operation.CROP, bitmap = original))
        failure?.let { throw it }
        return croppedBitmap
    }

    override fun createTestModeFileToUpload(image: TestModeImage): File {
        val failure = placeholderFailure
        calls.add(Call(Operation.TEST_MODE))
        failure?.let { throw it }
        return placeholderFile
    }

    private fun preparedFile(file: File, call: Call): File {
        val gate = preparationGate
        val failure = preparationFailure
        calls.add(call)
        gate?.let { check(it.await(10, TimeUnit.SECONDS)) { "Preparation gate was not released" } }
        failure?.let { throw it }
        return file
    }

    override fun createInternalFileUri(): ContentUriResult = error("Unexpected file creation")
    override fun createUriForFile(file: File): Uri = error("Unexpected URI creation")
    override fun createTFLiteFile(modelUrl: String): File = error("Unexpected model download")
    override fun createCacheFile(): File = error("Unexpected cache file")

    fun ensureAllEventsConsumed() = calls.ensureAllEventsConsumed()

    enum class Operation { READ_URI, RESIZE_BITMAP, CROP, TEST_MODE }

    data class Call(
        val operation: Operation,
        val uri: Uri? = null,
        val bitmap: Bitmap? = null,
        val thread: Thread = Thread.currentThread(),
    )
}
