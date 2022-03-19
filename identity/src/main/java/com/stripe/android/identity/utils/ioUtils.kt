package com.stripe.android.identity.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.net.Uri
import android.os.Environment
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

data class ContentUriResult(val contentUri: Uri, val absolutePath: String)

/**
 * Create a file in app's external files Dir and return the uri
 */
internal fun createInternalFileUri(context: Context): ContentUriResult {
    createImageFile(context).also { file ->
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

/**
 * Read the image at uri, resize it with corresponding resolution, compress it and save it as a
 * [File] with proper name.
 */
internal fun resizeUriAndCreateFileToUpload(
    context: Context,
    originalUri: Uri,
    verificationId: String,
    isFullFrame: Boolean,
    side: String? = null,
    maxDimension: Int,
    compressionQuality: Float
): File {
    context.contentResolver.openInputStream(originalUri).use { inputStream ->
        File(
            context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
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

/**
 * Resize the bitmap with corresponding resolution, compress it and save as a jpeg [File] with
 * proper name.
 */
internal fun resizeBitmapAndCreateFileToUpload(
    context: Context,
    bitmap: Bitmap,
    verificationId: String,
    isFullFrame: Boolean,
    side: String? = null,
    maxDimension: Int,
    compressionQuality: Float,
): File {
    File(
        context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
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

/**
 * Crop and pad a bitmap to upload as high resolution image.
 *
 * First find the center cropped bitmap of original bitmap, this is the same bitmap sent to ML model.
 * Then crop the the bitmap with boundingBox.
 * Finally pad the bitmap with the padding parameter.
 */
internal fun cropAndPadBitmap(
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

/**
 * Create a file for tflite model.
 */
internal fun createTFLiteFile(context: Context): File {
    return File(
        context.filesDir,
        generateTFLiteFileName()
    )
}

@Throws(IOException::class)
private fun createImageFile(context: Context): File {
    return File.createTempFile(
        "${generateJpgFileName()}_", /* prefix */
        ".jpg", /* suffix */
        context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    )
}

private fun generateJpgFileName() =
    "JPEG_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())

private fun generateTFLiteFileName() =
    "TFLITE_${(SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date()))}.tflite"
