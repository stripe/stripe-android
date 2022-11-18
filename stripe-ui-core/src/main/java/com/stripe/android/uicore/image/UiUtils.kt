package com.stripe.android.uicore.image

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.content.res.Resources
import android.content.res.Resources.NotFoundException
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.text.TextUtils
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.annotation.RestrictTo
import java.io.FileNotFoundException
import java.io.IOException

/**
 * Given an [Uri], find its [DrawableRes] id and [Resources] based on its authority(package name).
 *
 * This method is borrowed from [ContentResolver#getResourceId].
 * @see <a href="https://android.googlesource.com/platform/frameworks/base/+/9be54d400d68c735013bc8069fbcb66c3f98c3ee/core/java/android/content/ContentResolver.java#470">source</a>
 */
@SuppressLint("DiscouragedApi")
internal fun Context.getResourceId(uri: Uri): Pair<Resources, Int> {
    require(!TextUtils.isEmpty(uri.authority))
    val authority = requireNotNull(uri.authority)

    val r = packageManager.getResourcesForApplication(authority)

    val path: List<String> = uri.pathSegments
        ?: throw FileNotFoundException("No path: $uri")
    val id: Int = when (path.size) {
        1 -> {
            try {
                path[0].toInt()
            } catch (e: NumberFormatException) {
                throw FileNotFoundException("Single path segment is not a resource ID: $uri")
            }
        }
        2 -> {
            r.getIdentifier(path[1], path[0], authority)
        }
        else -> {
            throw FileNotFoundException("More than two path segments: $uri")
        }
    }
    if (id == 0) {
        throw FileNotFoundException("No resource found for: $uri")
    }
    return r to id
}

/**
 * Given a local [Uri] created from resource, content or local file, try to create a [Drawable] from it.
 *
 * This method is partially borrowed from [ImageView#getDrawableFromUri].
 * @see <a href="https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/widget/ImageView.java#1002">source</a>
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun Context.getDrawableFromUri(uri: Uri): Drawable? {
    val scheme = uri.scheme
    if (ContentResolver.SCHEME_ANDROID_RESOURCE == scheme) {
        try {
            // Load drawable through Resources, to get the source density information
            val (resource, id) = getResourceId(uri)
            return resource.getDrawable(id, theme)
        } catch (e: NotFoundException) {
            Log.e(TAG, "Unable to open content: $uri", e)
        }
    } else if (ContentResolver.SCHEME_CONTENT == scheme || ContentResolver.SCHEME_FILE == scheme) {
        try {
            contentResolver.openInputStream(uri).use {
                return Drawable.createFromStream(it, uri.toString())
            }
        } catch (e: IOException) {
            Log.w(TAG, "Unable to open content: $uri", e)
        }
    } else {
        return Drawable.createFromPath(uri.toString())
    }
    return null
}

internal fun String.isSupportedImageUrl() =
    ImageType.values().any { imageType ->
        imageType.suffixes.any { suffix ->
            endsWith(suffix, ignoreCase = true)
        }
    }

internal enum class ImageType(
    val suffixes: List<String>,
    val compressFormat: Bitmap.CompressFormat
) {
    PNG(
        suffixes = listOf("png"),
        compressFormat = Bitmap.CompressFormat.PNG
    ),
    WEBP(
        suffixes = listOf("webp"),
        compressFormat = Bitmap.CompressFormat.WEBP
    ),
    JPEG(
        suffixes = listOf("jpeg", "jpg"),
        compressFormat = Bitmap.CompressFormat.JPEG
    );

    companion object {
        fun fromUrl(url: String): ImageType? =
            values()
                .firstOrNull {
                    it.suffixes.any { suffix ->
                        url.endsWith(
                            suffix,
                            ignoreCase = true
                        )
                    }
                }
    }
}

private const val TAG = "stripe_ui_core_utils"
