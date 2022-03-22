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
import com.stripe.android.camera.framework.image.longerEdge
import com.stripe.android.camera.framework.image.size
import com.stripe.android.camera.framework.image.toJpeg
import com.stripe.android.camera.framework.util.maxAspectRatioInSize
import com.stripe.android.identity.ml.BoundingBox
import com.stripe.android.identity.networking.models.VerificationPageStaticContentDocumentCapturePage
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Default implementation of [IdentityIO].
 */
internal class DefaultIdentityIO(private val context: Context) : IdentityIO {
    override fun createInternalFileUri(): ContentUriResult {
        createImageFile().also { file ->
            return ContentUriResult(
                FileProvider.getUriForFile(
                    context,
                    "com.stripe.android.identity.fileprovider",
                    file
                ),
                file.absolutePath
            )
        }
    }

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
        isFullFrame: Boolean,
        side: String?,
        maxDimension: Int,
        compressionQuality: Float,
    ): File {
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
        docCapturePage: VerificationPageStaticContentDocumentCapturePage
    ): Bitmap {
        val modelInput = original.cropCenter(
            maxAspectRatioInSize(
                original.size(),
                1f
            )
        )

        val paddingSize = original.longerEdge() * docCapturePage.highResImageCropPadding
        return modelInput.cropWithFill(
            Rect(
                (modelInput.width * boundingBox.left - paddingSize).toInt(),
                (modelInput.height * boundingBox.top - paddingSize).toInt(),
                (modelInput.width * (boundingBox.left + boundingBox.width) + paddingSize).toInt(),
                (modelInput.height * (boundingBox.top + boundingBox.height) + paddingSize).toInt()
            )
        )
    }

    override fun createTFLiteFile(): File {
        return File(
            context.filesDir,
            generateTFLiteFileName()
        )
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        return File.createTempFile(
            "${generateJpgFileName()}_", /* prefix */
            ".jpg", /* suffix */
            context.filesDir
        )
    }

    private fun generateJpgFileName() =
        "JPEG_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())

    private fun generateTFLiteFileName() =
        "TFLITE_${(SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date()))}.tflite"
}
