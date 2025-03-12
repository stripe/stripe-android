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

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        data object Jpeg : ContentType {
            override val value = "image/jpeg"
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        data object Png : ContentType {
            override val value = "image/png"
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        data object Webp : ContentType {
            override val value = "image/webp"
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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
