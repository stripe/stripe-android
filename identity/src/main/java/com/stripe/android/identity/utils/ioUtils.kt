package com.stripe.android.identity.utils

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.util.Size
import androidx.core.content.FileProvider
import com.stripe.android.camera.framework.image.constrainToSize
import com.stripe.android.camera.framework.image.toJpeg
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
 * Read the image at uri, resize it with corresponding resolution and save it as a [File] with proper name.
 *
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
