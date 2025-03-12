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
        enum class Known(override val value: String) : ContentType {
            Jpeg("image/jpeg"),
            Png("image/png"),
            Webp("image/webp")
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        data class Unknown(override val value: String) : ContentType
    }

    private companion object {
        fun from(value: String): ContentType {
            return ContentType.Known.entries.find {
                it.value == value
            } ?: ContentType.Unknown(value)
        }
    }
}
