package com.stripe.android.identity.utils

import android.graphics.Bitmap
import android.net.Uri
import com.stripe.android.identity.ml.BoundingBox
import java.io.File
import java.io.IOException

/**
 * A wrapper data class to save the Uri and absolute path created for a file.
 */
data class ContentUriResult(val contentUri: Uri, val absolutePath: String)

/**
 * Interface for accessing IO for identity
 */
internal interface IdentityIO {
    /**
     * Create a file in app's internal files dir and return the uri
     */
    @Throws(IOException::class)
    fun createInternalFileUri(): ContentUriResult

    /**
     * Create a Uri from a file
     */
    @Throws(IOException::class)
    fun createUriForFile(file: File): Uri

    /**
     * Read the image at uri, resize it with corresponding resolution, compress it and save it as a
     * [File] with proper name.
     */
    fun resizeUriAndCreateFileToUpload(
        originalUri: Uri,
        verificationId: String,
        isFullFrame: Boolean,
        side: String? = null,
        maxDimension: Int,
        compressionQuality: Float
    ): File

    /**
     * Resize the bitmap with corresponding resolution, compress it and save as a jpeg [File] with
     * proper name.
     */
    fun resizeBitmapAndCreateFileToUpload(
        bitmap: Bitmap,
        verificationId: String,
        fileName: String,
        maxDimension: Int,
        compressionQuality: Float
    ): File

    /**
     * Crop and pad a bitmap to upload as high resolution image.
     *
     * First find the center cropped bitmap of original bitmap, this is the same bitmap sent to ML model.
     * Then crop the the bitmap with boundingBox.
     * Finally pad the bitmap with the padding parameter.
     */
    fun cropAndPadBitmap(
        original: Bitmap,
        boundingBox: BoundingBox,
        paddingSize: Float
    ): Bitmap

    /**
     * Create a file for tflite model.
     */
    fun createTFLiteFile(modelUrl: String): File

    /**
     * Create a file in cache.
     */
    fun createCacheFile(): File
}
