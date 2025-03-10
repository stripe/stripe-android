package com.stripe.android.uicore.image

import android.graphics.Bitmap
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class LoadedImage(
    val contentType: ContentType,
    val bitmap: Bitmap,
) {
    constructor(
        contentType: String,
        bitmap: Bitmap,
    ) : this(
        contentType = from(contentType),
        bitmap = bitmap,
    )

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    sealed interface ContentType {
        val value: String

        data object Jpeg : ContentType {
            override val value = "image/jpeg"
        }

        data object Png : ContentType {
            override val value = "image/png"
        }

        data object Webp : ContentType {
            override val value = "image/webp"
        }

        data class Unknown(override val value: String) : ContentType
    }

    private companion object {
        fun from(value: String) = when (value) {
            ContentType.Jpeg.value -> ContentType.Jpeg
            ContentType.Png.value -> ContentType.Png
            ContentType.Webp.value -> ContentType.Webp
            else -> ContentType.Unknown(value)
        }
    }
}
