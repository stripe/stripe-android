package com.stripe.android.identity.utils

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import java.io.File
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
