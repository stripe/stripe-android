package com.stripe.android.identity.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.net.Uri
import android.util.Size
import androidx.core.content.FileProvider
import com.stripe.android.camera.framework.image.constrainToSize
import com.stripe.android.camera.framework.image.cropCenter
import com.stripe.android.camera.framework.image.cropWithFill
import com.stripe.android.camera.framework.image.size
import com.stripe.android.camera.framework.util.centerOn
import com.stripe.android.camera.framework.util.intersectionWith
import com.stripe.android.camera.framework.util.maxAspectRatioInSize
import com.stripe.android.camera.framework.util.move
import com.stripe.android.camera.framework.util.toRect
import com.stripe.android.identity.ml.BoundingBox
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * Default implementation of [IdentityIO].
 */
internal class DefaultIdentityIO @Inject constructor(private val context: Context) : IdentityIO {
    override fun createInternalFileUri(): ContentUriResult {
        createImageFile().also { file ->
            return ContentUriResult(
                createUriForFile(file),
                file.absolutePath
            )
        }
    }

    override fun createUriForFile(file: File): Uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.StripeIdentityFileprovider",
        file
    )

    override fun resizeUriAndCreateFileToUpload(
        originalUri: Uri,
        verificationId: String,
        isFullFrame: Boolean,
        side: String?,
        maxDimension: Int,
        compressionQuality: Float
    ): File {
        val bitmap = context.contentResolver.openInputStream(originalUri)?.use { inputStream ->
            BitmapFactory.decodeStream(inputStream)
        } ?: throw IOException("Unable to decode identity image")
        try {
            return resizeBitmapAndCreateFileToUpload(
                bitmap = bitmap,
                verificationId = verificationId,
                fileName = buildString {
                    append(verificationId)
                    side?.let { append("_$it") }
                    if (isFullFrame) append("_full_frame")
                    append(".jpeg")
                },
                maxDimension = maxDimension,
                compressionQuality = compressionQuality
            )
        } finally {
            bitmap.recycle()
        }
    }

    override fun resizeBitmapAndCreateFileToUpload(
        bitmap: Bitmap,
        verificationId: String,
        fileName: String,
        maxDimension: Int,
        compressionQuality: Float
    ): File {
        val resized = bitmap.constrainToSize(Size(maxDimension, maxDimension))
        val quality = (compressionQuality * MAX_COMPRESSION_QUALITY).toInt()
        try {
            return writeImageFile(File(context.filesDir, fileName)) { output ->
                if (!resized.compress(Bitmap.CompressFormat.JPEG, quality, output)) {
                    throw IOException("Unable to encode identity image")
                }
            }
        } finally {
            // Camera frames can also be used by other uploads and the scanner.
            if (resized !== bitmap) resized.recycle()
        }
    }

    @Synchronized
    override fun createTestModeFileToUpload(image: TestModeImage): File {
        val asset = when (image) {
            TestModeImage.DOCUMENT_FRONT -> TestModeAsset(
                path = "stripe_identity_test_mode/document_front.png",
                fileName = "stripe_identity_test_mode_document_front.png"
            )
            TestModeImage.DOCUMENT_BACK -> TestModeAsset(
                path = "stripe_identity_test_mode/document_back.png",
                fileName = "stripe_identity_test_mode_document_back.png"
            )
            TestModeImage.SELFIE -> TestModeAsset(
                path = "stripe_identity_test_mode/selfie.png",
                fileName = "stripe_identity_test_mode_selfie.png"
            )
        }

        return File(context.cacheDir, asset.fileName).also { file ->
            if (!file.exists()) {
                writeImageFile(file) { output ->
                    context.assets.open(asset.path).use { input -> input.copyTo(output) }
                }
            }
        }
    }

    private fun writeImageFile(file: File, write: (FileOutputStream) -> Unit): File {
        var completed = false
        try {
            FileOutputStream(file, false).use(write)
            completed = true
            return file
        } finally {
            if (!completed) file.delete()
        }
    }

    override fun cropAndPadBitmap(
        original: Bitmap,
        boundingBox: BoundingBox,
        paddingSize: Float
    ): Bitmap {
        val modelSize = maxAspectRatioInSize(original.size(), 1f)
        if (original.config != Bitmap.Config.ARGB_8888) {
            // Bitmap.createBitmap(source, ...) can normalize other configs. Retain that behavior.
            val modelInput = original.cropCenter(modelSize)
            return modelInput.cropWithFill(cropRegion(modelInput.width, modelInput.height, boundingBox, paddingSize))
        }

        val modelBounds = modelSize.centerOn(original.size().toRect())
        // centerOn rounds odd dimensions down; use the actual centered bounds to preserve existing crops.
        val region = cropRegion(modelBounds.width(), modelBounds.height(), boundingBox, paddingSize)
        val intersection = Rect(0, 0, modelBounds.width(), modelBounds.height()).intersectionWith(region)
        val result = Bitmap.createBitmap(region.width(), region.height(), Bitmap.Config.ARGB_8888)
        var completed = false
        try {
            val canvas = Canvas(result)
            canvas.drawColor(Color.GRAY)
            // Draw directly from the camera frame instead of allocating a center crop and an intersection crop.
            canvas.drawBitmap(
                original,
                intersection.move(modelBounds.left, modelBounds.top),
                intersection.move(-region.left, -region.top),
                null
            )
            completed = true
            return result
        } finally {
            if (!completed) result.recycle()
        }
    }

    private fun cropRegion(width: Int, height: Int, boundingBox: BoundingBox, padding: Float): Rect = Rect(
        (width * boundingBox.left - padding).toInt(),
        (height * boundingBox.top - padding).toInt(),
        (width * (boundingBox.left + boundingBox.width) + padding).toInt(),
        (height * (boundingBox.top + boundingBox.height) + padding).toInt()
    )

    override fun createTFLiteFile(modelUrl: String): File {
        return File(
            context.cacheDir,
            generateTFLiteFileNameWithGitHash(modelUrl)
        )
    }

    override fun createCacheFile(): File {
        return File.createTempFile(
            generalCacheFileName(),
            ".tmp",
            context.filesDir
        )
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        return File.createTempFile(
            "JPEG_${generalCacheFileName()}_", /* prefix */
            ".jpg", /* suffix */
            context.filesDir
        )
    }

    private fun generalCacheFileName() =
        SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())

    // Find the githash part of the model URL and general a tflite file name.
    // Example of modelUrl: https://b.stripecdn.com/gelato/assets/50e98374a70b71b2ee7ec8c3060f187ee1d833bd/assets/id_detectors/tflite/2022-02-23/model.tflite
    // TODO(ccen): Add name and versioning of the model, calculate MD5 and build a more descriptive caching machanism.
    private fun generateTFLiteFileNameWithGitHash(modelUrl: String): String {
        return "${modelUrl.split('/')[5]}.tflite"
    }

    private data class TestModeAsset(
        val path: String,
        val fileName: String
    )
}

private const val MAX_COMPRESSION_QUALITY = 100
