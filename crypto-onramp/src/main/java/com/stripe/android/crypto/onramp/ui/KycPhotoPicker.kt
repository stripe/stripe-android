package com.stripe.android.crypto.onramp.ui

import android.webkit.MimeTypeMap
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import java.util.Locale

internal fun kycPhotoPickerMediaType(formats: List<String>): PickVisualMedia.VisualMediaType? {
    if (formats.isEmpty()) return PickVisualMedia.ImageOnly
    val imageTypes = formats.mapNotNull { format ->
        MimeTypeMap.getSingleton().getMimeTypeFromExtension(
            format.trim().removePrefix(".").lowercase(Locale.ROOT),
        )?.takeIf { it.startsWith("image/") }
    }.distinct()
    return when (imageTypes.size) {
        0 -> null
        1 -> PickVisualMedia.SingleMimeType(imageTypes.single())
        else -> PickVisualMedia.ImageOnly
    }
}
