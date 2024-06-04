package com.stripe.android.identity.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.net.Uri
import android.util.Size
import androidx.core.content.FileProvider
import com.stripe.android.camera.framework.image.constrainToSize
import com.stripe.android.camera.framework.image.cropCenter
import com.stripe.android.camera.framework.image.cropWithFill
import com.stripe.android.camera.framework.image.size
import com.stripe.android.camera.framework.image.toJpeg
import com.stripe.android.camera.framework.util.maxAspectRatioInSize
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
        context.contentResolver.openInputStream(originalUri).use { inputStream ->
            File(
                context.filesDir,
                StringBuilder().also { nameBuilder ->
                    nameBuilder.append(verificationId)
                    side?.let {
                        nameBuilder.append("_$side")
                    }
                    if (isFullFrame) {
                        nameBuilder.append("_full_frame")
                    }
                    nameBuilder.append(".jpeg")
                }.toString()
            ).let { fileToSave ->
                FileOutputStream(fileToSave, false).use { fileOutputStream ->
                    fileOutputStream.write(
                        BitmapFactory.decodeStream(inputStream).constrainToSize(
                            Size(
                                maxDimension,
                                maxDimension
                            )
                        ).toJpeg(quality = (compressionQuality * 100).toInt())
                    )
                }
                return fileToSave
            }
        }
    }

    override fun resizeBitmapAndCreateFileToUpload(
        bitmap: Bitmap,
        verificationId: String,
        fileName: String,
        maxDimension: Int,
        compressionQuality: Float
    ): File {
        File(
            context.filesDir,
            fileName
        ).let { fileToSave ->
            FileOutputStream(fileToSave, false).use { fileOutputStream ->
                fileOutputStream.write(
                    bitmap.constrainToSize(
                        Size(
                            maxDimension,
                            maxDimension
                        )
                    ).toJpeg(quality = (compressionQuality * 100).toInt())
                )
            }
            return fileToSave
        }
    }

    override fun cropAndPadBitmap(
        original: Bitmap,
        boundingBox: BoundingBox,
        paddingSize: Float
    ): Bitmap {
        val modelInput = original.cropCenter(
            maxAspectRatioInSize(
                original.size(),
                1f
            )
        )

        return modelInput.cropWithFill(
            Rect(
                (modelInput.width * boundingBox.left - paddingSize).toInt(),
                (modelInput.height * boundingBox.top - paddingSize).toInt(),
                (modelInput.width * (boundingBox.left + boundingBox.width) + paddingSize).toInt(),
                (modelInput.height * (boundingBox.top + boundingBox.height) + paddingSize).toInt()
            )
        )
    }

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
}
