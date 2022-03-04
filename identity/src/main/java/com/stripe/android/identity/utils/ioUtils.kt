package com.stripe.android.identity.utils

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
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
 * TODO(ccen) resize the image
 */
internal fun resizeUriAndCreateFileToUpload(
    context: Context,
    originalUri: Uri,
    verificationId: String,
    isFullFrame: Boolean,
    side: String? = null
): File {
    context.contentResolver.openInputStream(originalUri).use { inputStream ->
        val nameBuilder = StringBuilder().also { nameBuilder ->
            nameBuilder.append(verificationId)
            side?.let {
                nameBuilder.append("_$side")
            }
            if (isFullFrame) {
                nameBuilder.append("_full_frame")
            }
            nameBuilder.append(".jpeg")
        }

        val fileToSave = File(
            context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            nameBuilder.toString()
        )

        FileOutputStream(fileToSave, false).use { fileOutputStream ->
            var read: Int
            val bytes = ByteArray(DEFAULT_BUFFER_SIZE)
            while (inputStream!!.read(bytes).also { read = it } != -1) {
                fileOutputStream.write(bytes, 0, read)
            }
        }
        return fileToSave
    }
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
