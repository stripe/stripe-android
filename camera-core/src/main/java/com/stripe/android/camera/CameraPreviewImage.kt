package com.stripe.android.camera

import android.graphics.Rect
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class CameraPreviewImage<ImageType>(
    val image: ImageType,
    val viewBounds: Rect,
    val exposureIso: Float? = null,
    val focalLength: Float? = null,
    // Exposure duration in nanoseconds (from Camera2), if available
    val exposureDurationNs: Long? = null,
    // Whether this is a logical multi-camera (virtual camera) when known
    val isVirtualCamera: Boolean? = null,
)
